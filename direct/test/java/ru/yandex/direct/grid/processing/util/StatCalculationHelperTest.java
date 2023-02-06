package ru.yandex.direct.grid.processing.util;

import java.math.BigDecimal;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import static ru.yandex.direct.grid.processing.util.StatCalculationHelper.percent;
import static ru.yandex.direct.grid.processing.util.StatCalculationHelper.ratio;

public class StatCalculationHelperTest {
    @Test
    public void testRatio() {
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(ratio(BigDecimal.TEN, BigDecimal.ZERO))
                .isNull();
        soft.assertThat(ratio(BigDecimal.TEN, BigDecimal.ONE))
                .isEqualTo(BigDecimal.valueOf(1000, 2));
        soft.assertThat(ratio(BigDecimal.TEN, BigDecimal.valueOf(4)))
                .isEqualTo(BigDecimal.valueOf(250, 2));
        soft.assertThat(ratio(BigDecimal.TEN, BigDecimal.valueOf(3)))
                .isEqualTo(BigDecimal.valueOf(333, 2));
        soft.assertThat(ratio(BigDecimal.valueOf(1.256), BigDecimal.TEN))
                .isEqualTo(BigDecimal.valueOf(13, 2));

        soft.assertAll();
    }

    @Test
    public void testPercent() {
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(percent(BigDecimal.TEN, BigDecimal.ZERO))
                .isNull();
        soft.assertThat(percent(BigDecimal.TEN, BigDecimal.ONE))
                .isEqualTo(BigDecimal.valueOf(100000, 2));
        soft.assertThat(percent(BigDecimal.TEN, BigDecimal.valueOf(4)))
                .isEqualTo(BigDecimal.valueOf(25000, 2));
        soft.assertThat(percent(BigDecimal.TEN, BigDecimal.valueOf(3)))
                .isEqualTo(BigDecimal.valueOf(33333, 2));
        soft.assertThat(percent(BigDecimal.valueOf(1.256), BigDecimal.TEN))
                .isEqualTo(BigDecimal.valueOf(1256, 2));

        soft.assertAll();
    }

}
