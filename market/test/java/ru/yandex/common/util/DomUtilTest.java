package ru.yandex.common.util;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Dmitry Trofimov
 */
public class DomUtilTest extends TestCase {

    private static final String HTML = "<HTML>" +
        "<BODY>" +
        "<TABLE id='table'>" +
        "<TR>" +
        "</TR>" +
        "<TR>" +
        "<TD>" +
        "test" +
        "</TD>" +
        "<TD>" +
        "<A NAME='aaaa'></A>\n" +
        "</TD>" +
        "</TR>" +
        "</TABLE>" +
        "<TABLE><TBODY><TR><TD><A id='aaaa'></A></TD></TR></TBODY></TABLE>" +
        "</BODY>" +
        "</HTML>";


    public void testXPath() throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        final Document doc = documentBuilder.parse(new ByteArrayInputStream(HTML.getBytes(StandardCharsets.UTF_8)));

        final Node node = XPathUtils.queryNode("/HTML/BODY/TABLE[1]/TR/TD[2]/A", doc);
        assertEquals("A", node.getNodeName());

        final String xpath = DomUtils.getXPath(node);
        assertEquals("/HTML/BODY/TABLE[1]/TBODY/TR[2]/TD[2]/A", xpath);
        assertEquals("/HTML/BODY/TABLE[2]/TBODY/TR/TD/A", DomUtils.getXPath(
            XPathUtils.queryNode("//A[@id = 'aaaa']", doc)
        ));
    }

}
