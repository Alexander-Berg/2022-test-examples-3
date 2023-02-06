package ru.yandex.market.checkout.util;

public final class TestUtils {

    private TestUtils() {
    }

    public static boolean isYaTest() {
        return "1".equals(System.getenv("YA_TEST_RUNNER"));
    }
}
