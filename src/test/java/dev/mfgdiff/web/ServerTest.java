package dev.mfgdiff.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mfgdiff.B2mmlWriter;
import dev.mfgdiff.Definition;
import dev.mfgdiff.Node;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The screen, started on a free port of 127.0.0.1 and driven with the JDK's HTTP client. */
class ServerTest {
    private static Server server;
    private static String base;
    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private static final String BOUNDARY = "----mfgdifftest";

    @BeforeAll
    static void start() throws IOException {
        server = Server.start(0);
        base = "http://127.0.0.1:" + server.port();
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    // ------------------------------------------------------------ helpers

    private static byte[] multipart(String[][] fields) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String[] f : fields) {
            // f = {name, filename or null, content}
            out.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
            String disp = "Content-Disposition: form-data; name=\"" + f[0] + "\"";
            if (f[1] != null) {
                disp += "; filename=\"" + f[1] + "\"";
            }
            out.write((disp + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(f[2].getBytes(StandardCharsets.UTF_8));
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static HttpRequest.Builder post(byte[] body, String... headers) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + "/compare"))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        for (int i = 0; i < headers.length; i += 2) {
            b.header(headers[i], headers[i + 1]);
        }
        return b;
    }

    private static HttpResponse<String> text(HttpRequest r) throws Exception {
        return CLIENT.send(r, HttpResponse.BodyHandlers.ofString());
    }

    private static String sampleResultPath() throws Exception {
        HttpResponse<String> r = text(post(multipart(new String[][] {{"sample", null, "1"}}), "Origin", base).build());
        assertEquals(303, r.statusCode());
        String loc = r.headers().firstValue("Location").orElseThrow();
        assertTrue(loc.matches("/result/[0-9a-f]{32}"), loc);
        return loc;
    }

