package ru.yandex.market.olap2;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import lombok.SneakyThrows;

public class TestUtils {

    @SneakyThrows
    public static void setFinalStatic(Field field, Object newValue) {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}
