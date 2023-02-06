package ru.yandex.qe.mail.meetings.ws.booking.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.joda.time.Interval;

public final class DatePosition {
    private static final ThreadLocal<DateFormat> _DF = ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm"));

    private static final long START_TS;

    static {
        var cal = Calendar.getInstance();
        cal.setTime(new Date(System.currentTimeMillis()));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        START_TS = cal.getTimeInMillis();
    }

    private final int daysOffset;
    private final long timeOffset;

    public static DatePosition dp(int daysOffset, int hours, int minutes) {
        return new DatePosition(daysOffset, hours, minutes);
    }

    public static DatePosition fromDate(Date date) {
        var ts = date.getTime();
        int daysOffset = (int)((ts - START_TS) / TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
        return new DatePosition(daysOffset, ts - START_TS);
    }

    public static Interval week() {
        var leftBorder = dp(0, 0, 0);
        var rightBorder = dp(7, 0, 0);
        return new Interval(leftBorder.toMillis(), rightBorder.toMillis());
    }


    public static Interval weeks(int count) {
        var leftBorder = dp(0, 0, 0);
        var rightBorder = dp( 7 * count, 0, 0);
        return new Interval(leftBorder.toMillis(), rightBorder.toMillis());
    }

    public Date toDate() {
        return new Date(START_TS + timeOffset);
    }

    public long toMillis() {
        return toDate().getTime();
    }

    @Override
    public String toString() {
        return daysOffset + "::" + _DF.get().format(new Date(START_TS + timeOffset));
    }

    private DatePosition(int daysOffset, long timeOffset) {
        this.daysOffset = daysOffset;
        this.timeOffset = timeOffset;
    }

    private DatePosition(int daysOffset, int hours, int minutes) {
        this.daysOffset = daysOffset;
        this.timeOffset = TimeUnit.MILLISECONDS.convert(daysOffset, TimeUnit.DAYS)
                + TimeUnit.MILLISECONDS.convert(hours, TimeUnit.HOURS)
                + TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES);
    }

}
