package dev.mfgdiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mfgdiff.ChangeGenerator.Cat;
import dev.mfgdiff.ChangeGenerator.Generated;
import dev.mfgdiff.ChangeGenerator.Level;
import dev.mfgdiff.ChangeGenerator.Op;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The change generator applies exactly one kind of operation once; the tool must report exactly that.
 */
class GeneratorTest {

    static DiffResult compare(Generated g) throws Exception {
        byte[] xa = B2mmlWriter.write(g.a(), List.of()).getBytes(StandardCharsets.UTF_8);
        byte[] xb = B2mmlWriter.write(g.b(), List.of()).getBytes(StandardCharsets.UTF_8);
        return Differ.diff(B2mmlReader.read(xa), B2mmlReader.read(xb));
    }

    private static void assertExact(Generated g) throws Exception {
        DiffResult r = compare(g);
        for (Level l : Level.values()) {
            for (Cat c : Cat.values()) {
                assertEquals(g.truth().get(l, c), Metrics.predicted(g, r, l, c),
                        l + " " + c + " after " + g.ops());
            }
        }
    }

    private static void oneOperation(Op op) throws Exception {
        for (int seed = 1; seed <= 12; seed++) {
            Definition base = SampleData.base(seed, 14);
            Generated g = ChangeGenerator.applyOne(base, op, seed);
            assertEquals(1, g.ops().size());
            assertEquals(op, g.ops().get(0).op());
            assertExact(g);
        }
    }

    @Test
    void segmentInsert() throws Exception {
        oneOperation(Op.SEG_INSERT);
    }

    @Test
    void segmentRemove() throws Exception {
        oneOperation(Op.SEG_REMOVE);
    }

    @Test
    void segmentUpdate() throws Exception {
        oneOperation(Op.SEG_UPDATE);
    }

    @Test
    void segmentMove() throws Exception {
        oneOperation(Op.SEG_MOVE);
    }

    @Test
    void segmentRenumber() throws Exception {
        oneOperation(Op.SEG_RENUMBER);
    }

    @Test
    void itemInsert() throws Exception {
        oneOperation(Op.ITEM_INSERT);
    }

    @Test
    void itemInsertDuplicateRow() throws Exception {
        oneOperation(Op.ITEM_INSERT_DUP);
    }

    @Test
    void itemRemove() throws Exception {
        oneOperation(Op.ITEM_REMOVE);
    }

    @Test
    void itemUpdate() throws Exception {
        oneOperation(Op.ITEM_UPDATE);
    }

    @Test
    void theTruthOfASingleOperationHasTheExpectedShape() throws Exception {
        Generated g = ChangeGenerator.applyOne(SampleData.base(3, 14), Op.SEG_MOVE, 3);
        assertEquals(1, g.truth().get(Level.SEGMENT, Cat.MOVED).size());
        assertEquals(0, g.truth().get(Level.SEGMENT, Cat.CHANGED).size());
        assertEquals(14, g.truth().get(Level.SEGMENT, Cat.SAME).size());
        Generated r = ChangeGenerator.applyOne(SampleData.base(3, 14), Op.SEG_RENUMBER, 3);
        assertEquals(1, r.truth().get(Level.SEGMENT, Cat.CHANGED).size());
        assertEquals(13, r.truth().get(Level.SEGMENT, Cat.SAME).size());
    }

    @Test
    void sameSeedGivesTheSameOutputAndAnotherSeedDoesNot() {
        Generated g1 = ChangeGenerator.generate(SampleData.base(5, 20), 0.1, 77);
        Generated g2 = ChangeGenerator.generate(SampleData.base(5, 20), 0.1, 77);
        Generated g3 = ChangeGenerator.generate(SampleData.base(5, 20), 0.1, 78);
        String x1 = B2mmlWriter.write(g1.b(), List.of());
        assertEquals(x1, B2mmlWriter.write(g2.b(), List.of()));
        assertEquals(g1.ops(), g2.ops());
        assertFalse(x1.equals(B2mmlWriter.write(g3.b(), List.of())));
    }

    @Test
    void theShareOfChangedNodesIsAParameter() {
        Definition base = SampleData.base(5, 30);
        int nodes = base.segments.size() + base.itemCount();
        for (double p : new double[] {0.05, 0.10, 0.20}) {
            Generated g = ChangeGenerator.generate(base, p, 1);
            assertEquals(Math.max(1, Math.round(p * nodes)), g.ops().size(), "ratio " + p);
        }
    }

    @Test
    void theBaseRevisionIsNotModified() {
        Definition base = SampleData.base(5, 20);
        String before = B2mmlWriter.write(base, List.of());
        ChangeGenerator.generate(base, 0.2, 9);
        assertEquals(before, B2mmlWriter.write(base, List.of()));
    }

    @Test
    void aMixedRunHasBothSidesParseable() throws Exception {
        Generated g = ChangeGenerator.generate(SampleData.base(2, 30), 0.1, 2102);
        DiffResult r = compare(g);
        assertTrue(r.segments().size() >= g.b().segments.size());
    }
}
