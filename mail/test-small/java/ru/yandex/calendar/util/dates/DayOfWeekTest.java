package ru.yandex.calendar.util.dates;

import java.util.Calendar;

import net.fortuna.ical4j.model.WeekDay;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DayOfWeekTest {
    @Test
    public void byName() {
        assertThat(DayOfWeek.byName("Monday")).isEqualTo(DayOfWeek.MONDAY);
        assertThat(DayOfWeek.byName("fri")).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(DayOfWeek.byName("sa")).isEqualTo(DayOfWeek.SATURDAY);
    }

    @Test
    public void byWeekDay() {
        assertThat(DayOfWeek.byWeekDay(WeekDay.TH)).isEqualTo(DayOfWeek.THURSDAY);
    }

    @Test
    public void getJcal() {
        assertThat(DayOfWeek.MONDAY.getJcal()).isEqualTo(Calendar.MONDAY);
        assertThat(DayOfWeek.SATURDAY.getJcal()).isEqualTo(Calendar.SATURDAY);
        assertThat(DayOfWeek.SUNDAY.getJcal()).isEqualTo(Calendar.SUNDAY);
    }

    @Test
    public void getJoda() {
        assertThat(DayOfWeek.MONDAY.getJoda()).isEqualTo(DateTimeConstants.MONDAY);
        assertThat(DayOfWeek.SATURDAY.getJoda()).isEqualTo(DateTimeConstants.SATURDAY);
        assertThat(DayOfWeek.SUNDAY.getJoda()).isEqualTo(DateTimeConstants.SUNDAY);
    }

    @Test
    public void fromDay() {
        for (int i = 0; i < 7; ++i) {
            assertThat(DayOfWeek.fromDay(new LocalDate(2011, 1, 17 + i))).isEqualTo(DayOfWeek.values()[i]);
        }
    }
}
