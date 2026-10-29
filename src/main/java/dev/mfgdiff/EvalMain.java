package dev.mfgdiff;

import dev.mfgdiff.ChangeGenerator.Cat;
import dev.mfgdiff.ChangeGenerator.Generated;
import dev.mfgdiff.ChangeGenerator.Level;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.xml.sax.SAXException;

/**
 * Runs the measurement of docs/validation/metrics.md and writes docs/validation/results.md.
 * Usage: java -cp target/classes dev.mfgdiff.EvalMain docs/validation/results.md
 *
 * <p>Seeds 1 to 20 are the tuning seeds (the grid of ratio mode and F; the default is chosen by the rule in
 * metrics.md section 7); seeds 21 to 40 are the reporting seeds and are used only for the chosen default.
 */
public final class EvalMain {
    static final int[] PERCENTS = {5, 10, 20};
    static final int SEGMENTS = 30;
    static final int TUNE_FROM = 1;
    static final int TUNE_TO = 20;
    static final int REPORT_FROM = 21;
    static final int REPORT_TO = 40;
    static final double[] F_GRID = {0.5, 0.6, 0.7, 0.8, 0.9};
    static final RatioMode[] MODES = {RatioMode.FAST, RatioMode.ACCURATE};
    static final Cat[] SEG_CATS = {Cat.CHANGED, Cat.ADDED, Cat.REMOVED, Cat.MOVED};

    private EvalMain() {
    }

    /** One generated pair of revisions, already written, schema-checked and read back. */
    record Prepared(int percent, int seed, Generated g, Definition ra, Definition rb) {
    }

    record Run(Prepared p, DiffResult r, List<String> mismatches) {
    }

    record Setting(RatioMode mode, double f) {
        NodeMatcher matcher() {
            return new NodeMatcher(f, mode);
        }

        String label() {
            return mode.label() + ", F = " + String.format(Locale.ROOT, "%.1f", f);
        }
    }

    static Prepared prepare(int percent, int seed) throws IOException, SAXException {
        Definition base = SampleData.base(seed, SEGMENTS);
        Generated g = ChangeGenerator.generate(base, percent / 100.0, seed * 1000L + percent);
        // The tool sees only the two files, as a user would: write, validate against the official schema, read.
        byte[] xa = B2mmlWriter.write(g.a(), List.of()).getBytes(StandardCharsets.UTF_8);
        byte[] xb = B2mmlWriter.write(g.b(), List.of()).getBytes(StandardCharsets.UTF_8);
        SecureXml.validate(SecureXml.parse(xa));
        SecureXml.validate(SecureXml.parse(xb));
        Definition ra = B2mmlReader.read(xa);
        Definition rb = B2mmlReader.read(xb);
        if (ra.segments.size() != g.a().segments.size() || rb.segments.size() != g.b().segments.size()) {
            throw new IllegalStateException("round trip changed the number of segments");
        }
        return new Prepared(percent, seed, g, ra, rb);
    }

    static List<Prepared> prepareAll(int from, int to) throws IOException, SAXException {
        List<Prepared> out = new ArrayList<>();
        for (int percent : PERCENTS) {
            for (int seed = from; seed <= to; seed++) {
                out.add(prepare(percent, seed));
            }
        }
        return out;
    }

    static Run run(Prepared p, Setting s) {
        DiffResult r = Differ.diff(p.ra(), p.rb(), s.matcher());
        List<String> mism = Metrics.add(new Metrics.Table(), p.g(), r);
        return new Run(p, r, mism);
    }

    static String pct(double v) {
        return Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.1f%%", v * 100);
    }

    static long segmentErrors(Metrics.Table t) {
        long n = 0;
        for (Cat c : SEG_CATS) {
            n += t.get(Level.SEGMENT, c).fp + t.get(Level.SEGMENT, c).fn;
        }
        return n;
    }

    public static void main(String[] args) throws Exception {
        String md = build();
        if (args.length > 0) {
            Files.writeString(Path.of(args[0]), md, StandardCharsets.UTF_8);
        } else {
            System.out.print(md);
        }
    }

    static String build() throws IOException, SAXException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Validation results\n\n");
        sb.append("Produced by `java -cp target/classes dev.mfgdiff.EvalMain docs/validation/results.md`. ");
        sb.append("The formulas, the setup and the rule that chooses the default are in [metrics.md](metrics.md). ");
        sb.append("No target value was set before measuring; the numbers below are what the program printed.\n\n");
        sb.append("Setup: base revision = `SampleData.base(seed, ").append(SEGMENTS).append(")`; changes = ")
                .append("`ChangeGenerator.generate(base, percent/100, seed*1000+percent)`; both revisions are written as B2MML, ")
                .append("checked against the official schema, parsed again, and only then compared. ")
                .append("Seeds ").append(TUNE_FROM).append(" to ").append(TUNE_TO).append(" are the **tuning seeds** (part 1). ")
                .append("Seeds ").append(REPORT_FROM).append(" to ").append(REPORT_TO)
                .append(" are the **held-out seeds**: they were not used to choose anything (part 2).\n\n");

