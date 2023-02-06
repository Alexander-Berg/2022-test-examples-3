package ru.yandex.market.logistics.management.util;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to fill fields of classes contained only in entity.yado package.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class RandomObjectFiller {

    private static final Logger log = LoggerFactory.getLogger(RandomObjectFiller.class);
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private RandomObjectFiller() {
    }

    public static <T> T createAndFill(Class<T> clazz) {
        try {
            T instance = clazz.newInstance();
            Class<?> superclass;
            while ((superclass = instance.getClass().getSuperclass()) != Object.class) {
                for (Field field : superclass.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = getRandomValueForField(field);
                    field.set(instance, value);
                }
            }

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = getRandomValueForField(field);
                field.set(instance, value);
            }

            return instance;
        } catch (Exception e) {
            log.error("{}", e);
            return null;
        }
    }

    public static <T> List<T> fillList(Class<T> clazz, int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> RandomObjectFiller.createAndFill(clazz))
            .collect(Collectors.toList());
    }

    private static Object getRandomValueForField(Field field) {
        Class<?> type = field.getType();
        if ((type.equals(Integer.TYPE) || type.equals(Long.TYPE) ||
            type.equals(Integer.class) || type.equals(Long.class)) &&
            field.getName().equalsIgnoreCase("id")) {
            return null;
        }
        if (type.isEnum()) {
            Object[] enumValues = type.getEnumConstants();
            return enumValues[RANDOM.nextInt(enumValues.length)];
        } else if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
            return RANDOM.nextInt(32767);
        } else if (type.equals(Long.TYPE) || type.equals(Long.class)) {
            return RANDOM.nextLong();
        } else if (type.equals(String.class)) {
            return UUID.randomUUID().toString();
        } else if (type.equals(List.class)) {
            return new ArrayList<>();
        } else if (type.equals(LocalTime.class)) {
            return generateTime();
        } else if (type.equals(LocalDate.class)) {
            return LocalDate.now();
        } else if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
            return RANDOM.nextInt() % 2 == 0;
        }
        return createAndFill(type);
    }

    private static LocalTime generateTime() {
        int secondsInDay = 24 * 60 * 60;
        return LocalTime.ofSecondOfDay(RANDOM.nextInt(secondsInDay));
    }
}
