package ru.yandex.direct.test.utils;

public class RandomEnumUtils {
    private RandomEnumUtils() {
    }

    public static <T extends Enum<T>> T getRandomEnumValue(Class<T> enumClass) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException(enumClass.getName() + " is not Enum");
        }
        T[] enumValues = enumClass.getEnumConstants();
        return enumValues[RandomNumberUtils.nextPositiveInteger(enumValues.length)];
    }
}
