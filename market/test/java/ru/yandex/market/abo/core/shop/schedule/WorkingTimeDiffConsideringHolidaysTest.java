package ru.yandex.market.abo.core.shop.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.abo.core.calendar.db.CalendarEntry;
import ru.yandex.market.abo.core.calendar.db.CalendarService;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author valeriashanti
 * @data 03/02/2020
 */
class WorkingTimeDiffConsideringHolidaysTest {
    private final LocalDateTime DAY_BEFORE_HOLIDAY = LocalDateTime.parse("2019-02-22T10:00");
    private final List<ShopWorkingPeriod> periods = Collections.singletonList(
            createPeriod("10:00", "18:00", new DayOfWeek[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}));

    private final Set<LocalDate> holidays = StreamEx.of("2019-04-25", "2019-04-23", "2019-04-27", "2019-03-08",
            "2019-02-23", "2019-02-24", "2019-03-08").map(LocalDate::parse).toSet();

    @InjectMocks
    WorkingTimeDiff workingTimeDiff;
    @Mock
    CalendarService calendarService;
    @Mock
    private CalendarEntry holiday;
    @Mock
    private CalendarEntry workDay;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(holiday.isHoliday()).thenReturn(true);
        when(workDay.isHoliday()).thenReturn(false);
        when(calendarService.get(any(LocalDateTime.class))).then(inv -> {
            var date = ((LocalDateTime) inv.getArguments()[0]).toLocalDate();
            return holidays.contains(date) ? holiday : workDay;
        });

    }

    @ParameterizedTest(name = "workingMinutesTest_{index}")
    @CsvSource({
            "2019-02-22T16:30,2019-02-22T18:30,30",
            "2019-02-22T12:30,2019-02-22T13:00,30",
            "2019-02-22T09:30,2019-02-22T11:00,60"
    })
    void workingMinutesTest(LocalDateTime start, LocalDateTime end, int expectedDiff) {
        assertEquals(expectedDiff, workingTimeDiff.workingMinutes(start, end, periods));
    }

    @ParameterizedTest(name = "addWorkingMinutesTest_{index}")
    @CsvSource({
            "2019-02-22T16:30,2019-02-25T10:30,60",
            "2019-02-21T16:30,2019-02-25T11:30,600",
            "2019-02-22T13:30,2019-02-25T11:30,300",
            "2019-03-07T17:00,2019-03-11T11:00,60"
    })
    void addWorkingMinutesTest(LocalDateTime start, LocalDateTime expectedEnd, int minutesToAdd) {
        assertEquals(expectedEnd, workingTimeDiff.addWorkingMinutes(start, minutesToAdd, periods));
    }

    @Test
    void multipleHolidaysTest() {
        var start = LocalDateTime.parse("2019-04-22T10:00");
        assertEquals(LocalDateTime.parse("2019-04-29T11:00"), workingTimeDiff.addWorkingMinutes(start, 22 * 60, periods));
    }

    @ParameterizedTest(name = "petitePeriodsTest_{index}")
    @CsvSource({
            "2019-02-25T11:00,10:00,11:00,60",
            "2020-07-14T12:01,12:00,12:01,60",
            "2019-02-26T11:00,10:00,11:00,120"
    })
    void petitePeriodsTest(LocalDateTime end, String periodFrom, String periodTill, int minutesToAdd) {
        var petitePeriods = Collections.singletonList(
                createPeriod(periodFrom, periodTill, new DayOfWeek[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}));

        assertEquals(end, workingTimeDiff.addWorkingMinutes(DAY_BEFORE_HOLIDAY, minutesToAdd, petitePeriods));
        assertEquals(0, workingTimeDiff.workingMinutes(DAY_BEFORE_HOLIDAY, DAY_BEFORE_HOLIDAY.plusHours(1), petitePeriods));
    }

    @ParameterizedTest(name = "exceptionOnInvalidPeriodTest_{index}")
    @CsvSource({"12:00,12:00", "12:00,11:00"})
    void exceptionOnInvalidPeriodTest(String periodFrom, String periodTill) {
        var invalidPeriods = Collections.singletonList(
                createPeriod(periodFrom, periodTill, new DayOfWeek[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}));
        assertThrows(IllegalArgumentException.class,
                () -> workingTimeDiff.workingMinutes(DAY_BEFORE_HOLIDAY, DAY_BEFORE_HOLIDAY.plusHours(1), invalidPeriods));
    }

    private ShopWorkingPeriod createPeriod(String start, String end, DayOfWeek[] days) {
        ShopWorkingPeriod result = new ShopWorkingPeriod();
        result.setFromTime(LocalTime.parse(start));
        result.setToTime(LocalTime.parse(end));
        result.setDaysOfWeek(days);
        return result;
    }
}
