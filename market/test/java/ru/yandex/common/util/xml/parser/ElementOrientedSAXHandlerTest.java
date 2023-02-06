package ru.yandex.common.util.xml.parser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

public class ElementOrientedSAXHandlerTest {
    private static final String XML = "<a><b><c>d</c></b></a>";

    private String aValue;
    private String bValue;
    private String cValue;

    @Before
    public void setUp() {
        aValue = null;
        bValue = null;
        cValue = null;
    }

    @Test
    public void testParseWholeValues() throws IOException, SAXException {
        StringReader reader = new StringReader(XML);

        ElementOrientedSAXHandler contentHandler = new ElementOrientedSAXHandler();
        contentHandler.addElementWholeValueListener("/a/b", new StringElementValueSetter() {
            public void setValue(String value) {
                bValue = value;
            }
        });

        ElementOrientedSAXHandler.parseXmlReader(reader, contentHandler);

        Assert.assertEquals("<c>d</c>", bValue);
    }

    @Test
    public void testParseNestedWholeValues() throws IOException, SAXException {
        StringReader reader = new StringReader(XML);

        ElementOrientedSAXHandler contentHandler = new ElementOrientedSAXHandler();
        contentHandler.addElementWholeValueListener("/a", new StringElementValueSetter() {
            @Override
            public void setValue(String value) {
                aValue = value;
            }
        });
        contentHandler.addElementWholeValueListener("/a/b/c", new StringElementValueSetter() {
            @Override
            public void setValue(String value) {
                cValue = value;
            }
        });

        ElementOrientedSAXHandler.parseXmlReader(reader, contentHandler);

        Assert.assertEquals("<b><c>d</c></b>", aValue);
        Assert.assertEquals("d", cValue);
    }

    @Test
    public void testParseWholeValueWhileHavingInnerParsers() throws IOException, SAXException {
        StringReader reader = new StringReader("<a><b><c>d</c></b></a>");

        ElementOrientedSAXHandler contentHandler = new ElementOrientedSAXHandler();
        contentHandler.addElementWholeValueListener("/a/b", new StringElementValueSetter() {
            @Override
            public void setValue(String value) {
                bValue = value;
            }
        });
        contentHandler.addElementValueListener("/a/b/c", new StringElementValueSetter() {
            @Override
            public void setValue(String value) {
                // to /dev/null
                cValue = value;
            }
        });

        ElementOrientedSAXHandler.parseXmlReader(reader, contentHandler);

        Assert.assertEquals("<c>d</c>", bValue);
        Assert.assertEquals("d", cValue);
    }

}
