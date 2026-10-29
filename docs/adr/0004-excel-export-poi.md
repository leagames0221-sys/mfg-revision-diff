# ADR 0004: Write the Excel file with Apache POI, and pin log4j-api

Status: accepted

Source entries (S...), author's decisions (D...) and searches without result (N...) refer to [../sources.md](../sources.md).

## Context

The result of a comparison has to leave the screen as a file that people open in Excel, with the rows coloured the way the comparison products colour them. Other products do the same: OpenBOM writes its change report as an Excel file (S11.10d), Windchill exports the comparison list to a file (S11.10b), Oracle Fusion has an Excel output (sources.md S4.7).

Facts that shape the choice:

- **Text in the file comes from the input files.** A step called `=1+1` or `@SUM(...)` is the kind of text that a spreadsheet may read as a formula. The OWASP CSV Injection page says: "is used to open a CSV, any cells starting with `=` will be interpreted" and lists the characters `=`, `+`, `-`, `@`, tab, carriage return, line feed and, for example in Japanese environments, their full-width forms (S11.5).
- **The page is about CSV.** It does not say what happens in a cell of an .xlsx file (N9). In an .xlsx file every cell has a type; a text cell is stored as text.
- **Excel has a mark for text that must stay text**: the quote prefix, which POI sets on a cell style, "used to tell Excel that the thing which looks like" a number or a formula "shouldn't be treated as" one, and which is "like prefixing the cell value with a ' in Excel" (S11.6).
- **One new library is used**, Apache POI. POI is "the Java API for Microsoft Documents" (S11.12).
- **POI 5.5.1 brings log4j-api 2.24.3.** Its pom imports the log4j bill of materials 2.24.3 (`poi-5.5.1.pom` L56-L58 in the local Maven repository). OSV lists GHSA-qv9r-c865-cp47 (CVE-2026-49844) for log4j-api 2.24.3 and a fix in 2.25.5 (S11.13, S11.13b). The advisory title is "Improper encoding of non-finite floating-point values during MapMessage JSON serialization" (S11.13); this project does not use it, but a dependency check of the repository would report it.
- **POI writes nothing to a log that matters here, but Log4j complains.** With only log4j-api on the class path, writing a file printed `main ERROR Log4j API could not find a logging provider.` (S11.9; the line appeared when the screen wrote an Excel file).

## Decision

We will write the file with POI's `XSSFWorkbook` (`XlsxExport.java` L129 `write`) and pin `log4j-api` to 2.25.5.

- **Library.** `poi-ooxml` 5.5.1 is the only new dependency (`pom.xml` L30-L34). POI 6.0.0 was released on 22 October 2026 (S11.12); 5.5.1 is the version that was checked (S11.13b) and is kept (D25).
- **Pin.** `dependencyManagement` sets `log4j-api` to 2.25.5 (`pom.xml` L18-L27). OSV reports no vulnerability for 2.25.5 and for poi-ooxml 5.5.1 and each of its 12 compile dependencies (S11.13b: 13 libraries, 0 findings; the one finding is log4j-api 2.24.3). 2.25.5 is the smallest version that the advisory lists as fixed; 2.26.1 is also listed (D25). The test suite and the screen run against 2.25.5.
- **Text is always text.** `XlsxExport.text` (L91) writes every string with `setCellValue(String)`, which makes a string cell, never a formula. When the first character is one of the OWASP list, the cell also gets the quote-prefix mark (`startsLikeFormula` L48, `Styles.get` L79), so that editing the cell in Excel does not turn it into a formula. The stored text is not changed: no apostrophe or tab is added (D23). The OWASP page offers a prefix for CSV; for .xlsx the quote-prefix mark does the same without changing the data.
- **Colours** follow aras-bom-compare: changed row `lightyellow`, changed cell `yellow` (S11.7; `XlsxExport.java` L30-L31, `rowFill` L116); added, removed and moved-only rows have pale green, pink and blue (D18).
- **Sheets** are the summary (まとめ), the steps (工程) and the bill rows (部品) (D21). The header row of the two data sheets is frozen and has filter arrows (`XlsxExport.java` L149-L150), which are standard Excel features (S11.8, S11.8b, S11.8c).
- **Log4j.** `src/main/resources/log4j2.component.properties` selects the Simple Logger that is part of log4j-api (S11.9). It removes the line without a new library (D24). `ProjectFilesTest` checks that the file is on the class path.

## Alternatives considered

| candidate | what it is | why not |
|---|---|---|
| **CSV file** | text, opens in Excel | No colours, no frozen header, no filter arrows, no sheets. It is the format the OWASP page warns about: "There is no universal CSV sanitization strategy that is safe for all" spreadsheet applications and downstream consumers (S11.5); and its quoting advice carries the note "The above techniques are not reliable in Microsoft Excel after" saving and re-opening the file (S11.5). |
| **Write the OOXML by hand** (zip, `[Content_Types].xml`, workbook, styles, shared strings, sheet XML) | no library | No new dependency, but the project would own the file format: styles, string escaping, the zip container and the limits of a cell. A mistake produces a file that Excel repairs or refuses. Not tried (N11). POI exists for this. |
| **Apache POI** | S11.12 | **Chosen.** One library, an API for styles, freeze panes, filters and the quote-prefix mark. Cost: 13 jars (S11.13b) and a pin. |
| **POI 6.0.0** | the newest release (S11.12) | Not checked against OSV here and not in the local Maven repository (D25). |

## Consequences

- Positive: coloured, filterable, three-sheet files that open in Excel. The tests read the written file back with POI and check the sheets, the category of every row, the colours, the frozen header, the filter range, and that the cells for `=1+1`, `+1+1`, `-1+1`, `@SUM(1,1)`, a tab, a full-width `＝` and others are string cells with the original text (`XlsxExportTest`).
- Positive: the dependency check of the repository reports nothing for the 13 libraries (S11.13b). The pin has to be kept until POI's own pom moves past 2.24.3.
- Negative: 13 jars (poi-ooxml, poi, commons-codec, commons-math3, SparseBitSet, poi-ooxml-lite, xmlbeans, commons-compress, commons-lang3, commons-io, curvesapi, log4j-api, commons-collections4) instead of none. Their licences are listed in `NOTICE`.
- Negative: the quote-prefix mark and string cells protect the file as written. If a person saves the sheet as CSV from Excel, the text is written without protection; that is outside this tool (the OWASP page says the CSV mitigations are not reliable in Excel, S11.5).
- Negative: the effect of a string cell and of the quote-prefix mark on every spreadsheet program is not tested; only Excel is described by the sources. The tests read the file back with POI.
- Negative: the Simple Logger prints Log4j's own errors (level ERROR) to the console; there is no log file. POI logs little during a write.
- Negative: text over 32767 characters in one cell is cut (D21); in the tests of this project no input is that long.
