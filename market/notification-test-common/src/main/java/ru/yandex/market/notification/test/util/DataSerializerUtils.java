package ru.yandex.market.notification.test.util;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import ru.yandex.market.notification.service.serial.DataSerializer;
import ru.yandex.market.notification.simple.service.serial.DataSerializerFactory;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Утилитный класс для работы с {@link DataSerializer}.
 *
 * @author Vladislav Bauer
 */
public final class DataSerializerUtils {

    private static final DataSerializer DATA_SERIALIZER = DataSerializerFactory.create();
    private static final String FILE_POSTFIX = ".xml";


    private DataSerializerUtils() {
        throw new UnsupportedOperationException();
    }


    public static byte[] serialize(final Object object) {
        final byte[] data = DATA_SERIALIZER.serialize(object);

        assertThat(data, notNullValue());
        assertThat(data.length, greaterThan(0));

        return data;
    }

    public static <T> T deserialize(final byte[] data, final Class<T> objectClass) {
        final T restoredObject = DATA_SERIALIZER.deserialize(data, objectClass);

        assertThat(restoredObject, notNullValue());

        return restoredObject;
    }

    public static String serializeToString(final Object object) {
        final byte[] bytes = serialize(object);
        final String serialized = toString(bytes);

        assertThat(serialized, not(isEmptyOrNullString()));

        return serialized;
    }

    public static String toString(final byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static <T> T deserializeFromResource(final Class<T> clazz) throws Exception {
        final byte[] bytes = loadResource(clazz, clazz.getSimpleName() + FILE_POSTFIX);
        final T object = deserialize(bytes, clazz);

        assertThat(object, notNullValue());

        return object;
    }

    public static byte[] loadResource(final Class<?> clazz, final String fileName) throws Exception {
        final URL url = clazz.getResource(fileName);

        assertThat(url, notNullValue());

        return IOUtils.toByteArray(url);
    }

}
