package ru.yandex.market.api.test.infrastructure.prerequisites;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ReflexUtils {

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(final Object object, final String fieldName) {
        try {
            final Class<?> objectClass = object.getClass();
            final Field field = objectClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (final Exception ex) {
            return null;
        }
    }

    public static <T extends AccessibleObject> T makeAccessible(final T object) {
        if (!object.isAccessible()) {
            object.setAccessible(true);
        }
        return object;
    }

    public static Collection<Annotation> findAllAnnotations(final Class<?> clazz) {
        final List<Annotation> result = new ArrayList<Annotation>();
        Class<?> current = clazz;

        while (current != Object.class && current != null) {
            final Class<?>[] interfaces = current.getInterfaces();
            for (final Class<?> i : interfaces) {
                result.addAll(findAllAnnotations(i));
            }

            result.addAll(Arrays.asList(current.getAnnotations()));
            current = current.getSuperclass();
        }
        return result;
    }


}
