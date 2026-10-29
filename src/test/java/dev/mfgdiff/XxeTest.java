package dev.mfgdiff;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

/** XML External Entity inputs must be refused, and nothing outside the input may be read. */
class XxeTest {

    private static final String BODY = "<OperationsDefinitionInformation xmlns='http://www.mesa.org/xml/B2MML'><ID>&xxe;</ID>"
            + "<OperationsDefinition><ID>D</ID></OperationsDefinition></OperationsDefinitionInformation>";

    @Test
    void doctypeWithAnExternalFileEntityIsRejected() throws IOException {
        Path secret = Files.createTempFile("secret", ".txt");
        Files.writeString(secret, "TOP-SECRET-CONTENT");
        String xml = "<?xml version='1.0'?><!DOCTYPE r [<!ENTITY xxe SYSTEM '" + secret.toUri() + "'>]>" + BODY;
        SAXException e = assertThrows(SAXException.class, () -> B2mmlReader.read(xml.getBytes(StandardCharsets.UTF_8)));
        assertFalse(String.valueOf(e.getMessage()).contains("TOP-SECRET-CONTENT"));
        Files.deleteIfExists(secret);
    }

    @Test
    void doctypeWithANetworkEntityIsRejectedWithoutConnecting() {
        String xml = "<?xml version='1.0'?><!DOCTYPE r [<!ENTITY xxe SYSTEM 'http://127.0.0.1:9/never'>]>" + BODY;
        SAXException e = assertThrows(SAXException.class, () -> B2mmlReader.read(xml.getBytes(StandardCharsets.UTF_8)));
        assertTrue(e.getMessage().toLowerCase().contains("doctype"), e.getMessage());
    }

    @Test
    void doctypeWithAnInternalEntityBombIsRejected() {
        String xml = "<?xml version='1.0'?><!DOCTYPE r [<!ENTITY a 'aaaaaaaaaa'><!ENTITY b '&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;'>"
                + "<!ENTITY c '&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;'>]><r>&c;</r>";
        assertThrows(SAXException.class, () -> SecureXml.parseString(xml));
    }

    @Test
    void externalDtdSubsetIsRejected() {
        String xml = "<?xml version='1.0'?><!DOCTYPE r SYSTEM 'http://127.0.0.1:9/evil.dtd'><r/>";
        assertThrows(SAXException.class, () -> SecureXml.parseString(xml));
    }

    @Test
    void xincludeIsNotProcessed() throws IOException, SAXException {
        Path secret = Files.createTempFile("secret", ".txt");
        Files.writeString(secret, "TOP-SECRET-CONTENT");
        String xml = "<r xmlns:xi='http://www.w3.org/2001/XInclude'><xi:include parse='text' href='" + secret.toUri() + "'/></r>";
        var doc = SecureXml.parseString(xml);
        assertFalse(doc.getDocumentElement().getTextContent().contains("TOP-SECRET-CONTENT"));
        Files.deleteIfExists(secret);
    }

    @Test
    void inputOverTheSizeLimitIsRejected() {
        byte[] big = new byte[SecureXml.MAX_BYTES + 1];
        java.util.Arrays.fill(big, (byte) ' ');
        assertThrows(SAXException.class, () -> SecureXml.parse(big));
    }

    @Test
    void streamOverTheSizeLimitIsRejected() {
        java.io.InputStream in = new java.io.InputStream() {
            long n = 0;

            @Override
            public int read() {
                return n++ < SecureXml.MAX_BYTES + 10L ? ' ' : -1;
            }
        };
        assertThrows(IOException.class, () -> SecureXml.readLimited(in));
    }

    @Test
    void nestingDeeperThanTheLimitIsRejected() {
        StringBuilder sb = new StringBuilder();
        int depth = SecureXml.MAX_DEPTH + 10;
        for (int i = 0; i < depth; i++) {
            sb.append("<a>");
        }
        for (int i = 0; i < depth; i++) {
            sb.append("</a>");
        }
        assertThrows(SAXException.class, () -> SecureXml.parseString(sb.toString()));
    }

    @Test
    void aNormalDocumentStillParses() throws Exception {
        assertTrue(B2mmlReader.read(Path.of("examples/revA.xml")).segments.size() == 12);
    }
}
