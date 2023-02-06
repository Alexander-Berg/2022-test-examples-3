package ru.yandex.direct.currency;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Тест проверяет, что значения {@link Percent} равны даже когда их внутренние {@link BigDecimal} величины
 * не равны по {@code equals}, но равны по {@link BigDecimal#compareTo(Object) compareTo(..)}
 */
public class PercentEqualsTest {

    private Percent one = Percent.fromRatio(BigDecimal.ONE);
    private Percent morePreciseOne = Percent.fromRatio(BigDecimal.valueOf(1_000_000, 6));

    @Test
    public void ratioValues_notEqual() {
        Assertions.assertThat(one.asRatio()).isNotEqualTo(morePreciseOne.asRatio());
    }

    @Test
    public void percent_equals_whenRatioValuesDiffer() {
        Assertions.assertThat(one).isEqualTo(morePreciseOne);
    }

    @Test
    public void hashcode_equals_whenRatioValuesDiffer() {
        Assertions.assertThat(one.hashCode()).isEqualTo(morePreciseOne.hashCode());
    }

}
