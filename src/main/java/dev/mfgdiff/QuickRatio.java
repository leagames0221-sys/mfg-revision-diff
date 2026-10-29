package dev.mfgdiff;

import java.util.HashMap;
import java.util.Map;

/**
 * Port of Python difflib.SequenceMatcher.quick_ratio (PSF License; see NOTICE). The matches are the
 * size of the multiset intersection of the two sequences, ratio = 2.0 * matches / total length,
 * and 1.0 when both sequences are empty (difflib._calculate_ratio).
 */
public final class QuickRatio {
    private QuickRatio() {
    }

    public static double ratio(String a, String b) {
        int[] sa = a.codePoints().toArray();
        int[] sb = b.codePoints().toArray();
        Map<Integer, Integer> fullbcount = new HashMap<>();
        for (int elt : sb) {
            fullbcount.merge(elt, 1, Integer::sum);
        }
        Map<Integer, Integer> avail = new HashMap<>();
        int matches = 0;
        for (int elt : sa) {
            int numb;
            if (avail.containsKey(elt)) {
                numb = avail.get(elt);
            } else {
                numb = fullbcount.getOrDefault(elt, 0);
            }
            avail.put(elt, numb - 1);
            if (numb > 0) {
                matches++;
            }
        }
        int length = sa.length + sb.length;
        if (length > 0) {
            return 2.0 * matches / length;
        }
        return 1.0;
    }
}
