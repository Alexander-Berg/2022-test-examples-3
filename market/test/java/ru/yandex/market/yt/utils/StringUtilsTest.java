package ru.yandex.market.yt.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringUtilsTest {

    @ParameterizedTest
    @MethodSource("escapeYt")
    void testEscapeYt(String expect, String value) {
        assertEquals(expect, StringUtils.escape(value));
    }

    static Object[] escapeYt() {
        return new Object[][]{
                {"test", "test"},
                {"\\\"test\\\"", "\"test\""},
                {"\\\"te\\\\st\\\"", "\"te\\st\""},
                {"\\\\\\\\\\\\\\\\\\\\", "\\\\\\\\\\"},
        };
    }

}
