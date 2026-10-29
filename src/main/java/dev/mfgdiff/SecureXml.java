package dev.mfgdiff;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * XML access with XXE switched off, following the Java section of the OWASP XML External Entity
 * Prevention Cheat Sheet: DOCTYPE declarations are rejected, secure processing is on, XInclude is off,
 * and schema compilation/validation may not reach any external resource except the bundled B2MML files.
 */
public final class SecureXml {
    /** Largest input accepted (author's decision, see docs/sources.md). */
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    /** Deepest element nesting accepted (author's decision, see docs/sources.md). */
    public static final int MAX_DEPTH = 64;
    /** Most elements accepted in one document (author's decision, see docs/sources.md). */
    public static final int MAX_ELEMENTS = 200_000;

    public static final String B2MML_NS = "http://www.mesa.org/xml/B2MML";
    private static final String ROOT_XSD = "B2MML-OperationsDefinition.xsd";
    private static final Pattern BUNDLED = Pattern.compile("(B2MML|BatchML)-[A-Za-z]+\\.xsd");

    private static volatile Schema schema;

    private SecureXml() {
    }

    private static ErrorHandler throwing() {
        return new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) {
                // warnings are not errors
            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        };
    }

    static DocumentBuilder newBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        // The cheat sheet says to stop when a security setting is unsupported: no catch here.
        dbf.setAttribute("jdk.xml.maxElementDepth", Integer.toString(MAX_DEPTH));
        DocumentBuilder b = dbf.newDocumentBuilder();
        b.setErrorHandler(throwing());
        b.setEntityResolver((publicId, systemId) -> {
            throw new SAXException("external resource blocked: " + systemId);
        });
        return b;
    }

    /** Parses bytes (never a file name or URL) into a DOM document. */
    public static Document parse(byte[] data) throws IOException, SAXException {
        if (data.length > MAX_BYTES) {
            throw new SAXException("input larger than " + MAX_BYTES + " bytes");
        }
        try {
            DocumentBuilder b = newBuilder();
            Document doc = b.parse(new ByteArrayInputStream(data));
            int n = doc.getElementsByTagNameNS("*", "*").getLength();
            if (n > MAX_ELEMENTS) {
                throw new SAXException("more than " + MAX_ELEMENTS + " elements");
            }
            return doc;
        } catch (ParserConfigurationException e) {
            throw new SAXException("XML parser could not be configured safely: " + e.getMessage(), e);
        }
    }

    public static byte[] readLimited(InputStream in) throws IOException {
        byte[] data = in.readNBytes(MAX_BYTES + 1);
        if (data.length > MAX_BYTES) {
            throw new IOException("input larger than " + MAX_BYTES + " bytes");
        }
        return data;
    }

    /** Resolves schemaLocation references only to the bundled B2MML schema files; rejects the rest. */
    private static final class BundledResolver implements LSResourceResolver {
        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            String name = systemId == null ? "" : systemId.substring(systemId.lastIndexOf('/') + 1);
            if (!BUNDLED.matcher(name).matches()) {
                throw new IllegalArgumentException("schema reference not allowed: " + systemId);
            }
            InputStream in = SecureXml.class.getResourceAsStream("/b2mml/" + name);
            if (in == null) {
                throw new IllegalArgumentException("bundled schema missing: " + name);
            }
            return new BytesInput(in, name);
        }
    }

    private static final class BytesInput implements LSInput {
        private final InputStream stream;
        private final String systemId;

        BytesInput(InputStream stream, String systemId) {
            this.stream = stream;
            this.systemId = systemId;
        }

        @Override public Reader getCharacterStream() { return null; }
        @Override public void setCharacterStream(Reader r) { }
        @Override public InputStream getByteStream() { return stream; }
        @Override public void setByteStream(InputStream s) { }
        @Override public String getStringData() { return null; }
        @Override public void setStringData(String s) { }
        @Override public String getSystemId() { return systemId; }
        @Override public void setSystemId(String s) { }
        @Override public String getPublicId() { return null; }
        @Override public void setPublicId(String s) { }
        @Override public String getBaseURI() { return null; }
        @Override public void setBaseURI(String s) { }
        @Override public String getEncoding() { return null; }
        @Override public void setEncoding(String s) { }
        @Override public boolean getCertifiedText() { return false; }
        @Override public void setCertifiedText(boolean b) { }
    }

    private static Schema schema() throws SAXException, IOException {
        Schema s = schema;
        if (s == null) {
            synchronized (SecureXml.class) {
                s = schema;
                if (s == null) {
                    SchemaFactory sf = SchemaFactory.newDefaultInstance();
                    sf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                    sf.setResourceResolver(new BundledResolver());
                    try (InputStream in = SecureXml.class.getResourceAsStream("/b2mml/" + ROOT_XSD)) {
                        if (in == null) {
                            throw new IOException("bundled schema missing: " + ROOT_XSD);
                        }
                        s = sf.newSchema(new StreamSource(in, ROOT_XSD));
                    }
                    schema = s;
                }
            }
        }
        return s;
    }

    /** Validates a parsed document against the official B2MML OperationsDefinition schema. */
    public static void validate(Document doc) throws SAXException, IOException {
        Validator v = schema().newValidator();
        v.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        v.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        v.setResourceResolver(new BundledResolver());
        v.setErrorHandler(throwing());
        v.validate(new DOMSource(doc));
    }

    /** Convenience for tests: parse a string. */
    public static Document parseString(String xml) throws IOException, SAXException {
        return parse(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
