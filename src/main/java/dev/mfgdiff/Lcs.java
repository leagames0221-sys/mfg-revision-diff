package dev.mfgdiff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Longest common subsequence by the Myers O(ND) algorithm with the per-diagonal history technique,
 * ported from xmldiff utils.longest_common_subsequence (MIT License; see NOTICE). Returns index pairs
 * (index in left, index in right) in increasing order.
 */
public final class Lcs {
    private Lcs() {
    }

    private record State(int x, List<int[]> history) {
    }

    public static <T> List<int[]> compute(List<T> left, List<T> right, BiPredicate<T, T> eq) {
        int start = 0;
        int lend = left.size();
        int rend = right.size();
        int lslen = lend;
        int rslen = rend;

        while (start < lend && start < rend && eq.test(left.get(start), right.get(start))) {
            start++;
        }
        while (start < lend && start < rend && eq.test(left.get(lend - 1), right.get(rend - 1))) {
            lend--;
            rend--;
        }

        List<T> l = left.subList(start, lend);
        List<T> r = right.subList(start, rend);
        int lmax = l.size();
        int rmax = r.size();
        Map<Integer, State> furthest = new HashMap<>();
        furthest.put(1, new State(0, new ArrayList<>()));

        if (lmax + rmax == 0) {
            List<int[]> all = new ArrayList<>();
            for (int i = 0; i < lslen; i++) {
                all.add(new int[] {i, i});
            }
            return all;
        }

        for (int d = 0; d <= lmax + rmax; d++) {
            for (int k = -d; k <= d; k += 2) {
                State from;
                int x;
                if (k == -d || (k != d && furthest.get(k - 1).x() < furthest.get(k + 1).x())) {
                    from = furthest.get(k + 1);
                    x = from.x();
                } else {
                    from = furthest.get(k - 1);
                    x = from.x() + 1;
                }
                List<int[]> history = new ArrayList<>(from.history());
                int y = x - k;
                while (x < lmax && y < rmax && eq.test(l.get(x), r.get(y))) {
                    history.add(new int[] {x + start, y + start});
                    x++;
                    y++;
                }
                if (x >= lmax && y >= rmax) {
                    List<int[]> result = new ArrayList<>();
                    for (int e = 0; e < start; e++) {
                        result.add(new int[] {e, e});
                    }
                    result.addAll(history);
                    for (int i = lend, j = rend; i < lslen && j < rslen; i++, j++) {
                        result.add(new int[] {i, j});
                    }
                    return result;
                }
                furthest.put(k, new State(x, history));
            }
        }
        throw new IllegalStateException("unreachable: LCS search ended without a result");
    }
}
