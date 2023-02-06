package ru.yandex.common.framework.core.servantletchecker.response;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Verifies that the actual document contains the expected one.
 *
 * @author agorbunov @ Oct 25, 2010
 */
public class XmlContainmentMatcher {
    private String expected;
    private String actual;

    public XmlContainmentMatcher(String expected, String actual) {
        this.expected = expected;
        this.actual = actual;
    }

    public boolean matches() {
        return getDifference().isEmpty();
    }

    public String getDifference() {
        try {
            return getDifferenceImp();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getDifferenceImp() throws Exception {
        Node expectedDoc = getDocument(expected);
        Node actualDoc = getDocument(actual);
        return new NodeContainmentMatcher(expectedDoc, actualDoc).getDifference();
    }

    private Document getDocument(String xml) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(xml)));
        return parser.getDocument();
    }
}
