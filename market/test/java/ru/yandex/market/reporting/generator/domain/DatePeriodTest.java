package ru.yandex.market.reporting.generator.domain;

import org.junit.Test;

import java.time.YearMonth;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
public class DatePeriodTest {

    @Test
    public void monthsRange() {
        assertThat(new DatePeriod(YearMonth.of(2016, 8), YearMonth.of(2016, 11)).getDurationInMonth(), is(4));
        assertThat(new DatePeriod(YearMonth.of(2016, 8), YearMonth.of(2016, 8)).getDurationInMonth(), is(1));
    }

    @Test
    public void splitByMonths() {
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 8), YearMonth.of(2016, 11));

        assertThat(period.splitByMonths(1), hasSize(4));
        assertThat(period.splitByMonths(4), hasSize(1));

        assertThat(period.splitByMonths(2), contains(
            new DatePeriod(YearMonth.of(2016, 8), YearMonth.of(2016, 9)),
            new DatePeriod(YearMonth.of(2016, 10), YearMonth.of(2016, 11))));

        assertThat(period.splitByMonths(3), contains(
            new DatePeriod(YearMonth.of(2016, 8), YearMonth.of(2016, 10)),
            new DatePeriod(YearMonth.of(2016, 11), YearMonth.of(2016, 11))));
    }

}
