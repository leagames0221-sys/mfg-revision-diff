package dev.mfgdiff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Makes a second revision from a first one by applying random operations, and records what was done.
 *
 * <p>Model: the change generator of the X-Diff paper (section 5: insert, delete and update applied at
 * random on each level, with the share of changed nodes as a parameter), extended with {@code move}
 * because that paper's generator never permuted nodes, and with {@code renumber}. See docs/sources.md
 * and docs/validation/metrics.md.
 */
public final class ChangeGenerator {
    public enum Op {
        SEG_INSERT, SEG_REMOVE, SEG_UPDATE, SEG_MOVE, SEG_RENUMBER,
        ITEM_INSERT, ITEM_INSERT_DUP, ITEM_REMOVE, ITEM_UPDATE
    }

    public enum Level { SEGMENT, ITEM }

    public enum Cat { SAME, CHANGED, ADDED, REMOVED, MOVED }

    /** What was applied, in words, for the report. */
    public record Applied(Op op, String description) {
    }

    /**
     * The correct answer. Keys are "uidA|uidB" for SAME / CHANGED / MOVED, "|uid" for ADDED and "uid|"
     * for REMOVED, where uid is the identity the generator gave the node (same uid on both sides =
     * same object).
     */
    public record Truth(Map<String, Set<String>> keys) {
        public Set<String> get(Level l, Cat c) {
            return keys.getOrDefault(l + ":" + c, Set.of());
        }
    }

    public record Generated(Definition a, Definition b, List<Applied> ops, Truth truth) {
    }

    private final Random rnd;
    private final Definition a;
    private final Definition b;
    private final List<Applied> applied = new ArrayList<>();
    private final Set<Long> movedUids = new HashSet<>();
    private final Set<Long> consumed = new HashSet<>();
    private final List<SampleData.Part> catalogue;
    private long nextUid;
    private int freshSeg = 900;
    private int freshLine = 0;

    private ChangeGenerator(Definition base, long seed) {
        this.rnd = new Random(seed);
        this.a = base;
        this.b = base.deepCopy();
        this.catalogue = SampleData.catalogue(new Random(seed ^ 0x5DEECE66DL));
        long max = 0;
        for (Node s : base.segments) {
            max = Math.max(max, s.uid);
            for (Node it : s.children) {
                max = Math.max(max, it.uid);
            }
        }
        this.nextUid = max + 1;
    }

    /** Applies changes to about {@code ratio} of the nodes (segments plus bill items). */
    public static Generated generate(Definition base, double ratio, long seed) {
        ChangeGenerator g = new ChangeGenerator(base, seed);
        List<Node> candidates = new ArrayList<>();
        for (Node s : g.b.segments) {
            candidates.add(s);
            candidates.addAll(s.children);
        }
        int wanted = Math.max(1, (int) Math.round(ratio * candidates.size()));
        Collections.shuffle(candidates, g.rnd);
        int done = 0;
        for (Node t : candidates) {
            if (done >= wanted) {
                break;
            }
            if (g.consumed.contains(t.uid) || g.parentRemoved(t)) {
                continue;
            }
            Op op = g.choose(t);
            if (!g.apply(op, t) && t.kind == Node.Kind.SEGMENT) {
                op = Op.SEG_UPDATE;
                if (!g.apply(op, t)) {
                    continue;
                }
            }
            g.consumed.add(t.uid);
            done++;
        }
        return g.finish();
    }

    /** Applies exactly one operation of the given kind to a randomly chosen eligible node. */
    public static Generated applyOne(Definition base, Op op, long seed) {
        ChangeGenerator g = new ChangeGenerator(base, seed);
        List<Node> targets = new ArrayList<>();
        for (Node s : g.b.segments) {
            if (op.name().startsWith("SEG_")) {
                targets.add(s);
            } else {
                targets.addAll(s.children);
            }
        }
        Collections.shuffle(targets, g.rnd);
        for (Node t : targets) {
            if (g.apply(op, t)) {
                return g.finish();
            }
        }
        throw new IllegalStateException("no eligible target for " + op);
    }

    private boolean parentRemoved(Node t) {
        return t.kind == Node.Kind.ITEM && parentOf(t) == null;
    }

    private Node parentOf(Node item) {
        for (Node s : b.segments) {
            if (s.children.contains(item)) {
                return s;
            }
        }
        return null;
    }

