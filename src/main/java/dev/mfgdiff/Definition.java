package dev.mfgdiff;

import java.util.ArrayList;
import java.util.List;

/** One revision: an OperationsDefinition with its segments and the material bill items under each. */
public final class Definition {
    public String infoId = "INFO";
    public String defId = "DEF";
    public String defVersion;
    public final List<Node> segments = new ArrayList<>();
    /** Elements that were present but are not compared, and other reader notices. */
    public final List<String> warnings = new ArrayList<>();

    public Definition deepCopy() {
        Definition d = new Definition();
        d.infoId = infoId;
        d.defId = defId;
        d.defVersion = defVersion;
        for (Node s : segments) {
            d.segments.add(s.deepCopy());
        }
        d.warnings.addAll(warnings);
        return d;
    }

    public int itemCount() {
        int n = 0;
        for (Node s : segments) {
            n += s.children.size();
        }
        return n;
    }
}
