package ru.yandex.direct.jobs.segment.log;

import java.time.LocalDate;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.jobs.segment.SegmentTestUtils.createYtTable;
import static ru.yandex.direct.jobs.segment.log.LogTableNavigator.IGNORE_MISSED_DAYS_FALSE;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

// ВАЖНО: последний доступный день логов навигатором не учитывается
public class LogTableNavigatorIteratorIgnoreMissedDaysFalseTest extends LogTableNavigatorTestBase {

    private static final int DEFAULT_MAX_DAYS = 10;

    public LogTableNavigatorIteratorIgnoreMissedDaysFalseTest() {
        super(IGNORE_MISSED_DAYS_FALSE);
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

    @Test
    public void hasNextMethodDoesntFailBeforeSpaceInLogs() {
        mockLogPeriod(TODAY.minusDays(4), TODAY.minusDays(1));
        mockMissedDays(TODAY.minusDays(3));
        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(4), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isTrue();
    }

    @Test
    public void nextMethodDoesntFailBeforeSpaceInLogs() {
        mockLogPeriod(TODAY.minusDays(4), TODAY.minusDays(1));
        mockMissedDays(TODAY.minusDays(3));

        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(4), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(4)));
    }

    @Test
    public void hasNextMethodFailsOnSpaceInLogs() {
        mockLogPeriod(TODAY.minusDays(4), TODAY.minusDays(1));
        mockMissedDays(TODAY.minusDays(3));

        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(4), DEFAULT_MAX_DAYS);
        assumeThat(iterator.hasNext(), equalTo(true));
        assumeThat(iterator.next(), equalTo(createDayLog(TODAY.minusDays(4))));
        assertThrows(IllegalStateException.class, iterator::hasNext);
    }

    @Test
    public void nextMethodFailsOnSpaceInLogs() {
        mockLogPeriod(TODAY.minusDays(4), TODAY.minusDays(1));
        mockMissedDays(TODAY.minusDays(3));

        Iterator<DayLog> iterator = logTableNavigator.getDayLogsIterator(TODAY.minusDays(4), DEFAULT_MAX_DAYS);
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(createDayLog(TODAY.minusDays(4)));
        assertThrows(IllegalStateException.class, iterator::next);
    }

    private DayLog createDayLog(LocalDate localDate) {
        return new DayLog(localDate, createYtTable(localDate));
    }
}
