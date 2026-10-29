package dev.mfgdiff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One compared object: an OperationsSegment (with the items of the material bill it references as
 * children) or one OperationsMaterialBillItem. Only simple fields are modelled; structure that is not
 * modelled is reported as a warning by the reader, never dropped silently.
 */
public final class Node {
    public enum Kind { SEGMENT, ITEM }

    public final Kind kind;
    /** Simple child elements by name (multi-valued elements keep every value, in document order). */
    public final LinkedHashMap<String, List<String>> fields = new LinkedHashMap<>();
    /** Items of the referenced material bill (segments only). */
    public final List<Node> children = new ArrayList<>();
    /** Value of the 'Indice' parameter (segments only); the position key, not a compared field. */
    public String order;
    /** Identity used only by the change generator; 0 for nodes that come from a file. */
    public long uid;

    public Node(Kind kind) {
        this.kind = kind;
    }

    public void put(String name, String value) {
        fields.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    public void set(String name, String value) {
        List<String> l = new ArrayList<>();
        l.add(value);
        fields.put(name, l);
    }

    /** Values of one field joined with '|' ("" when absent). */
    public String get(String name) {
        List<String> v = fields.get(name);
        return v == null ? "" : String.join("|", v);
    }

    public boolean has(String name) {
        return fields.containsKey(name);
    }

    /** Matching key: segment ID or item MaterialSpecificationID; null when the node has none. */
    public String key() {
        String name = kind == Kind.SEGMENT ? "ID" : "MaterialSpecificationID";
        return has(name) ? get(name) : null;
    }

    /** Field name to joined value, for comparison. */
    public Map<String, String> flat() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String k : fields.keySet()) {
            m.put(k, get(k));
        }
        return m;
    }

    public boolean sameFields(Node o) {
        return kind == o.kind && flat().equals(o.flat());
    }

    /**
     * Text used for the own-similarity ratio: the kind label followed by "name:value" pairs sorted by
     * name, whitespace collapsed (same construction as xmldiff Differ.node_text, which uses the tag and
     * the sorted attributes).
     */
    public String text() {
        StringBuilder sb = new StringBuilder(kind == Kind.SEGMENT ? "OperationsSegment" : "OperationsMaterialBillItem");
        for (Map.Entry<String, String> e : new TreeMap<>(flat()).entrySet()) {
            sb.append(' ').append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    public Node deepCopy() {
        Node n = new Node(kind);
        for (Map.Entry<String, List<String>> e : fields.entrySet()) {
            n.fields.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        n.order = order;
        n.uid = uid;
        for (Node c : children) {
            n.children.add(c.deepCopy());
        }
        return n;
    }

    @Override
    public String toString() {
        return kind + fields.toString();
    }
}
