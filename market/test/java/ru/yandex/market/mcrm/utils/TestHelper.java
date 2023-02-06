package ru.yandex.market.mcrm.utils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;

public class TestHelper {

    public static String randomString() {
        return UUID.randomUUID().toString();
    }

    public static void assertEquals(OffsetDateTime expected, OffsetDateTime actual, int secondDif) {
        if (null == expected && null == actual) {
            return;
        }
        Assertions.assertNotNull(expected);
        Assertions.assertNotNull(actual);

        long seconds = Duration.between(expected, actual).getSeconds();
        Assertions.assertTrue(Math.abs(seconds) <= secondDif,
                "Date '" + expected + "' differs from '" + actual + "' for more than " + secondDif + " seconds");
    }

    public static void assertEquals(String msg, Duration expected, Duration actual, int secondDif) {
        long seconds = expected.minus(actual).getSeconds();
        Assertions.assertTrue(Math.abs(seconds) <= secondDif,
                msg + " (diff of duration '" + expected + "' and '" + actual + "' is more than " + secondDif + " seconds)");
    }
}
