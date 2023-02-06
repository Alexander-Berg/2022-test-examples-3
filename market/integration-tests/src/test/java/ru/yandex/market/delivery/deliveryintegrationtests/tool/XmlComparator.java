package ru.yandex.market.delivery.deliveryintegrationtests.tool;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.placeholder.PlaceholderDifferenceEvaluator;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlComparator {
    public void assertXmlValuesAreEqual(String actualResponseContent, String expectedValueString) {
        assertThatResponseIsXml(actualResponseContent);

        Diff diff = DiffBuilder.compare(expectedValueString)
                .withTest(actualResponseContent)
                .withDifferenceEvaluator(new PlaceholderDifferenceEvaluator())
                .ignoreWhitespace()
                .ignoreComments()
                .checkForSimilar()
                .build();

        assertThat(diff.hasDifferences())
                .as(diff.toString())
                .isFalse();
    }

    private void assertThatResponseIsXml(String xml) {
        DocumentBuilder builder = createBuilder();

        try {
            builder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Failed to parse actual response xml [" + xml + "]");
        }
    }

    private DocumentBuilder createBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        try {
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
