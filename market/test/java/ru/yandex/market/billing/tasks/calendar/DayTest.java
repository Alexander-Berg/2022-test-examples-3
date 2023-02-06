package ru.yandex.market.billing.tasks.calendar;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.core.calendar.Day;
import ru.yandex.market.core.calendar.DayType;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
public class DayTest extends Assert {

    @Test
    public void shouldSortWell() {
        LocalDate now = LocalDate.now();
        Day day1 = new Day(now, DayType.DELIVERY_WORKDAY);
        Day day2 = new Day(now, DayType.REGION_HOLIDAY);
        Day day3 = new Day(now.plusDays(1), DayType.DELIVERY_WORKDAY);
        Day day4 = new Day(now.plusDays(1), DayType.REGION_HOLIDAY);
        Day day5 = new Day(now.plusDays(10), DayType.DELIVERY_WORKDAY);
        Day day6 = new Day(now.plusDays(10), DayType.REGION_HOLIDAY);

        List<Day> days = Arrays.asList(day1, day2);
        Collections.sort(days);
        assertEquals(Arrays.asList(day1, day2), days);

        days = Arrays.asList(day2, day2);
        Collections.sort(days);
        assertEquals(Arrays.asList(day2, day2), days);

        days = Arrays.asList(day2, day1);
        Collections.sort(days);
        assertEquals(Arrays.asList(day1, day2), days);

        days = Arrays.asList(day2, day1, day6, day3, day4, day5);
        Collections.sort(days);
        assertEquals(Arrays.asList(day1, day2, day3, day4, day5, day6), days);
    }

}