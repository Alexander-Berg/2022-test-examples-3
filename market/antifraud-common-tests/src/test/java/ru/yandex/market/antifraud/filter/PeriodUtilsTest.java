package ru.yandex.market.antifraud.filter;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class PeriodUtilsTest {
    @Test
    public void testAllCases() {
        assertThat(
                PeriodUtils.truncateToDay(DateTime.parse("2010-06-30T01:20")),
                is(DateTime.parse("2010-06-30")));
        assertThat(
                PeriodUtils.truncateToDay(DateTime.parse("2010-06-30T01:20")),
                not(DateTime.parse("2010-06-30T01:20")));

        assertThat(
                PeriodUtils.truncateToHour(DateTime.parse("2010-06-30T01:20")),
                is(DateTime.parse("2010-06-30T01:00")));
        assertThat(
                PeriodUtils.truncateToHour(DateTime.parse("2010-06-30T01:20")),
                not(DateTime.parse("2010-06-30T01:20")));

        assertThat(
                PeriodUtils.truncateToMinute(DateTime.parse("2010-06-30T01:20:45")),
                is(DateTime.parse("2010-06-30T01:20:00")));
        assertThat(
                PeriodUtils.truncateToMinute(DateTime.parse("2010-06-30T01:20:45")),
                not(DateTime.parse("2010-06-30T01:20:45")));
    }
}
