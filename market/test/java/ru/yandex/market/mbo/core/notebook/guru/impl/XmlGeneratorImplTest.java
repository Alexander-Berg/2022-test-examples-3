package ru.yandex.market.mbo.core.notebook.guru.impl;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

/**
 * Created by chervotkin on 28.04.17.
 */
public class XmlGeneratorImplTest {

    @Test
    public void startElementSaxon() throws Exception {
        try {
            final SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance(
                    "net.sf.saxon.TransformerFactoryImpl", this.getClass().getClassLoader()
            );
            transform(tf);
        } catch (javax.xml.transform.TransformerFactoryConfigurationError e) {
            // pass test if saxon library is absent
            if (e.getCause() instanceof java.lang.ClassNotFoundException
                    && e.getMessage().equals("Provider net.sf.saxon.TransformerFactoryImpl not found")) {

                return;
            }
            throw e;
        }
    }

    @Test
    public void startElement() throws Exception {
        final SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        transform(tf);
    }

    private void transform(SAXTransformerFactory tf) throws TransformerConfigurationException, SAXException {
        TransformerHandler th = tf.newTransformerHandler();
        final StreamResult streamResult = new StreamResult(new StringWriter());
        final Transformer serializer = th.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.VERSION, "1.0");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setErrorListener(new XmlGeneratorImpl.RethrowingListener());
        th.setResult(streamResult);

        // When Saxon XML library is found in the CLASSPATH line below throws
        // org.xml.sax.SAXException: Parser configuration problem: namespace reporting is not enabled
        th.startElement("", "", "any_tag", new AttributesImpl());
    }
}
