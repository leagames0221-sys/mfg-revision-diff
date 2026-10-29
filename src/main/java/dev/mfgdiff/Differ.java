package dev.mfgdiff;

import dev.mfgdiff.DiffResult.FieldChange;
import dev.mfgdiff.DiffResult.ItemRow;
import dev.mfgdiff.DiffResult.SegmentRow;
import dev.mfgdiff.DiffResult.Status;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the classification: segments are compared as an ordered list (a segment that is paired but not
 * on the longest common subsequence of the paired order is "moved"); the items of a bill are compared as
 * an unordered collection.
 */
public final class Differ {
    private Differ() {
    }

    public static DiffResult diff(Definition a, Definition b) {
        return diff(a, b, new NodeMatcher());
    }

    public static DiffResult diff(Definition a, Definition b, NodeMatcher matcher) {
        NodeMatcher.Matching m = matcher.matchSegments(a.segments, b.segments);

        // Order: pairs whose position is not on the LCS of the paired order are moved (xmldiff L399-L418).
        List<Integer> lch = new ArrayList<>();
        for (int i = 0; i < a.segments.size(); i++) {
            if (m.l2r()[i] >= 0) {
                lch.add(i);
            }
        }
        List<Integer> rch = new ArrayList<>();
        for (int j = 0; j < b.segments.size(); j++) {
            if (m.r2l()[j] >= 0) {
                rch.add(j);
            }
        }
        Set<Integer> inOrder = new HashSet<>();
        for (int[] p : Lcs.compute(lch, rch, (x, y) -> m.l2r()[x] == y)) {
            inOrder.add(lch.get(p[0]));
        }

        List<SegmentRow> rows = new ArrayList<>();
        for (int j = 0; j < b.segments.size(); j++) {
            Node sb = b.segments.get(j);
            int i = m.r2l()[j];
            if (i < 0) {
                rows.add(new SegmentRow(Status.ADDED, false, -1, j, List.of(), cascade(sb, Status.ADDED)));
                continue;
            }
            Node sa = a.segments.get(i);
            List<FieldChange> ch = changes(sa, sb);
            rows.add(new SegmentRow(ch.isEmpty() ? Status.SAME : Status.CHANGED, !inOrder.contains(i), i, j, ch,
                    itemRows(matcher, sa, sb)));
        }
        for (int i = 0; i < a.segments.size(); i++) {
            if (m.l2r()[i] < 0) {
                rows.add(new SegmentRow(Status.REMOVED, false, i, -1, List.of(), cascade(a.segments.get(i), Status.REMOVED)));
            }
        }
        return new DiffResult(a, b, rows);
    }

    /** Items of an added / removed segment are added / removed with it. */
    private static List<ItemRow> cascade(Node seg, Status status) {
        List<ItemRow> rows = new ArrayList<>();
        for (int k = 0; k < seg.children.size(); k++) {
            rows.add(status == Status.ADDED ? new ItemRow(status, -1, k, List.of()) : new ItemRow(status, k, -1, List.of()));
        }
        return rows;
    }

    /** Item rows for a matched pair of segments. */
    private static List<ItemRow> itemRows(NodeMatcher matcher, Node sa, Node sb) {
        List<ItemRow> rows = new ArrayList<>();
        NodeMatcher.Matching im = matcher.matchItems(sa.children, sb.children);
        for (int k = 0; k < sb.children.size(); k++) {
            int i = im.r2l()[k];
            if (i < 0) {
                rows.add(new ItemRow(Status.ADDED, -1, k, List.of()));
            } else {
                List<FieldChange> ch = changes(sa.children.get(i), sb.children.get(k));
                rows.add(new ItemRow(ch.isEmpty() ? Status.SAME : Status.CHANGED, i, k, ch));
            }
        }
        for (int i = 0; i < sa.children.size(); i++) {
            if (im.l2r()[i] < 0) {
                rows.add(new ItemRow(Status.REMOVED, i, -1, List.of()));
            }
        }
        return rows;
    }

    static List<FieldChange> changes(Node a, Node b) {
        Map<String, String> fa = a.flat();
        Map<String, String> fb = b.flat();
        Set<String> names = new LinkedHashSet<>(fa.keySet());
        names.addAll(fb.keySet());
        List<FieldChange> out = new ArrayList<>();
        for (String n : names) {
            String va = fa.get(n);
            String vb = fb.get(n);
            if (va == null || vb == null || !va.equals(vb)) {
                out.add(new FieldChange(n, va, vb));
            }
        }
        return out;
    }
}
