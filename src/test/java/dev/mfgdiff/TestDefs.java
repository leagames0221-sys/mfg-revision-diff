package dev.mfgdiff;

import dev.mfgdiff.DiffResult.ItemRow;
import dev.mfgdiff.DiffResult.SegmentRow;
import dev.mfgdiff.DiffResult.Status;
import java.util.ArrayList;
import java.util.List;

/** Small builders and counters shared by the tests. */
final class TestDefs {
    private TestDefs() {
    }

    static Node item(String lineId, String part, String desc, String qty) {
        Node it = new Node(Node.Kind.ITEM);
        it.set("ID", lineId);
        it.set("Description", desc);
        it.set("MaterialSpecificationID", part);
        it.set("Quantity", qty);
        it.set("Quantity/UnitOfMeasure", "EA");
        return it;
    }

    static Node seg(String id, String desc, Node... items) {
        Node s = new Node(Node.Kind.SEGMENT);
        s.set("ID", id);
        s.set("Description", desc);
        s.set("Duration", "PT5M");
        s.set("OperationsMaterialBillID", "MB-" + id);
        for (Node it : items) {
            s.children.add(it);
        }
        return s;
    }

    static Definition def(Node... segs) {
        Definition d = new Definition();
        int i = 0;
        for (Node s : segs) {
            s.order = String.format("%04d", ++i * 10);
            d.segments.add(s);
        }
        return d;
    }

    static DiffResult diff(Definition a, Definition b) {
        return Differ.diff(a, b);
    }

    static long segments(DiffResult r, Status s) {
        return r.segments().stream().filter(x -> x.status() == s).count();
    }

    static long items(DiffResult r, Status s) {
        return r.segments().stream().flatMap(x -> x.items().stream()).filter(x -> x.status() == s).count();
    }

    static long moved(DiffResult r) {
        return r.segments().stream().filter(SegmentRow::moved).count();
    }

    /** The row for the B-side segment at position j. */
    static SegmentRow rowB(DiffResult r, int j) {
        return r.segments().stream().filter(x -> x.b() == j).findFirst().orElseThrow();
    }

    static SegmentRow rowA(DiffResult r, int i) {
        return r.segments().stream().filter(x -> x.a() == i).findFirst().orElseThrow();
    }

    static List<ItemRow> allItems(DiffResult r) {
        List<ItemRow> out = new ArrayList<>();
        for (SegmentRow s : r.segments()) {
            out.addAll(s.items());
        }
        return out;
    }

    /** Segments in a realistic sequence, all different enough to be told apart by their text. */
    static Definition baseline() {
        return def(
                seg("OP-010", "Cut base plate blanks", item("a1", "P-1", "Steel sheet", "1")),
                seg("OP-020", "Press base plate", item("b1", "P-2", "Press oil", "2")),
                seg("OP-030", "Paint base plate", item("c1", "P-3", "Powder coat", "3"), item("c2", "P-4", "Primer", "1")),
                seg("OP-040", "Mount rubber feet", item("d1", "P-5", "Rubber foot", "4")),
                seg("OP-050", "Wind motor stator", item("e1", "P-6", "Copper wire", "5")));
    }
}
