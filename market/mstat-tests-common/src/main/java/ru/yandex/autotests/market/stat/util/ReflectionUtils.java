package ru.yandex.autotests.market.stat.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Created by entarrion on 08.05.15.
 */
public class ReflectionUtils {

    public static Field findFirstField(Class clazz, String fieldName) {
        return getAllFields(clazz).stream().filter(it -> it.getName().equals(fieldName)).findFirst().orElse(null);
    }

    public static List<Field> getAllFields(Class clazz) {
        ArrayList<Field> result = new ArrayList<>();
        while (clazz.getSuperclass() != null) {
            result.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return result;
    }

    public static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Can't create new instance for class: " + clazz, e);
        }
    }

    public static <O> Object getField(Field field, O object) {
        return fieldAction(field, it -> {
            try {
                return it.get(object);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Can't get field value.", e);
            }
        });
    }

    public static <O> Object getField(String fieldName, O object) {
        Field field;
        if (object != null && (field = findFirstField(object.getClass(), fieldName)) != null) {
            return getField(field, object);
        }
        return null;
    }

    public static <O, V> void setField(String fieldName, O object, V value) {
        Field field;
        if (object != null && (field = findFirstField(object.getClass(), fieldName)) != null) {
            setField(field, object, value);
        }
    }

    public static <O, V> void setField(Field field, O object, V value) {
        fieldAction(field, it -> {
            try {
                it.set(object, value);
                return true;
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Can't set field value.", e);
            }
        });
    }

    private static <R> R fieldAction(Field field, Function<Field, R> function) {
        boolean accessible = field.isAccessible();
        try {
            field.setAccessible(true);
            return function.apply(field);
        } finally {
            field.setAccessible(accessible);
        }
    }
}
