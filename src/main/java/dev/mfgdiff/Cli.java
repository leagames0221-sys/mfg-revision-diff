package dev.mfgdiff;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.xml.sax.SAXException;

/** java -cp target/classes dev.mfgdiff.Cli revA.xml revB.xml [--filter Differences] [--validate] [--f 0.7] [--ratio fast|accurate] */
public final class Cli {
    private Cli() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        System.exit(run(args, out, err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        String a = null;
        String b = null;
        Report.Filter filter = Report.Filter.ALL;
        boolean validate = false;
        double f = NodeMatcher.DEFAULT_F;
        RatioMode mode = NodeMatcher.DEFAULT_MODE;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--filter" -> filter = Report.Filter.parse(args[++i]);
                    case "--validate" -> validate = true;
                    case "--f" -> f = Double.parseDouble(args[++i]);
                    case "--ratio" -> mode = RatioMode.parse(args[++i]);
                    default -> {
                        if (a == null) {
                            a = args[i];
                        } else if (b == null) {
                            b = args[i];
                        } else {
                            throw new IllegalArgumentException("unexpected argument " + args[i]);
                        }
                    }
                }
            }
            if (a == null || b == null) {
                throw new IllegalArgumentException("two files are required");
            }
        } catch (RuntimeException e) {
            err.println("error: " + e.getMessage());
            err.println("usage: java -cp target/classes dev.mfgdiff.Cli revA.xml revB.xml [--filter All|Differences|Added|Changes|Removed|Unchanged|Moved] [--validate] [--f 0.7] [--ratio fast|accurate]");
            return 2;
        }
        try {
            Definition da = load(Path.of(a), validate);
            Definition db = load(Path.of(b), validate);
            DiffResult r = Differ.diff(da, db, new NodeMatcher(f, mode));
            out.print(Report.render(r, a, b, filter));
            return 0;
        } catch (IOException | SAXException e) {
            err.println("error: " + e.getMessage());
            return 2;
        }
    }

    private static Definition load(Path p, boolean validate) throws IOException, SAXException {
        byte[] data;
        try (InputStream in = Files.newInputStream(p)) {
            data = SecureXml.readLimited(in);
        }
        org.w3c.dom.Document doc = SecureXml.parse(data);
        if (validate) {
            SecureXml.validate(doc);
        }
        return B2mmlReader.fromDocument(doc);
    }
}
