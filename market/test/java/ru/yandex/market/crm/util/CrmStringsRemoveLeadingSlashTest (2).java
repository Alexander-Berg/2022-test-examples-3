package ru.yandex.market.crm.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runners.Parameterized;

public class CrmStringsRemoveLeadingSlashTest {

    @Parameterized.Parameters(name = "{index}: {0} -> {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"abc", "abc"},
                {"//abc", "/abc"},
                {"/abc", "abc"},
                {"", ""},
                {null, null}
        });
    }

    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @MethodSource("data")
    public void checkResult(String target,
                            String expected) {
        Assertions.assertEquals(expected, CrmStrings.removeLeadingSlash(target));
    }
}
