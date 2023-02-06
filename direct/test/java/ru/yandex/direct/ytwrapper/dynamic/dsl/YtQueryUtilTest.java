package ru.yandex.direct.ytwrapper.dynamic.dsl;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class YtQueryUtilTest {

    @Test
    public void constantDecimalMultiplier_valid() {
        // да, этот тест проверяет не логику, а значение константы, использование которой размазано по коду
        assertThat(YtQueryUtil.DECIMAL_MULT.longValue()).isEqualTo(1_000_000L);
    }

    @Test
    public void constantPercentDecimalMultiplier_valid() {
        // да, этот тест проверяет не логику, а значение константы, использование которой размазано по коду
        assertThat(YtQueryUtil.PERCENT_DECIMAL_MULT.longValue()).isEqualTo(100_000_000L);
    }

}
