package ru.yandex.market.core.calendar;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DailyCalendarExportUtils}.
 *
 * @author ivmelnik
 * @since 13.12.17
 */
public class DailyCalendarExportUtilsTest {

    private static final int DAYS = 5;

    private static final LocalDate TEST_DAY = LocalDate.now();
    private static final LocalDate TEST_DAY_PLUS = TEST_DAY.plusDays(DAYS);
    private static final LocalDate TEST_DAY_MINUS = TEST_DAY.minusDays(DAYS);

    private static final DatePeriod TEST_PERIOD = DatePeriod.of(TEST_DAY, TEST_DAY_PLUS);
    private static final List<LocalDate> TEST_DAYS = asList(TEST_DAY, TEST_DAY.plusDays(1), TEST_DAY_PLUS, TEST_DAY_MINUS, TEST_DAY_PLUS.plusDays(1));
    private static final List<Integer> TEST_DAY_NUMBERS = asList(-DAYS, 0, 1, DAYS, DAYS + 1);
    private static final List<Integer> TEST_DAY_NUMBERS_WITHIN = asList(0, 1, DAYS);
    private static final List<LocalDate> TEST_DAYS_OUT = asList(TEST_DAY_MINUS, TEST_DAY_PLUS.plusDays(1));

    @Test
    public void dayNumberCommon() throws Exception {
        int dayNumber = DailyCalendarExportUtils.getDayNumber(TEST_DAY, TEST_DAY_PLUS);
        assertEquals(DAYS, dayNumber);
    }

    @Test
    public void dayNumberZero() throws Exception {
        int dayNumber = DailyCalendarExportUtils.getDayNumber(TEST_DAY, TEST_DAY);
        assertEquals(0, dayNumber);
    }

    @Test
    public void dayNumberNegative() throws Exception {
        int dayNumber = DailyCalendarExportUtils.getDayNumber(TEST_DAY, TEST_DAY_MINUS);
        assertEquals(-DAYS, dayNumber);
    }

    @Test
    public void dayNumberPeriodCommon() throws Exception {
        int dayNumber = DailyCalendarExportUtils.getDayNumber(TEST_PERIOD, TEST_DAY_PLUS);
        assertEquals(DAYS, dayNumber);
    }

    @Test
    public void dayNumberPeriodZero() throws Exception {
        int dayNumber = DailyCalendarExportUtils.getDayNumber(TEST_PERIOD, TEST_DAY);
        assertEquals(0, dayNumber);
    }

    @Test
    public void dayNumberPeriodNegative() throws Exception {
        int dayNumber = DailyCalendarExportUtils.getDayNumber(TEST_PERIOD, TEST_DAY_MINUS);
        assertEquals(-DAYS, dayNumber);
    }

    @Test
    public void daysNumbersCommon() throws Exception {
        List<Integer> daysNumbers = DailyCalendarExportUtils.getDaysNumbers(TEST_DAY, TEST_DAYS);
        assertEquals(TEST_DAY_NUMBERS, daysNumbers);
    }

    @Test
    public void daysNumbersEmpty() throws Exception {
        List<Integer> daysNumbers = DailyCalendarExportUtils.getDaysNumbers(TEST_DAY, Collections.emptyList());
        assertEquals(Collections.emptyList(), daysNumbers);
    }

    @Test
    public void daysNumbersPeriodCommon() throws Exception {
        List<Integer> daysNumbers = DailyCalendarExportUtils.getDaysNumbers(TEST_PERIOD, TEST_DAYS);
        assertEquals(TEST_DAY_NUMBERS, daysNumbers);
    }

    @Test
    public void daysNumbersPeriodEmpty() throws Exception {
        List<Integer> daysNumbers = DailyCalendarExportUtils.getDaysNumbers(TEST_PERIOD, Collections.emptyList());
        assertEquals(Collections.emptyList(), daysNumbers);
    }

    @Test
    public void daysNumbersWithinCommon() throws Exception {
        List<Integer> daysNumbers = DailyCalendarExportUtils.getDaysNumbersWithin(TEST_PERIOD, TEST_DAYS);
        assertEquals(TEST_DAY_NUMBERS_WITHIN, daysNumbers);
    }

    @Test
    public void daysNumbersWithinOutside() throws Exception {
        List<Integer> daysNumbers = DailyCalendarExportUtils.getDaysNumbersWithin(TEST_PERIOD, TEST_DAYS_OUT);
        assertEquals(Collections.emptyList(), daysNumbers);
    }

    @Test
    public void daysNumbersWithinEmpty() throws Exception {
        List<Integer> daysNumbers = DailyCalendarExportUtils.getDaysNumbersWithin(TEST_PERIOD, Collections.emptyList());
        assertEquals(Collections.emptyList(), daysNumbers);
    }
}
