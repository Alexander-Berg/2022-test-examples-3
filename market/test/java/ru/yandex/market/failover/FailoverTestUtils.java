package ru.yandex.market.failover;

import java.lang.reflect.Field;

public class FailoverTestUtils {
    public static void setPrivate(Object o, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = getPrivateField(o, fieldName);
        field.set(o, value);
    }

    public static Field getPrivateField(Object o, String fieldName)
            throws NoSuchFieldException {
        Field field = getField(o.getClass(), fieldName);
        field.setAccessible(true);
        return field;
    }

    public static Field getField(Class clazzz, String fieldName) throws NoSuchFieldException {
        try {
            return clazzz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ex) {
            return getFieldFromSuperclass(clazzz, fieldName);
        }
    }

    public static Field getFieldFromSuperclass(Class clazzz,
                                               String fieldName)
            throws NoSuchFieldException {
        Class superclass = clazzz.getSuperclass();
        if (superclass == null || superclass.equals(clazzz)) {
            throw new NoSuchFieldException("No such field: " + fieldName);
        }
        return getField(superclass, fieldName);

    }

    public static <T> T getPrivate(Object o, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        return (T) getPrivateField(o, fieldName).get(o);
    }

    public static <T> boolean in(T obj, T[] list) {
        for (T item : list) {
            if (eq(obj, item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean eq(Object o1, Object o2) {
        return o1 == null && o2 == null ||
                o1 != null && o1.equals(o2);
    }

    public static boolean between(long value, long left, long right) {
        return value >= left && value <= right;
    }
}
