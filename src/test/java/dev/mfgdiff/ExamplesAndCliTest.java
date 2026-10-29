package dev.mfgdiff;

import static dev.mfgdiff.TestDefs.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mfgdiff.DiffResult.Status;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExamplesAndCliTest {

    private static DiffResult example() throws Exception {
        return Differ.diff(B2mmlReader.read(Path.of("examples/revA.xml")), B2mmlReader.read(Path.of("examples/revB.xml")));
    }

    @Test
    void sampleRevisionsContainEveryCategory() throws Exception {
        DiffResult r = example();
        assertEquals(12, r.a().segments.size());
        assertEquals(12, r.b().segments.size());
        assertEquals(1, segments(r, Status.REMOVED), "OP-070 is gone");
        assertEquals(1, segments(r, Status.ADDED), "OP-065 is new");
        assertEquals(7, rowB(r, 7).b(), "OP-065 sits after OP-060");
        assertEquals(Status.ADDED, rowB(r, 7).status());
        assertEquals(1, rowB(r, 7).items().size());
        assertEquals(3, segments(r, Status.CHANGED), "OP-030 duration, OP-080 description, OP-120 renumbered");
        assertEquals(8, segments(r, Status.SAME));
        assertEquals(1, moved(r), "OP-080 moved ahead of OP-050");
        assertEquals(2, items(r, Status.CHANGED));
        assertEquals(1, items(r, Status.REMOVED), "cable clip");
        assertEquals(3, items(r, Status.ADDED), "second screw row in OP-060, the clay of OP-065 and the label in OP-100");
        assertTrue(r.a().warnings.isEmpty() && r.b().warnings.isEmpty());
        var renumbered = r.segments().stream().filter(s -> s.a() == 11).findFirst().orElseThrow();
        assertEquals(11, renumbered.b());
        assertEquals("ID", renumbered.changes().get(0).field());
        assertEquals(1, renumbered.changes().size());
        // OP-100 lists the same rows in another order and has one added row: only that one row is a difference
        var op100 = r.segments().stream().filter(s -> s.a() == 9).findFirst().orElseThrow();
        assertEquals(Status.SAME, op100.status());
        assertEquals(4, op100.items().stream().filter(i -> i.status() == Status.SAME).count());
        assertEquals(1, op100.items().stream().filter(i -> i.status() == Status.ADDED).count());
    }

    @Test
    void anAddedStepThatLooksLikeARemovedOneIsNotPairedAtTheDefaultThreshold() throws Exception {
        Definition a = B2mmlReader.read(Path.of("examples/revA.xml"));
        Definition b = B2mmlReader.read(Path.of("examples/revB.xml"));
        DiffResult r = Differ.diff(a, b);
        assertTrue(r.segments().stream().noneMatch(s -> s.a() == 6 && s.b() >= 0), "OP-070 (Test motor) stays removed");
        assertTrue(r.segments().stream().noneMatch(s -> s.b() == 7 && s.a() >= 0), "OP-065 (Balance rotor) stays added");
        // with the threshold of xmldiff's own default the two are read as one changed step
        DiffResult loose = Differ.diff(a, b, new NodeMatcher(0.5));
        assertEquals(6, loose.segments().stream().filter(s -> s.b() == 7).findFirst().orElseThrow().a());
    }

    @Test
    void writerAndReaderRoundTrip() throws Exception {
        Definition a = B2mmlReader.read(Path.of("examples/revA.xml"));
        String xml = B2mmlWriter.write(a, List.of());
        Definition again = B2mmlReader.read(xml.getBytes(StandardCharsets.UTF_8));
        assertEquals(a.segments.size(), again.segments.size());
        for (int i = 0; i < a.segments.size(); i++) {
            assertTrue(a.segments.get(i).sameFields(again.segments.get(i)));
            assertEquals(a.segments.get(i).order, again.segments.get(i).order);
            assertEquals(a.segments.get(i).children.size(), again.segments.get(i).children.size());
            for (int k = 0; k < a.segments.get(i).children.size(); k++) {
                assertTrue(a.segments.get(i).children.get(k).sameFields(again.segments.get(i).children.get(k)));
            }
        }
    }

    @Test
    void elementsThatAreNotComparedAreReportedNotDropped() throws Exception {
        String xml = B2mmlWriter.write(B2mmlReader.read(Path.of("examples/revA.xml")), List.of())
                .replaceFirst("<Duration>PT3M</Duration>", "<Duration>PT3M</Duration><BillOfMaterialsID>X</BillOfMaterialsID><HierarchyScope><EquipmentID>E</EquipmentID></HierarchyScope>");
        Definition d = B2mmlReader.read(xml.getBytes(StandardCharsets.UTF_8));
        assertTrue(d.warnings.stream().anyMatch(w -> w.contains("HierarchyScope")), d.warnings.toString());
    }

    private static String[] run(String... args) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ByteArrayOutputStream e = new ByteArrayOutputStream();
        int code = Cli.run(args, new PrintStream(o, true, StandardCharsets.UTF_8), new PrintStream(e, true, StandardCharsets.UTF_8));
        return new String[] {Integer.toString(code), o.toString(StandardCharsets.UTF_8), e.toString(StandardCharsets.UTF_8)};
    }

    @Test
    void cliPrintsOneTablePerCategory() {
        String[] r = run("examples/revA.xml", "examples/revB.xml", "--validate");
        assertEquals("0", r[0], r[2]);
        String out = r[1];
        for (String h : new String[] {"[=] Same", "[≠] Changed", "[A] Only in A (removed)", "[B] Only in B (added)", "[↕] Moved"}) {
            assertTrue(out.contains(h), h);
        }
        assertTrue(out.contains("ID: OP-120 -> OP-130"));
        assertTrue(out.contains("Duration: PT6M -> PT7M"));
        assertTrue(out.contains("Quantity: 4 -> 6"));
    }

    @Test
    void cliFilterShowsOnlyTheRequestedCategory() {
        String[] r = run("examples/revA.xml", "examples/revB.xml", "--filter", "Removed");
        assertEquals("0", r[0]);
        assertTrue(r[1].contains("OP-070"));
        assertTrue(!r[1].contains("OP-130"), "the renumbered segment is a change, not a removal");
    }

    @Test
    void cliFailsWithMessageOnBadInput() {
        assertEquals("2", run("examples/revA.xml")[0]);
        assertEquals("2", run("examples/revA.xml", "no-such-file.xml")[0]);
        assertEquals("2", run("examples/revA.xml", "examples/revB.xml", "--filter", "Nope")[0]);
    }
}
