package ru.yandex.market.core.calendar.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.calendar.CalendarType;
import ru.yandex.market.core.calendar.DailyCalendarRepository;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.calendar.Day;
import ru.yandex.market.core.calendar.DayType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "JdbcDailyCalendarRepositoryTest.csv")
class JdbcDailyCalendarRepositoryTest extends FunctionalTest {

    @Autowired
    DailyCalendarRepository dailyCalendarRepository;

    @Test
    void testSameDays() {
        persistAndCheckDays(1001, getDays(LocalDate.of(2021, 11, 8), 3));
    }

    @Test
    void testDeleteDays() {
        var beginDate1 = LocalDate.of(2021, 11, 8);
        var beginDate2 = LocalDate.of(2021, 12, 8);
        var days1 = getDays(beginDate1, 7);
        var days2 = getDays(beginDate2, 7);
        persistAndCheckDays(1001, days1);
        persistAndCheckDays(1002, days2);

        var deletePeriod1 = DatePeriod.of(beginDate1.plusDays(2), 3);

        dailyCalendarRepository.deleteDays(1001, deletePeriod1);
        dailyCalendarRepository.deleteDays(1002);

        days1 = getDaysOutOfPeriod(days1, deletePeriod1);
        days2 = List.of();

        checkDays(1001, days1);
        checkDays(1002, days2);
    }

    @Test
    void testGetDays() {
        var beginDate = LocalDate.of(2021, 11, 8);
        var listDayWorking = getDays(beginDate, 3);
        var listDayHoliday = getDays(beginDate.plusDays(3), 2, DayType.DELIVERY_HOLIDAY);
        var listDayRegionHoliday = getDays(beginDate.plusDays(6), 1, DayType.REGION_HOLIDAY);

        persistAndCheckDays(1001, listDayWorking);
        persistAndCheckDays(1001, listDayHoliday);
        persistAndCheckDays(1001, listDayRegionHoliday);

        DatePeriod period = DatePeriod.of(beginDate, 10);

        assertThat(dailyCalendarRepository.getDays(1001, period, Set.of(DayType.DELIVERY_HOLIDAY, DayType.DELIVERY_WORKDAY)))
                .containsExactlyInAnyOrderElementsOf(
                        Stream.concat(listDayHoliday.stream(), listDayWorking.stream())
                                .collect(Collectors.toList())
                );
        assertThat(dailyCalendarRepository.getDays(1001, period, Set.of(DayType.DELIVERY_HOLIDAY, DayType.REGION_HOLIDAY)))
                .containsExactlyInAnyOrderElementsOf(
                        Stream.concat(listDayRegionHoliday.stream(), listDayHoliday.stream())
                                .collect(Collectors.toList())
                );
        assertThat(dailyCalendarRepository.getDays(1001, period, Set.of(DayType.DELIVERY_WORKDAY)))
                .containsExactlyInAnyOrderElementsOf(listDayWorking);
        assertThat(dailyCalendarRepository.getDays(1001, period, Set.of(DayType.DELIVERY_HOLIDAY)))
                .containsExactlyInAnyOrderElementsOf(listDayHoliday);
        assertThat(dailyCalendarRepository.getDays(1001, period, Set.of(DayType.REGION_HOLIDAY)))
                .containsExactlyInAnyOrderElementsOf(listDayRegionHoliday);
        assertThat(dailyCalendarRepository.getDays(1001, DatePeriod.of(
                LocalDate.of(2021, 11, 11),
                LocalDate.of(2021, 11, 20)
        ))).containsExactlyInAnyOrderElementsOf(
                Stream.concat(listDayHoliday.stream(), listDayRegionHoliday.stream()).collect(Collectors.toList())
        );
    }

    @Test
    void testGetDaysWithOwnersByCalendarType() {
        var beginDate = LocalDate.of(2021, 11, 8);
        var period = DatePeriod.of(beginDate, 5);
        var days = getDays(beginDate, 5);
        persistAndCheckDays(1001, days);
        persistAndCheckDays(1002, days);
        Map<LocalDate, DayType> mapLocalDateToDayType = new HashMap<>();
        days.forEach(day -> mapLocalDateToDayType.put(day.getDate(), day.getType()));

        Map<Long, Map<LocalDate, DayType>> expected = new HashMap<>();

        expected.put(187L, mapLocalDateToDayType);
        expected.put(225L, mapLocalDateToDayType);

        Map<Long, Map<LocalDate, DayType>> result = new HashMap<>();
        dailyCalendarRepository.getDaysWithOwnersByCalendarType(
                CalendarType.REGION_HOLIDAYS,
                period,
                (calendarId, ownerId, date, dayType) ->
                        result.computeIfAbsent(ownerId, key -> new HashMap<>()).put(date, dayType));
        assertThat(result).isEqualTo(expected);
    }

    private List<Day> getDaysOutOfPeriod(List<Day> days, DatePeriod datePeriod) {
        return days.stream()
                .filter(day -> !datePeriod.contains(day.getDate()))
                .collect(Collectors.toList());
    }

    private void persistAndCheckDays(int calendarId, List<Day> days) {
        dailyCalendarRepository.persist(calendarId, days);
        checkDays(calendarId, days);
    }

    private void checkDays(int calendarId, List<Day> days) {
        assertThat(dailyCalendarRepository.getDays(calendarId))
                .containsAll(days);
    }

    private static List<Day> getDays(LocalDate beginDate, int numberOfDays, DayType dayType) {
        return Stream.iterate(
                        beginDate,
                        date -> date.plusDays(1))
                .limit(numberOfDays)
                .map(date -> new Day(date, dayType))
                .collect(Collectors.toList());
    }

    private static List<Day> getDays(LocalDate beginDate, int numberOfDays) {
        return getDays(beginDate, numberOfDays, DayType.DELIVERY_WORKDAY);
    }
}
