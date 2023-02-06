package ru.yandex.market.antifraud.filter;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RndUtil {
    public static boolean nextBool() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    // not starting with 0
    public static String randomNumeric(int length) {
        return Integer.toString(nextInt(9) + 1) +
                RandomStringUtils.randomNumeric(length - 1);
    }

    public static String randomAlphabetic(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }

    public static <E> E choice(List<E> list) {
        return list.get(nextInt(list.size()));
    }

    public static int choice(int[] arr) {
        return arr[nextInt(arr.length)];
    }

    public static <E> E choice(Set<E> set) {
        List<E> l = new ArrayList<E>(set);
        return l.get(nextInt(l.size()));
    }

    public static Long nextPostitiveLong() {
        return ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    }
}
