package ru.yandex.market.tpl.common.util.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.tpl.common.util.DateTimeUtil;

class RelativeTimeIntervalTest {

    @ParameterizedTest
    @CsvSource({
            "00:00-23:59",
            "1.00:00-2.23:59",
            "10:00-1.02:00",
            "10:00:00-1.02:00:00"
    })
    void shouldParseRelativeTimeInterval(String value) {
        RelativeTimeInterval.valueOf(value);
    }

    @ParameterizedTest
    @CsvSource({
            "asdasd",
    })
    void shouldFailToParseRelativeTimeInterval(String value) {
        Throwable throwable = Assertions.catchThrowable(() -> {
            RelativeTimeInterval.valueOf(value);
        });

        Assertions.assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "00:00,0,23:59,0,00:00-23:59",
            "10:00,0,02:00,1,10:00-1.02:00",
            "10:00,0,10:00,0,10:00-10:00",
            "00:00,1,23:59,1,1.00:00-1.23:59",
            "10:00,1,02:59,2,1.10:00-2.02:59"
    })
    void shouldConstructNewInstance(LocalTime startTime,
                                    int startDaysOffset,
                                    LocalTime endTime,
                                    int endDaysOffset,
                                    String expectedInterval) {
        var interval = new RelativeTimeInterval(startTime, startDaysOffset, endTime, endDaysOffset);
        Assertions.assertThat(interval.toDashStringNoSeconds()).isEqualTo(expectedInterval);
    }

    @ParameterizedTest
    @CsvSource({
            "23:59,0,00:00,0",
            "02:00,1,10:00,0",
            "23:59,1,00:00,1",
            "02:59,2,10:00,1"
    })
    void shouldNotConstructNewInstance(LocalTime startTime, int startDaysOffset, LocalTime endTime, int endDaysOffset) {
        Throwable throwable = Assertions.catchThrowable(() -> {
            new RelativeTimeInterval(startTime, startDaysOffset, endTime, endDaysOffset);
        });

        Assertions.assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreateFromInterval() {
        Interval interval = new Interval(
                ZonedDateTime.of(2021, 7, 23, 15, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant(),
                ZonedDateTime.of(2021, 7, 24, 2, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant()
        );
        RelativeTimeInterval interval2  = RelativeTimeInterval.fromInterval(interval, DateTimeUtil.DEFAULT_ZONE_ID);
        Assertions.assertThat(interval2.getStart()).isEqualTo("15:00:00");
        Assertions.assertThat(interval2.getStartDaysOffset()).isEqualTo(0);
        Assertions.assertThat(interval2.getEnd()).isEqualTo("02:00:00");
        Assertions.assertThat(interval2.getEndDaysOffSet()).isEqualTo(1);
    }

    @Test
    void shouldCreateFromAbsolute() {
        LocalDate base = LocalDate.of(2021, 7, 23);
        LocalDateTime start = LocalDateTime.of(base, LocalTime.of(15, 0, 0));
        LocalDateTime end = LocalDateTime.of(base.plusDays(1), LocalTime.of(2, 0, 0));
        RelativeTimeInterval interval2  = RelativeTimeInterval.fromAbsolute(base, start, end);
        Assertions.assertThat(interval2.getStart()).isEqualTo("15:00:00");
        Assertions.assertThat(interval2.getStartDaysOffset()).isEqualTo(0);
        Assertions.assertThat(interval2.getEnd()).isEqualTo("02:00:00");
        Assertions.assertThat(interval2.getEndDaysOffSet()).isEqualTo(1);
    }
}
