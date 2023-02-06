package ru.yandex.market.core.xml;

import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author fbokovikov
 */
public class XmlUtilsTest {

    public static Stream<Arguments> xmlStringsForTest() {
        return Stream.of(
                Arguments.of(
                        "<datasource-info><id>1</id></datasource-info>",
                        buildNode()
                )
        );
    }

    private static Document buildNode() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element root = doc.createElement("datasource-info");
            doc.appendChild(root);
            Element item = doc.createElement("id");
            root.appendChild(item);
            item.appendChild(doc.createTextNode("1"));
            return doc;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("xmlStringsForTest")
    public void fromString(String xmlString, Document expectedNode) {
        XMLAssert.assertXMLEqual(
                XmlUtils.convertToDocument(xmlString),
                expectedNode
        );
    }
}
