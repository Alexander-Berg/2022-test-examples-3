package ru.yandex.market.core.delivery.calendar.impl;

import javax.annotation.Nonnull;

import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.delivery.calendar.CalendarWriterFactory;
import ru.yandex.market.core.delivery.calendar.HolidayCalendarWriter;

/**
 * Not thread-safe!
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class CalendarWriterFactoryMock implements CalendarWriterFactory {

    private HolidayCalendarWriterMock instance;

    @Nonnull
    @Override
    public HolidayCalendarWriter createWriter(DatePeriod period) {
        if (instance == null) {
            instance = new HolidayCalendarWriterMock(period);
        }
        return instance;
    }

    public HolidayCalendarWriterMock getWriterMock() {
        return instance;
    }
}
