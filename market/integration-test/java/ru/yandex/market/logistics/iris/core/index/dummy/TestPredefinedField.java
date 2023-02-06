package ru.yandex.market.logistics.iris.core.index.dummy;

import ru.yandex.market.logistics.iris.core.index.field.Field;
import ru.yandex.market.logistics.iris.core.index.field.FieldBuilder;

/**
 * Перечень полей специально для тестов.
 */
public class TestPredefinedField {

    public static Field<String> DUMMY = FieldBuilder.builder("dummy", String.class)
        .isNullable(true)
        .build();
    public static Field<String> YUMMY = FieldBuilder.builder("yummy", String.class)
        .isNullable(true)
        .build();
    public static Field<String> GUMMY = FieldBuilder.builder("gummy", String.class)
        .isNullable(false)
        .build();

    private TestPredefinedField() {
        throw new UnsupportedOperationException();
    }
}
