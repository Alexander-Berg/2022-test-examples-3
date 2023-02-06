package ru.yandex.market.checkout.pushapi.client.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.pushapi.client.entity.BuilderUtil.buildList;
import static ru.yandex.market.checkout.pushapi.client.entity.BuilderUtil.buildMap;

/**
 * @author msavelyev
 */
public abstract class BaseBuilder<T, E extends BaseBuilder<T, E>> implements Builder<T> {
    private static final Logger log = LoggerFactory.getLogger(BaseBuilder.class);

    protected T object;

    protected BaseBuilder(T value) {
        this.object = value;
    }

    protected E copy() {
        try {
            final Class aClass = getClass();
            final E newInstance = (E) aClass.newInstance();

            Class<? super T> objectClass = (Class<T>) object.getClass();
            final T newObject = (T) objectClass.newInstance();

            while (objectClass != null) {
                for (Field field : objectClass.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                        field.setAccessible(true);
                        field.set(newObject, field.get(object));
                    }
                }
                objectClass = objectClass.getSuperclass();
            }

            newInstance.object = newObject;

            return newInstance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("can't CREATE new instance of builder", e);
        }
    }

    protected <V> E withField(String fieldName, Builder<V> value) {
        return withField(fieldName, value.build());
    }

    protected <V> E withField(String fieldName, Builder<V>[] values) {
        return withField(fieldName, buildList(values));
    }

    protected <V> E withField(String fieldName, List<Builder<V>> value) {
        return withField(fieldName, buildList(value));
    }

    protected <K, V> E withField(String fieldName, Map<K, Builder<V>> value) {
        return withField(fieldName, buildMap(value));
    }

    protected <V> E withField(String fieldName, V value) {
        final E copy = copy();

        final T obj = copy.object;
        try {
            trySetField(fieldName, value, obj);
        } catch (Exception e) {
            try {
                trySetBySetter(fieldName, value, obj);
            } catch (Exception e1) {
                log.error(e.getMessage(), e);
                log.error(e1.getMessage(), e1);
                fail("can't set value '" + value + "' to field '" + fieldName + "'");
            }
        }

        return copy;
    }

    private Field findField(final Class clazz, String fieldName) {
        Class objectClass = clazz;
        while (objectClass != null) {
            for (Field field : objectClass.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    field.setAccessible(true);
                    return field;
                }
            }
            objectClass = objectClass.getSuperclass();
        }

        throw new RuntimeException("no such field '" + fieldName + "' of class '" + clazz + "'");
    }

    private Method findMethod(final Class clazz, String methodName/*, Class ... types*/) {
        Class objectClass = clazz;
        while (objectClass != null) {
            for (Method method : objectClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)/* && Arrays.equals(method.getParameterTypes(), types)*/) {
                    return method;
                }
            }
            objectClass = objectClass.getSuperclass();
        }

        throw new RuntimeException("no such method '" + methodName + "' of class '" + clazz + "'");
    }

    private <V> void trySetBySetter(String fieldName, V value, T obj) {
        final Class<T> aClass = (Class<T>) obj.getClass();
        final String methodName = createSetterName(fieldName);
        try {
            final Method method = findMethod(aClass, methodName/*, getTypeOf(value)*/);
            method.setAccessible(true);
            method.invoke(obj, value);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(
                    "can't call setter '" + methodName + "' of class " + aClass + " with value '" + value + "'", e
            );
        }
    }

    private <V> Class<?> getTypeOf(V value) {
        return value == null ? null : value.getClass();
    }

    private <V> void trySetField(String fieldName, V value, T obj) {
        final Class<T> aClass = (Class<T>) obj.getClass();
        try {
            final Field field = findField(aClass, fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "can't set new value '" + value + "' to field '" + fieldName + "' of class " + aClass, e
            );
        }
    }

    private String createSetterName(String fieldName) {
        final char firstLetter = fieldName.charAt(0);
        final char upperFirstLetter = Character.toUpperCase(firstLetter);

        final String camelCaseFieldName = upperFirstLetter + fieldName.substring(1);

        return "set" + camelCaseFieldName;
    }

    @Override
    public T build() {
        return object;
    }
}
