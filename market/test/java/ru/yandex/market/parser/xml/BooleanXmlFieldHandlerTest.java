package ru.yandex.market.parser.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.yandex.market.parser.XmlUtil;

public class BooleanXmlFieldHandlerTest extends Assert {

    private static Mapping mapping;
    private static InputStream in;

    @AfterClass
    public static void freeClass() throws IOException {
        in.close();
    }

    @BeforeClass
    public static void initClass() throws IOException {
        mapping = new Mapping();
        in = BooleanXmlFieldHandlerTest.class.getResourceAsStream("bool-mapping.xml");
        mapping.loadMapping(new InputSource(in));
    }

    @Test
    public void shouldBeFalseValues() throws IOException, MappingException, MarshalException, ValidationException,
            SAXException {
        Bean bean = new Bean();
        bean.setBool1(false);
        bean.setBool2(false);
        bean.setBool3(false);
        XmlUtil.assertEqual(getClass(), "false-bean.xml", marshal(bean));
    }

    @Test
    public void shouldBeTrueValues() throws IOException, MappingException, MarshalException, ValidationException,
            SAXException {
        Bean bean = new Bean();
        bean.setBool1(true);
        bean.setBool2(true);
        bean.setBool3(true);
        XmlUtil.assertEqual(getClass(), "true-bean.xml", marshal(bean));
    }

    @Test
    public void shouldNotWriteNullValue() throws IOException, MappingException, MarshalException, ValidationException
            , SAXException {
        Bean bean = new Bean();
        XmlUtil.assertEqual(getClass(), "empty-bean.xml", marshal(bean));
    }

    @Test
    public void shouldUnmarshalEmptyObject() throws MarshalException, IOException, ValidationException {
        Bean bean = unmarshal("empty-bean.xml");
        assertFalse(bean.isBool1());
        assertNull(bean.isBool2());
        assertNull(bean.isBool3());
    }

    @Test
    public void shouldUnmarshalFalseObject() throws MarshalException, IOException, ValidationException {
        Bean bean = unmarshal("false-bean.xml");
        assertFalse(bean.isBool1());
        assertFalse(bean.isBool2());
        assertFalse(bean.isBool3());
    }

    @Test
    public void shouldUnmarshalTrueObject() throws MarshalException, IOException, ValidationException {
        Bean bean = unmarshal("true-bean.xml");
        assertTrue(bean.isBool1());
        assertTrue(bean.isBool2());
        assertTrue(bean.isBool3());
    }

    private String marshal(Object obj) throws IOException, MappingException, MarshalException, ValidationException {
        try (StringWriter writer = new StringWriter()) {
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(mapping);
            marshaller.marshal(obj);
            return writer.toString();
        }
    }

    private Bean unmarshal(String file) throws IOException, MarshalException, ValidationException {
        Unmarshaller unmarshaller = new Unmarshaller(Bean.class);
        try (InputStream in = getClass().getResourceAsStream(file)) {
            return (Bean) unmarshaller.unmarshal(new InputSource(in));
        }
    }
}
