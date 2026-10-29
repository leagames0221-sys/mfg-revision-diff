package dev.mfgdiff;

import dev.mfgdiff.DiffResult.FieldChange;
import dev.mfgdiff.DiffResult.ItemRow;
import dev.mfgdiff.DiffResult.SegmentRow;
import dev.mfgdiff.DiffResult.Status;
import java.util.ArrayList;
import java.util.List;

/**
 * The rows of a comparison as the screen and the Excel file show them. One list for the steps and one for
 * the bill rows, with the columns of the text report in {@link Report}: A#, B#, A id, B id, name, what
 * changed (old to new). The screen and the Excel writer both read these rows, so they cannot disagree.
 */
public final class RowModel {
    private RowModel() {
    }

    /**
     * One table row. {@code a} / {@code b} are 1-based positions, 0 when the object is absent on that side.
     * {@code idA} / {@code idB} are the step IDs (steps) or the bill row IDs (items), "" when absent.
     * {@code segment} and {@code part} are only set for bill rows.
     */
    public record Row(Status status, boolean moved, int a, int b, String segment, String idA, String idB,
            String part, String name, List<FieldChange> changes) {

        /** True when the row is something other than "same and in place". */
        public boolean isDifference() {
            return status != Status.SAME || moved;
        }

        /** Symbol of the SAP routing comparison: = (same), not-equal (changed), A (only in A), B (only in B). */
        public String symbol() {
            return switch (status) {
                case SAME -> "=";
                case CHANGED -> "≠";
                case REMOVED -> "A";
                case ADDED -> "B";
            };
        }

        public boolean changed(String field) {
            for (FieldChange c : changes) {
                if (c.field().equals(field)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static int pos(int i) {
        return i + 1;
    }

    private static String idOf(Node n, String field) {
        return n == null ? "" : n.get(field);
    }

    public static List<Row> segmentRows(DiffResult r) {
        List<Row> rows = new ArrayList<>();
        for (SegmentRow s : r.segments()) {
            Node na = s.a() >= 0 ? r.a().segments.get(s.a()) : null;
            Node nb = s.b() >= 0 ? r.b().segments.get(s.b()) : null;
            Node shown = nb != null ? nb : na;
            rows.add(new Row(s.status(), s.moved(), pos(s.a()), pos(s.b()), "", idOf(na, "ID"), idOf(nb, "ID"), "",
                    shown.get("Description"), s.changes()));
        }
        return rows;
    }

    public static List<Row> itemRows(DiffResult r) {
        List<Row> rows = new ArrayList<>();
        for (SegmentRow s : r.segments()) {
            Node sa = s.a() >= 0 ? r.a().segments.get(s.a()) : null;
            Node sb = s.b() >= 0 ? r.b().segments.get(s.b()) : null;
            Node seg = sb != null ? sb : sa;
            for (ItemRow it : s.items()) {
                Node ia = it.a() >= 0 ? sa.children.get(it.a()) : null;
                Node ib = it.b() >= 0 ? sb.children.get(it.b()) : null;
                Node shown = ib != null ? ib : ia;
                rows.add(new Row(it.status(), false, pos(it.a()), pos(it.b()), seg.get("ID"), idOf(ia, "ID"),
                        idOf(ib, "ID"), shown.get("MaterialSpecificationID"), shown.get("Description"), it.changes()));
            }
        }
        return rows;
    }

    /** Same filter rule as the text report ({@link Report#wants}). */
    public static boolean wants(Report.Filter f, Row row) {
        return Report.wants(f, row.status(), row.moved());
    }

    public static List<Row> filter(List<Row> rows, Report.Filter f) {
        List<Row> out = new ArrayList<>();
        for (Row r : rows) {
            if (wants(f, r)) {
                out.add(r);
            }
        }
        return out;
    }

    /** "field: old → new; field: ..." with "(なし)" for a side where the field is absent. */
    public static String describe(List<FieldChange> changes) {
        List<String> parts = new ArrayList<>();
        for (FieldChange c : changes) {
            parts.add(c.field() + ": " + (c.oldValue() == null ? "(なし)" : c.oldValue()) + " → "
                    + (c.newValue() == null ? "(なし)" : c.newValue()));
        }
        return String.join("; ", parts);
    }
}