    private static String get(String path) throws Exception {
        HttpResponse<String> r = text(HttpRequest.newBuilder(URI.create(base + path)).GET().build());
        assertEquals(200, r.statusCode(), path);
        return r.body();
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    private static String xml(Definition d) {
        return B2mmlWriter.write(d, List.of("test"));
    }

    private static Definition defWithDescription(String description) {
        Definition d = new Definition();
        Node s = new Node(Node.Kind.SEGMENT);
        s.set("ID", "OP-010");
        s.set("Description", description);
        s.set("OperationsMaterialBillID", "MB-OP-010");
        s.order = "0010";
        Node it = new Node(Node.Kind.ITEM);
        it.set("ID", "OP-010-L1");
        it.set("Description", "Steel sheet");
        it.set("MaterialSpecificationID", "P-1");
        it.set("Quantity", "1");
        s.children.add(it);
        d.segments.add(s);
        return d;
    }

    // ------------------------------------------------------------ pages

    @Test
    void theUploadPageIsServedWithAPolicyThatForbidsInlineScripts() throws Exception {
        HttpResponse<String> r = text(HttpRequest.newBuilder(URI.create(base + "/")).GET().build());
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("name=\"fileA\"") && r.body().contains("name=\"fileB\""));
        assertTrue(r.body().contains("見本で試す"));
        String csp = r.headers().firstValue("Content-Security-Policy").orElseThrow();
        assertTrue(csp.contains("default-src 'none'") && csp.contains("script-src 'self'") && !csp.contains("unsafe-inline"));
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElseThrow());
        assertFalse(r.body().contains("<script"), "no script on the upload page");
    }

    @Test
    void theSampleRevisionsShowAllFiveCategories() throws Exception {
        String html = get(sampleResultPath());
        // steps (13 rows) and bill rows (25 rows)
        assertEquals(13 + 25, count(html, "<tr class=\"st-"));
        assertTrue(html.contains("class=\"st-same\"") || html.contains("class=\"st-same "), "same");
        assertTrue(html.contains("st-changed"), "changed");
        assertTrue(html.contains("st-added"), "added");
        assertTrue(html.contains("st-removed"), "removed");
        assertEquals(1, count(html, "class=\"mv\""), "one moved step");
        assertTrue(html.contains("<td class=\"kind\">≠</td>") && html.contains("<td class=\"kind\">A</td>")
                && html.contains("<td class=\"kind\">B</td>") && html.contains("<td class=\"kind\">=</td>"));
        // changed cells carry the class that is painted yellow
        assertTrue(html.contains("<td class=\"chg\">Duration: PT6M → PT7M</td>"));
        assertTrue(html.contains("<td class=\"chg\">OP-120</td><td class=\"chg\">OP-130</td>"));
        assertTrue(html.contains("src=\"/app.js\"") && html.contains("id=\"prev\"") && html.contains("id=\"next\""));
    }

    @Test
    void eachOfTheSevenFiltersShowsOnlyItsRows() throws Exception {
        String path = sampleResultPath();
        // expected rows (steps + bill rows) for the sample revisions, see ExamplesAndCliTest
        Object[][] expected = {
            {"all", 38, 11}, {"differences", 11, 11}, {"added", 4, 4}, {"changes", 5, 5}, {"removed", 2, 2},
            {"unchanged", 27, 0}, {"moved", 1, 1},
        };
        for (Object[] e : expected) {
            String html = get(path + "?filter=" + e[0]);
            assertEquals((int) e[1], count(html, "<tr class=\"st-"), "rows for filter " + e[0]);
            assertEquals((int) e[2], count(html, "data-diff=\"1\""), "difference rows for filter " + e[0]);
            assertTrue(html.contains("aria-current=\"page\""), "current filter is marked: " + e[0]);
            assertTrue(html.contains("?filter=" + e[0] + "\" class=\"on\""), "marked filter is " + e[0]);
        }
        String added = get(path + "?filter=added");
        assertFalse(added.contains("st-removed") || added.contains("st-changed"), "added shows no removed or changed row");
        assertEquals(get(path), get(path + "?filter=nonsense"), "an unknown filter shows everything");
    }

    @Test
    void theExcelFileCanBeDownloadedAndOpened() throws Exception {
        String path = sampleResultPath();
        HttpResponse<byte[]> r = CLIENT.send(HttpRequest.newBuilder(URI.create(base + path + "/export.xlsx")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, r.statusCode());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", r.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(r.headers().firstValue("Content-Disposition").orElseThrow().startsWith("attachment;"));
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(r.body()))) {
            assertEquals(List.of("まとめ", "工程", "部品"), List.of(wb.getSheetName(0), wb.getSheetName(1), wb.getSheetName(2)));
            assertEquals(13, wb.getSheet("工程").getLastRowNum());
            assertEquals(25, wb.getSheet("部品").getLastRowNum());
        }
    }

    @Test
    void unknownPathsAndMethodsAreRefused() throws Exception {
        assertEquals(404, text(HttpRequest.newBuilder(URI.create(base + "/result/" + "0".repeat(32))).GET().build()).statusCode());
        assertEquals(404, text(HttpRequest.newBuilder(URI.create(base + "/result/..%2f..")).GET().build()).statusCode());
        assertEquals(404, text(HttpRequest.newBuilder(URI.create(base + "/nothing")).GET().build()).statusCode());
        assertEquals(405, text(HttpRequest.newBuilder(URI.create(base + "/compare")).GET().build()).statusCode());
        assertEquals(405, text(HttpRequest.newBuilder(URI.create(base + "/")).PUT(HttpRequest.BodyPublishers.noBody()).build()).statusCode());
    }

    // ------------------------------------------------------------ protection: output encoding

    @Test
    void textFromTheFilesIsEscapedInTheResultPage() throws Exception {
        String evil = "<script>alert(1)</script>";
        String evilName = "<img src=x onerror=alert(2)>.xml";
        String a = xml(defWithDescription("Cut plate"));
        String b = xml(defWithDescription(evil));
        HttpResponse<String> r = text(post(multipart(new String[][] {{"fileA", evilName, a}, {"fileB", "b.xml", b}}), "Origin", base).build());
        assertEquals(303, r.statusCode());
        String html = get(r.headers().firstValue("Location").orElseThrow());
        assertFalse(html.contains("<script>alert"), "a script element from the file");
        assertFalse(html.contains("<img src=x"), "an element from the file name");
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"), "the name is shown as text");
        assertTrue(html.contains("&lt;img src=x onerror=alert(2)&gt;.xml"), "the file name is shown as text");
        // the only script element is the one that loads /app.js
        assertEquals(1, count(html, "<script"));
    }

    @Test
    void anErrorMessageThatContainsFileTextIsEscaped() throws Exception {
        String bad = "<?xml version=\"1.0\"?><x><a></b></x><script>alert(3)</script>";
        HttpResponse<String> r = text(post(multipart(new String[][] {{"fileA", "a.xml", bad}, {"fileB", "b.xml", bad}}), "Origin", base).build());
        assertEquals(400, r.statusCode());
        assertFalse(r.body().contains("<script>alert"));
    }

    @Test
    void aDoctypeIsRefused() throws Exception {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///C:/Windows/win.ini\">]><x>&e;</x>";
        HttpResponse<String> r = text(post(multipart(new String[][] {{"fileA", "a.xml", xxe}, {"fileB", "b.xml", xxe}}), "Origin", base).build());
        assertEquals(400, r.statusCode());
        assertFalse(r.body().contains("[fonts]"));
    }

    // ------------------------------------------------------------ protection: source of a POST

    @Test
    void aPostFromAnotherOriginIsRefused() throws Exception {
        byte[] body = multipart(new String[][] {{"sample", null, "1"}});
        String[][] refused = {
            {"Origin", "http://evil.example"},
            {"Origin", "null"},
            {"Origin", "http://127.0.0.1:" + server.port() + ".evil.example"},
            {"Origin", "https://127.0.0.1:" + server.port()},
            {"Origin", "http://127.0.0.1:1"},
        };
        for (String[] h : refused) {
            assertEquals(403, text(post(body, h).build()).statusCode(), String.join(": ", h));
        }
        assertEquals(403, text(post(body).build()).statusCode(), "neither Origin nor Referer");
        assertEquals(403, text(post(body, "Referer", "http://evil.example/page").build()).statusCode(), "Referer of another site");
        assertEquals(403, text(post(body, "Origin", base, "Sec-Fetch-Site", "cross-site").build()).statusCode(), "cross-site fetch metadata");
        // from a page of this server
        assertEquals(303, text(post(body, "Origin", base).build()).statusCode());
        assertEquals(303, text(post(body, "Origin", "http://localhost:" + server.port()).build()).statusCode());
        assertEquals(303, text(post(body, "Referer", base + "/").build()).statusCode(), "Referer when there is no Origin");
        assertEquals(303, text(post(body, "Origin", base, "Sec-Fetch-Site", "same-origin").build()).statusCode());
    }

    @Test
    void aRequestForAnotherHostNameIsRefused() throws Exception {
        try (Socket s = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
            s.getOutputStream().write("GET / HTTP/1.1\r\nHost: evil.example\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            s.getOutputStream().flush();
            String answer = new String(s.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(answer.startsWith("HTTP/1.1 403"), answer.substring(0, Math.min(60, answer.length())));
        }
    }

    @Test
    void theServerListensOnTheLoopbackAddressOnly() throws Exception {
        assertTrue(server.port() > 0);
        int tried = 0;
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (ni.isLoopback() || !ni.isUp()) {
                continue;
            }
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (a.isLinkLocalAddress() || a.isLoopbackAddress()) {
                    continue;
                }
                tried++;
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(a, server.port()), 1000);
                    assertTrue(false, "connected through " + a);
                } catch (IOException expected) {
                    // refused or timed out: the server is not listening there
                }
            }
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(tried > 0, "this PC has no address other than loopback to try");
    }

    // ------------------------------------------------------------ protection: size

    @Test
    void aBodyLargerThanTheLimitIsRefused() throws Exception {
        String filler = "x".repeat(Server.MAX_BODY + 1024);
        byte[] body = multipart(new String[][] {{"fileA", "a.xml", filler}, {"fileB", "b.xml", "<x/>"}});
        assertTrue(body.length > Server.MAX_BODY);
        HttpResponse<String> r = text(post(body, "Origin", base).build());
        assertEquals(413, r.statusCode());
    }

    @Test
    void aBodyWithoutLengthThatGrowsPastTheLimitIsRefused() throws Exception {
        // sent in chunks (no Content-Length), so the limit has to be applied while reading
        String filler = "x".repeat(Server.MAX_BODY + 1024);
        byte[] body = multipart(new String[][] {{"fileA", "a.xml", filler}, {"fileB", "b.xml", "<x/>"}});
        InputStream in = new ByteArrayInputStream(body);
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/compare"))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .header("Origin", base)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> in)).build();
        assertEquals(413, text(req).statusCode());
    }

    @Test
    void aBodyJustUnderTheLimitIsReadAndRefusedOnlyBecauseItIsNotB2mml() throws Exception {
        String filler = "x".repeat(Server.MAX_BODY - 4096);
        byte[] body = multipart(new String[][] {{"fileA", "a.xml", filler}, {"fileB", "b.xml", "<x/>"}});
        assertTrue(body.length < Server.MAX_BODY);
        assertEquals(400, text(post(body, "Origin", base).build()).statusCode());
    }

    @Test
    void anIncompleteSubmissionIsRefused() throws Exception {
        assertEquals(400, text(post(multipart(new String[][] {{"fileA", "a.xml", "<x/>"}}), "Origin", base).build()).statusCode());
        assertEquals(400, text(post("not multipart".getBytes(StandardCharsets.UTF_8), "Origin", base).build()).statusCode());
    }
}
