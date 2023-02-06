package ru.yandex.market.crm.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CrmStringsNormalizeTest {

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, null},
                {"", ""},
                {"abc", "abc"},
                {"\0", ""},
                {"abc\0", "abc"},
                {"\0abc", "abc"},
                {"a\0bc", "abc"},
        });
    }

    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @MethodSource("data")
    public void checkResult(String target,
                            String expected) {
        String actual = CrmStrings.normalize(target);
        Assertions.assertEquals(expected, actual);
    }
}
