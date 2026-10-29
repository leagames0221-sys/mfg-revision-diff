# Does the test suite notice a broken matcher?

On 2026-10-29 the matching code was broken in six places, one at a time (default `fast`, F = 0.7; 64 tests). After each change `mvn -o test` was run; the original code was put back each time (the working tree matched the commit again afterwards, and 64 tests passed). The tests are meant to fail every time.

| no. | change made | file and line (before the change) | tests that failed |
|---|---|---|---|
| M1 | the threshold F is ignored: both places that compare a score with `f` compare with `2.0`, so no pair is ever made by similarity | `NodeMatcher.java` L206 and L226 | `DiffCategoriesTest`: `childSharesDecideBetweenSegmentsWithAlmostIdenticalText`, `renumberedSegmentIsPairedAndOnlyTheIdChanges`, `theThresholdFDecidesWhetherSimilarSegmentsArePaired`; `ExamplesAndCliTest`: `anAddedStepThatLooksLikeARemovedOneIsNotPairedAtTheDefaultThreshold`, `cliPrintsOneTablePerCategory`, `sampleRevisionsContainEveryCategory`; `GeneratorTest`: `segmentRenumber` (7 failed) |
| M2 | the segment ID rule no longer requires "exactly once on each side": the first of each side is paired | `NodeMatcher.java` L94 | `OssFailureScenariosTest`: `odooPr91494_duplicateOperationIdsInAnotherOrderArePairedByContentNotByIdOrder` (1 failed) |
| M3 | the share of matched bill rows is dropped from the similarity (only the step's own text counts) | `NodeMatcher.java` L158 | `DiffCategoriesTest`: `childSharesDecideBetweenSegmentsWithAlmostIdenticalText`; `ExamplesAndCliTest`: `anAddedStepThatLooksLikeARemovedOneIsNotPairedAtTheDefaultThreshold`, `cliFilterShowsOnlyTheRequestedCategory`, `sampleRevisionsContainEveryCategory` (4 failed) |
| M4 | moved detection is switched off: every paired step counts as being in order | `Differ.java` L45 | `DiffCategoriesTest`: `childSharesDecideBetweenSegmentsWithAlmostIdenticalText`, `movedAndChangedAtTheSameTime`, `movedSegmentIsReportedAsMovedOnly`; `ExamplesAndCliTest`: `sampleRevisionsContainEveryCategory`; `GeneratorTest`: `segmentMove`; `OssFailureScenariosTest`: `erpnextPr60755_reorderedUnchangedProcessIsMovedNotChanged`, `odooPr91494_duplicateOperationIdsInAnotherOrderArePairedByContentNotByIdOrder` (7 failed) |
| M5 | rows of the same part are no longer paired "identical content first"; they are only paired in order of appearance | `NodeMatcher.java` L123 | `ExamplesAndCliTest`: `sampleRevisionsContainEveryCategory`; `GeneratorTest`: `itemInsertDuplicateRow`; `OssFailureScenariosTest`: `erpnextPr60755_reorderingDuplicateRowsProducesNoFalseChange`, `erpnextPr60755_sameRowTwice_removingOneRowIsReported` (4 failed) |
| M6 | new ratio code: in `AccurateRatio` the part of the sequences to the left of the longest matching block is no longer searched | `AccurateRatio.java` L110 | `QuickRatioAndLcsTest`: `accurateRatioMatchesThePythonDocumentationExamples` (1 failed) |

M2 is caught only by `odooPr91494_duplicateOperationIdsInAnotherOrderArePairedByContentNotByIdOrder` (two steps with the same ID that swap places).

To repeat one by hand: make the change in the file named, run `mvn -o test`, then put the original file back from the commit.

## Second set: the screen and the Excel file

On 2026-10-29 the code of the screen and the Excel file was broken in three places, one at a time (98 tests: the 64 above plus 34). After each change `mvn -o test` was run and the original file was put back (afterwards the working tree matched the commit and 98 tests passed). The tests are meant to fail every time.

| no. | change made | file and line (before the change) | tests that failed |
|---|---|---|---|
| E1 | output encoding: `<` is no longer converted to `&lt;` | `web/Html.java` L23 | `ServerTest.textFromTheFilesIsEscapedInTheResultPage`, `SmallPartsTest.escapesTheFiveCharactersOfTheOwaspTable` (2 failed) |
| O1 | origin check: a request that has an `Origin` header is always accepted | `web/OriginGuard.java` L40 | `ServerTest.aPostFromAnotherOriginIsRefused`, `SmallPartsTest.theOriginIsComparedAsAWholeString`, `SmallPartsTest.theRefererIsUsedOnlyWithoutOriginAndOnlyByItsOrigin` (3 failed) |
| X1 | Excel colour: a changed row gets no fill colour | `XlsxExport.java` L118 | `XlsxExportTest.changedRowsArePaleYellowAndChangedCellsYellow` (1 failed) |

What the table does not show: with E1 the test `ServerTest.anErrorMessageThatContainsFileTextIsEscaped` still passed, because the XML parser's message for that input names only the element `a` and the expected end tag, and the test checks only that the script text of the file is not echoed; the two tests above are the ones that catch the change. The screen's previous / next buttons (`app.js`) have no automated test; they were checked in Microsoft Edge (README, "How it was checked").
