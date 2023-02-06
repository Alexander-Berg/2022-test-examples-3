package ru.yandex.market.marketpromo.test.utils;

/**
 * @author kl1san
 */
public final class TestUtils {

    private TestUtils() {
    }

    public static boolean isYaTest() {
        return "1".equals(System.getenv("YA_TEST_RUNNER"));
    }
}
