package ru.yandex.direct.jobs.segment.log;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.jobs.segment.log.LogTableNavigator.DAYS_BACK_LIMIT;
import static ru.yandex.direct.jobs.segment.log.LogTableNavigator.IGNORE_MISSED_DAYS_TRUE;

public class LogTableNavigatorFreshnessIgnoreMissedDaysTrueTest extends LogTableNavigatorTestBase {

    public LogTableNavigatorFreshnessIgnoreMissedDaysTrueTest() {
        super(IGNORE_MISSED_DAYS_TRUE);
    }

    @Test
    public void returnsYesterdayWhenTodayLogExists() {
        mockLogPeriod(TODAY.minusDays(2), TODAY);
        LocalDate actual = logTableNavigator.getTheMostFreshLogDate();
        assertThat(actual).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    public void returnsYesterdayWhenTomorrowLogExists() {
        mockLogPeriod(TODAY.minusDays(2), TODAY.plusDays(1));
        LocalDate actual = logTableNavigator.getTheMostFreshLogDate();
        assertThat(actual).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    public void returnsDayBeforeYesterdayWhenYesterdayLogExists() {
        mockLogPeriod(TODAY.minusDays(3), TODAY.minusDays(1));
        LocalDate actual = logTableNavigator.getTheMostFreshLogDate();
        assertThat(actual).isEqualTo(TODAY.minusDays(2));
    }

    @Test
    public void returns10DayAgoWhen9DayAgoLogExists() {
        mockLogPeriod(TODAY.minusDays(DAYS_BACK_LIMIT), TODAY.minusDays(DAYS_BACK_LIMIT - 1));
        LocalDate actual = logTableNavigator.getTheMostFreshLogDate();
        assertThat(actual).isEqualTo(TODAY.minusDays(DAYS_BACK_LIMIT));
    }

    @Test
    public void returnsDayBeforeYesterdayWhenTodayLogExistsButYesterdayDoesnt() {
        mockLogPeriod(TODAY.minusDays(2), TODAY);
        mockMissedDays(TODAY.minusDays(1));
        LocalDate actual = logTableNavigator.getTheMostFreshLogDate();
        assertThat(actual).isEqualTo(TODAY.minusDays(2));
    }

    @Test
    public void returnsDayBeforeGapInSeveralDays() {
        mockLogPeriod(TODAY.minusDays(8), TODAY.minusDays(2));
        mockMissedDays(TODAY.minusDays(4), TODAY.minusDays(3));
        LocalDate actual = logTableNavigator.getTheMostFreshLogDate();
        assertThat(actual).isEqualTo(TODAY.minusDays(5));
    }

    @Test
    public void failsOnNoLogs() {
        mockLogPeriod(TODAY.plusDays(1), TODAY);
        assertThrows(IllegalStateException.class, () -> logTableNavigator.getTheMostFreshLogDate());
    }

    @Test
    public void failsOnOnlyOneVeryOldDayLogExistingInCheckedPeriod() {
        mockLogPeriod(TODAY.minusDays(DAYS_BACK_LIMIT + 1),
                TODAY.minusDays(DAYS_BACK_LIMIT));
        assertThrows(IllegalStateException.class, () -> logTableNavigator.getTheMostFreshLogDate());
    }

    @Test
    public void failsOnOnlyOneDayLogExistingInCheckedPeriod() {
        mockLogPeriod(TODAY.minusDays(2), TODAY.minusDays(2));
        assertThrows(IllegalStateException.class, () -> logTableNavigator.getTheMostFreshLogDate());
    }
}
