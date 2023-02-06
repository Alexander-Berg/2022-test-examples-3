package ru.yandex.direct.test.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.lang3.RandomUtils;

public class RandomNumberUtils {
    private RandomNumberUtils() {
    }

    public static long nextPositiveLong() {
        return nextPositiveLong(Long.MAX_VALUE);
    }

    public static long nextPositiveLong(long endExclusive) {
        return RandomUtils.nextLong(1, endExclusive);
    }

    public static int nextPositiveInteger() {
        return RandomUtils.nextInt(1, Integer.MAX_VALUE);
    }

    public static int nextPositiveInteger(int endExclusive) {
        return RandomUtils.nextInt(1, endExclusive);
    }

    public static BigInteger nextPositiveBigInteger() {
        return BigInteger.valueOf(nextPositiveLong());
    }

    public static double nextPositiveDouble() {
        return RandomUtils.nextDouble(Double.MIN_NORMAL, Integer.MAX_VALUE);
    }

    public static BigDecimal nextPositiveBigDecimal() {
        return BigDecimal.valueOf(nextPositiveDouble());
    }
}
