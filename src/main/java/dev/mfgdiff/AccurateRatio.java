package dev.mfgdiff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of Python difflib.SequenceMatcher.ratio() with the default arguments (isjunk=None,
 * autojunk=True) from CPython v3.12.0 Lib/difflib.py (PSF License; see NOTICE): the Ratcliff/Obershelp
 * method. The longest matching block of the two sequences is found, the parts to its left and to its
 * right are searched the same way, and ratio = 2.0 * (total size of the blocks) / (total length).
 * This is what xmldiff uses for ratio_mode "accurate".
 *
 * <p>Mapping: {@code chainB} = __chain_b (L266, without the isjunk part, which is unused with isjunk=None),
 * {@link #findLongestMatch} = find_longest_match (L305), {@link #matchedSize} = get_matching_blocks (L421,
 * with the queue and the merge of adjacent blocks), {@link #ratio} = ratio (L597).
 */
public final class AccurateRatio {
    private AccurateRatio() {
    }

    private record Match(int a, int b, int size) {
    }

    private static final class Seq {
        final int[] a;
        final int[] b;
        final Map<Integer, List<Integer>> b2j = new HashMap<>();

        Seq(int[] a, int[] b) {
            this.a = a;
            this.b = b;
            chainB();
        }

        // __chain_b: index of every element in b; elements that occur more than n/100 + 1 times are
        // dropped when b has 200 or more elements (autojunk "popular" elements).
        private void chainB() {
            for (int i = 0; i < b.length; i++) {
                b2j.computeIfAbsent(b[i], k -> new ArrayList<>()).add(i);
            }
            int n = b.length;
            if (n >= 200) {
                int ntest = n / 100 + 1;
                Set<Integer> popular = new HashSet<>();
                for (Map.Entry<Integer, List<Integer>> e : b2j.entrySet()) {
                    if (e.getValue().size() > ntest) {
                        popular.add(e.getKey());
                    }
                }
                for (int p : popular) {
                    b2j.remove(p);
                }
            }
        }

        Match findLongestMatch(int alo, int ahi, int blo, int bhi) {
            int besti = alo;
            int bestj = blo;
            int bestsize = 0;
            Map<Integer, Integer> j2len = new HashMap<>();
            List<Integer> nothing = List.of();
            for (int i = alo; i < ahi; i++) {
                Map<Integer, Integer> newj2len = new HashMap<>();
                for (int j : b2j.getOrDefault(a[i], nothing)) {
                    if (j < blo) {
                        continue;
                    }
                    if (j >= bhi) {
                        break;
                    }
                    int k = j2len.getOrDefault(j - 1, 0) + 1;
                    newj2len.put(j, k);
                    if (k > bestsize) {
                        besti = i - k + 1;
                        bestj = j - k + 1;
                        bestsize = k;
                    }
                }
                j2len = newj2len;
            }
            // Extend the best match by elements that were left out of b2j (popular elements). With
            // isjunk=None there are no junk elements, so the two "suck up junk" loops of the original
            // do nothing and are not ported.
            while (besti > alo && bestj > blo && a[besti - 1] == b[bestj - 1]) {
                besti--;
                bestj--;
                bestsize++;
            }
            while (besti + bestsize < ahi && bestj + bestsize < bhi && a[besti + bestsize] == b[bestj + bestsize]) {
                bestsize++;
            }
            return new Match(besti, bestj, bestsize);
        }

        // get_matching_blocks: the sum of the sizes of the blocks. Adjacent blocks are merged in the
        // original; merging does not change the sum, so it is not done here.
        int matchedSize() {
            int total = 0;
            List<int[]> queue = new ArrayList<>();
            queue.add(new int[] {0, a.length, 0, b.length});
            while (!queue.isEmpty()) {
                int[] q = queue.remove(queue.size() - 1);
                Match m = findLongestMatch(q[0], q[1], q[2], q[3]);
                if (m.size() > 0) {
                    total += m.size();
                    if (q[0] < m.a() && q[2] < m.b()) {
                        queue.add(new int[] {q[0], m.a(), q[2], m.b()});
                    }
                    if (m.a() + m.size() < q[1] && m.b() + m.size() < q[3]) {
                        queue.add(new int[] {m.a() + m.size(), q[1], m.b() + m.size(), q[3]});
                    }
                }
            }
            return total;
        }
    }

    /** ratio of SequenceMatcher(None, a, b): a is the left sequence, b the right one. */
    public static double ratio(String a, String b) {
        int[] sa = a.codePoints().toArray();
        int[] sb = b.codePoints().toArray();
        int matches = new Seq(sa, sb).matchedSize();
        int length = sa.length + sb.length;
        if (length > 0) {
            return 2.0 * matches / length;
        }
        return 1.0;
    }
}
