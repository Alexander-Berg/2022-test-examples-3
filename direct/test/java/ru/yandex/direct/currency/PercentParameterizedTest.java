package ru.yandex.direct.currency;

import java.math.BigDecimal;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class PercentParameterizedTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Parameterized.Parameter(0)
    public BigDecimal ratioValue;

    @Parameterized.Parameter(1)
    public BigDecimal percentValue;

    @Parameterized.Parameters(name = "Check that ratio({0}) is equal to percent({1})")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {ZERO, ZERO},
                {ONE, ONE_HUNDRED},
        });
    }

    @Test
    public void fromPercent_success() {
        assertThat(Percent.fromPercent(percentValue).asRatio()).isEqualByComparingTo(ratioValue);
    }

    @Test
    public void fromRatio_success() {
        assertThat(Percent.fromRatio(ratioValue).asPercent()).isEqualByComparingTo(percentValue);
    }

}