        // ---- part 1: grid on the tuning seeds
        List<Prepared> tune = prepareAll(TUNE_FROM, TUNE_TO);
        Map<Setting, Map<Integer, Metrics.Table>> grid = new HashMap<>();
        List<Setting> settings = new ArrayList<>();
        for (RatioMode m : MODES) {
            for (double f : F_GRID) {
                Setting s = new Setting(m, f);
                settings.add(s);
                Map<Integer, Metrics.Table> perShare = new TreeMap<>();
                for (int percent : PERCENTS) {
                    perShare.put(percent, new Metrics.Table());
                }
                for (Prepared p : tune) {
                    Metrics.add(perShare.get(p.percent()), p.g(), Differ.diff(p.ra(), p.rb(), s.matcher()));
                }
                grid.put(s, perShare);
            }
        }
        sb.append("## Part 1. Grid on the tuning seeds ").append(TUNE_FROM).append(" to ").append(TUNE_TO).append("\n\n");
        sb.append("Score of a setting = segment-level errors (false positives + false negatives over changed, added, removed and moved), ")
                .append("summed over the three shares. Lowest score wins; ties go to the fast mode, then to the lower F (metrics.md section 7).\n\n");
        sb.append("| ratio mode | F | errors at 5% | errors at 10% | errors at 20% | score |\n|---|---:|---:|---:|---:|---:|\n");
        Setting best = null;
        long bestScore = Long.MAX_VALUE;
        for (Setting s : settings) {
            long total = 0;
            StringBuilder row = new StringBuilder();
            for (int percent : PERCENTS) {
                long e = segmentErrors(grid.get(s).get(percent));
                total += e;
                row.append(" ").append(e).append(" |");
            }
            sb.append("| ").append(s.mode().label()).append(" | ").append(String.format(Locale.ROOT, "%.1f", s.f())).append(" |")
                    .append(row).append(" ").append(total).append(" |\n");
            // settings are listed fast first, F ascending, so a strict < keeps the tie-break of the rule
            if (total < bestScore) {
                bestScore = total;
                best = s;
            }
        }
        sb.append("\nChosen by the rule: **").append(best.label()).append("** (score ").append(bestScore).append(").\n");
        boolean sameAsCode = best.mode() == NodeMatcher.DEFAULT_MODE && best.f() == NodeMatcher.DEFAULT_F;
        sb.append("The default in `NodeMatcher` is ").append(NodeMatcher.DEFAULT_MODE.label()).append(", F = ")
                .append(String.format(Locale.ROOT, "%.1f", NodeMatcher.DEFAULT_F)).append(": ")
                .append(sameAsCode ? "it equals the chosen setting.\n\n" : "**it differs from the chosen setting; fix the code.**\n\n");

        sb.append("Segment-level precision / recall on the tuning seeds, per setting and share:\n\n");
        sb.append("| ratio mode | F | share | changed P / R | added P / R | removed P / R | moved P / R |\n|---|---:|---:|---|---|---|---|\n");
        for (Setting s : settings) {
            for (int percent : PERCENTS) {
                sb.append("| ").append(s.mode().label()).append(" | ").append(String.format(Locale.ROOT, "%.1f", s.f()))
                        .append(" | ").append(percent).append("% |");
                for (Cat c : SEG_CATS) {
                    Metrics.Tally t = grid.get(s).get(percent).get(Level.SEGMENT, c);
                    sb.append(' ').append(pct(t.precision())).append(" / ").append(pct(t.recall())).append(" |");
                }
                sb.append('\n');
            }
        }

