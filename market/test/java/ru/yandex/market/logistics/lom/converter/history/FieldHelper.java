package ru.yandex.market.logistics.lom.converter.history;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
@ParametersAreNonnullByDefault
class FieldHelper {
    private static final Map<Class<?>, FieldSetter> SPECIFIC_FIELD_SETTERS = Map.of(
        boolean.class, (target, field, value) -> toggleBooleanField(target, field),
        Set.class, (target, field, value) -> emptySet(target, field),
        List.class, (target, field, value) -> emptyList(target, field),
        Map.class, (target, field, value) -> emptyMap(target, field),
        Instant.class, (target, field, value) -> datePlusOneDay(target, field)
    );

    @FunctionalInterface
    private interface FieldSetter {
        void setValue(Object target, Field field, @Nullable Object value);
    }

    /**
     * Установить значение поля объекта.
     *
     * @param target    объект, в котором устанавливается поле
     * @param fieldName название устанавливаемого поля
     * @param value     значение
     */
    @SneakyThrows
    void setFieldValue(Object target, String fieldName, @Nullable Object value) {
        Class<?> c = target.getClass();
        Field field = getField(c, fieldName);
        field.setAccessible(true);
        SPECIFIC_FIELD_SETTERS.getOrDefault(field.getType(), FieldHelper::setDefaultValue)
            .setValue(target, field, value);
    }

    /**
     * Получить значение поля объекта.
     *
     * @param target    объект
     * @param fieldName имя поля
     * @return значение
     */
    @SneakyThrows
    Object getFieldValue(Object target, String fieldName) {
        Class<?> c = target.getClass();
        Field field = getField(c, fieldName);
        if (Collection.class.isAssignableFrom(field.getType())) {
            field.setAccessible(true);
            Collection<?> result = (Collection<?>) field.get(target);
            return result.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Collection is empty"));
        }
        field.setAccessible(true);
        return field.get(target);
    }

    /**
     * Получить поле класса по имени.
     *
     * @param c         класс
     * @param fieldName имя поля
     * @return поле
     */
    @Nonnull
    private Field getField(Class<?> c, String fieldName) throws NoSuchFieldException {
        try {
            return c.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> sc = c.getSuperclass();
            if (sc == null) {
                throw e;
            }
            return getField(sc, fieldName);
        }
    }

    @SneakyThrows
    private void setDefaultValue(Object target, Field field, @Nullable Object value) {
        field.set(target, value);
    }

    @SneakyThrows
    private void toggleBooleanField(Object target, Field field) {
        field.setBoolean(target, !(boolean) getFieldValue(target, field.getName()));
    }

    @SneakyThrows
    private void emptySet(Object target, Field field) {
        field.set(target, Set.of());
    }

    @SneakyThrows
    private void emptyList(Object target, Field field) {
        field.set(target, List.of());
    }

    @SneakyThrows
    private void emptyMap(Object target, Field field) {
        field.set(target, Map.of());
    }

    @SneakyThrows
    private void datePlusOneDay(Object target, Field field) {
        field.set(target, ((Instant) getFieldValue(target, field.getName())).plus(Duration.ofDays(1)));
    }
}
