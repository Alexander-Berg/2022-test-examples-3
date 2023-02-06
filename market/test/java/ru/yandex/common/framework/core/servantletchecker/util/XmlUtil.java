package ru.yandex.common.framework.core.servantletchecker.util;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * @author agorbunov @ Oct 22, 2010
 */
public class XmlUtil {

    public static String format(String input) {
        try {
            return formatImp(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String formatImp(String input) throws TransformerException {
        Source xmlInput = new StreamSource(new StringReader(input));
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        int indent = 2;
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
        transformer.transform(xmlInput, xmlOutput);
        return xmlOutput.getWriter().toString();
    }
}
