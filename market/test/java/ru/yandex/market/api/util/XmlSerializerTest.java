package ru.yandex.market.api.util;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.junit.Assert;
import org.junit.Ignore;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * @author Denis Chernyshov
 */
@Ignore
public class XmlSerializerTest extends Assert {

    private static Mapping mapping;
    private static InputStream in;

    public static void freeClass() throws IOException {
        in.close();
    }

    public static void initClass(Class clazz, String resourcePath) throws IOException {
        mapping = new Mapping();
        in = clazz.getResourceAsStream(resourcePath);
        mapping.loadMapping(new InputSource(in));
    }

    protected String marshal(Object obj) throws IOException, MappingException, MarshalException, ValidationException {
        try (StringWriter writer = new StringWriter()) {
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(mapping);
            marshaller.marshal(obj);
            return writer.toString();
        }
    }

    protected <T> T unmarshal(Class clazz, String xml) throws IOException, MarshalException, ValidationException {
        Unmarshaller unmarshaller = new Unmarshaller(clazz);
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(ApiStrings.UTF8))) {
            return (T) unmarshaller.unmarshal(new InputSource(in));
        }
    }
}
