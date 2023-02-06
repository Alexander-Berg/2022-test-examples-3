package ru.yandex.market.pricelabs.misc;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    @Test
    void parseFromJson() {
        var json = "[null,{\"value\":1}]";
        var list = Utils.fromJsonStringList(json, SampleObject.class);
        assertEquals(Arrays.asList(null, new SampleObject(1)), list);
    }

    @ParameterizedTest
    @MethodSource("bytesToMibs")
    void testBytesToMibs(long value, String expect) {
        assertEquals(expect, Utils.bytesToMibs(value));
    }

    static Object[][] bytesToMibs() {
        return new Object[][]{
                {0, "0,00"},
                {1, "0,01"},
                {3973, "0,01"},
                {10 * 1024, "0,01"},
                {11 * 1024, "0,01"},
                {20 * 1024, "0,02"}
        };
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SampleObject implements ToJsonString {
        int value;
    }
}
