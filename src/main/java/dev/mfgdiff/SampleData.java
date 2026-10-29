package dev.mfgdiff;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic synthetic base revision for the change generator and the measurements. All names are
 * invented; nothing here describes a real product.
 */
public final class SampleData {
    static final String[] VERBS = {"Cut", "Press", "Weld", "Paint", "Mount", "Wind", "Assemble", "Test", "Mold",
        "Attach", "Inspect", "Pack", "Drill", "Polish", "Solder", "Label"};
    static final String[] NOUNS = {"base plate", "motor", "blade hub", "guard", "housing", "cable", "switch",
        "bracket", "gearbox", "frame", "cover", "shaft"};
    static final String[] ADJ = {"Steel", "Rubber", "Copper", "Brass", "Nylon", "Zinc", "Aluminium", "ABS",
        "Ceramic", "Silicone"};
    static final String[] PARTS = {"screw", "washer", "bracket", "spacer", "clip", "bushing", "gasket", "plate",
        "wire", "tube", "pin", "nut"};
    static final String[] UOM = {"EA", "EA", "EA", "KG", "L", "M"};

    private SampleData() {
    }

    /** One catalogue entry the generator draws bill items from. */
    record Part(String key, String description, String uom) {
    }

    static List<Part> catalogue(Random rnd) {
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            String d = ADJ[rnd.nextInt(ADJ.length)] + " " + PARTS[rnd.nextInt(PARTS.length)] + " "
                    + (2 + rnd.nextInt(30)) + " mm";
            parts.add(new Part(String.format("P-%04d", 2001 + i), d, UOM[rnd.nextInt(UOM.length)]));
        }
        return parts;
    }

    static String quantity(Random rnd, String uom) {
        if (uom.equals("EA")) {
            return Integer.toString(1 + rnd.nextInt(12));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", 0.05 + rnd.nextInt(200) / 100.0);
    }

    static String description(Random rnd) {
        return VERBS[rnd.nextInt(VERBS.length)] + " " + NOUNS[rnd.nextInt(NOUNS.length)];
    }

    /** A base revision with the given number of segments, bill items 0..6 per segment, uids assigned. */
    public static Definition base(long seed, int segmentCount) {
        Random rnd = new Random(seed);
        List<Part> catalogue = catalogue(rnd);
        Definition d = new Definition();
        d.infoId = "SYN-INFO";
        d.defId = "SYN-ROUTE";
        d.defVersion = "A";
        long uid = 1;
        for (int i = 0; i < segmentCount; i++) {
            Node s = new Node(Node.Kind.SEGMENT);
            s.uid = uid++;
            String id = String.format("OP-%03d", (i + 1) * 10);
            s.set("ID", id);
            s.set("Description", description(rnd) + " " + (char) ('A' + rnd.nextInt(6)));
            s.set("OperationsType", "Production");
            s.set("Duration", "PT" + (1 + rnd.nextInt(9)) + "M");
            s.set("OperationsMaterialBillID", "MB-" + id);
            s.set("Param[Station]", String.format("ST-%02d", 1 + rnd.nextInt(20)));
            s.set("Param[Station]/DataType", "string");
            s.order = String.format("%04d", (i + 1) * 10);
            int n = rnd.nextInt(7);
            List<Part> pool = new ArrayList<>(catalogue);
            for (int k = 0; k < n; k++) {
                Part p = pool.remove(rnd.nextInt(pool.size()));
                s.children.add(item(rnd, p, uid++, id + "-L" + (s.children.size() + 1)));
                if (rnd.nextInt(100) < 6) {
                    s.children.add(item(rnd, p, uid++, id + "-L" + (s.children.size() + 1)));
                }
            }
            d.segments.add(s);
        }
        return d;
    }

    static Node item(Random rnd, Part p, long uid, String lineId) {
        Node it = new Node(Node.Kind.ITEM);
        it.uid = uid;
        it.set("ID", lineId);
        it.set("Description", p.description());
        it.set("MaterialSpecificationID", p.key());
        it.set("Quantity", quantity(rnd, p.uom()));
        it.set("Quantity/DataType", "decimal");
        it.set("Quantity/UnitOfMeasure", p.uom());
        return it;
    }
}
