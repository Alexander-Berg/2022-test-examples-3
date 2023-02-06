package ru.yandex.market.mbo.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 25.12.2017
 */
public class ReflectionUtils {
    private ReflectionUtils() {
    }


    /**
     * Example, for method:
     * {@code Map<Integer, Set<String>> getMappings(long categoryId, List<Double> rates) throws OperationException}.
     * <p>
     * returns following:
     * Map, Integer, Set, String, long, List, Double, OperationException
     *
     * @return all classes which are used as return type, parameters and exceptions,
     * directly or by generic parameter
     */
    public static Set<Class> getUsedClasses(Method method) {
        Set<Class> classes = new HashSet<>();
        Stream.of(
                method.getGenericParameterTypes(),
                method.getGenericExceptionTypes(),
                new Type[]{method.getGenericReturnType()}
        ).flatMap(Stream::of)
                .map(ReflectionUtils::getGenericClasses)
                .forEach(classes::addAll);
        return classes;
    }

    /**
     * Returns all fields, including privates and inherited.
     */
    public static Set<Field> getAllFields(Class<?> aClass) {
        Set<Field> fields = Stream.of(aClass.getDeclaredFields()).collect(Collectors.toSet());
        if (aClass.getSuperclass() != null) {
            fields.addAll(getAllFields(aClass.getSuperclass()));
        }
        return fields;
    }

    /**
     * Example, for type {@code Map<Integer, Set<String>>} returns four classes: Map, Integer, Set, String.
     *
     * @return all classes bound in type
     */
    public static Set<Class> getGenericClasses(Type type) {
        if (type instanceof Class) {
            return Collections.singleton((Class) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Set<Class> classes = new HashSet<>();
            classes.add((Class) pt.getRawType());
            for (Type arg : pt.getActualTypeArguments()) {
                classes.addAll(getGenericClasses(arg));
            }
            return classes;
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            return getGenericClasses(arrayType.getGenericComponentType());
        } else if (type instanceof WildcardType) {
            return Collections.emptySet();
        } else if (type instanceof TypeVariable) {
            return Collections.emptySet();
        } else {
            throw new IllegalArgumentException("unexpected type: " + type);
        }
    }

    public static void set(Object object, String dottedPath, Object value) throws Exception {
        // This could be replaced with PropertyAccessorFactory.forBeanPropertyAccess(object).setPropertyValue,
        // but in Spring 3.2 it doesn't support nested path access... in 4.3 it does.
        String[] parts = dottedPath.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            Field field = object.getClass().getDeclaredField(parts[i]);
            field.setAccessible(true);
            if (i == parts.length - 1) {
                field.set(object, value);
            } else {
                Object v = field.get(object);
                if (v == null) {
                    Constructor<?> constructor = field.getType().getDeclaredConstructor();
                    constructor.setAccessible(true);
                    v = constructor.newInstance();
                    field.set(object, v);
                }
                object = v;
            }
        }
    }
}
