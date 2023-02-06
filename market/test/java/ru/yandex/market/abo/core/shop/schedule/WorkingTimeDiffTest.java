package ru.yandex.market.abo.core.shop.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTimeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.abo.core.calendar.db.CalendarEntry;
import ru.yandex.market.abo.core.calendar.db.CalendarService;

import static java.time.DayOfWeek.*;
import static java.time.Month.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
class WorkingTimeDiffTest {

    @InjectMocks
    WorkingTimeDiff workingTimeDiff;
    @Mock
    CalendarService calendarService;

    @Mock
    private CalendarEntry holiday;

    @Mock
    private CalendarEntry workDay;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(holiday.isHoliday()).thenReturn(true);
        when(workDay.isHoliday()).thenReturn(false);
        when(calendarService.get(any(LocalDateTime.class))).thenReturn(workDay);
    }

    @Test
    void testWorkingTime1() {
        LocalDateTime start = LocalDateTime.of(2018, MAY, 4, 17, 20); // 4 мая 2018, пятница, 17:20
        LocalDateTime end = LocalDateTime.of(2018, MAY, 7, 10, 10); // 7 мая 2018, понедельник, 10:10
        List<ShopWorkingPeriod> periods = Arrays.asList(
                createPeriod("10:00", "18:00", new DayOfWeek[]{FRIDAY, MONDAY}),
                createPeriod("10:00", "16:00", new DayOfWeek[]{SATURDAY})
        );
        int res = workingTimeDiff.workingMinutes(start, end, periods);
        assertEquals(50 + 6 * DateTimeConstants.MINUTES_PER_HOUR, res);
    }

    @Test
    void testWorkingTime2() {
        LocalDateTime start = LocalDateTime.of(2018, MAY, 4, 18, 0); // 4 мая 2018, пятница, 18:00
        LocalDateTime end = LocalDateTime.of(2018, MAY, 7, 9, 30); // 7 мая 2018, понедельник, 9:30 (до начала работы)
        List<ShopWorkingPeriod> periods = Arrays.asList(
                createPeriod("10:00", "18:00", new DayOfWeek[]{FRIDAY, MONDAY}),
                createPeriod("10:00", "16:00", new DayOfWeek[]{SATURDAY})
        );
        int res = workingTimeDiff.workingMinutes(start, end, periods);
        assertEquals(6 * DateTimeConstants.MINUTES_PER_HOUR, res);
    }


    @Test
    void testWorkingTime3() {
        LocalDateTime start = LocalDateTime.of(2018, MAY, 4, 18, 0); // 4 мая 2018, пятница, 18:00
        LocalDateTime end = LocalDateTime.of(2018, MAY, 11, 10, 30); // 11 мая 2018, пятница, 10:30
        List<ShopWorkingPeriod> periods = Collections.singletonList(
                createPeriod("10:00", "18:30", new DayOfWeek[]{FRIDAY})
        );
        int res = workingTimeDiff.workingMinutes(start, end, periods);
        assertEquals(DateTimeConstants.MINUTES_PER_HOUR, res);
    }


    @Test
    void testWorkingTimeWithHolidays() {
        LocalDateTime start = LocalDateTime.of(2018, MAY, 4, 17, 20); // 4 мая 2018, пятница, 17:20
        LocalDateTime end = LocalDateTime.of(2018, MAY, 10, 10, 10); // 10 мая 2018, четверг, 10:10
        List<ShopWorkingPeriod> periods = Arrays.asList(
                createPeriod("10:00", "18:00", new DayOfWeek[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}),
                createPeriod("10:00", "16:00", new DayOfWeek[]{SATURDAY})
        );
        when(calendarService.get(any(LocalDateTime.class))).then(inv -> {
            LocalDateTime dateTime = (LocalDateTime) inv.getArguments()[0];
            return dateTime.toLocalDate().equals(LocalDate.of(2018, MAY, 9)) ? holiday : workDay;
        });

        int res = workingTimeDiff.workingMinutes(start, end, periods);
        assertEquals(40 +  // остаток пятницы
                        6 * DateTimeConstants.MINUTES_PER_HOUR +  // суббота
                        8 * DateTimeConstants.MINUTES_PER_HOUR +  // понедельник
                        7 * DateTimeConstants.MINUTES_PER_HOUR +  // вторник
                        // + 0 среда - праздник
                        10, // начало четверга
                res);
    }

    @Test
    void testWorkingTimeWithHolidays2() {
        LocalDateTime start = LocalDateTime.of(2018, MAY, 9, 10, 20); // 9 мая 2018, пятница, 10:20
        LocalDateTime end = LocalDateTime.of(2018, MAY, 10, 22, 10); // 10 мая 2018, четверг, 22:10
        List<ShopWorkingPeriod> periods = Arrays.asList(
                createPeriod("10:00", "18:00", new DayOfWeek[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}),
                createPeriod("10:00", "16:00", new DayOfWeek[]{SATURDAY})
        );
        when(calendarService.get(any(LocalDateTime.class))).then(inv -> {
            LocalDateTime dateTime = (LocalDateTime) inv.getArguments()[0];
            return dateTime.toLocalDate().equals(LocalDate.of(2018, MAY, 9)) ? holiday : workDay;
        });

        int res = workingTimeDiff.workingMinutes(start, end, periods);
        assertEquals(8 * DateTimeConstants.MINUTES_PER_HOUR, res); //только весь четверг 10го
    }

    @Test
    void testWorkingTimeWithNewYear() {
        LocalDateTime start = LocalDateTime.of(2018, DECEMBER, 28, 17, 20); // 28 декабря 2018, пятница, 17:20
        LocalDateTime end = LocalDateTime.of(2019, JANUARY, 10, 10, 10); // 10 января 2019, четверг, 10:10
        List<ShopWorkingPeriod> periods = Arrays.asList(
                createPeriod("10:00", "18:00", new DayOfWeek[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}),
                createPeriod("10:00", "16:00", new DayOfWeek[]{SATURDAY})
        );
        when(calendarService.get(any(LocalDateTime.class))).then(inv -> {
            LocalDate date = ((LocalDateTime) inv.getArguments()[0]).toLocalDate();
            return date.isAfter(LocalDate.of(2018, DECEMBER, 29)) && date.isBefore(LocalDate.of(2019, JANUARY, 9)) ?
                    holiday : workDay;
        });

        int res = workingTimeDiff.workingMinutes(start, end, periods);
        assertEquals(40 +  // остаток пятницы 28 дек
                        5 * DateTimeConstants.MINUTES_PER_HOUR +  // суббота 29 дек
                        // + 0 НГ праздники
                        8 * DateTimeConstants.MINUTES_PER_HOUR +  // 9 янв среда
                        10, // начало 10 янв четверг
                res);
    }

    @Test
    void testAddWorkingTime1() {
        LocalDateTime start = LocalDateTime.of(2018, MAY, 4, 18, 0); // 4 мая 2018, пятница, 18:00
        int minutesToAdd = 90;
        List<ShopWorkingPeriod> periods = Arrays.asList(
                createPeriod("10:00", "18:30", new DayOfWeek[]{MONDAY, FRIDAY}),
                createPeriod("13:00", "20:00", new DayOfWeek[]{SATURDAY, SUNDAY})
        );
        LocalDateTime res = workingTimeDiff.addWorkingMinutes(start, minutesToAdd, periods);
        LocalDateTime expected = LocalDateTime.of(2018, MAY, 5, 14, 0); // 5 мая 2018, суббота, 14:00
        assertEquals(expected, res);
    }

    @Test
    void testAddWorkingTime2() {
        LocalDateTime start = LocalDateTime.of(2018, MAY, 4, 18, 0); // 4 мая 2018, пятница, 18:00
        int minutesToAdd = 120;
        List<ShopWorkingPeriod> periods = Collections.singletonList(
                createPeriod("10:00", "11:00", new DayOfWeek[]{FRIDAY})
        );
        LocalDateTime res = workingTimeDiff.addWorkingMinutes(start, minutesToAdd, periods);
        LocalDateTime expected = LocalDateTime.of(2018, MAY, 18, 11, 0); // 18 мая 2018, пятница, 11:00
        assertEquals(expected, res);
    }

    @Test
    void testAddWorkingTimeWithHolidays() {
        LocalDateTime start = LocalDateTime.of(2018, MAY, 4, 15, 30); // 4 мая 2018, пятница, 15:30
        List<ShopWorkingPeriod> periods = Arrays.asList(
                createPeriod("10:00", "18:00", new DayOfWeek[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}),
                createPeriod("10:00", "16:00", new DayOfWeek[]{SATURDAY})
        );
        when(calendarService.get(any(LocalDateTime.class))).then(inv -> {
            LocalDateTime dateTime = (LocalDateTime) inv.getArguments()[0];
            return dateTime.toLocalDate().equals(LocalDate.of(2018, MAY, 9)) ? holiday : workDay;
        });
        int minutesToAdd = 5 * 8 * 60; // 5 полных рабочих дней в минутах (чтоб с захватом выходного и праздника)

        LocalDateTime res = workingTimeDiff.addWorkingMinutes(start, minutesToAdd, periods);
        LocalDateTime expected = LocalDateTime.of(2018, MAY, 12, 10, 30); // 11 мая 2018, пятница, 17:30
        assertEquals(expected, res);
    }

    @Test
    void testAddWorkingTimeWithNewYear() {
        LocalDateTime start = LocalDateTime.of(2018, DECEMBER, 28, 17, 20); // 28 декабря 2018, пятница, 17:20
        List<ShopWorkingPeriod> periods = Arrays.asList(
                createPeriod("10:00", "18:00", new DayOfWeek[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}),
                createPeriod("10:00", "16:00", new DayOfWeek[]{SATURDAY})
        );
        when(calendarService.get(any(LocalDateTime.class))).then(inv -> {
            LocalDate date = ((LocalDateTime) inv.getArguments()[0]).toLocalDate();
            return date.isAfter(LocalDate.of(2018, DECEMBER, 29)) && date.isBefore(LocalDate.of(2019, JANUARY, 9)) ?
                    holiday : workDay;
        });
        int minutesToAdd = 360 + 60; // рабочая суббота, а потом после праздников

        LocalDateTime res = workingTimeDiff.addWorkingMinutes(start, minutesToAdd, periods);
        LocalDateTime expected = LocalDateTime.of(2019, JANUARY, 9, 11, 20); // 9 января 2019, среда, 11 20
        assertEquals(expected, res);
    }

    private ShopWorkingPeriod createPeriod(String start, String end, DayOfWeek[] days) {
        ShopWorkingPeriod result = new ShopWorkingPeriod();
        result.setFromTime(LocalTime.parse(start));
        result.setToTime(LocalTime.parse(end));
        result.setDaysOfWeek(days);
        return result;
    }
}
