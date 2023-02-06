package ru.yandex.market.abo.core.calendar;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.calendar.db.CalendarEntry;
import ru.yandex.market.abo.core.calendar.db.CalendarService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 22/11/2019.
 */
class WorkHourIntegrationTest extends EmptyTest {
    private static final LocalDate NOW = LocalDate.now();

    private WorkHour workHour;

    @Autowired
    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        workHour = new WorkHour(calendarService);
        calendarService.updateCalendar(List.of(
                new CalendarEntry(Date.valueOf(NOW), false, false, "пн"),
                new CalendarEntry(Date.valueOf(NOW.plusDays(1)), false, false, "вт"),
                new CalendarEntry(Date.valueOf(NOW.plusDays(2)), false, true, "какая-нибудь праздничная среда"),
                new CalendarEntry(Date.valueOf(NOW.plusDays(3)), false, false, "чт"),
                new CalendarEntry(Date.valueOf(NOW.plusDays(4)), false, false, "пт"),
                new CalendarEntry(Date.valueOf(NOW.plusDays(5)), true, true, "какая-нибудь праздничная суббота"),
                new CalendarEntry(Date.valueOf(NOW.plusDays(6)), true, false, "самое обычное воскресенье"),
                new CalendarEntry(Date.valueOf(NOW.plusDays(7)), false, false, "снова понедельник")
        ));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 28, 49})
    void testConsistency(int delta) {
        var start = DateUtil.asDate(NOW);
        var finish = workHour.add(start, delta);
        assertTrue(finish.after(start));
        assertEquals(delta, workHour.diff(start, finish));
        assertFalse(calendarService.get(finish).isHolidayOrWeekend());
    }

    @Test
    void diff() {
        assertEquals(24, workHour.diff(DateUtil.asDate(NOW), DateUtil.asDate(NOW.plusDays(1))));
        assertEquals(48, workHour.diff(DateUtil.asDate(NOW), DateUtil.asDate(NOW.plusDays(2))));
        assertEquals(48, workHour.diff(DateUtil.asDate(NOW), DateUtil.asDate(NOW.plusDays(3))));

        assertEquals(24, workHour.diff(DateUtil.asDate(NOW.plusDays(1)), DateUtil.asDate(NOW.plusDays(3))));
        assertEquals(24, workHour.diff(DateUtil.asDate(NOW.plusDays(4)), DateUtil.asDate(NOW.plusDays(6))));
        assertEquals(0, workHour.diff(DateUtil.asDate(NOW.plusDays(5)), DateUtil.asDate(NOW.plusDays(6))));
    }

    @Test
    void addSameDay() {
        var start = DateUtil.asDate(NOW);
        var sameDay = workHour.add(start, 10);
        assertTrue(sameDay.after(start));
        assertEquals(DateUtil.asLocalDateTime(start).getDayOfWeek(), DateUtil.asLocalDateTime(sameDay).getDayOfWeek());
    }

    @Test
    void addHolidayInBetween() {
        var monday = DateUtil.asDate(NOW);
        var thursdayAfterHoliday = workHour.add(monday, 49); // должна быть среда, но она праздник
        assertTrue(thursdayAfterHoliday.after(monday));

        long daysBetween = ChronoUnit.DAYS.between(DateUtil.asLocalDate(monday), DateUtil.asLocalDate(thursdayAfterHoliday));
        assertEquals(3, daysBetween);
    }

    @Test
    void addWeekendInBetween() {
        var friday = DateUtil.asDate(NOW.plusDays(4));
        var nextMonday = workHour.add(DateUtil.asDate(NOW.plusDays(4)), 25);
        assertTrue(nextMonday.after(friday));

        long daysBetween = ChronoUnit.DAYS.between(DateUtil.asLocalDate(friday), DateUtil.asLocalDate(nextMonday));
        assertEquals(3, daysBetween);
    }
}