        // ---- part 2: held-out seeds, chosen default only
        List<Prepared> held = prepareAll(REPORT_FROM, REPORT_TO);
        sb.append("\n## Part 2. Held-out seeds ").append(REPORT_FROM).append(" to ").append(REPORT_TO).append(": ")
                .append(best.label()).append("\n\n");
        sb.append("### Per category (sum over ").append(REPORT_TO - REPORT_FROM + 1).append(" seeds)\n\n");
        sb.append("| changed share | level | category | applied (truth) | reported | true positives | precision | recall |\n");
        sb.append("|---|---|---|---:|---:|---:|---:|---:|\n");
        List<String> exactLines = new ArrayList<>();
        Map<String, int[]> kinds = new TreeMap<>();
        List<Run> misses = new ArrayList<>();
        Metrics.Table heldAll = new Metrics.Table();
        for (int pi = 0; pi < PERCENTS.length; pi++) {
            int percent = PERCENTS[pi];
            Metrics.Table table = new Metrics.Table();
            int exact = 0;
            int runs = 0;
            long ops = 0;
            for (Prepared p : held) {
                if (p.percent() != percent) {
                    continue;
                }
                Run run = new Run(p, Differ.diff(p.ra(), p.rb(), best.matcher()), null);
                List<String> mism = Metrics.add(table, p.g(), run.r());
                Metrics.add(heldAll, p.g(), run.r());
                for (String line : mism) {
                    kinds.computeIfAbsent(line.substring(0, line.indexOf(':')), k -> new int[PERCENTS.length])[pi]++;
                }
                ops += p.g().ops().size();
                runs++;
                if (mism.isEmpty()) {
                    exact++;
                } else {
                    misses.add(new Run(p, run.r(), mism));
                }
            }
            exactLines.add("- " + percent + "%: " + exact + " of " + runs + " runs reproduced the applied changes exactly in every category and level (" + ops + " operations applied in total).");
            for (Level l : Level.values()) {
                for (Cat c : Cat.values()) {
                    Metrics.Tally t = table.get(l, c);
                    sb.append("| ").append(percent).append("% | ").append(l.name().toLowerCase(Locale.ROOT)).append(" | ")
                            .append(c.name().toLowerCase(Locale.ROOT)).append(" | ").append(t.truth()).append(" | ")
                            .append(t.predicted()).append(" | ").append(t.tp).append(" | ").append(pct(t.precision()))
                            .append(" | ").append(pct(t.recall())).append(" |\n");
                }
            }
        }
        sb.append("\nprecision = true positives / reported; recall = true positives / applied (truth). `n/a` = the denominator is 0.\n\n");
        sb.append("### Exact runs\n\n");
        for (String l : exactLines) {
            sb.append(l).append('\n');
        }

        // reference: xmldiff's own default setting on the same held-out seeds (information only)
        Setting old = new Setting(RatioMode.FAST, 0.5);
        Metrics.Table oldAll = new Metrics.Table();
        for (Prepared p : held) {
            Metrics.add(oldAll, p.g(), Differ.diff(p.ra(), p.rb(), old.matcher()));
        }
        sb.append("\nFor reference, segment-level errors (false positives + false negatives, changed/added/removed/moved) over all held-out runs: ")
                .append(segmentErrors(heldAll)).append(" with ").append(best.label()).append(", ")
                .append(segmentErrors(oldAll)).append(" with ").append(old.label()).append(" (xmldiff's own default setting). ")
                .append("This comparison was not used to choose anything.\n");

        sb.append("\n### Kinds of difference (number of rows, all held-out runs)\n\n");
        sb.append("| kind | 5% | 10% | 20% |\n|---|---:|---:|---:|\n");
        for (Map.Entry<String, int[]> e : kinds.entrySet()) {
            sb.append("| ").append(e.getKey()).append(" | ").append(e.getValue()[0]).append(" | ").append(e.getValue()[1])
                    .append(" | ").append(e.getValue()[2]).append(" |\n");
        }
        if (kinds.isEmpty()) {
            sb.append("| (none) | 0 | 0 | 0 |\n");
        }

        sb.append("\n### Runs that did not match (the first 3 in order of share and seed)\n\n");
        int shown = 0;
        for (Run run : misses) {
            if (shown++ >= 3) {
                break;
            }
            sb.append("#### ").append(run.p().percent()).append("% changed, seed ").append(run.p().seed()).append("\n\n");
            sb.append("Differences between what the tool reported and what was applied (first ")
                    .append(Math.min(8, run.mismatches().size())).append(" of ").append(run.mismatches().size()).append("):\n\n");
            for (int i = 0; i < Math.min(8, run.mismatches().size()); i++) {
                sb.append("- ").append(run.mismatches().get(i)).append('\n');
            }
            sb.append("\nOperations applied in this run:\n\n");
            for (ChangeGenerator.Applied a : run.p().g().ops()) {
                sb.append("- ").append(a.op()).append(": ").append(a.description()).append('\n');
            }
            sb.append('\n');
        }
        if (shown == 0) {
            sb.append("Every held-out run matched exactly; there is nothing to show.\n\n");
        } else if (misses.size() < 3) {
            sb.append("Only ").append(misses.size()).append(" held-out runs did not match, so fewer than 3 are shown.\n");
        }
        return sb.toString();
    }
}
