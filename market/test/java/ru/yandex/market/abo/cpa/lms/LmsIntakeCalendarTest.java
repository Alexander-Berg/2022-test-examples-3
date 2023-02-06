package ru.yandex.market.abo.cpa.lms;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author artemmz
 * @date 10/09/19.
 */
class LmsIntakeCalendarTest extends EmptyTest {
    private static final List<DayOfWeek> CALENDAR = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    private static final Integer[] INT_CALENDAR = CALENDAR.stream().map(DayOfWeek::getValue).toArray(Integer[]::new);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @EnumSource(value = DayOfWeek.class, mode = EnumSource.Mode.MATCH_ALL)
    void testCalendar(DayOfWeek dayOfWeek) {
        LocalDate inputDate = getPrevOrSame(dayOfWeek);
        DayOfWeek expectedNextDay = CALENDAR.stream()
                .filter(dow -> dow.getValue() >= dayOfWeek.getValue())
                .findFirst()
                .orElseGet(() -> CALENDAR.stream().min(Comparator.comparing(DayOfWeek::getValue)).orElseThrow());
        LocalDate expected = getNextOrSame(inputDate, expectedNextDay);
        assertEquals(expected, applyCalendar(inputDate));
    }

    @Test
    void testEmptyIntakes() {
        LocalDate now = LocalDate.now();
        assertEquals(now, applyCalendar(now, new Integer[]{}));
        assertEquals(now, applyCalendar(now, null));
    }

    @Test
    void testBugCalendar() {
        LocalDate now = LocalDate.now();
        assertEquals(now, applyCalendar(now, new Integer[]{-5, 654, 34}));
    }

    @Test
    void testNullShipmentDate() {
        assertNull(applyCalendar(null, INT_CALENDAR));
    }

    private LocalDate applyCalendar(LocalDate date) {
        return applyCalendar(date, INT_CALENDAR);
    }

    private LocalDate applyCalendar(LocalDate date, Integer[] calendar) {
        return jdbcTemplate.queryForObject("select apply_lms_intake_calendar(?, ?)", LocalDate.class,
                calendar != null ? Arrays.stream(calendar).mapToInt(el -> el).toArray() : null, date);
    }

    private static LocalDate getPrevOrSame(DayOfWeek dayOfWeek) {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(dayOfWeek));
    }

    private static LocalDate getNextOrSame(LocalDate date, DayOfWeek dayOfWeek) {
        return date.with(TemporalAdjusters.nextOrSame(dayOfWeek));
    }
}
