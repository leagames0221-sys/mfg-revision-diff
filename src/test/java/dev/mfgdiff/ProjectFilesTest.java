package dev.mfgdiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The start scripts, the pom and the logging setting have to agree with each other. */
class ProjectFilesTest {
    private static final Path M2 = Path.of(System.getProperty("user.home"), ".m2", "repository");
    private static final Pattern JAR = Pattern.compile("\"([A-Za-z0-9_./-]+\\.jar)\"");

    private static List<String> jars(String script) throws IOException {
        String text = Files.readString(Path.of("scripts", script), StandardCharsets.UTF_8);
        List<String> out = new ArrayList<>();
        Matcher m = JAR.matcher(text);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    @Test
    void bothScriptsListTheSameLibrariesAndEveryOneIsInTheLocalRepository() throws IOException {
        List<String> ps = jars("run.ps1");
        List<String> sh = jars("run.sh");
        assertEquals(13, ps.size(), "poi-ooxml and its 12 compile dependencies");
        assertEquals(ps, sh);
        for (String jar : ps) {
            assertTrue(Files.isRegularFile(M2.resolve(jar)), "not in ~/.m2: " + jar);
        }
    }

    @Test
    void theScriptsUseTheVersionsOfThePom() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);
        Matcher poi = Pattern.compile("<artifactId>poi-ooxml</artifactId>\\s*<version>([^<]+)</version>").matcher(pom);
        assertTrue(poi.find());
        Matcher log4j = Pattern.compile("<artifactId>log4j-api</artifactId>\\s*<version>([^<]+)</version>").matcher(pom);
        assertTrue(log4j.find());
        for (String script : new String[] {"run.ps1", "run.sh"}) {
            List<String> jars = jars(script);
            assertTrue(jars.contains("org/apache/poi/poi-ooxml/" + poi.group(1) + "/poi-ooxml-" + poi.group(1) + ".jar"), script);
            assertTrue(jars.contains("org/apache/logging/log4j/log4j-api/" + log4j.group(1) + "/log4j-api-" + log4j.group(1) + ".jar"), script);
        }
    }

    @Test
    void theLoggingProviderIsSetSoThatLog4jDoesNotComplain() throws IOException {
        try (var in = ProjectFilesTest.class.getResourceAsStream("/log4j2.component.properties")) {
            assertNotNull(in);
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(text.contains("log4j.provider = org.apache.logging.log4j.simple.internal.SimpleProvider"));
        }
    }
}
