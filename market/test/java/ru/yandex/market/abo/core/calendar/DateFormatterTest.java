package ru.yandex.market.abo.core.calendar;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Anton Irinev (airinev@yandex-team.ru)
 */
public class DateFormatterTest {
    private static Date getDate(final int year, final int month, final int day) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return calendar.getTime();
    }

    @Test
    public void testDateFormattingHighBoundary() {
        final Date date = getDate(2008, 11, 31);
        final String formattedString = DateFormatter.dateToString(date);

        assertEquals("2008-12-31", formattedString);
    }

    @Test
    public void testDateFormattingLowBoundary() {
        final Date date = getDate(2009, 0, 1);
        final String formattedString = DateFormatter.dateToString(date);

        assertEquals("2009-01-01", formattedString);
    }

    // utility methods

    @Test
    public void testDateFormattingWithOffset() {
        final Date date = getDate(2008, 11, 9);
        final int offset = 30;
        final String formattedString = DateFormatter.dateToString(date, offset);

        assertEquals("2009-01-08", formattedString);
    }
}
