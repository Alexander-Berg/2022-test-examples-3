package ru.yandex.market.stat.hyperduct.utils;

import java.time.LocalDate;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;

/**
 * Created by kateleb on 24.10.19.
 */
public class PeriodUtilsTest {

    @Test
    public void testToPeriod() {
        Assert.assertThat(PeriodUtils.toPeriod(LocalDate.parse("2019-06-22")), is("Jun-19"));
        Assert.assertThat(PeriodUtils.toPeriod(LocalDate.parse("2017-01-31")), is("Jan-17"));
        Assert.assertThat(PeriodUtils.toPeriod(LocalDate.parse("2003-12-22")), is("Dec-03"));
    }

    @Test
    public void testOnePeriod() {
        Assert.assertThat(PeriodUtils.lastPeriods(1, LocalDate.parse("2019-10-22")), is(Set.of("Oct-19")));
    }

    @Test
    public void test4PeriodsStartOfMonth() {
        Set<String> actual = PeriodUtils.lastPeriods(4, LocalDate.parse("2019-10-01"));
        Set<String> expected = Set.of("Jul-19", "Aug-19", "Sep-19", "Oct-19");

        Assert.assertThat(actual.size(), is(4));
        actual.removeAll(expected);
        Assert.assertThat(actual, is(Set.of()));
    }

    @Test
    public void test4PeriodsEndOfMonth() {
        Set<String> actual = PeriodUtils.lastPeriods(4, LocalDate.parse("2019-09-30"));
        Set<String> expected = Set.of("Jun-19", "Jul-19", "Aug-19", "Sep-19");

        Assert.assertThat(actual.size(), is(4));
        actual.removeAll(expected);
        Assert.assertThat(actual, is(Set.of()));
    }
}
