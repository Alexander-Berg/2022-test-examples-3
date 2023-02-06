package ru.yandex.market.notification.simple.service.serial;

import org.junit.Test;

import ru.yandex.market.notification.service.serial.DataSerializer;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link DataSerializerFactory}.
 *
 * @author Vladislav Bauer
 */
public class DataSerializerFactoryTest {

    @Test
    public void testConstructorContract() {
        ClassUtils.checkConstructor(DataSerializerFactory.class);
    }

    @Test
    public void testCreate() {
        final DataSerializer serializer = DataSerializerFactory.create();

        assertThat(serializer, notNullValue());
        assertThat(serializer, instanceOf(XmlDataSerializer.class));
    }

}
