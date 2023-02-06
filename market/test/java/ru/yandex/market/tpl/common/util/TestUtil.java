package ru.yandex.market.tpl.common.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;
import org.springframework.util.StreamUtils;

import static org.mockito.Mockito.mock;

/**
 * @author valter
 */
public class TestUtil {

    @SneakyThrows
    public static String loadResourceAsString(String resourceName) {
        return StreamUtils.copyToString(getInputStream(resourceName), StandardCharsets.UTF_8);
    }

    private static InputStream getInputStream(String resourceName) {
        InputStream inputStream = TestUtil.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new RuntimeException("can't get resource " + resourceName);
        }
        return inputStream;
    }

    @SneakyThrows
    public static void setPrivateFinalField(Object o, String fieldName, Object value) {
        var field = o.getClass().getDeclaredField(fieldName);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.setAccessible(true);
        field.set(o, value);
        field.setAccessible(false);
        modifiersField.setInt(field, field.getModifiers() & Modifier.FINAL);
        modifiersField.setAccessible(false);
    }

    public static <T> T createStrictMock(Class<T> classToMock) {
        return mock(
                classToMock,
                invocation -> {
                    throw new RuntimeException("Not Mocked");
                });
    }


}
