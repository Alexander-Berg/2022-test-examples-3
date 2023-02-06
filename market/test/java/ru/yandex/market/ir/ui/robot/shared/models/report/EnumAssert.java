package ru.yandex.market.ir.ui.robot.shared.models.report;

import org.junit.Assert;

import java.util.Arrays;

public class EnumAssert {
    public static void assertEnumsHaveSameValues(Enum[] expected, Enum[] actual) {
        Assert.assertArrayEquals(
            Arrays.stream(expected).map(Enum::name).sorted().toArray(),
            Arrays.stream(actual).map(Enum::name).sorted().toArray());
    }
}