    private Op choose(Node t) {
        if (t.kind == Node.Kind.SEGMENT) {
            Op[] ops = {Op.SEG_INSERT, Op.SEG_REMOVE, Op.SEG_UPDATE, Op.SEG_MOVE, Op.SEG_RENUMBER};
            return ops[rnd.nextInt(ops.length)];
        }
        Op[] ops = {Op.ITEM_INSERT, Op.ITEM_REMOVE, Op.ITEM_UPDATE};
        Op op = ops[rnd.nextInt(ops.length)];
        if (op == Op.ITEM_INSERT && rnd.nextInt(4) == 0) {
            return Op.ITEM_INSERT_DUP;
        }
        return op;
    }

    private boolean apply(Op op, Node t) {
        switch (op) {
            case SEG_INSERT -> {
                Node s = newSegment();
                b.segments.add(b.segments.indexOf(t) + 1, s);
                applied.add(new Applied(op, "insert segment " + s.get("ID") + " after " + t.get("ID")));
                return true;
            }
            case SEG_REMOVE -> {
                if (b.segments.size() <= 3) {
                    return false;
                }
                b.segments.remove(t);
                for (Node it : t.children) {
                    consumed.add(it.uid);
                }
                applied.add(new Applied(op, "remove segment " + t.get("ID") + " with " + t.children.size() + " items"));
                return true;
            }
            case SEG_UPDATE -> {
                int which = rnd.nextInt(3);
                if (which == 0) {
                    String old = t.get("Description");
                    String nu;
                    do {
                        nu = SampleData.description(rnd) + " " + (char) ('A' + rnd.nextInt(26));
                    } while (nu.equals(old));
                    t.set("Description", nu);
                } else if (which == 1) {
                    String old = t.get("Duration");
                    String nu;
                    do {
                        nu = "PT" + (1 + rnd.nextInt(30)) + "M";
                    } while (nu.equals(old));
                    t.set("Duration", nu);
                } else {
                    String old = t.get("Param[Station]");
                    String nu;
                    do {
                        nu = String.format("ST-%02d", 1 + rnd.nextInt(40));
                    } while (nu.equals(old));
                    t.set("Param[Station]", nu);
                }
                applied.add(new Applied(op, "update segment " + t.get("ID")));
                return true;
            }
            case SEG_MOVE -> {
                int idx = b.segments.indexOf(t);
                int n = b.segments.size();
                List<Integer> valid = new ArrayList<>();
                for (int j = 0; j < n; j++) {
                    if (Math.abs(j - idx) >= 2) {
                        valid.add(j);
                    }
                }
                if (valid.isEmpty()) {
                    return false;
                }
                int j = valid.get(rnd.nextInt(valid.size()));
                b.segments.remove(idx);
                b.segments.add(j, t);
                movedUids.add(t.uid);
                applied.add(new Applied(op, "move segment " + t.get("ID") + " from position " + (idx + 1) + " to " + (j + 1)));
                return true;
            }
            case SEG_RENUMBER -> {
                String old = t.get("ID");
                String nu = freshSegmentId();
                t.set("ID", nu);
                applied.add(new Applied(op, "renumber segment " + old + " to " + nu));
                return true;
            }
            case ITEM_INSERT -> {
                Node parent = parentOf(t);
                if (parent == null) {
                    return false;
                }
                Set<String> used = new HashSet<>();
                for (Node c : parent.children) {
                    used.add(c.get("MaterialSpecificationID"));
                }
                SampleData.Part p;
                do {
                    p = catalogue.get(rnd.nextInt(catalogue.size()));
                } while (used.contains(p.key()));
                Node it = SampleData.item(rnd, p, nextUid++, freshLineId());
                parent.children.add(rnd.nextInt(parent.children.size() + 1), it);
                applied.add(new Applied(op, "insert item " + p.key() + " into " + parent.get("ID")));
                return true;
            }
            case ITEM_INSERT_DUP -> {
                Node parent = parentOf(t);
                if (parent == null) {
                    return false;
                }
                Node it = t.deepCopy();
                it.uid = nextUid++;
                it.set("ID", freshLineId());
                it.set("Quantity", it.get("Quantity") + "1");
                parent.children.add(rnd.nextInt(parent.children.size() + 1), it);
                applied.add(new Applied(op, "insert duplicate row of " + t.get("MaterialSpecificationID") + " into " + parent.get("ID")));
                return true;
            }
            case ITEM_REMOVE -> {
                Node parent = parentOf(t);
                if (parent == null) {
                    return false;
                }
                parent.children.remove(t);
                applied.add(new Applied(op, "remove item " + t.get("MaterialSpecificationID") + " from " + parent.get("ID")));
                return true;
            }
            case ITEM_UPDATE -> {
                Node parent = parentOf(t);
                if (parent == null) {
                    return false;
                }
                if (rnd.nextBoolean()) {
                    t.set("Quantity", t.get("Quantity") + "7");
                } else {
                    t.set("Description", t.get("Description") + " rev2");
                }
                applied.add(new Applied(op, "update item " + t.get("MaterialSpecificationID") + " in " + parent.get("ID")));
                return true;
            }
            default -> throw new IllegalStateException();
        }
    }

