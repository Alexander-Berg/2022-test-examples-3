package ru.yandex.market.crm.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CrmStringsNullIfTrimEmptyTest {

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, null},
                {"", null},
                {" \n \t \r", null},
                {"value", "value"},
                {" value ", " value "}
        });
    }

    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @MethodSource("data")
    public void checkResult(String target,
                            String expected) {
        Assertions.assertEquals(expected, CrmStrings.nullIfTrimEmpty(target));
    }
}
