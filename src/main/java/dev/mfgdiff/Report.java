package dev.mfgdiff;

import dev.mfgdiff.DiffResult.FieldChange;
import dev.mfgdiff.DiffResult.ItemRow;
import dev.mfgdiff.DiffResult.SegmentRow;
import dev.mfgdiff.DiffResult.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Text tables, one block per category. The symbols and the filter names follow the SAP routing
 * comparison (= not-equal A B) and the Oracle structure comparison filter (All, Differences, Added,
 * Changes, Removed, Unchanged); "Moved" is added by this tool (docs/adr/0001-matching.md).
 */
public final class Report {
    public enum Filter {
        ALL, DIFFERENCES, ADDED, CHANGES, REMOVED, UNCHANGED, MOVED;

        public static Filter parse(String s) {
            for (Filter f : values()) {
                if (f.name().equalsIgnoreCase(s)) {
                    return f;
                }
            }
            throw new IllegalArgumentException("unknown filter '" + s + "' (All, Differences, Added, Changes, Removed, Unchanged, Moved)");
        }
    }

    private Report() {
    }

    static String describe(List<FieldChange> changes) {
        List<String> parts = new ArrayList<>();
        for (FieldChange c : changes) {
            parts.add(c.field() + ": " + (c.oldValue() == null ? "(absent)" : c.oldValue()) + " -> "
                    + (c.newValue() == null ? "(absent)" : c.newValue()));
        }
        return String.join("; ", parts);
    }

    private static String pos(int i) {
        return i < 0 ? "-" : Integer.toString(i + 1);
    }

    private static void table(StringBuilder sb, List<String> head, List<List<String>> rows) {
        int[] w = new int[head.size()];
        for (int c = 0; c < w.length; c++) {
            w[c] = head.get(c).length();
            for (List<String> r : rows) {
                w[c] = Math.max(w[c], r.get(c).length());
            }
        }
        line(sb, head, w);
        for (List<String> r : rows) {
            line(sb, r, w);
        }
    }

    private static void line(StringBuilder sb, List<String> cells, int[] w) {
        sb.append("    ");
        for (int c = 0; c < w.length; c++) {
            String cell = cells.get(c);
            sb.append(cell);
            if (c < w.length - 1) {
                sb.append(" ".repeat(w[c] - cell.length() + 2));
            }
        }
        sb.append('\n');
    }

    static boolean wants(Filter f, Status s, boolean moved) {
        return switch (f) {
            case ALL -> true;
            case DIFFERENCES -> s != Status.SAME || moved;
            case ADDED -> s == Status.ADDED;
            case CHANGES -> s == Status.CHANGED;
            case REMOVED -> s == Status.REMOVED;
            case UNCHANGED -> s == Status.SAME && !moved;
            case MOVED -> moved;
        };
    }

    public static String render(DiffResult r, String nameA, String nameB, Filter filter) {
        StringBuilder sb = new StringBuilder();
        sb.append("A: ").append(nameA).append("  (").append(r.a().segments.size()).append(" segments, ")
                .append(r.a().itemCount()).append(" bill items)\n");
        sb.append("B: ").append(nameB).append("  (").append(r.b().segments.size()).append(" segments, ")
                .append(r.b().itemCount()).append(" bill items)\n");
        sb.append("Legend: = same   ≠ changed   A only in A (removed)   B only in B (added)   ↕ moved\n");
        sb.append("Filter: ").append(filter.name().charAt(0)).append(filter.name().substring(1).toLowerCase(Locale.ROOT)).append('\n');
        for (String w : r.a().warnings) {
            sb.append("warning (A): ").append(w).append('\n');
        }
        for (String w : r.b().warnings) {
            sb.append("warning (B): ").append(w).append('\n');
        }

        block(sb, r, filter, "=", "Same", Status.SAME, false);
        block(sb, r, filter, "≠", "Changed", Status.CHANGED, false);
        block(sb, r, filter, "A", "Only in A (removed)", Status.REMOVED, false);
        block(sb, r, filter, "B", "Only in B (added)", Status.ADDED, false);
        block(sb, r, filter, "↕", "Moved (position in the segment order changed)", Status.SAME, true);
        return sb.toString();
    }

    private static void block(StringBuilder sb, DiffResult r, Filter filter, String sym, String title, Status status,
            boolean movedBlock) {
        List<List<String>> segRows = new ArrayList<>();
        List<List<String>> itemRows = new ArrayList<>();
        for (SegmentRow s : r.segments()) {
            boolean inBlock = movedBlock ? s.moved() : (s.status() == status);
            if (inBlock && wants(filter, s.status(), s.moved())) {
                Node n = s.b() >= 0 ? r.b().segments.get(s.b()) : r.a().segments.get(s.a());
                String idA = s.a() >= 0 ? r.a().segments.get(s.a()).get("ID") : "-";
                String idB = s.b() >= 0 ? r.b().segments.get(s.b()).get("ID") : "-";
                String tag = s.moved() && !movedBlock ? " [moved]" : "";
                segRows.add(List.of(pos(s.a()), pos(s.b()), idA, idB, n.get("Description"),
                        describe(s.changes()) + tag));
            }
            if (!movedBlock) {
                for (ItemRow it : s.items()) {
                    if (it.status() == status && wants(filter, it.status(), false)) {
                        Node seg = s.b() >= 0 ? r.b().segments.get(s.b()) : r.a().segments.get(s.a());
                        Node item = it.b() >= 0 ? seg.children.get(it.b()) : r.a().segments.get(s.a()).children.get(it.a());
                        itemRows.add(List.of(seg.get("ID"), pos(it.a()), pos(it.b()), item.get("MaterialSpecificationID"),
                                item.get("Description"), describe(it.changes())));
                    }
                }
            }
        }
        sb.append("\n[").append(sym).append("] ").append(title).append('\n');
        sb.append("  segments (").append(segRows.size()).append(")\n");
        if (!segRows.isEmpty()) {
            table(sb, List.of("A#", "B#", "A id", "B id", "description", "details"), segRows);
        }
        if (!movedBlock) {
            sb.append("  bill items (").append(itemRows.size()).append(")\n");
            if (!itemRows.isEmpty()) {
                table(sb, List.of("segment", "A#", "B#", "part", "description", "details"), itemRows);
            }
        }
    }
}
