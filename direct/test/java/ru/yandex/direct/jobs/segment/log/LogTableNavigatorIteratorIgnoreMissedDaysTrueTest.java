package ru.yandex.direct.jobs.segment.log;

import java.time.LocalDate;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.jobs.segment.SegmentTestUtils.createYtTable;
import static ru.yandex.direct.jobs.segment.log.LogTableNavigator.IGNORE_MISSED_DAYS_TRUE;

// ВАЖНО: последний доступный день логов навигатором не учитывается
public class LogTableNavigatorIteratorIgnoreMissedDaysTrueTest extends LogTableNavigatorTestBase {

    private static final int DEFAULT_MAX_DAYS = 10;

    public LogTableNavigatorIteratorIgnoreMissedDaysTrueTest() {
        super(IGNORE_MISSED_DAYS_TRUE);
    }

    @Test
    public void getOneDayIteratorOverOneDayLog() {
        mockLogPeriod(TODAY.minusDays(1), TODAY);
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(1), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(1)));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void getOneDayIteratorOverTwoDaysLog() {
        mockLogPeriod(TODAY.minusDays(2), TODAY);
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(1), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(1)));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void getTwoDaysIteratorOverTwoDaysLog() {
        mockLogPeriod(TODAY.minusDays(2), TODAY);
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(2), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(2)));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(1)));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void getTwoDaysIteratorOverTwoDaysLogInPastLimitedByMaxDays() {
        mockLogPeriod(TODAY.minusDays(70), TODAY.minusDays(6));
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(50), 2);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(50)));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(49)));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void getOneDayIteratorLimitedByMaxParam() {
        mockLogPeriod(TODAY.minusDays(8), TODAY.minusDays(3));
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(8), 1);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(8)));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void getTwoDayIteratorLimitedByMaxParam() {
        mockLogPeriod(TODAY.minusDays(8), TODAY.minusDays(3));
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(8), 2);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(8)));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(7)));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void failsOnUnexistingLogs() {
        mockLogPeriod(TODAY.plusDays(1), TODAY);
        assertThrows(IllegalStateException.class, () -> {
            logTableNavigator.getDayLogsIterator(TODAY.minusDays(8), DEFAULT_MAX_DAYS);
        });
    }

    // в итераторе самой свежей датой будет today-7
    @Test
    public void getOneDayIteratorWhenDayIsMissing() {
        mockLogPeriod(TODAY.minusDays(10), TODAY.minusDays(5));
        mockMissedDays(TODAY.minusDays(6));
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(6), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterator.hasNext()).isFalse();
    }

    // в итераторе самой свежей датой будет today-7
    @Test
    public void nextMethodFailsOnOneDayLogWithMissingDay() {
        mockLogPeriod(TODAY.minusDays(10), TODAY.minusDays(5));
        mockMissedDays(TODAY.minusDays(6));
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(6), DEFAULT_MAX_DAYS);
        assertThrows(IllegalStateException.class, iterator::next);
    }

    // в итераторе самой свежей датой будет today-7
    @Test
    public void nextMethodFailsOnOneDayLogWithMissingDayAfterHasNextCall() {
        mockLogPeriod(TODAY.minusDays(10), TODAY.minusDays(5));
        mockMissedDays(TODAY.minusDays(6));
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(6), 1);
        assertThat(iterator.hasNext()).isFalse();
        assertThrows(IllegalStateException.class, iterator::next);
    }

    @Test
    public void getTwoDayIteratorWhenFirstDayIsMissing() {
        mockLogPeriod(TODAY.minusDays(10), TODAY.minusDays(5));
        mockMissedDays(TODAY.minusDays(7));

        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(7), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(6)));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void getTwoDayIteratorWhenFirstDayIsMissingLimitedByMaxDays() {
        mockLogPeriod(TODAY.minusDays(10), TODAY.minusDays(5));
        mockMissedDays(TODAY.minusDays(8));
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(8), 2);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(7)));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void getTwoDayIteratorWhenSecondDayIsMissing() {
        mockLogPeriod(TODAY.minusDays(10), TODAY.minusDays(5));
        mockMissedDays(TODAY.minusDays(6));

        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(7), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(7)));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void getSeveralDaysIteratorWhenTwoDaysInTheMiddleIsMissing() {
        mockLogPeriod(TODAY.minusDays(10), TODAY.minusDays(3));
        mockMissedDays(TODAY.minusDays(6), TODAY.minusDays(5));

        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(8), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(8)));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(7)));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(4)));
        assertThat(iterator.hasNext()).isFalse();
    }

    private DayLog createDayLog(LocalDate localDate) {
        return new DayLog(localDate, createYtTable(localDate));
    }
}
