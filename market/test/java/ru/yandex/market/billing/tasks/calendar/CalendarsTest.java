package ru.yandex.market.billing.tasks.calendar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.core.calendar.Calendars;
import ru.yandex.market.core.calendar.DatePeriod;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
public class CalendarsTest extends Assert {

    @Test
    public void shouldConvertWorkHoursToDate() {
  /*      assertEquals(
                new Date(130, 0, 1, 11, 12, 0),
                Calendars.toDate(DayOfWeek.MONDAY, LocalTime.of(11, 12)));
        assertEquals(
                new Date(130, 0, 3, 14, 15, 0),
                Calendars.toDate(DayOfWeek.WEDNESDAY, LocalTime.of(14, 15)));
        assertEquals(
                new Date(130, 0, 7, 23, 59, 0),
                Calendars.toDate(DayOfWeek.SUNDAY, LocalTime.of(23, 59)));*/
    }

    @Test
    public void shouldConvertEmptyDateListToEmptyListOfDatePeriods() {
        List<DatePeriod> result = Calendars.convertDatesToDatePeriods(Collections.emptyList());
        assertEquals(Collections.emptyList(), result);

    }

    @Test
    public void shouldConvertSingletonDateListToSingletonListOfDatePeriods() {
        List<LocalDate> dates = Collections.singletonList(LocalDate.of(1, 2, 3));
        List<DatePeriod> result = Calendars.convertDatesToDatePeriods(dates);
        assertEquals(Collections.singletonList(DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 4))), result);

    }

    @Test
    public void shouldMergeTwoDaysPeriod() {
        List<LocalDate> dates = Arrays.asList(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 4));
        List<DatePeriod> result = Calendars.convertDatesToDatePeriods(dates);
        assertEquals(Collections.singletonList(DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 5))), result);
    }

    @Test
    public void shouldMergeTreeDaysPeriod() {
        List<LocalDate> dates = Arrays.asList(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 4), LocalDate.of(1, 2, 5));
        List<DatePeriod> result = Calendars.convertDatesToDatePeriods(dates);
        assertEquals(Collections.singletonList(DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 6))), result);

    }

    @Test
    public void shouldMergeTreeDaysPeri1od() {
        List<LocalDate> dates = Arrays.asList(
                LocalDate.of(1, 2, 3),
                LocalDate.of(1, 2, 4),
                LocalDate.of(1, 2, 4),
                LocalDate.of(1, 2, 5));
        List<DatePeriod> result = Calendars.convertDatesToDatePeriods(dates);
        assertEquals(Collections.singletonList(DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 6))), result);

    }

    @Test
    public void shouldMergeTreeDaysPeriodAndSingletonPeriod() {
        List<LocalDate> dates = Arrays.asList(
                LocalDate.of(1, 2, 3),
                LocalDate.of(1, 2, 4),
                LocalDate.of(1, 2, 5),
                LocalDate.of(1, 2, 7));
        List<DatePeriod> result = Calendars.convertDatesToDatePeriods(dates);
        assertEquals(
                Arrays.asList(
                        DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 6)),
                        DatePeriod.of(LocalDate.of(1, 2, 7), LocalDate.of(1, 2, 8))),
                result);

    }

    @Test
    public void shouldMergeSingletonPeriodTreeDaysPeriod() {
        List<LocalDate> dates = Arrays.asList(
                LocalDate.of(1, 2, 3),
                LocalDate.of(1, 2, 5),
                LocalDate.of(1, 2, 6),
                LocalDate.of(1, 2, 7));
        List<DatePeriod> result = Calendars.convertDatesToDatePeriods(dates);
        assertEquals(
                Arrays.asList(
                        DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 4)),
                        DatePeriod.of(LocalDate.of(1, 2, 5), LocalDate.of(1, 2, 8))
                ),
                result);
    }

    @Test
    public void shouldReturnEmptyDatesOnEmptyPeriods() {
        List<DatePeriod> periods = new ArrayList<>();
        List<LocalDate> dates = Calendars.convertDatePeriodsToDates(periods);
        assertEquals(dates, Collections.emptyList());
    }


    @Test
    public void shouldReturnEmptyDatesOnEmptyPeriod() {
        List<DatePeriod> periods = Arrays.asList(DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 3)));
        List<LocalDate> dates = Calendars.convertDatePeriodsToDates(periods);
        assertEquals(dates, Collections.emptyList());
    }

    @Test
    public void shouldReturnOneDateOnOneDayPeriod() {
        List<DatePeriod> periods = Arrays.asList(DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 4)));
        List<LocalDate> dates = Calendars.convertDatePeriodsToDates(periods);
        assertEquals(dates, Arrays.asList(LocalDate.of(1, 2, 3)));
    }

    @Test
    public void shouldReturnTreeDatesOnTreeDayPeriod() {
        List<DatePeriod> periods = Arrays.asList(DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 6)));
        List<LocalDate> dates = Calendars.convertDatePeriodsToDates(periods);
        assertEquals(dates, Arrays.asList(
                LocalDate.of(1, 2, 3),
                LocalDate.of(1, 2, 4),
                LocalDate.of(1, 2, 5)));
    }

    @Test
    public void shouldReturnFourDatesOnTreeDayPeriodAndOneDayPeriod() {
        List<DatePeriod> periods = Arrays.asList(
                DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 6)),
                DatePeriod.of(LocalDate.of(1, 2, 8), LocalDate.of(1, 2, 9)));
        List<LocalDate> dates = Calendars.convertDatePeriodsToDates(periods);
        assertEquals(dates, Arrays.asList(
                LocalDate.of(1, 2, 3),
                LocalDate.of(1, 2, 4),
                LocalDate.of(1, 2, 5),
                LocalDate.of(1, 2, 8)));
    }

    @Test
    public void shouldReturnFourDatesOnOneDayPeriodAndTreeDayPeriod() {
        List<DatePeriod> periods = Arrays.asList(
                DatePeriod.of(LocalDate.of(1, 2, 8), LocalDate.of(1, 2, 9)),
                DatePeriod.of(LocalDate.of(1, 2, 3), LocalDate.of(1, 2, 6))
        );
        List<LocalDate> dates = Calendars.convertDatePeriodsToDates(periods);
        assertEquals(dates, Arrays.asList(
                LocalDate.of(1, 2, 8),
                LocalDate.of(1, 2, 3),
                LocalDate.of(1, 2, 4),
                LocalDate.of(1, 2, 5)
        ));
    }
}