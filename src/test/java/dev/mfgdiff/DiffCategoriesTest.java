package dev.mfgdiff;

import static dev.mfgdiff.TestDefs.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mfgdiff.DiffResult.SegmentRow;
import dev.mfgdiff.DiffResult.Status;
import org.junit.jupiter.api.Test;

/** One or more tests per result category. */
class DiffCategoriesTest {

    @Test
    void identicalRevisionsAreAllSame() {
        DiffResult r = diff(baseline(), baseline());
        assertEquals(5, segments(r, Status.SAME));
        assertEquals(0, segments(r, Status.CHANGED) + segments(r, Status.ADDED) + segments(r, Status.REMOVED));
        assertEquals(0, moved(r));
        assertEquals(6, items(r, Status.SAME));
    }

    @Test
    void changedSegmentReportsFieldOldAndNew() {
        Definition b = baseline();
        b.segments.get(2).set("Duration", "PT9M");
        DiffResult r = diff(baseline(), b);
        SegmentRow row = rowB(r, 2);
        assertEquals(Status.CHANGED, row.status());
        assertEquals(1, row.changes().size());
        assertEquals("Duration", row.changes().get(0).field());
        assertEquals("PT5M", row.changes().get(0).oldValue());
        assertEquals("PT9M", row.changes().get(0).newValue());
        assertEquals(1, segments(r, Status.CHANGED));
    }

    @Test
    void changedItemReportsFieldOldAndNew() {
        Definition b = baseline();
        b.segments.get(2).children.get(0).set("Quantity", "8");
        DiffResult r = diff(baseline(), b);
        assertEquals(1, items(r, Status.CHANGED));
        var ch = allItems(r).stream().filter(x -> x.status() == Status.CHANGED).findFirst().orElseThrow().changes();
        assertEquals("Quantity", ch.get(0).field());
        assertEquals("3", ch.get(0).oldValue());
        assertEquals("8", ch.get(0).newValue());
        assertEquals(Status.SAME, rowB(r, 2).status(), "the segment's own fields did not change");
    }

    @Test
    void addedSegmentWithItsItems() {
        Definition b = baseline();
        b.segments.add(2, seg("OP-025", "Deburr edges", item("z1", "P-9", "Deburring wheel", "1")));
        for (int i = 0; i < b.segments.size(); i++) {
            b.segments.get(i).order = String.format("%04d", (i + 1) * 10);
        }
        DiffResult r = diff(baseline(), b);
        assertEquals(1, segments(r, Status.ADDED));
        assertEquals(1, items(r, Status.ADDED));
        assertEquals(5, segments(r, Status.SAME));
        assertEquals(0, moved(r));
    }

    @Test
    void removedSegmentWithItsItems() {
        Definition b = baseline();
        b.segments.remove(2);
        DiffResult r = diff(baseline(), b);
        assertEquals(1, segments(r, Status.REMOVED));
        assertEquals(2, items(r, Status.REMOVED));
        assertEquals(4, segments(r, Status.SAME));
        assertEquals(0, moved(r));
    }

    @Test
    void addedAndRemovedItems() {
        Definition b = baseline();
        b.segments.get(2).children.remove(1);
        b.segments.get(3).children.add(item("d2", "P-10", "Screw", "4"));
        DiffResult r = diff(baseline(), b);
        assertEquals(1, items(r, Status.REMOVED));
        assertEquals(1, items(r, Status.ADDED));
        assertEquals(0, items(r, Status.CHANGED));
    }

    @Test
    void movedSegmentIsReportedAsMovedOnly() {
        Definition b = baseline();
        Node n = b.segments.remove(1);
        b.segments.add(3, n); // OP-020 now after OP-040
        for (int i = 0; i < b.segments.size(); i++) {
            b.segments.get(i).order = String.format("%04d", (i + 1) * 10);
        }
        DiffResult r = diff(baseline(), b);
        assertEquals(1, moved(r));
        SegmentRow row = rowA(r, 1);
        assertTrue(row.moved());
        assertEquals(Status.SAME, row.status(), "content did not change");
        assertEquals(5, segments(r, Status.SAME));
    }

