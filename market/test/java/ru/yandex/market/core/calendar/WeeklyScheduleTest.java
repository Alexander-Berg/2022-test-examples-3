package ru.yandex.market.core.calendar;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyScheduleTest {

    @Test
    void testContains() {
        var weeklySchedule = new WeeklySchedule(DayOfWeek.MONDAY, 10, 0, DayOfWeek.FRIDAY, 24, 0, null);

        var localDateTime = LocalDateTime.of(2018, Month.FEBRUARY, 28, 10, 0, 0, 0);
        assertThat(weeklySchedule.contains(localDateTime)).isTrue();

        localDateTime = LocalDateTime.of(2018, Month.FEBRUARY, 26, 10, 0, 0, 0);
        assertThat(weeklySchedule.contains(localDateTime)).isTrue();

        localDateTime = LocalDateTime.of(2018, Month.FEBRUARY, 27, 0, 0, 0, 0);
        assertThat(weeklySchedule.contains(localDateTime)).isTrue();

        localDateTime = LocalDateTime.of(2018, Month.MARCH, 3, 0, 0, 0, 0);
        assertThat(weeklySchedule.contains(localDateTime)).isTrue();

        localDateTime = LocalDateTime.of(2018, Month.FEBRUARY, 26, 9, 55, 0, 0);
        assertThat(weeklySchedule.contains(localDateTime)).isFalse();
    }

    @Test
    @DisplayName("Пересечение расписаний: пересекаются")
    void testIntersects() {
        var schedule1 = new WeeklySchedule(DayOfWeek.TUESDAY, 4, 30, DayOfWeek.FRIDAY, 20, 23, null);
        var schedule2 = new WeeklySchedule(DayOfWeek.THURSDAY, 14, 0, DayOfWeek.SUNDAY, 23, 11, null);
        assertThat(schedule1.intersects(schedule2)).isTrue();
        assertThat(schedule2.intersects(schedule1)).isTrue();
        assertThat(schedule1.intersects(schedule1)).isTrue();
    }

    @Test
    @DisplayName("Пересечение расписаний: не пересекаются")
    void testNotIntersects() {
        var schedule1 = new WeeklySchedule(DayOfWeek.TUESDAY, 4, 30, DayOfWeek.WEDNESDAY, 20, 23, null);
        var schedule2 = new WeeklySchedule(DayOfWeek.THURSDAY, 14, 0, DayOfWeek.SUNDAY, 23, 11, null);
        assertThat(schedule1.intersects(schedule2)).isFalse();
        assertThat(schedule2.intersects(schedule1)).isFalse();
    }
}
