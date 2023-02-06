package ru.yandex.market.ydb.integration.utils;

public final class TestUtils {

    private TestUtils() {
    }

    public static boolean isYaTest() {
        return "1".equals(System.getenv("YA_TEST_RUNNER"));
    }
}

