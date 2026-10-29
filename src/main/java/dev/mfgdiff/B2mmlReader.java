package dev.mfgdiff;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Reads a B2MML OperationsDefinitionInformation document into the compared model.
 *
 * <p>Modelled: the OperationsSegments of the first OperationsDefinition, their simple fields, their
 * ParameterSpecification entries, and the OperationsMaterialBillItems of the bill each segment points
 * to with OperationsMaterialBillID. Everything else that is present is listed in
 * {@link Definition#warnings}, so a difference in an element that is not compared is never silent.
 */
public final class B2mmlReader {
    public static final String ORDER_PARAMETER = "Indice";

    static final List<String> SEG_SIMPLE = List.of("ID", "Description", "Version", "OperationsType",
            "DefinitionType", "Duration", "ProcessSegmentID", "BillOfMaterialsID", "WorkMasterSourceID",
            "BillOfResourcesID", "OperationsMaterialBillID", "OperationsSegmentPatternID");
    static final List<String> ITEM_SIMPLE = List.of("ID", "Description", "Version", "UseType",
            "AssemblyType", "AssemblyRelationship", "MaterialSpecificationID");

    private B2mmlReader() {
    }

    public static Definition read(Path file) throws IOException, SAXException {
        try (InputStream in = Files.newInputStream(file)) {
            return read(SecureXml.readLimited(in));
        }
    }

    public static Definition read(byte[] data) throws IOException, SAXException {
        return fromDocument(SecureXml.parse(data));
    }

    static List<Element> elements(Element parent) {
        List<Element> out = new ArrayList<>();
        for (org.w3c.dom.Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                out.add(e);
            }
        }
        return out;
    }

    static String text(Element e) {
        return e.getTextContent().trim();
    }

    private static void warn(Definition d, String what) {
        String w = "not compared: " + what;
        if (!d.warnings.contains(w)) {
            d.warnings.add(w);
        }
    }

    public static Definition fromDocument(Document doc) throws SAXException {
        Element root = doc.getDocumentElement();
        if (!SecureXml.B2MML_NS.equals(root.getNamespaceURI()) || !"OperationsDefinitionInformation".equals(root.getLocalName())) {
            throw new SAXException("root element must be {" + SecureXml.B2MML_NS + "}OperationsDefinitionInformation");
        }
        Definition d = new Definition();
        Map<String, List<Element>> bills = new LinkedHashMap<>();
        Element definition = null;
        for (Element e : elements(root)) {
            switch (e.getLocalName()) {
                case "ID" -> d.infoId = text(e);
                case "OperationsDefinition" -> {
                    if (definition == null) {
                        definition = e;
                    } else {
                        warn(d, "second OperationsDefinition");
                    }
                }
                case "OperationsMaterialBill" -> {
                    String id = null;
                    List<Element> items = new ArrayList<>();
                    for (Element c : elements(e)) {
                        switch (c.getLocalName()) {
                            case "ID" -> id = text(c);
                            case "OperationsMaterialBillItem" -> items.add(c);
                            default -> warn(d, "OperationsMaterialBill/" + c.getLocalName());
                        }
                    }
                    if (id == null || bills.containsKey(id)) {
                        warn(d, "OperationsMaterialBill without a unique ID (" + id + ")");
                    } else {
                        bills.put(id, items);
                    }
                }
                default -> warn(d, "OperationsDefinitionInformation/" + e.getLocalName());
            }
        }
        if (definition == null) {
            throw new SAXException("no OperationsDefinition element");
        }
        for (Element e : elements(definition)) {
            switch (e.getLocalName()) {
                case "ID" -> d.defId = text(e);
                case "Version" -> d.defVersion = text(e);
                case "OperationsSegment" -> d.segments.add(readSegment(d, e, bills));
                default -> warn(d, "OperationsDefinition/" + e.getLocalName());
            }
        }
        sortByOrder(d);
        return d;
    }

    private static Node readSegment(Definition d, Element seg, Map<String, List<Element>> bills) {
        Node s = new Node(Node.Kind.SEGMENT);
        for (Element e : elements(seg)) {
            String n = e.getLocalName();
            if (SEG_SIMPLE.contains(n)) {
                s.put(n, text(e));
            } else if ("ParameterSpecification".equals(n)) {
                readParameter(d, s, e);
            } else {
                warn(d, "OperationsSegment/" + n);
            }
        }
        if (s.has("OperationsMaterialBillID")) {
            String billId = s.fields.get("OperationsMaterialBillID").get(0);
            List<Element> items = bills.get(billId);
            if (items == null) {
                warn(d, "OperationsMaterialBillID " + billId + " has no OperationsMaterialBill");
            } else {
                for (Element ie : items) {
                    s.children.add(readItem(d, ie));
                }
            }
            if (s.fields.get("OperationsMaterialBillID").size() > 1) {
                warn(d, "second OperationsMaterialBillID of a segment");
            }
        }
        return s;
    }

    private static void readParameter(Definition d, Node s, Element p) {
        String id = null;
        String value = null;
        String dataType = null;
        String uom = null;
        for (Element c : elements(p)) {
            switch (c.getLocalName()) {
                case "ID" -> id = text(c);
                case "Value" -> {
                    if (value != null) {
                        warn(d, "second Value of a ParameterSpecification");
                        continue;
                    }
                    for (Element v : elements(c)) {
                        switch (v.getLocalName()) {
                            case "ValueString" -> value = text(v);
                            case "DataType" -> dataType = text(v);
                            case "UnitOfMeasure" -> uom = text(v);
                            default -> warn(d, "ParameterSpecification/Value/" + v.getLocalName());
                        }
                    }
                }
                default -> warn(d, "ParameterSpecification/" + c.getLocalName());
            }
        }
        if (id == null || value == null) {
            warn(d, "ParameterSpecification without ID or ValueString");
            return;
        }
        if (ORDER_PARAMETER.equals(id)) {
            s.order = value;
            return;
        }
        s.set("Param[" + id + "]", value);
        if (dataType != null) {
            s.set("Param[" + id + "]/DataType", dataType);
        }
        if (uom != null) {
            s.set("Param[" + id + "]/UnitOfMeasure", uom);
        }
    }

    private static Node readItem(Definition d, Element ie) {
        Node it = new Node(Node.Kind.ITEM);
        boolean quantitySeen = false;
        for (Element e : elements(ie)) {
            String n = e.getLocalName();
            if (ITEM_SIMPLE.contains(n)) {
                it.put(n, text(e));
            } else if ("Quantity".equals(n) && !quantitySeen) {
                quantitySeen = true;
                for (Element q : elements(e)) {
                    switch (q.getLocalName()) {
                        case "QuantityString" -> it.set("Quantity", text(q));
                        case "DataType" -> it.set("Quantity/DataType", text(q));
                        case "UnitOfMeasure" -> it.set("Quantity/UnitOfMeasure", text(q));
                        default -> warn(d, "Quantity/" + q.getLocalName());
                    }
                }
            } else {
                warn(d, "OperationsMaterialBillItem/" + n + (n.equals("Quantity") ? " (second)" : ""));
            }
        }
        return it;
    }

    /** Segments are ordered by their Indice value when every segment has one, else by document order. */
    private static void sortByOrder(Definition d) {
        for (Node s : d.segments) {
            if (s.order == null) {
                return;
            }
        }
        d.segments.sort(Comparator.comparing((Node s) -> s.order, B2mmlReader::compareOrder));
    }

    static int compareOrder(String a, String b) {
        boolean na = a.matches("[0-9]{1,30}");
        boolean nb = b.matches("[0-9]{1,30}");
        if (na && nb) {
            return new BigInteger(a).compareTo(new BigInteger(b));
        }
        if (na != nb) {
            return na ? -1 : 1;
        }
        return a.compareTo(b);
    }
}
