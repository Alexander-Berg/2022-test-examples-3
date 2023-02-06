package ru.yandex.market.core.util.xml;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import com.google.common.base.Throwables;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * @author fbokovikov
 */
public class XpathResolver {

    public Object getXPathValue(String path, String xmlString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
            XPathExpression xPathExpression = XPathFactory.newInstance().newXPath().compile(path);
            return xPathExpression.evaluate(doc, XPathConstants.STRING);

        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

}
