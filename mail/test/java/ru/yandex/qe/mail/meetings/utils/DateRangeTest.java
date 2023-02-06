package ru.yandex.qe.mail.meetings.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateRangeTest {
    public static final long AUG_26_2019_MON_12_00_MSK = 1566810000000L;

    @Test(expected = IllegalArgumentException.class)
    public void range() {
        long now = System.currentTimeMillis();
        DateRange.range(new Date(now), new Date(now - 1));
    }

    @Test
    public void lastBusinessWeek() {
        DateRange range = DateRange.lastBusinessWeek(new Date(AUG_26_2019_MON_12_00_MSK));
        verify(range, 19, 23);
        range = DateRange.lastBusinessWeek(new Date(AUG_26_2019_MON_12_00_MSK + Duration.ofDays(1).toMillis()));
        verify(range, 19, 23);
        range = DateRange.lastBusinessWeek(new Date(AUG_26_2019_MON_12_00_MSK - Duration.ofDays(1).toMillis()));
        verify(range, 19, 23);
        range = DateRange.lastBusinessWeek(new Date(AUG_26_2019_MON_12_00_MSK + Duration.ofDays(5).toMillis()));
        verify(range, 26, 30);
    }

    @Test
    public void nextBusinessWeek() {
        DateRange range = DateRange.nextBusinessWeek(new Date(AUG_26_2019_MON_12_00_MSK));
        verify(range, 2, 6);
        range = DateRange.nextBusinessWeek(new Date(AUG_26_2019_MON_12_00_MSK - Duration.ofDays(1).toMillis()));
        verify(range, 26, 30);
    }

    private void verify(DateRange range, int fromDay, int toDay) {
        Calendar c = Calendar.getInstance();
        c.setTime(range.getFrom());
        assertEquals(fromDay, c.get(Calendar.DAY_OF_MONTH));
        c.setTime(range.getTo());
        assertEquals(toDay, c.get(Calendar.DAY_OF_MONTH));
    }

    public static Date toDate(String date) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            parser.setTimeZone(TimeZone.getTimeZone("GMT"));
            return parser.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