    private String freshSegmentId() {
        Set<String> used = new HashSet<>();
        for (Node s : b.segments) {
            used.add(s.get("ID"));
        }
        for (Node s : a.segments) {
            used.add(s.get("ID"));
        }
        String id;
        do {
            id = "OP-" + (++freshSeg);
        } while (used.contains(id));
        return id;
    }

    private String freshLineId() {
        return String.format("LN-%05d", ++freshLine);
    }

    private Node newSegment() {
        Node s = new Node(Node.Kind.SEGMENT);
        s.uid = nextUid++;
        String id = freshSegmentId();
        s.set("ID", id);
        s.set("Description", SampleData.description(rnd) + " " + (char) ('A' + rnd.nextInt(26)));
        s.set("OperationsType", "Production");
        s.set("Duration", "PT" + (1 + rnd.nextInt(9)) + "M");
        s.set("OperationsMaterialBillID", "MB-" + id);
        s.set("Param[Station]", String.format("ST-%02d", 1 + rnd.nextInt(40)));
        s.set("Param[Station]/DataType", "string");
        int n = rnd.nextInt(4);
        Set<String> used = new HashSet<>();
        for (int k = 0; k < n; k++) {
            SampleData.Part p = catalogue.get(rnd.nextInt(catalogue.size()));
            if (used.add(p.key())) {
                s.children.add(SampleData.item(rnd, p, nextUid++, freshLineId()));
            }
        }
        return s;
    }

    private Generated finish() {
        for (int i = 0; i < b.segments.size(); i++) {
            b.segments.get(i).order = String.format("%04d", (i + 1) * 10);
        }
        return new Generated(a, b, applied, truth());
    }

    private Truth truth() {
        Map<String, Set<String>> keys = new LinkedHashMap<>();
        Map<Long, Node> segA = new LinkedHashMap<>();
        Map<Long, Node> segB = new LinkedHashMap<>();
        Map<Long, Node> itemA = new LinkedHashMap<>();
        Map<Long, Node> itemB = new LinkedHashMap<>();
        for (Node s : a.segments) {
            segA.put(s.uid, s);
            for (Node it : s.children) {
                itemA.put(it.uid, it);
            }
        }
        for (Node s : b.segments) {
            segB.put(s.uid, s);
            for (Node it : s.children) {
                itemB.put(it.uid, it);
            }
        }
        classify(keys, Level.SEGMENT, segA, segB, movedUids);
        classify(keys, Level.ITEM, itemA, itemB, Set.of());
        return new Truth(keys);
    }

    private static void add(Map<String, Set<String>> keys, Level l, Cat c, String key) {
        keys.computeIfAbsent(l + ":" + c, k -> new LinkedHashSet<>()).add(key);
    }

    private static void classify(Map<String, Set<String>> keys, Level l, Map<Long, Node> na, Map<Long, Node> nb,
            Set<Long> moved) {
        for (Map.Entry<Long, Node> e : na.entrySet()) {
            long uid = e.getKey();
            Node other = nb.get(uid);
            if (other == null) {
                add(keys, l, Cat.REMOVED, uid + "|");
                continue;
            }
            add(keys, l, e.getValue().sameFields(other) ? Cat.SAME : Cat.CHANGED, uid + "|" + uid);
            if (moved.contains(uid)) {
                add(keys, l, Cat.MOVED, uid + "|" + uid);
            }
        }
        for (long uid : nb.keySet()) {
            if (!na.containsKey(uid)) {
                add(keys, l, Cat.ADDED, "|" + uid);
            }
        }
    }

    /** Human-readable labels for uids, for the report of mismatches. */
    public static Map<Long, String> labels(Generated g) {
        Map<Long, String> m = new HashMap<>();
        for (Node s : g.a().segments) {
            m.put(s.uid, "segment " + s.get("ID") + " \"" + s.get("Description") + "\"");
            for (Node it : s.children) {
                m.put(it.uid, "item " + it.get("MaterialSpecificationID") + " (" + it.get("ID") + ") in " + s.get("ID"));
            }
        }
        for (Node s : g.b().segments) {
            m.putIfAbsent(s.uid, "segment " + s.get("ID") + " \"" + s.get("Description") + "\"");
            for (Node it : s.children) {
                m.putIfAbsent(it.uid, "item " + it.get("MaterialSpecificationID") + " (" + it.get("ID") + ") in " + s.get("ID"));
            }
        }
        return m;
    }
}
