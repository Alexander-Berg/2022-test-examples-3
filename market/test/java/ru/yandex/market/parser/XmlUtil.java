package ru.yandex.market.parser;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.function.Predicate;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Attr;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import ru.yandex.market.parser.utils.IOUtils;

import static org.junit.Assert.assertFalse;

public class XmlUtil {

    public static void assertEqual(Class clazz, String resourceExpected, String actual) {
        assertEqual(clazz, resourceExpected, actual, attr -> true);
    }

    public static void assertEqual(Class clazz, String resourceExpected, String actual, Predicate<Attr> filter) {
        try {
            String expected = IOUtils.readInputStream(clazz.getResourceAsStream(resourceExpected));
            Diff xmlDiff = DiffBuilder.compare(expected)
                    .withTest(actual)
                    .ignoreWhitespace()
                    .withAttributeFilter(filter::test)
                    .build();

            System.out.println("Expected: " + format(expected, 2));

            assertFalse("Compared XMLs are different: " + xmlDiff.toString(), xmlDiff.hasDifferences());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String format(String xml, int indent) {

        try {
            Source xmlInput = new StreamSource(new StringReader(xml));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            throw new RuntimeException(e); // simple exception handling, please review it
        }
    }

    public static void print(String xml) {
        System.out.println(format(xml, 2));
    }
}
