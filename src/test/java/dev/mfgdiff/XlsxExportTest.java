package dev.mfgdiff;

import static dev.mfgdiff.TestDefs.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class XlsxExportTest {

    private static XSSFWorkbook exampleWorkbook() throws Exception {
        DiffResult r = Differ.diff(B2mmlReader.read(Path.of("examples/revA.xml")), B2mmlReader.read(Path.of("examples/revB.xml")));
        return open(XlsxExport.write(r, "revA.xml", "revB.xml", 0.7, RatioMode.FAST));
    }

    private static XSSFWorkbook open(byte[] data) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(data));
    }

    private static String fill(Cell c) {
        XSSFCellStyle st = (XSSFCellStyle) c.getCellStyle();
        XSSFColor col = st.getFillForegroundColorColor();
        if (col == null || st.getFillPattern() != org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND) {
            return null;
        }
        byte[] rgb = col.getRGB();
        return String.format("%02X%02X%02X", rgb[0] & 255, rgb[1] & 255, rgb[2] & 255);
    }

    private static Row rowWith(Sheet sh, int col, String text) {
        for (int i = 1; i <= sh.getLastRowNum(); i++) {
            Row row = sh.getRow(i);
            Cell c = row == null ? null : row.getCell(col);
            if (c != null && c.getCellType() == CellType.STRING && c.getStringCellValue().equals(text)) {
                return row;
            }
        }
        throw new AssertionError("no row with " + text + " in column " + col);
    }

    @Test
    void hasThreeSheetsWithOneRowPerStepAndPerBillRow() throws Exception {
        try (XSSFWorkbook wb = exampleWorkbook()) {
            assertEquals(3, wb.getNumberOfSheets());
            assertEquals("まとめ", wb.getSheetName(0));
            assertEquals("工程", wb.getSheetName(1));
            assertEquals("部品", wb.getSheetName(2));
            assertEquals(13, wb.getSheet("工程").getLastRowNum(), "12 steps of revision B plus the removed one");
            assertEquals(25, wb.getSheet("部品").getLastRowNum());
        }
    }

    @Test
    void theCategoryColumnMatchesTheComparison() throws Exception {
        try (XSSFWorkbook wb = exampleWorkbook()) {
            Sheet steps = wb.getSheet("工程");
            int same = 0, changed = 0, added = 0, removed = 0, moved = 0;
            for (int i = 1; i <= steps.getLastRowNum(); i++) {
                switch (steps.getRow(i).getCell(0).getStringCellValue()) {
                    case "同じ" -> same++;
                    case "変更" -> changed++;
                    case "追加" -> added++;
                    case "削除" -> removed++;
                    default -> throw new AssertionError();
                }
                if (steps.getRow(i).getCell(1).getStringCellValue().equals("↕")) {
                    moved++;
                }
            }
            assertEquals(8, same);
            assertEquals(3, changed);
            assertEquals(1, added);
            assertEquals(1, removed);
            assertEquals(1, moved);
            Row op80 = rowWith(steps, 4, "OP-080");
            assertEquals("変更", op80.getCell(0).getStringCellValue());
            assertEquals("↕", op80.getCell(1).getStringCellValue());
            assertEquals(8, (int) op80.getCell(2).getNumericCellValue(), "A# is written as a number");
            assertEquals(5, (int) op80.getCell(3).getNumericCellValue());

            Sheet parts = wb.getSheet("部品");
            long addedParts = 0;
            for (int i = 1; i <= parts.getLastRowNum(); i++) {
                if (parts.getRow(i).getCell(0).getStringCellValue().equals("追加")) {
                    addedParts++;
                }
            }
            assertEquals(3, addedParts);
        }
    }

    @Test
    void changedRowsArePaleYellowAndChangedCellsYellow() throws Exception {
        try (XSSFWorkbook wb = exampleWorkbook()) {
            Sheet steps = wb.getSheet("工程");
            Row op30 = rowWith(steps, 4, "OP-030"); // Duration changed
            assertEquals("FFFFE0", fill(op30.getCell(0)), "changed row");
            assertEquals("FFFFE0", fill(op30.getCell(4)), "an unchanged cell of a changed row keeps the row colour");
            assertEquals("FFFF00", fill(op30.getCell(7)), "the cell that says what changed");
            Row renumbered = rowWith(steps, 4, "OP-120");
            assertEquals("FFFF00", fill(renumbered.getCell(4)), "A id changed");
            assertEquals("FFFF00", fill(renumbered.getCell(5)), "B id changed");
            assertEquals("FFFFE0", fill(renumbered.getCell(0)));
            Row op80 = rowWith(steps, 4, "OP-080"); // Description changed
            assertEquals("FFFF00", fill(op80.getCell(6)), "name cell of a changed description");
            Row op10 = rowWith(steps, 4, "OP-010");
            assertEquals(null, fill(op10.getCell(0)), "a same row has no colour");
            assertEquals(XlsxExport.COLOR_ADDED_ROW, fill(rowWith(steps, 5, "OP-065").getCell(0)));
            assertEquals(XlsxExport.COLOR_REMOVED_ROW, fill(rowWith(steps, 4, "OP-070").getCell(0)));
            Sheet parts = wb.getSheet("部品");
            Row qty = rowWith(parts, 6, "P-1003");
            assertEquals("FFFFE0", fill(qty.getCell(0)));
            assertEquals("FFFF00", fill(qty.getCell(8)));
        }
    }

    @Test
    void theHeaderRowIsFrozenAndHasFilterArrows() throws Exception {
        try (XSSFWorkbook wb = exampleWorkbook()) {
            XSSFSheet steps = wb.getSheet("工程");
            assertEquals(1, steps.getPaneInformation().getHorizontalSplitTopRow());
            assertTrue(steps.getCTWorksheet().isSetAutoFilter());
            assertEquals("A1:H1", steps.getCTWorksheet().getAutoFilter().getRef());
            XSSFSheet parts = wb.getSheet("部品");
            assertEquals(1, parts.getPaneInformation().getHorizontalSplitTopRow());
            assertEquals("A1:I1", parts.getCTWorksheet().getAutoFilter().getRef());
        }
    }

    @Test
    void theSummaryCountsAreNumbers() throws Exception {
        try (XSSFWorkbook wb = exampleWorkbook()) {
            Sheet s = wb.getSheet("まとめ");
            Row changed = rowWith(s, 0, "変更");
            assertEquals(CellType.NUMERIC, changed.getCell(2).getCellType());
            assertEquals(3, (int) changed.getCell(2).getNumericCellValue());
            assertEquals(2, (int) changed.getCell(3).getNumericCellValue());
            assertEquals(1, (int) rowWith(s, 0, "入れ替え").getCell(2).getNumericCellValue());
            assertEquals("revA.xml", rowWith(s, 0, "版 A").getCell(1).getStringCellValue());
            assertEquals(1, rowsWith(s, 0, "版 A"), "the samples have no element that is not compared");
        }
    }

    @Test
    void elementsThatAreNotComparedAreListedOnTheSummarySheet() throws Exception {
        String a = B2mmlWriter.write(B2mmlReader.read(Path.of("examples/revA.xml")), java.util.List.of());
        String b = a.replaceFirst("<Duration>PT3M</Duration>",
                "<Duration>PT3M</Duration><HierarchyScope><EquipmentID>E-1</EquipmentID></HierarchyScope>");
        assertNotEquals(a, b, "the extra element was inserted");
        DiffResult r = Differ.diff(B2mmlReader.read(a.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                B2mmlReader.read(b.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        try (XSSFWorkbook wb = open(XlsxExport.write(r, "a.xml", "b.xml", 0.7, RatioMode.FAST))) {
            Sheet s = wb.getSheet("まとめ");
            assertEquals(2, rowsWith(s, 0, "版 B"), "the file row and one warning row");
            assertEquals(1, rowsWith(s, 0, "版 A"));
            rowWith(s, 1, "OperationsSegment/HierarchyScope");
        }
    }

    private static int rowsWith(Sheet sh, int col, String text) {
        int n = 0;
        for (int i = 0; i <= sh.getLastRowNum(); i++) {
            Row row = sh.getRow(i);
            Cell c = row == null ? null : row.getCell(col);
            if (c != null && c.getCellType() == CellType.STRING && c.getStringCellValue().equals(text)) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------ text that looks like a formula

    @Test
    void textThatLooksLikeAFormulaIsStoredAsTextAndMarkedAsText() throws Exception {
        String[] names = {"=1+1", "+1+1", "-1+1", "@SUM(1,1)", "\t=1+1", "=HYPERLINK(\"http://evil.example\",\"x\")", "＝1+1", "＋1", "－1", "＠SUM(1)"};
        for (String name : names) {
            Definition a = def(seg("OP-010", "Cut plate", item("a1", "P-1", "Steel", "1")));
            Definition b = def(seg("OP-010", name, item("a1", "P-1", name, "1")));
            try (XSSFWorkbook wb = open(XlsxExport.write(Differ.diff(a, b), name, name, 0.7, RatioMode.FAST))) {
                for (Sheet sh : wb) {
                    for (Row row : sh) {
                        for (Cell c : row) {
                            assertNotEquals(CellType.FORMULA, c.getCellType(), sh.getSheetName() + " " + c.getAddress());
                        }
                    }
                }
                Cell step = wb.getSheet("工程").getRow(1).getCell(6);
                assertEquals(CellType.STRING, step.getCellType());
                assertEquals(name, step.getStringCellValue(), "the text is kept as it was");
                assertTrue(((XSSFCellStyle) step.getCellStyle()).getQuotePrefixed(), "marked as text: " + name);
                Cell part = wb.getSheet("部品").getRow(1).getCell(7);
                assertEquals(name, part.getStringCellValue());
                assertTrue(((XSSFCellStyle) part.getCellStyle()).getQuotePrefixed());
                // a cell that does not start with such a character is not marked
                assertFalse(((XSSFCellStyle) wb.getSheet("工程").getRow(1).getCell(4).getCellStyle()).getQuotePrefixed());
                // no formula element in the sheet XML either
                assertFalse(wb.getSheet("工程").getCTWorksheet().xmlText().matches("(?s).*<(\\w+:)?f[ >/].*"));
            }
        }
    }

    @Test
    void theFormulaTriggerCharactersAreTheOnesTheOwaspPageLists() {
        for (String s : new String[] {"=a", "+a", "-a", "@a", "\ta", "\ra", "\na", "＝a", "＋a", "－a", "＠a"}) {
            assertTrue(XlsxExport.startsLikeFormula(s), s);
        }
        for (String s : new String[] {"a=", "a+", " =a", "OP-010", "", "1", "（=）"}) {
            assertFalse(XlsxExport.startsLikeFormula(s), s);
        }
        assertFalse(XlsxExport.startsLikeFormula(null));
    }

    @Test
    void aVeryLongTextIsCutToTheLimitOfACell() throws Exception {
        String longText = "x".repeat(40000);
        Definition a = def(seg("OP-010", "Cut plate"));
        Definition b = def(seg("OP-010", longText));
        try (XSSFWorkbook wb = open(XlsxExport.write(Differ.diff(a, b), "a", "b", 0.7, RatioMode.FAST))) {
            assertEquals(32767, wb.getSheet("工程").getRow(1).getCell(6).getStringCellValue().length());
        }
    }
}
