package ru.yandex.market.notification.simple.util;

import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import ru.yandex.market.notification.test.util.ClassUtils;
import ru.yandex.market.notification.test.util.DataSerializerUtils;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link XmlUtils}.
 *
 * @author Vladislav Bauer
 */
public class XmlUtilsTest {

    @Test
    public void testConstructor() {
        ClassUtils.checkConstructor(XmlUtils.class);
    }

    @Test
    public void testCreateMarshaller() throws Exception {
        final Marshaller marshaller = XmlUtils.createMarshaller(Object.class);
        assertThat(marshaller, notNullValue());
    }

    @Test
    public void testCreateUnmarshaller() throws Exception {
        final Unmarshaller unmarshaller = XmlUtils.createUnmarshaller(Object.class);
        assertThat(unmarshaller, notNullValue());
    }

    @Test(expected = MarshalException.class)
    public void testMarshal() throws Exception {
        final Marshaller marshaller = XmlUtils.createMarshaller(String.class);
        final byte[] bytes = XmlUtils.marshal(marshaller, 5);
        final String xml = DataSerializerUtils.toString(bytes);

        fail(xml);
    }

}
