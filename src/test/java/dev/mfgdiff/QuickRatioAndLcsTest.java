package dev.mfgdiff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class QuickRatioAndLcsTest {

    // Python documentation of difflib: SequenceMatcher(None, "abcd", "bcde") has quick_ratio() 0.75.
    @Test
    void quickRatioMatchesThePythonDocumentationExample() {
        assertEquals(0.75, QuickRatio.ratio("abcd", "bcde"), 1e-12);
    }

    @Test
    void quickRatioIsOneForEqualAndZeroForDisjointAndOneForTwoEmptyStrings() {
        assertEquals(1.0, QuickRatio.ratio("same text", "same text"), 1e-12);
        assertEquals(0.0, QuickRatio.ratio("abc", "xyz"), 1e-12);
        assertEquals(1.0, QuickRatio.ratio("", ""), 1e-12);
        assertEquals(0.0, QuickRatio.ratio("abc", ""), 1e-12);
    }

    // multiset intersection of "Hello" and "World" is {l, o}: 2 * 2 / 10
    @Test
    void quickRatioCountsTheMultisetIntersection() {
        assertEquals(0.4, QuickRatio.ratio("Hello", "World"), 1e-12);
        assertEquals(0.4, QuickRatio.ratio("World", "Hello"), 1e-12);
    }

    // Python documentation of difflib: ratio() of "abcd" and "bcde" is 0.75, and 0.866 for the Thread example
    @Test
    void accurateRatioMatchesThePythonDocumentationExamples() {
        assertEquals(0.75, AccurateRatio.ratio("abcd", "bcde"), 1e-12);
        assertEquals(0.866, AccurateRatio.ratio("private Thread currentThread;", "private volatile Thread currentThread;"), 0.0005);
    }

    @Test
    void accurateRatioKnowsOrderWhereQuickRatioDoesNot() {
        // same letters, reversed: quick ratio counts letters only; the longest matching block is 1 letter
        assertEquals(1.0, QuickRatio.ratio("abcd", "dcba"), 1e-12);
        assertEquals(0.25, AccurateRatio.ratio("abcd", "dcba"), 1e-12);
        assertEquals(1.0, AccurateRatio.ratio("same text", "same text"), 1e-12);
        assertEquals(1.0, AccurateRatio.ratio("", ""), 1e-12);
        assertEquals(0.0, AccurateRatio.ratio("abc", "xyz"), 1e-12);
    }

    @Test
    void accurateRatioBelowOrEqualQuickRatioAndPopularElementsStillMatchWhenEqual() {
        Random rnd = new Random(7);
        for (int i = 0; i < 200; i++) {
            StringBuilder x = new StringBuilder();
            StringBuilder y = new StringBuilder();
            for (int k = 0; k < 20; k++) {
                x.append((char) ('a' + rnd.nextInt(5)));
                y.append((char) ('a' + rnd.nextInt(5)));
            }
            assertEquals(true, AccurateRatio.ratio(x.toString(), y.toString()) <= QuickRatio.ratio(x.toString(), y.toString()) + 1e-12);
        }
        // 250 equal characters: the character is "popular" (autojunk) and is found by the extension step
        String longText = "a".repeat(250);
        assertEquals(1.0, AccurateRatio.ratio(longText, longText), 1e-12);
    }

    private static int lcsLength(int[] a, int[] b) {
        int[][] d = new int[a.length + 1][b.length + 1];
        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                d[i][j] = a[i - 1] == b[j - 1] ? d[i - 1][j - 1] + 1 : Math.max(d[i - 1][j], d[i][j - 1]);
            }
        }
        return d[a.length][b.length];
    }

    @Test
    void lcsHasTheOptimalLengthAndIncreasingMatchingIndices() {
        Random rnd = new Random(42);
        for (int round = 0; round < 300; round++) {
            int n = rnd.nextInt(12);
            int m = rnd.nextInt(12);
            int[] a = new int[n];
            int[] b = new int[m];
            List<Integer> la = new ArrayList<>();
            List<Integer> lb = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                a[i] = rnd.nextInt(5);
                la.add(a[i]);
            }
            for (int i = 0; i < m; i++) {
                b[i] = rnd.nextInt(5);
                lb.add(b[i]);
            }
            List<int[]> lcs = Lcs.compute(la, lb, Integer::equals);
            assertEquals(lcsLength(a, b), lcs.size(), la + " vs " + lb);
            int px = -1;
            int py = -1;
            for (int[] p : lcs) {
                assertEquals(a[p[0]], b[p[1]]);
                assertEquals(true, p[0] > px && p[1] > py);
                px = p[0];
                py = p[1];
            }
        }
    }

    @Test
    void lcsOfAMovedElementLeavesOnlyThatElementOut() {
        List<Integer> a = List.of(1, 2, 3, 4, 5);
        List<Integer> b = List.of(1, 3, 4, 2, 5);
        assertEquals(4, Lcs.compute(a, b, Integer::equals).size());
    }
}
