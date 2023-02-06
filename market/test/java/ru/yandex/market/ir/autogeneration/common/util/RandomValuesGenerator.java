package ru.yandex.market.ir.autogeneration.common.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

public class RandomValuesGenerator {
    private static final long SEED = 1003;
    private static final Random RANDOM = new Random(SEED);


    public static String randomAlphabetic(int length) {
        return RandomStringUtils.random(length, 0, 0, true, false, null, RANDOM);
    }

    public static String randomAlphabetic8() {
        return randomAlphabetic(8);
    }

    public static String randomAlphanumeric(int length) {
        return RandomStringUtils.random(length, 0, 0, true, true, null, RANDOM);
    }

    public static int randomPositiveInt() {
        return RANDOM.nextInt(Integer.MAX_VALUE);
    }

    public static long randomPositiveIntAsLong() {
        return Long.valueOf(RANDOM.nextInt(Integer.MAX_VALUE));
    }
}
