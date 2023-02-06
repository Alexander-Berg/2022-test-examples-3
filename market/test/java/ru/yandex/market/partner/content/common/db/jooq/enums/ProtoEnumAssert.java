package ru.yandex.market.partner.content.common.db.jooq.enums;

import org.junit.Assert;

import java.util.Arrays;

public class ProtoEnumAssert {
    public static void assertEnumsHaveSameValues(Enum[] expected, Enum[] actual) {
        Assert.assertArrayEquals(
            "Proto enum needs to be updated",
            Arrays.stream(expected).map(Enum::name).sorted().toArray(),
            Arrays.stream(actual).map(Enum::name).sorted().toArray());
    }
}
