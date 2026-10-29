package dev.mfgdiff;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

/** The two sample revisions (and generated ones) must pass the official B2MML schema. */
class SchemaValidationTest {

    @Test
    void sampleRevisionsAreValidB2mml() {
        for (String file : new String[] {"examples/revA.xml", "examples/revB.xml"}) {
            assertDoesNotThrow(() -> SecureXml.validate(SecureXml.parse(Files.readAllBytes(Path.of(file)))), file);
        }
    }

    @Test
    void generatedRevisionsAreValidB2mml() {
        for (int seed = 1; seed <= 5; seed++) {
            ChangeGenerator.Generated g = ChangeGenerator.generate(SampleData.base(seed, 15), 0.2, seed);
            assertDoesNotThrow(() -> SecureXml.validate(SecureXml.parseString(B2mmlWriter.write(g.a(), java.util.List.of()))));
            assertDoesNotThrow(() -> SecureXml.validate(SecureXml.parseString(B2mmlWriter.write(g.b(), java.util.List.of()))));
        }
    }

    @Test
    void anElementThatIsNotInTheSchemaIsRejected() throws Exception {
        String xml = Files.readString(Path.of("examples/revA.xml"), StandardCharsets.UTF_8)
                .replaceFirst("<OperationsType>Production</OperationsType>", "<MadeUpElement>x</MadeUpElement>");
        assertThrows(SAXException.class, () -> SecureXml.validate(SecureXml.parseString(xml)));
    }

    @Test
    void aWrongValueIsRejected() throws Exception {
        String xml = Files.readString(Path.of("examples/revA.xml"), StandardCharsets.UTF_8)
                .replaceFirst("<OperationsType>Production</OperationsType>", "<OperationsType>Whatever</OperationsType>");
        assertThrows(SAXException.class, () -> SecureXml.validate(SecureXml.parseString(xml)));
    }

    @Test
    void aSchemaLocationHintInTheInputIsNotFollowed() throws Exception {
        Path hinted = Files.createTempFile("hint", ".xsd");
        Files.writeString(hinted, "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema' targetNamespace='http://www.mesa.org/xml/B2MML'>"
                + "<xs:element name='OperationsDefinitionInformation' type='xs:anyType'/></xs:schema>");
        String xml = "<OperationsDefinitionInformation xmlns='http://www.mesa.org/xml/B2MML' "
                + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='http://www.mesa.org/xml/B2MML "
                + hinted.toUri() + "'><NotAllowed/></OperationsDefinitionInformation>";
        assertThrows(SAXException.class, () -> SecureXml.validate(SecureXml.parseString(xml)));
        Files.deleteIfExists(hinted);
    }
}
