package dev.mfgdiff;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes the compared model back as a B2MML OperationsDefinitionInformation document. Element order
 * follows the sequences in B2MML-OperationsDefinition.xsd, so the output validates against it.
 */
public final class B2mmlWriter {
    private static final Pattern PARAM = Pattern.compile("Param\\[([^\\]]+)\\]");

    private B2mmlWriter() {
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void el(StringBuilder sb, int ind, String name, String value) {
        sb.append("  ".repeat(ind)).append('<').append(name).append('>').append(esc(value)).append("</").append(name).append(">\n");
    }

    private static void open(StringBuilder sb, int ind, String name) {
        sb.append("  ".repeat(ind)).append('<').append(name).append(">\n");
    }

    private static void close(StringBuilder sb, int ind, String name) {
        sb.append("  ".repeat(ind)).append("</").append(name).append(">\n");
    }

    /** @param headerComment lines for a comment at the top of the file (may be empty); must not contain "--" */
    public static String write(Definition d, List<String> headerComment) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (!headerComment.isEmpty()) {
            sb.append("<!--\n");
            for (String l : headerComment) {
                if (l.contains("--")) {
                    throw new IllegalArgumentException("comment line contains '--': " + l);
                }
                sb.append("  ").append(l).append('\n');
            }
            sb.append("-->\n");
        }
        sb.append("<OperationsDefinitionInformation xmlns=\"").append(SecureXml.B2MML_NS).append("\">\n");
        el(sb, 1, "ID", d.infoId);
        open(sb, 1, "OperationsDefinition");
        el(sb, 2, "ID", d.defId);
        if (d.defVersion != null) {
            el(sb, 2, "Version", d.defVersion);
        }
        for (Node s : d.segments) {
            writeSegment(sb, s);
        }
        close(sb, 1, "OperationsDefinition");
        for (Node s : d.segments) {
            if (!s.has("OperationsMaterialBillID")) {
                if (!s.children.isEmpty()) {
                    throw new IllegalStateException("segment " + s.get("ID") + " has items but no OperationsMaterialBillID");
                }
                continue;
            }
            open(sb, 1, "OperationsMaterialBill");
            el(sb, 2, "ID", s.fields.get("OperationsMaterialBillID").get(0));
            for (Node it : s.children) {
                writeItem(sb, it);
            }
            close(sb, 1, "OperationsMaterialBill");
        }
        sb.append("</OperationsDefinitionInformation>\n");
        return sb.toString();
    }

    private static void writeSegment(StringBuilder sb, Node s) {
        open(sb, 2, "OperationsSegment");
        for (String n : B2mmlReader.SEG_SIMPLE) {
            if (s.has(n)) {
                for (String v : s.fields.get(n)) {
                    el(sb, 3, n, v);
                }
            }
        }
        if (s.order != null) {
            parameter(sb, B2mmlReader.ORDER_PARAMETER, s.order, "integer", null);
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String k : s.fields.keySet()) {
            Matcher m = PARAM.matcher(k);
            if (m.matches()) {
                ids.add(m.group(1));
            }
        }
        for (String id : ids) {
            String base = "Param[" + id + "]";
            parameter(sb, id, s.get(base), s.has(base + "/DataType") ? s.get(base + "/DataType") : null,
                    s.has(base + "/UnitOfMeasure") ? s.get(base + "/UnitOfMeasure") : null);
        }
        close(sb, 2, "OperationsSegment");
    }

    private static void parameter(StringBuilder sb, String id, String value, String dataType, String uom) {
        open(sb, 3, "ParameterSpecification");
        el(sb, 4, "ID", id);
        open(sb, 4, "Value");
        el(sb, 5, "ValueString", value);
        if (dataType != null) {
            el(sb, 5, "DataType", dataType);
        }
        if (uom != null) {
            el(sb, 5, "UnitOfMeasure", uom);
        }
        close(sb, 4, "Value");
        close(sb, 3, "ParameterSpecification");
    }

    private static void writeItem(StringBuilder sb, Node it) {
        open(sb, 2, "OperationsMaterialBillItem");
        for (String n : B2mmlReader.ITEM_SIMPLE) {
            if (it.has(n)) {
                for (String v : it.fields.get(n)) {
                    el(sb, 3, n, v);
                }
            }
        }
        if (it.has("Quantity")) {
            open(sb, 3, "Quantity");
            el(sb, 4, "QuantityString", it.get("Quantity"));
            if (it.has("Quantity/DataType")) {
                el(sb, 4, "DataType", it.get("Quantity/DataType"));
            }
            if (it.has("Quantity/UnitOfMeasure")) {
                el(sb, 4, "UnitOfMeasure", it.get("Quantity/UnitOfMeasure"));
            }
            close(sb, 3, "Quantity");
        }
        close(sb, 2, "OperationsMaterialBillItem");
    }
}
