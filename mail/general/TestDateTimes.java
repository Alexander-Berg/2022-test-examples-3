package ru.yandex.calendar.logic.ics;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;

import ru.yandex.misc.time.TimeUtils;

/**
 * @author Stepan Koltsov
 */
public class TestDateTimes {

    public static Instant utc(int year, int month, int day, int hour, int minute) {
        return new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC).toInstant();
    }

    public static Instant moscow(int year, int month, int day, int hour, int minute) {
        return moscowDateTime(year, month, day, hour, minute).toInstant();
    }

    public static DateTime moscowDateTime(int year, int month, int day, int hour, int minute) {
        return new DateTime(year, month, day, hour, minute, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
    }

    public static Instant plusDays(Instant instant, int days) {
        return instant.plus(Duration.standardDays(days));
    }

    public static Instant plusHours(Instant instant, int hours) {
        return instant.plus(Duration.standardHours(hours));
    }

    public static Instant addDaysMoscow(Instant instant, int amount) {
        DateTime dt = instant.toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        return dt.plusDays(amount).toInstant();
    }

} //~
