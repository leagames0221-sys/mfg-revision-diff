package dev.mfgdiff;

import static dev.mfgdiff.TestDefs.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mfgdiff.DiffResult.Status;
import org.junit.jupiter.api.Test;

/**
 * Situations in which other open source bill-of-material comparisons were reported to fail. Each test
 * name names the report it is taken from; the source lines are listed in docs/sources.md.
 */
class OssFailureScenariosTest {

    private static Definition withBill(Node... items) {
        return def(seg("OP-010", "Assemble housing", items));
    }

    // ERPNext PR #60755: the old code kept only the last row per key, so extra rows of the same part hid
    // additions and removals.
    @Test
    void erpnextPr60755_sameRowTwice_removingOneRowIsReported() {
        Definition a = withBill(item("L1", "P-1", "Screw", "2"), item("L2", "P-1", "Screw", "5"));
        Definition b = withBill(item("L2", "P-1", "Screw", "5"));
        DiffResult r = diff(a, b);
        assertEquals(1, items(r, Status.REMOVED));
        assertEquals(1, items(r, Status.SAME));
        assertEquals(0, items(r, Status.CHANGED) + items(r, Status.ADDED));
    }

    @Test
    void erpnextPr60755_sameRowTwice_addingAThirdRowIsReported() {
        Definition a = withBill(item("L1", "P-1", "Screw", "2"), item("L2", "P-1", "Screw", "5"));
        Definition b = withBill(item("L1", "P-1", "Screw", "2"), item("L2", "P-1", "Screw", "5"), item("L3", "P-1", "Screw", "9"));
        DiffResult r = diff(a, b);
        assertEquals(1, items(r, Status.ADDED));
        assertEquals(2, items(r, Status.SAME));
        assertEquals(0, items(r, Status.CHANGED) + items(r, Status.REMOVED));
    }

    @Test
    void erpnextPr60755_sameRowTwice_eachChangedOccurrenceIsReportedSeparately() {
        Definition a = withBill(item("L1", "P-1", "Screw", "2"), item("L2", "P-1", "Screw", "5"));
        Definition b = withBill(item("L1", "P-1", "Screw", "3"), item("L2", "P-1", "Screw", "6"));
        DiffResult r = diff(a, b);
        assertEquals(2, items(r, Status.CHANGED));
        assertEquals(0, items(r, Status.SAME) + items(r, Status.ADDED) + items(r, Status.REMOVED));
    }

    // ERPNext PR #60755 expectation: "Reordering otherwise unchanged rows should not produce false changes."
    @Test
    void erpnextPr60755_reorderingDuplicateRowsProducesNoFalseChange() {
        Definition a = withBill(item("L1", "P-1", "Screw", "2"), item("L2", "P-1", "Screw", "5"), item("L3", "P-2", "Nut", "7"));
        Definition b = withBill(item("L3", "P-2", "Nut", "7"), item("L2", "P-1", "Screw", "5"), item("L1", "P-1", "Screw", "2"));
        DiffResult r = diff(a, b);
        assertEquals(3, items(r, Status.SAME));
        assertEquals(0, items(r, Status.CHANGED) + items(r, Status.ADDED) + items(r, Status.REMOVED));
    }

    // Same ERPNext report (process rows): an unchanged process that only changed position is not a change.
    // This tool reports it in its own category instead.
    @Test
    void erpnextPr60755_reorderedUnchangedProcessIsMovedNotChanged() {
        Definition a = baseline();
        Definition b = baseline();
        Node n = b.segments.remove(0);
        b.segments.add(3, n);
        for (int i = 0; i < b.segments.size(); i++) {
            b.segments.get(i).order = String.format("%04d", (i + 1) * 10);
        }
        DiffResult r = diff(a, b);
        assertEquals(0, segments(r, Status.CHANGED));
        assertEquals(5, segments(r, Status.SAME));
        assertEquals(1, moved(r));
        assertEquals(true, rowA(r, 0).moved());
    }