    @Test
    void movedAndChangedAtTheSameTime() {
        Definition b = baseline();
        Node n = b.segments.remove(1);
        n.set("Duration", "PT1M");
        b.segments.add(3, n);
        for (int i = 0; i < b.segments.size(); i++) {
            b.segments.get(i).order = String.format("%04d", (i + 1) * 10);
        }
        DiffResult r = diff(baseline(), b);
        SegmentRow row = rowA(r, 1);
        assertTrue(row.moved());
        assertEquals(Status.CHANGED, row.status());
        assertEquals("Duration", row.changes().get(0).field());
        assertEquals(1, moved(r));
    }

    @Test
    void renumberedSegmentIsPairedAndOnlyTheIdChanges() {
        Definition b = baseline();
        b.segments.get(3).set("ID", "OP-340");
        DiffResult r = diff(baseline(), b);
        SegmentRow row = rowB(r, 3);
        assertEquals(3, row.a());
        assertEquals(Status.CHANGED, row.status());
        assertEquals(1, row.changes().size());
        assertEquals("ID", row.changes().get(0).field());
        assertEquals("OP-040", row.changes().get(0).oldValue());
        assertEquals("OP-340", row.changes().get(0).newValue());
        assertEquals(0, segments(r, Status.ADDED) + segments(r, Status.REMOVED));
        assertEquals(0, items(r, Status.ADDED) + items(r, Status.REMOVED), "its items stay paired");
    }

    @Test
    void indiceDecidesTheOrderNotTheDocumentOrder() throws Exception {
        String xml = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("examples/revA.xml")), java.nio.charset.StandardCharsets.UTF_8);
        // swap the Indice values of OP-010 and OP-020 in the text; the document order stays the same
        xml = xml.replaceFirst("(<ID>OP-010</ID>[\\s\\S]*?<ValueString>)0010(</ValueString>)", "$10020$2");
        xml = xml.replaceFirst("(<ID>OP-020</ID>[\\s\\S]*?<ValueString>)0020(</ValueString>)", "$10010$2");
        Definition d = B2mmlReader.read(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("OP-020", d.segments.get(0).get("ID"));
        assertEquals("OP-010", d.segments.get(1).get("ID"));
    }

    @Test
    void childSharesDecideBetweenSegmentsWithAlmostIdenticalText() {
        // No segment ID is unique on both sides. The own text of all four segments is almost the same; only
        // the bill items tell which segment is which.
        Definition a = def(
                seg("S-1", "Assemble unit", item("a1", "P-1", "One", "1"), item("a2", "P-2", "Two", "1"), item("a3", "P-3", "Three", "1")),
                seg("S-2", "Assemble unit", item("b1", "P-7", "Seven", "1"), item("b2", "P-8", "Eight", "1"), item("b3", "P-9", "Nine", "1")));
        Definition b = def(
                seg("T-1", "Assemble unit", item("b1", "P-7", "Seven", "1"), item("b2", "P-8", "Eight", "1"), item("b3", "P-9", "Nine", "1")),
                seg("T-2", "Assemble unit", item("a1", "P-1", "One", "1"), item("a2", "P-2", "Two", "1"), item("a3", "P-3", "Three", "1")));
        DiffResult r = diff(a, b);
        assertEquals(0, segments(r, Status.ADDED) + segments(r, Status.REMOVED));
        assertEquals(6, items(r, Status.SAME));
        assertEquals(1, rowB(r, 0).a(), "T-1 holds the items of S-2");
        assertEquals(0, rowB(r, 1).a(), "T-2 holds the items of S-1");
        assertEquals(1, moved(r), "two pairs crossed: one of them is out of order");
    }

    @Test
    void theThresholdFDecidesWhetherSimilarSegmentsArePaired() {
        Definition a = def(seg("OP-1", "Weld frame", item("a1", "P-1", "Rod", "2")));
        Definition b = def(seg("QQ-9", "Weld frames", item("q1", "P-99", "qq", "9")));
        DiffResult paired = Differ.diff(a, b, new NodeMatcher(0.5));
        assertEquals(1, segments(paired, Status.CHANGED));
        assertEquals(0, segments(paired, Status.ADDED) + segments(paired, Status.REMOVED));
        DiffResult strict = Differ.diff(a, b, new NodeMatcher(0.99));
        assertEquals(1, segments(strict, Status.ADDED));
        assertEquals(1, segments(strict, Status.REMOVED));
        assertFalse(strict.segments().stream().anyMatch(x -> x.status() == Status.CHANGED));
    }
}
