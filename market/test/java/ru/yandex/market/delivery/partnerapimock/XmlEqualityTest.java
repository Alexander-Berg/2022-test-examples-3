package ru.yandex.market.delivery.partnerapimock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.partnerapimock.component.XmlMatcher;

import static org.junit.jupiter.params.provider.Arguments.of;

class XmlEqualityTest {

    private static final String TEST_PACKAGE = "/equalitytest/";
    private static final String SAMPLE_XML_FILE_NAME = "sample.xml";

    static Stream<Arguments> getParameters() {
        return Stream.of(
            of("ignore-comments.xml", true),
            of("ignore-whitespaces.xml", true),
            of("invalid-value.xml", false),
            of("swapped-tags.xml", true)
        );
    }

    @MethodSource("getParameters")
    @ParameterizedTest(name = "xml to compare: {0}, match expected: {1}")
    void testXmlEquality(String xmlFileName, boolean match) throws URISyntaxException, IOException {
        String sampleXml = getXmlContent(SAMPLE_XML_FILE_NAME);
        String requestXml = getXmlContent(xmlFileName);

        XmlMatcher xmlMatcher = new XmlMatcher();
        Assertions.assertEquals(
            match,
            xmlMatcher.isXmlsEqual(sampleXml, requestXml),
            "xmlComparator should return: " + match
        );
    }

    private String getXmlContent(String path) throws URISyntaxException, IOException {
        Path filePath = Paths.get(this.getClass().getResource(TEST_PACKAGE + path).toURI());
        return String.join("", Files.readAllLines(filePath));
    }

}
