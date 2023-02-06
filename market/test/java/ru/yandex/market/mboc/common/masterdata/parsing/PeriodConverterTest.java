package ru.yandex.market.mboc.common.masterdata.parsing;

import java.time.LocalDate;

import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.parsing.utils.PeriodConverter;

import static org.assertj.core.api.Assertions.assertThat;

public class PeriodConverterTest {

    @Test
    public void testCorrectness() {
        var now = LocalDate.of(2020, 6, 1);
        assertThat(PeriodConverter.convertToStringRussian(null)).isNull();

        assertThat(PeriodConverter.convertToStringRussian("P1Y")).isEqualTo("1 год");
        assertThat(PeriodConverter.convertToStringRussian("P2Y")).isEqualTo("2 года");
        assertThat(PeriodConverter.convertToStringRussian("P5Y")).isEqualTo("5 лет");

        assertThat(PeriodConverter.convertToStringRussian("P1Y1M")).isEqualTo("13 месяцев");
        assertThat(PeriodConverter.convertToStringRussian("P1Y9M")).isEqualTo("21 месяц");
        assertThat(PeriodConverter.convertToStringRussian("P1Y10M")).isEqualTo("22 месяца");

        assertThat(PeriodConverter.convertToStringRussian("P1D")).isEqualTo("1 день");
        assertThat(PeriodConverter.convertToStringRussian("P2D")).isEqualTo("2 дня");
        assertThat(PeriodConverter.convertToStringRussian("P5D")).isEqualTo("5 дней");

        assertThat(PeriodConverter.convertToStringRussian("P1Y5D", now)).isEqualTo("370 дней");
        assertThat(PeriodConverter.convertToStringRussian("P1M2D", now)).isEqualTo("32 дня");
    }

}
