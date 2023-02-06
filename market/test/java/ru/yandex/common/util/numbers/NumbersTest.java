package ru.yandex.common.util.numbers;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class NumbersTest {
    @Test
    public void stripTrailingZerosAfterDot() {
        assertThat(Numbers.stripTrailingZerosAfterDot(null), nullValue());
        assertThat(Numbers.stripTrailingZerosAfterDot(BigDecimal.ZERO), sameInstance(BigDecimal.ZERO));
        assertThat(Numbers.stripTrailingZerosAfterDot(BigDecimal.ONE), sameInstance(BigDecimal.ONE));
        assertThat(Numbers.stripTrailingZerosAfterDot(BigDecimal.TEN), sameInstance(BigDecimal.TEN));
        assertThat(Numbers.stripTrailingZerosAfterDot(BigDecimal.valueOf(0)), sameInstance(BigDecimal.ZERO));

        Arrays.asList(
                BigDecimal.valueOf(-1L),
                BigDecimal.valueOf(1L),
                new BigDecimal("-1"),
                new BigDecimal("1")
        ).forEach(bd -> {
            assertThat(Numbers.stripTrailingZerosAfterDot(bd), sameInstance(bd));
        });

        checkEq("-1.", BigDecimal.valueOf(-1L));
        checkEq("1.000", BigDecimal.valueOf(1L));
        checkEq("-1.1", BigDecimal.valueOf(-1.1));
        checkEq("1.100000", BigDecimal.valueOf(1.1));
        checkEq("-1.000001", BigDecimal.valueOf(-1.000001));
        checkEq("100.000000", BigDecimal.valueOf(100L));
        checkEq("-100.0", BigDecimal.valueOf(-100L));
        checkEq("100.", BigDecimal.valueOf(100L));

        ensureStripped("1.000");
        ensureStripped("1.100");
        ensureStripped("100.000000");
    }

    private static void checkEq(String input, BigDecimal expected) {
        assertThat(Numbers.stripTrailingZerosAfterDot(new BigDecimal(input)), equalTo(expected));
    }

    private static void ensureStripped(String input) {
        BigDecimal bd = new BigDecimal(input);
        assertThat(Numbers.stripTrailingZerosAfterDot(bd), not(equalTo(bd)));
    }
}
