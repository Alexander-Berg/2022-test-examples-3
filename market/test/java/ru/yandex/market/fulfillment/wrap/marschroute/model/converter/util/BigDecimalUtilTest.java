package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BigDecimalUtilTest {

    @Test
    void testRoundUp() {
        BigDecimal actual = BigDecimalUtil.roundUp(new BigDecimal("3.333"), 2);

        assertThat(actual).isEqualTo(new BigDecimal("3.34"));
    }

    @Test
    void testMultiply() {
        BigDecimal actual = BigDecimalUtil.multiply(new BigDecimal("3.33"), 3);

        assertThat(actual).isEqualTo(new BigDecimal("9.99"));
    }

    @Test
    void testRoundUpToInt() {
        Integer actual = BigDecimalUtil.roundUpToInt(new BigDecimal("3.33"));

        assertThat(actual).isEqualTo(4);
    }
}
