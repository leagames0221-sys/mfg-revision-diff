package dev.mfgdiff.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.Headers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Html.esc, OriginGuard and Multipart on their own. */
class SmallPartsTest {

    // ------------------------------------------------------------ Html.esc

    @Test
    void escapesTheFiveCharactersOfTheOwaspTable() {
        assertEquals("&amp;&lt;&gt;&quot;&#x27;", Html.esc("&<>\"'"));
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", Html.esc("<script>alert(1)</script>"));
        assertEquals("", Html.esc(null));
        assertEquals("日本語 plain", Html.esc("日本語 plain"));
        assertEquals("&amp;lt;", Html.esc("&lt;"), "an existing entity is escaped again, not trusted");
    }

    // ------------------------------------------------------------ OriginGuard

    private static Headers headers(String... kv) {
        Headers h = new Headers();
        for (int i = 0; i < kv.length; i += 2) {
            h.add(kv[i], kv[i + 1]);
        }
        return h;
    }

    @Test
    void theHostHeaderMustNameTheServer() {
        OriginGuard g = new OriginGuard(8080);
        assertTrue(g.hostAllowed(headers("Host", "127.0.0.1:8080")));
        assertTrue(g.hostAllowed(headers("Host", "localhost:8080")));
        assertFalse(g.hostAllowed(headers("Host", "evil.example:8080")));
        assertFalse(g.hostAllowed(headers("Host", "127.0.0.1")));
        assertFalse(g.hostAllowed(headers("Host", "127.0.0.1:8081")));
        assertFalse(g.hostAllowed(headers()));
    }

    @Test
    void theOriginIsComparedAsAWholeString() {
        OriginGuard g = new OriginGuard(8080);
        assertTrue(g.sourceAllowed(headers("Origin", "http://127.0.0.1:8080")));
        assertTrue(g.sourceAllowed(headers("Origin", "http://localhost:8080")));
        assertFalse(g.sourceAllowed(headers("Origin", "null")));
        assertFalse(g.sourceAllowed(headers("Origin", "http://127.0.0.1:8080.evil.example")));
        assertFalse(g.sourceAllowed(headers("Origin", "http://127.0.0.1:8080/")));
        assertFalse(g.sourceAllowed(headers("Origin", "http://evil.example")));
        assertFalse(g.sourceAllowed(headers()), "neither header: refused");
    }

    @Test
    void theRefererIsUsedOnlyWithoutOriginAndOnlyByItsOrigin() {
        OriginGuard g = new OriginGuard(8080);
        assertTrue(g.sourceAllowed(headers("Referer", "http://127.0.0.1:8080/result/abc?filter=added")));
        assertFalse(g.sourceAllowed(headers("Referer", "http://127.0.0.1:8080@evil.example/")));
        assertFalse(g.sourceAllowed(headers("Referer", "http://127.0.0.1:8080.evil.example/")));
        assertFalse(g.sourceAllowed(headers("Referer", "http://evil.example/http://127.0.0.1:8080/")));
        assertFalse(g.sourceAllowed(headers("Referer", "::not a url::")));
        assertFalse(g.sourceAllowed(headers("Origin", "http://evil.example", "Referer", "http://127.0.0.1:8080/")),
                "a wrong Origin is not rescued by the Referer");
    }

    // ------------------------------------------------------------ Multipart

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static final String CT = "multipart/form-data; boundary=XyZ";

    @Test
    void readsFieldsFilesAndTextWithLineBreaks() throws IOException {
        String body = "--XyZ\r\nContent-Disposition: form-data; name=\"a\"\r\n\r\nline1\r\nline2\r\n"
                + "--XyZ\r\nContent-Disposition: form-data; name=\"fileB\"; filename=\"C:\\x\\b.xml\"\r\nContent-Type: text/xml\r\n\r\n<x/>\r\n"
                + "--XyZ--\r\n";
        List<Multipart.Part> parts = Multipart.parse(CT, b(body));
        assertEquals(2, parts.size());
        assertEquals("a", parts.get(0).name());
        assertEquals("line1\r\nline2", new String(parts.get(0).data(), StandardCharsets.UTF_8));
        assertEquals("C:\\x\\b.xml", parts.get(1).filename());
        assertEquals("<x/>", new String(Multipart.find(parts, "fileB").data(), StandardCharsets.UTF_8));
    }

    @Test
    void aTabBeforeTheParameterNameIsWhitespaceToo() throws IOException {
        String body = "--XyZ\r\nContent-Disposition: form-data;\tname=\"fileA\";\tfilename=\"a.xml\"\r\n\r\n<a/>\r\n--XyZ--\r\n";
        List<Multipart.Part> parts = Multipart.parse("multipart/form-data;\tboundary=XyZ", b(body));
        assertEquals("fileA", parts.get(0).name());
        assertEquals("a.xml", parts.get(0).filename());
    }

    @Test
    void acceptsAQuotedBoundary() throws IOException {
        String body = "--q q\r\nContent-Disposition: form-data; name=\"a\"\r\n\r\nv\r\n--q q--";
        assertEquals(1, Multipart.parse("multipart/form-data; boundary=\"q q\"", b(body)).size());
    }

    @Test
    void refusesWhatDoesNotFollowTheFormat() {
        assertThrows(IOException.class, () -> Multipart.parse("text/plain", b("x")));
        assertThrows(IOException.class, () -> Multipart.parse("multipart/form-data", b("x")), "no boundary");
        assertThrows(IOException.class, () -> Multipart.parse(CT, b("nothing")), "no first delimiter");
        assertThrows(IOException.class, () -> Multipart.parse(CT, b("--XyZ\r\nContent-Disposition: form-data; name=\"a\"\r\n\r\nv")),
                "part not closed");
        assertThrows(IOException.class, () -> Multipart.parse(CT, b("--XyZ\r\nContent-Type: text/plain\r\n\r\nv\r\n--XyZ--")),
                "no name");
        assertThrows(IOException.class, () -> Multipart.parse(CT, b("--XyZ\r\nContent-Disposition: attachment; name=\"a\"\r\n\r\nv\r\n--XyZ--")),
                "not form-data");
        assertThrows(IOException.class, () -> Multipart.parse(CT, b("--XyZxx")), "bad delimiter line");
    }

    @Test
    void refusesMoreThanTheMostPartsAllowed() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= Multipart.MAX_PARTS; i++) {
            sb.append("--XyZ\r\nContent-Disposition: form-data; name=\"f").append(i).append("\"\r\n\r\nv\r\n");
        }
        sb.append("--XyZ--");
        assertThrows(IOException.class, () -> Multipart.parse(CT, b(sb.toString())));
    }
}
