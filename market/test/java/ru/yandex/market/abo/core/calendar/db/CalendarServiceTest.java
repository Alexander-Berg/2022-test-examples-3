package ru.yandex.market.abo.core.calendar.db;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Anton Irinev (airinev@yandex-team.ru)
 */
@Transactional("pgTransactionManager")
public class CalendarServiceTest extends EmptyTest {
    @Autowired
    private CalendarService calendarService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    public void testNullEntry() {
        calendarService.updateCalendar(Collections.emptyList());
        assertThrows(IllegalStateException.class,
                () -> calendarService.getCalendarEntry(Date.valueOf("2109-01-01")));
    }

    @Test
    public void testSimpleUpdating() {
        Date firstDate = Date.valueOf("2009-01-01");

        List<CalendarEntry> entries = new ArrayList<>();
        entries.add(new CalendarEntry(firstDate, false, true, "title"));
        calendarService.updateCalendar(entries);

        CalendarEntry entry = calendarService.getCalendarEntry(firstDate);
        assertEquals(firstDate, entry.getDate());
        assertFalse(entry.isWeekend());
        assertTrue(entry.isHoliday());
        assertEquals("title", entry.getTitle());
    }

    @Test
    void testSqlWorkHourDiff() {
        Date weekDay1 = Date.valueOf("2049-03-31");
        Date holidayWeekDay = Date.valueOf("2049-04-01");
        Date weekDay2 = Date.valueOf("2049-04-02");
        Date weekEnd = Date.valueOf("2049-04-03");
        calendarService.updateCalendar(List.of(
                new CalendarEntry(holidayWeekDay, false, true, ""),
                new CalendarEntry(weekDay1, false, false, ""),
                new CalendarEntry(weekDay2, false, false, ""),
                new CalendarEntry(weekEnd, true, false, "")
        ));

        assertEquals(0, pgJdbcTemplate.queryForObject("select workhour_diff(?, ?)", Long.class, weekDay1, weekDay1));
        assertEquals(0, pgJdbcTemplate.queryForObject("select workhour_diff(?, ?)", Long.class, holidayWeekDay, weekDay1));
        assertEquals(24, pgJdbcTemplate.queryForObject("select workhour_diff(?, ?)", Long.class, holidayWeekDay, weekEnd));
        assertEquals(24, pgJdbcTemplate.queryForObject("select workhour_diff(?, ?)", Long.class, weekDay1, weekDay2));
        assertEquals(48, pgJdbcTemplate.queryForObject("select workhour_diff(?, ?)", Long.class, weekDay1, weekEnd));
    }
}
