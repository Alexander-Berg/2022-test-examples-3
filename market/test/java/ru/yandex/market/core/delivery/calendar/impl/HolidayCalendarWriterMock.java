package ru.yandex.market.core.delivery.calendar.impl;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.delivery.calendar.HolidayCalendar;
import ru.yandex.market.core.delivery.calendar.HolidayCalendarWriter;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class HolidayCalendarWriterMock implements HolidayCalendarWriter {

    private final DatePeriod period;
    private final List<HolidayCalendar> holidays;

    public HolidayCalendarWriterMock(DatePeriod period) {
        this.period = period;
        holidays = new ArrayList<>();
    }

    public DatePeriod getPeriod() {
        return period;
    }

    public List<HolidayCalendar> getHolidays() {
        return holidays;
    }

    @Override
    public void before() {
    }

    @Override
    public void addCalendar(HolidayCalendar calendar) {
        this.holidays.add(calendar);
    }

    @Override
    public void after() {
    }

    @Override
    public void close() {
    }

}
