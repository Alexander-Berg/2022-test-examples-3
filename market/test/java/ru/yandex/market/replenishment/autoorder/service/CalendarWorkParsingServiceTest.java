package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.api.dto.CalendarWorkDTO;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.CalendarWorkDateItem;

import static org.junit.Assert.assertEquals;

public class CalendarWorkParsingServiceTest extends FunctionalTest {
    private final LocalDate from = LocalDate.of(2022, 1, 1);
    private final LocalDate to = from.plusMonths(11);
    private final CalendarWorkParsingService calendarWorkParsingService = new CalendarWorkParsingService(from, to);

    @Autowired
    CalendarWorkService calendarWorkService;

    @Test
    @DbUnitDataSet(before = "CalendarWorkParsingServiceTest.daily.csv")
    public void testDailyParsing() {
        CalendarWorkDTO calendarWorkDTO = calendarWorkService.getCalendar(from, to);
        List<CalendarWorkDateItem> calendarWorkDateItem =
            calendarWorkParsingService.parseCalendarEvents(calendarWorkDTO);
        assertEquals(5, calendarWorkDateItem.size());
        assertEquals(2, calendarWorkDateItem.stream().filter(item -> Boolean.FALSE.equals(item.getWorking())).count());
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkParsingServiceTest.weekly.csv")
    public void testWeeklyParsing() {
        CalendarWorkDTO calendarWorkDTO = calendarWorkService.getCalendar(from, to,1L);
        List<CalendarWorkDateItem> calendarWorkDateItem =
            calendarWorkParsingService.parseCalendarEvents(calendarWorkDTO);
        assertEquals(15, calendarWorkDateItem.size());
        assertEquals(1, calendarWorkDateItem.stream().filter(item -> Boolean.FALSE.equals(item.getWorking())).count());
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkParsingServiceTest.monthly_number.csv")
    public void testMonthlyNumberParsing() {
        CalendarWorkDTO calendarWorkDTO = calendarWorkService.getCalendar(from, to);
        List<CalendarWorkDateItem> calendarWorkDateItem =
            calendarWorkParsingService.parseCalendarEvents(calendarWorkDTO);
        assertEquals(12, calendarWorkDateItem.size());
        assertEquals(4, calendarWorkDateItem.stream().filter(item -> Boolean.FALSE.equals(item.getWorking())).count());
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkParsingServiceTest.monthly_day_weekno.csv")
    public void testMonthlyDayWeeknoParsing() {
        CalendarWorkDTO calendarWorkDTO = calendarWorkService.getCalendar(from, to);
        List<CalendarWorkDateItem> calendarWorkDateItem =
            calendarWorkParsingService.parseCalendarEvents(calendarWorkDTO);
        assertEquals(11, calendarWorkDateItem.size());
        assertEquals(1, calendarWorkDateItem.stream().filter(item -> Boolean.FALSE.equals(item.getWorking())).count());
    }

    @Test
    @DbUnitDataSet(before = "CalendarWorkParsingServiceTest.yearly.csv")
    public void testYearlyParsing() {
        CalendarWorkDTO calendarWorkDTO = calendarWorkService.getCalendar(from, to);
        List<CalendarWorkDateItem> calendarWorkDateItem =
            calendarWorkParsingService.parseCalendarEvents(calendarWorkDTO);
        assertEquals(1, calendarWorkDateItem.size());
    }
}
