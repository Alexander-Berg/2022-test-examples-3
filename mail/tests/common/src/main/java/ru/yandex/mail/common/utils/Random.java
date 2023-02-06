package ru.yandex.mail.common.utils;

import org.apache.commons.lang3.RandomStringUtils;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class Random {
    public static String address() {
        return randomAlphabetic(8) + "@yandex.ru";
    }

    public static String string() {
        return RandomStringUtils.random(10, true, true);
    }

    public static int shortInt() {
        return new java.util.Random().nextInt(6) + 1;
    }

    public static String longString() {
        return randomAlphanumeric(800);
    }
}
