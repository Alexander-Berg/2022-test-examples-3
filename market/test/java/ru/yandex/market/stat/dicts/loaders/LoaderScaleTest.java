package ru.yandex.market.stat.dicts.loaders;

import org.junit.Test;

import ru.yandex.inside.yt.kosher.cypress.RichYPath;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.stat.utils.DateUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class LoaderScaleTest {

    private static final Clock TEST_CLOCK = DateUtil.fixedClock("2019-04-03T03:14:00");

    @Test
    public void testDefaultScale() {
        LoaderScale scale = LoaderScale.DEFAULT;
        LocalDateTime defaultPartition = scale.defaultDateTimeToLoad(TEST_CLOCK);
        assertThat(defaultPartition, equalTo(LocalDate.parse("2019-04-02").atStartOfDay()));
        assertThat(scale.formatPartition(defaultPartition), equalTo("2019-04-02"));
        assertThat(scale.getNextPartition(defaultPartition, TEST_CLOCK), equalTo(LocalDate.parse("2019-04-03").atStartOfDay()));
    }

    @Test
    public void testDailyScale() {
        LoaderScale scale = LoaderScale.DAYLY;
        LocalDateTime defaultPartition = scale.defaultDateTimeToLoad(TEST_CLOCK);
        assertThat(defaultPartition, equalTo(LocalDate.parse("2019-04-02").atStartOfDay()));
        assertThat(scale.formatPartition(defaultPartition), equalTo("1d/2019-04-02"));
        assertThat(scale.getNextPartition(defaultPartition, TEST_CLOCK), equalTo(LocalDate.parse("2019-04-03").atStartOfDay()));
    }

    @Test
    public void testMonthlyScale() {
        LoaderScale scale = LoaderScale.DEFAULT_MONTH;
        LocalDateTime defaultPartition = scale.defaultDateTimeToLoad(TEST_CLOCK);
        assertThat(defaultPartition, equalTo(LocalDate.parse("2019-04-03").atStartOfDay()));
        assertThat(scale.formatPartition(defaultPartition), equalTo("2019-04-01"));
        assertThat(scale.getNextPartition(defaultPartition, TEST_CLOCK), equalTo(LocalDate.parse("2019-04-03").atStartOfDay()));
    }

    @Test
    public void testHourlyScale() {
        LoaderScale scale = LoaderScale.HOURLY;
        LocalDateTime defaultPartition = scale.defaultDateTimeToLoad(TEST_CLOCK);
        assertThat(defaultPartition, equalTo(LocalDateTime.parse("2019-04-03T02:00:00")));
        assertThat(scale.formatPartition(defaultPartition), equalTo("1h/2019-04-03T02:00:00"));
        assertThat(scale.getNextPartition(defaultPartition, TEST_CLOCK), equalTo(LocalDateTime.parse("2019-04-03T02:00:00")));
    }

    @Test
    public void testStringToTime() {
        assertThat(LoaderScale.HOURLY.stringToTime("2021-01-13T15:00:00"), equalTo(
                LocalDateTime.of(2021, 1, 13, 15, 0, 0)
        ));
        assertThat(LoaderScale.DAYLY.stringToTime("2021-01-13"), equalTo(
                LocalDateTime.of(2021, 1, 13, 0, 0, 0)
        ));
    }

}
