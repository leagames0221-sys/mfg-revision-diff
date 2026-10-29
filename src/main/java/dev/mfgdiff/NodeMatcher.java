package dev.mfgdiff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleBiFunction;

/**
 * Decides which node of revision A is the same object as which node of revision B.
 *
 * <p>The structure follows xmldiff diff.py (MIT License, see NOTICE and docs/adr/0001): a unique key
 * decides alone when it is unique on both sides; the rest are paired by a similarity of at least F, where
 * the similarity combines the node's own text ratio with the share of matched children
 * (sqrt((own^2 + child^2) / 2)). See docs/adr/0001-matching.md for the line-by-line mapping.
 */
public final class NodeMatcher {
    /**
     * Default threshold. xmldiff's own default is 0.5; 0.7 is the setting chosen by the rule in
     * docs/validation/metrics.md section 7 on the tuning seeds (author's decision D17).
     */
    public static final double DEFAULT_F = 0.7;
    public static final RatioMode DEFAULT_MODE = RatioMode.FAST;

    private final double f;
    private final RatioMode mode;

    public NodeMatcher() {
        this(DEFAULT_F, DEFAULT_MODE);
    }

    public NodeMatcher(double f) {
        this(f, DEFAULT_MODE);
    }

    public NodeMatcher(double f, RatioMode mode) {
        this.f = f;
        this.mode = mode;
    }

    public RatioMode mode() {
        return mode;
    }

    public double f() {
        return f;
    }

    /** l2r[i] is the index in the right list matched to left[i], or -1; r2l is the inverse. */
    public record Matching(int[] l2r, int[] r2l) {
        static Matching empty(int nl, int nr) {
            int[] a = new int[nl];
            int[] b = new int[nr];
            Arrays.fill(a, -1);
            Arrays.fill(b, -1);
            return new Matching(a, b);
        }

        void pair(int l, int r) {
            l2r[l] = r;
            r2l[r] = l;
        }

        public int count() {
            int n = 0;
            for (int x : l2r) {
                if (x >= 0) {
                    n++;
                }
            }
            return n;
        }
    }

    private static Map<String, List<Integer>> groupByKey(List<Node> nodes) {
        Map<String, List<Integer>> g = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            String k = nodes.get(i).key();
            if (k != null) {
                g.computeIfAbsent(k, x -> new ArrayList<>()).add(i);
            }
        }
        return g;
    }

    /** Segments: the ID decides alone when it occurs exactly once on each side; the rest by similarity. */
    public Matching matchSegments(List<Node> left, List<Node> right) {
        Matching m = Matching.empty(left.size(), right.size());
        Map<String, List<Integer>> gl = groupByKey(left);
        Map<String, List<Integer>> gr = groupByKey(right);
        for (Map.Entry<String, List<Integer>> e : gl.entrySet()) {
            List<Integer> rs = gr.get(e.getKey());
            if (rs != null && e.getValue().size() == 1 && rs.size() == 1) {
                m.pair(e.getValue().get(0), rs.get(0));
            }
        }
        pairBySimilarity(left, right, m, this::segmentSimilarity);
        return m;
    }

    /**
     * Material bill items of one pair of segments. Rows with the same part key are paired first: rows
     * with identical content, then the remaining ones in order of appearance. Rows without a part key are
     * paired by similarity.
     */
    public Matching matchItems(List<Node> left, List<Node> right) {
        Matching m = Matching.empty(left.size(), right.size());
        Map<String, List<Integer>> gl = groupByKey(left);
        Map<String, List<Integer>> gr = groupByKey(right);
        for (Map.Entry<String, List<Integer>> e : gl.entrySet()) {
            List<Integer> rs0 = gr.get(e.getKey());
            if (rs0 == null) {
                continue;
            }
            List<Integer> ls = new ArrayList<>(e.getValue());
            List<Integer> rs = new ArrayList<>(rs0);
            // identical rows first
            for (int i = 0; i < ls.size(); ) {
                int li = ls.get(i);
                int hit = -1;
                for (int j = 0; j < rs.size(); j++) {
                    if (left.get(li).sameFields(right.get(rs.get(j)))) {
                        hit = j;
                        break;
                    }
                }
                if (hit >= 0) {
                    m.pair(li, rs.get(hit));
                    ls.remove(i);
                    rs.remove(hit);
                } else {
                    i++;
                }
            }
            // then in order of appearance
            int n = Math.min(ls.size(), rs.size());
            for (int i = 0; i < n; i++) {
                m.pair(ls.get(i), rs.get(i));
            }
        }
        // A row that has a part key is never paired with a different key, however similar the text is
        // (xmldiff: nodes whose unique attribute differs "will not match regardless of their structural
        // similarity"). Only rows without any key are paired by similarity.
        pairBySimilarity(left, right, m,
                (a, b) -> a.key() != null || b.key() != null ? 0.0 : mode.ratio(a.text(), b.text()));
        return m;
    }

    /** Own-text ratio combined with the share of matched children, as xmldiff Differ.node_ratio. */
    double segmentSimilarity(Node a, Node b) {
        double own = mode.ratio(a.text(), b.text());
        if (a.children.isEmpty() && b.children.isEmpty()) {
            return own;
        }
        int matched = matchItems(a.children, b.children).count();
        double child = (double) matched / Math.max(a.children.size(), b.children.size());
        return Math.sqrt((own * own + child * child) / 2);
    }

    private record Candidate(int left, int right, double score) {
    }

    /**
     * Pairs the still unpaired nodes: first every pair with similarity exactly 1.0, then the best
     * remaining candidates in descending score (xmldiff best_match, L136-169), then a plain greedy pass
     * over what is left (L171-192). Only scores of at least F pair.
     */
    private void pairBySimilarity(List<Node> left, List<Node> right, Matching m,
            ToDoubleBiFunction<Node, Node> sim) {
        List<Integer> rfree = new ArrayList<>();
        for (int j = 0; j < right.size(); j++) {
            if (m.r2l[j] < 0) {
                rfree.add(j);
            }
        }
        List<Candidate> unmatched = new ArrayList<>();
        for (int i = 0; i < left.size(); i++) {
            if (m.l2r[i] >= 0) {
                continue;
            }
            double max = 0;
            int best = -1;
            boolean perfect = false;
            for (int idx = 0; idx < rfree.size(); idx++) {
                int j = rfree.get(idx);
                double s = sim.applyAsDouble(left.get(i), right.get(j));
                if (s == 1.0) {
                    m.pair(i, j);
                    rfree.remove(idx);
                    perfect = true;
                    break;
                }
                if (s > max) {
                    best = j;
                    max = s;
                }
            }
            if (!perfect) {
                unmatched.add(new Candidate(i, best, max));
            }
        }
        unmatched.sort((x, y) -> Double.compare(y.score(), x.score()));
        List<Integer> lrest = new ArrayList<>();
        for (Candidate c : unmatched) {
            if (c.score() >= f && c.right() >= 0 && rfree.contains(c.right())) {
                m.pair(c.left(), c.right());
                rfree.remove((Integer) c.right());
            } else {
                lrest.add(c.left());
            }
        }
        for (int i : lrest) {
            double max = 0;
            int best = -1;
            for (int j : rfree) {
                double s = sim.applyAsDouble(left.get(i), right.get(j));
                if (s > max) {
                    best = j;
                    max = s;
                }
                if (s == 1.0) {
                    break;
                }
            }
            if (best >= 0 && max >= f) {
                m.pair(i, best);
                rfree.remove((Integer) best);
            }
        }
    }
}
