package ru.yandex.direct.core.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.apache.commons.lang3.RandomUtils.nextLong;

/**
 * @see ru.yandex.direct.core.entity.keyword.service.KeywordUtilsKeywordCloneTest
 * @see ru.yandex.direct.core.entity.banner.bean.BannerBeanUtilTest
 */
@ParametersAreNonnullByDefault
public class CloneTestUtil {

    private static final BiFunction<Object, Field, Object> RANDOM_SETTER =
            (object, field) -> {
                Object value = generateRandomValue(field);
                set(field, object, value);
                return value;
            };

    /**
     * Заполнить все поля объекта случайными значениями.
     */
    public static List<Object> fill(Object object) {
        return StreamEx.of(getAllFields(object.getClass()))
                .map(field -> RANDOM_SETTER.apply(object, field))
                .toList();
    }

    public static List<Object> fillIgnoring(Object object, String... ignoredFields) {
        Set<String> ignoredFieldsSet = new HashSet<>(asList(ignoredFields));
        return fillIgnoring(object, ignoredFieldsSet);
    }

    public static List<Object> fillIgnoring(Object object, Set<String> ignoredFieldsSet) {
        return StreamEx.of(getAllFields(object.getClass()))
                .remove(field -> ignoredFieldsSet.contains(field.getName()))
                .map(field -> RANDOM_SETTER.apply(object, field))
                .toList();
    }

    public static Object generateRandomValue(Field field) {
        Class<?> type = field.getType();

        if (type == Long.class) {
            return nextLong(0, Long.MAX_VALUE);
        } else if (type == Integer.class) {
            return nextInt(0, Integer.MAX_VALUE);
        } else if (type == BigInteger.class) {
            return BigInteger.valueOf(nextLong(0, Long.MAX_VALUE));
        } else if (type == BigDecimal.class) {
            return BigDecimal.valueOf(nextLong(0, Long.MAX_VALUE));
        } else if (type == String.class) {
            return randomAlphabetic(10);
        } else if (type == Boolean.class) {
            return Integer.valueOf(0).equals(nextInt(0, 2));
        } else if (type == LocalDateTime.class) {
            return LocalDateTime.now().plusDays(nextLong(1, 5000)).plusMinutes(nextLong(0, 5000));
        } else if (Enum.class.isAssignableFrom(type)) {
            return field.getType().getEnumConstants()[0];
        } else {
            throw new IllegalStateException(
                    String.format("невозможно сгенерировать значение типа \"%s\" для поля \"%s\"",
                            type.getCanonicalName(), field.getName()));
        }
    }

    private static void set(Field field, Object obj, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Object get(Field field, Object obj) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> allFields = new ArrayList<>();

        Class<?> currentClass = clazz;
        while (currentClass != Object.class) {
            Arrays.stream(currentClass.getDeclaredFields())
                    // skip possible $jacocoData field
                    .filter(f -> !f.isSynthetic())
                    .filter(f -> !Modifier.isStatic(f.getModifiers()))
                    .forEach(allFields::add);
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }
}
