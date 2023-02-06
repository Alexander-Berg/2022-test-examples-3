package ru.yandex.direct.jobs.segment.log;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.ytwrapper.model.YtTable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.segment.SegmentTestUtils.TEST_LOG_PATH_PROVIDER;
import static ru.yandex.direct.jobs.segment.common.SegmentUtils.ROW_COUNT_ATTRIBUTE_NAME;
import static ru.yandex.direct.jobs.segment.log.LogTableNavigator.DAYS_BACK_LIMIT;
import static ru.yandex.direct.jobs.segment.log.LogTableNavigator.IGNORE_MISSED_DAYS_FALSE;

public class LogTableNavigatorFreshnessIgnoreMissedDaysFalseTest extends LogTableNavigatorTestBase {

    public LogTableNavigatorFreshnessIgnoreMissedDaysFalseTest() {
        super(IGNORE_MISSED_DAYS_FALSE);
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
    public void failsOnSpaceInLogs() {
        mockLogPeriod(TODAY.minusDays(DAYS_BACK_LIMIT), TODAY);
        mockMissedDays(TODAY.minusDays(1));
        assertThrows(IllegalStateException.class, () -> logTableNavigator.getTheMostFreshLogDate());
    }

    @Test
    public void failsOnNoLogs() {
        mockLogPeriod(TODAY.plusDays(1), TODAY);
        assertThrows(IllegalStateException.class, () -> logTableNavigator.getTheMostFreshLogDate());
    }

    @Test
    public void failsOnOnlyOneDayLogExistingInCheckedPeriod() {
        mockLogPeriod(TODAY.minusDays(DAYS_BACK_LIMIT + 1),
                TODAY.minusDays(DAYS_BACK_LIMIT));
        assertThrows(IllegalStateException.class, () -> logTableNavigator.getTheMostFreshLogDate());
    }

    @Test
    public void checksAlsoTableRowCount() {
        mockLogPeriod(TODAY.minusDays(4), TODAY);
        YtTable todayLogTable = new YtTable(TEST_LOG_PATH_PROVIDER.apply(TODAY));
        when(ytOperator.readTableNumericAttribute(eq(todayLogTable), eq(ROW_COUNT_ATTRIBUTE_NAME)))
                .thenReturn(0L);
        LocalDate actual = logTableNavigator.getTheMostFreshLogDate();
        assertThat(actual).isEqualTo(TODAY.minusDays(2));
    }
}
