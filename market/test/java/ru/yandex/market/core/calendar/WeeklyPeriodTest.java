package ru.yandex.market.core.calendar;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
public class WeeklyPeriodTest {

    @Test
    public void shouldReturnOneDayOfWeekForOneDay() {
        List<DayOfWeek> dayOfWeeks = WeeklyPeriod.of(DayOfWeek.FRIDAY).toDays();
        assertEquals(Arrays.asList(DayOfWeek.FRIDAY), dayOfWeeks);
    }

}
