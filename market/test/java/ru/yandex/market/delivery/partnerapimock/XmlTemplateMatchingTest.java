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
import static ru.yandex.market.delivery.partnerapimock.component.XmlParser.parseXml;

class XmlTemplateMatchingTest {

    static Stream<Arguments> getParameters() {
        return Stream.of(
            of("/template/simple-template.xml", "/request/simple-matching.xml", true),
            of("/template/simple-template.xml", "/request/absolutely-different-nodes.xml", false),
            of("/template/simple-template.xml", "/request/invalid-attribute.xml", false),
            of("/template/simple-template.xml", "/request/invalid-value.xml", false),
            of("/template/simple-template.xml", "/request/whitespaces.xml", true),
            of("/template/complex-template.xml", "/request/nodes-lack.xml", false),
            of("/template/complex-template.xml", "/request/swap-node-values.xml", false),
            of("/template/complex-template.xml", "/request/swap-nodes.xml", true)
        );
    }

    @MethodSource("getParameters")
    @ParameterizedTest(name = "request: {1}, template: {0}, match expected: {2}")
    void testXmlMatching(
        String templateXmlPath,
        String requestXmlPath,
        boolean match
    ) throws URISyntaxException, IOException {
        String templateXml = getXmlContent(templateXmlPath);
        String requestXml = getXmlContent(requestXmlPath);

        XmlMatcher xmlMatcher = new XmlMatcher();
        Assertions.assertEquals(
            match,
            xmlMatcher.isXmlMatchTemplate(parseXml(templateXml), parseXml(requestXml)),
            "xmlComparator should return: " + match
        );
    }

    private String getXmlContent(String path) throws URISyntaxException, IOException {
        Path filePath = Paths.get(this.getClass().getResource(path).toURI());
        return String.join("", Files.readAllLines(filePath));
    }

}
