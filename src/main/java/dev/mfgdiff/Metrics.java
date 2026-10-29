package dev.mfgdiff;

import dev.mfgdiff.ChangeGenerator.Cat;
import dev.mfgdiff.ChangeGenerator.Generated;
import dev.mfgdiff.ChangeGenerator.Level;
import dev.mfgdiff.DiffResult.ItemRow;
import dev.mfgdiff.DiffResult.SegmentRow;
import dev.mfgdiff.DiffResult.Status;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compares what the tool reported with what the generator did (see docs/validation/metrics.md). */
public final class Metrics {
    private Metrics() {
    }

    /** True positives, false positives, false negatives of one category. */
    public static final class Tally {
        public long tp;
        public long fp;
        public long fn;

        public long predicted() {
            return tp + fp;
        }

        public long truth() {
            return tp + fn;
        }

        /** Precision, or NaN when nothing was predicted. */
        public double precision() {
            return predicted() == 0 ? Double.NaN : (double) tp / predicted();
        }

        /** Recall, or NaN when the truth set is empty. */
        public double recall() {
            return truth() == 0 ? Double.NaN : (double) tp / truth();
        }
    }

    /** All category / level tallies of one or many runs. */
    public static final class Table {
        public final Map<Level, Map<Cat, Tally>> t = new EnumMap<>(Level.class);

        public Table() {
            for (Level l : Level.values()) {
                Map<Cat, Tally> m = new EnumMap<>(Cat.class);
                for (Cat c : Cat.values()) {
                    m.put(c, new Tally());
                }
                t.put(l, m);
            }
        }

        public Tally get(Level l, Cat c) {
            return t.get(l).get(c);
        }
    }

    /** The reported rows as keys in the same format as {@link ChangeGenerator.Truth}. */
    public static Set<String> predicted(Generated g, DiffResult r, Level level, Cat cat) {
        Set<String> out = new LinkedHashSet<>();
        for (SegmentRow s : r.segments()) {
            long ua = s.a() >= 0 ? g.a().segments.get(s.a()).uid : 0;
            long ub = s.b() >= 0 ? g.b().segments.get(s.b()).uid : 0;
            if (level == Level.SEGMENT) {
                if (cat == Cat.MOVED) {
                    if (s.moved()) {
                        out.add(ua + "|" + ub);
                    }
                } else if (matches(s.status(), cat)) {
                    out.add(key(s.status(), ua, ub));
                }
                continue;
            }
            for (ItemRow it : s.items()) {
                long ia = s.a() >= 0 && it.a() >= 0 ? g.a().segments.get(s.a()).children.get(it.a()).uid : 0;
                long ib = s.b() >= 0 && it.b() >= 0 ? g.b().segments.get(s.b()).children.get(it.b()).uid : 0;
                if (matches(it.status(), cat)) {
                    out.add(key(it.status(), ia, ib));
                }
            }
        }
        return out;
    }

    private static boolean matches(Status s, Cat c) {
        return switch (c) {
            case SAME -> s == Status.SAME;
            case CHANGED -> s == Status.CHANGED;
            case ADDED -> s == Status.ADDED;
            case REMOVED -> s == Status.REMOVED;
            case MOVED -> false;
        };
    }

    private static String key(Status s, long ua, long ub) {
        return switch (s) {
            case ADDED -> "|" + ub;
            case REMOVED -> ua + "|";
            default -> ua + "|" + ub;
        };
    }

    /** Adds one run to the table; returns the mismatch lines (empty when the run is exact). */
    public static List<String> add(Table table, Generated g, DiffResult r) {
        Map<Long, String> labels = ChangeGenerator.labels(g);
        List<String> mismatches = new ArrayList<>();
        for (Level l : Level.values()) {
            for (Cat c : Cat.values()) {
                Set<String> truth = g.truth().get(l, c);
                Set<String> pred = predicted(g, r, l, c);
                Tally tl = table.get(l, c);
                for (String k : pred) {
                    if (truth.contains(k)) {
                        tl.tp++;
                    } else {
                        tl.fp++;
                        mismatches.add(l + " " + c + " reported but not applied: " + label(k, labels));
                    }
                }
                for (String k : truth) {
                    if (!pred.contains(k)) {
                        tl.fn++;
                        mismatches.add(l + " " + c + " applied but not reported: " + label(k, labels));
                    }
                }
            }
        }
        return mismatches;
    }

    private static String label(String key, Map<Long, String> labels) {
        String[] p = key.split("\\|", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            if (p[i].isEmpty()) {
                continue;
            }
            long uid = Long.parseLong(p[i]);
            if (sb.length() > 0) {
                sb.append("  <->  ");
            }
            sb.append(labels.getOrDefault(uid, "uid " + uid));
        }
        return key.startsWith("|") ? sb + " (B side)" : key.endsWith("|") ? sb + " (A side)" : sb.toString();
    }
}
