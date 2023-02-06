package ru.yandex.autotests.market.stat.util;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author nettoyeur
 */
public class DateUtils {
    public static int getOffsetHours(LocalDateTime time) {
        try {
            return ZonedDateTime.from(time).getOffset().getTotalSeconds() / 60 / 60;
        } catch (DateTimeException e) {
            return 4;
        }
    }

    public static long getMillis(LocalDateTime time) {
        return ZonedDateTime.of(time, ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static LocalDateTime fromMillis(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
}