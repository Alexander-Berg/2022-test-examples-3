package ru.yandex.market.logistics.management.service.calendar;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.Calendar;
import ru.yandex.market.logistics.management.domain.entity.CalendarDay;

public class CalendarTest extends AbstractTest {
    @Test
    void updateCalendarTest() {
        Calendar calendar = new Calendar();
        CalendarDay day1 = new CalendarDay().setDay(LocalDate.of(2020, 8, 3)).setIsHoliday(true);
        CalendarDay day2 = new CalendarDay().setDay(LocalDate.of(2020, 8, 21)).setIsHoliday(true);
        CalendarDay day3 = new CalendarDay().setDay(LocalDate.of(2020, 8, 23)).setIsHoliday(false);
        CalendarDay day4 = new CalendarDay().setDay(LocalDate.of(2020, 8, 29)).setIsHoliday(false);
        CalendarDay day5 = new CalendarDay().setDay(LocalDate.of(2020, 8, 31)).setIsHoliday(true);
        calendar.addCalendarDays(Set.of(day1, day2, day3, day4, day5));

        CalendarDay day6 = new CalendarDay().setDay(LocalDate.of(2020, 8, 3)).setIsHoliday(true);
        CalendarDay day7 = new CalendarDay().setDay(LocalDate.of(2020, 8, 21)).setIsHoliday(false);
        CalendarDay day8 = new CalendarDay().setDay(LocalDate.of(2020, 8, 23)).setIsHoliday(true);
        CalendarDay day9 = new CalendarDay().setDay(LocalDate.of(2020, 8, 29)).setIsHoliday(false);
        CalendarDay day10 = new CalendarDay().setDay(LocalDate.of(2020, 8, 30)).setIsHoliday(false);
        CalendarDay day11 = new CalendarDay().setDay(LocalDate.of(2020, 8, 30)).setIsHoliday(true);

        softly.assertThat(calendar.updateCalendarDays(Set.of(day6, day7, day8, day9, day10, day11))).isTrue();

        CalendarDay day12 = new CalendarDay().setDay(LocalDate.of(2020, 8, 3)).setIsHoliday(true);
        CalendarDay day13 = new CalendarDay().setDay(LocalDate.of(2020, 8, 21)).setIsHoliday(false);
        CalendarDay day14 = new CalendarDay().setDay(LocalDate.of(2020, 8, 23)).setIsHoliday(true);
        CalendarDay day15 = new CalendarDay().setDay(LocalDate.of(2020, 8, 29)).setIsHoliday(false);
        CalendarDay day16 = new CalendarDay().setDay(LocalDate.of(2020, 8, 30)).setIsHoliday(true);
        CalendarDay day17 = new CalendarDay().setDay(LocalDate.of(2020, 8, 31)).setIsHoliday(true);


        softly.assertThat(calendar.copyCalendarDays())
            .containsExactlyInAnyOrderElementsOf(Set.of(day12, day13, day14, day15, day16, day17));
    }
    @Test
    void noCalendarUpdateTest() {
        Calendar calendar = new Calendar();
        CalendarDay day1 = new CalendarDay().setDay(LocalDate.of(2020, 8, 3)).setIsHoliday(true);
        CalendarDay day2 = new CalendarDay().setDay(LocalDate.of(2020, 8, 21)).setIsHoliday(true);
        CalendarDay day3 = new CalendarDay().setDay(LocalDate.of(2020, 8, 23)).setIsHoliday(false);
        CalendarDay day4 = new CalendarDay().setDay(LocalDate.of(2020, 8, 29)).setIsHoliday(false);
        CalendarDay day5 = new CalendarDay().setDay(LocalDate.of(2020, 8, 31)).setIsHoliday(true);
        calendar.addCalendarDays(Set.of(day1, day2, day3, day4, day5));

        CalendarDay day6 = new CalendarDay().setDay(LocalDate.of(2020, 8, 3)).setIsHoliday(true);
        CalendarDay day7 = new CalendarDay().setDay(LocalDate.of(2020, 8, 21)).setIsHoliday(true);
        CalendarDay day8 = new CalendarDay().setDay(LocalDate.of(2020, 8, 21)).setIsHoliday(false);
        CalendarDay day9 = new CalendarDay().setDay(LocalDate.of(2020, 8, 29)).setIsHoliday(false);

        softly.assertThat(calendar.updateCalendarDays(Set.of(day6, day7, day8, day9))).isFalse();

        softly.assertThat(calendar.copyCalendarDays())
            .containsExactlyInAnyOrderElementsOf(Set.of(day1, day2, day3, day4, day5));
    }
}