    // Odoo PR #91494: duplicated operations got new identifiers and a comparison on (product, quantity,
    // operation) produced false changes; several lines of the same product could not be told apart.
    @Test
    void odooPr91494_duplicateOperationIdsProduceNoFalseChange() {
        Definition a = def(
                seg("OP-020", "Weld frame", item("a1", "P-1", "Rod", "2")),
                seg("OP-020", "Grind frame", item("b1", "P-1", "Rod", "2")),
                seg("OP-030", "Paint frame", item("c1", "P-3", "Powder", "1")));
        Definition b = def(
                seg("OP-020", "Weld frame", item("a1", "P-1", "Rod", "2")),
                seg("OP-020", "Grind frame", item("b1", "P-1", "Rod", "2")),
                seg("OP-030", "Paint frame", item("c1", "P-3", "Powder", "1")));
        DiffResult r = diff(a, b);
        assertEquals(3, segments(r, Status.SAME));
        assertEquals(3, items(r, Status.SAME));
        assertEquals(0, segments(r, Status.CHANGED) + segments(r, Status.ADDED) + segments(r, Status.REMOVED));
        assertEquals(0, moved(r));
        assertEquals(0, rowB(r, 0).a());
        assertEquals(1, rowB(r, 1).a());
    }

    @Test
    void odooPr91494_duplicateOperationIdsWithOneChangedStillPairByContent() {
        Definition a = def(
                seg("OP-020", "Weld frame", item("a1", "P-1", "Rod", "2")),
                seg("OP-020", "Grind frame", item("b1", "P-1", "Rod", "2")));
        Definition b = def(
                seg("OP-020", "Weld frame", item("a1", "P-1", "Rod", "2")),
                seg("OP-020", "Grind frame", item("b1", "P-1", "Rod", "4")));
        DiffResult r = diff(a, b);
        assertEquals(0, segments(r, Status.ADDED) + segments(r, Status.REMOVED));
        assertEquals(1, items(r, Status.CHANGED));
        assertEquals(1, items(r, Status.SAME));
    }

    @Test
    void odooPr91494_duplicateOperationIdsInAnotherOrderArePairedByContentNotByIdOrder() {
        Definition a = def(
                seg("OP-020", "Weld frame", item("a1", "P-1", "Rod", "2")),
                seg("OP-020", "Grind frame", item("b1", "P-1", "Rod", "3")),
                seg("OP-030", "Paint frame", item("c1", "P-3", "Powder", "1")),
                seg("OP-040", "Pack frame", item("d1", "P-4", "Box", "1")));
        Definition b = def(
                seg("OP-020", "Grind frame", item("b1", "P-1", "Rod", "3")),
                seg("OP-030", "Paint frame", item("c1", "P-3", "Powder", "1")),
                seg("OP-040", "Pack frame", item("d1", "P-4", "Box", "1")),
                seg("OP-020", "Weld frame", item("a1", "P-1", "Rod", "2")));
        DiffResult r = diff(a, b);
        assertEquals(4, segments(r, Status.SAME));
        assertEquals(0, segments(r, Status.CHANGED) + segments(r, Status.ADDED) + segments(r, Status.REMOVED));
        assertEquals(4, items(r, Status.SAME));
        assertEquals(1, moved(r), "only the Weld segment changed place");
        assertEquals(0, rowB(r, 3).a());
    }

    // InvenTree issue #12445: a check value computed over the rows in order differed after a pure reorder.
    @Test
    void inventreeIssue12445_reorderedBillRowsAreSame() {
        Definition a = withBill(item("L1", "P-1", "Screw", "2"), item("L2", "P-2", "Nut", "4"), item("L3", "P-3", "Washer", "8"));
        Definition b = withBill(item("L3", "P-3", "Washer", "8"), item("L1", "P-1", "Screw", "2"), item("L2", "P-2", "Nut", "4"));
        DiffResult r = diff(a, b);
        assertEquals(3, items(r, Status.SAME));
        assertEquals(1, segments(r, Status.SAME));
        assertEquals(0, items(r, Status.CHANGED) + items(r, Status.ADDED) + items(r, Status.REMOVED));
    }
}
