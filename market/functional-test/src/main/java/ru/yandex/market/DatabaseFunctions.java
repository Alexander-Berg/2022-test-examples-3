package ru.yandex.market;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import com.google.common.collect.Sets;

public class DatabaseFunctions {

    private static final ZoneOffset LOCAL = ZoneOffset.ofHours(3);

    private DatabaseFunctions() {
    }

    public static Date myTrunc(Date date, String mode) {
        ZonedDateTime zonedDateTime = date.toInstant().atZone(LOCAL).truncatedTo(ChronoUnit.DAYS);
        if (mode.equalsIgnoreCase("month")) {
            return Date.from(
                    zonedDateTime
                            .with(TemporalAdjusters.firstDayOfMonth())
                            .toInstant());
        } else if (mode.equalsIgnoreCase("iw")) {
            return Date.from(
                    zonedDateTime
                            .minusDays(zonedDateTime.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue())
                            .toInstant());
        } else if (mode.equalsIgnoreCase("ddd")) {
            return Date.from(zonedDateTime.toInstant());
        }
        throw new UnsupportedOperationException("Implement mode " + mode + " yourself");
    }

    public static Date myTrunc(Date date) {
        return Date.from(date.toInstant().atZone(LOCAL).truncatedTo(ChronoUnit.DAYS).toInstant());
    }

    public static Date myLastDay(Date date) {
        ZonedDateTime dateTime = date.toInstant().atZone(LOCAL);
        return Date.from(dateTime.plusMonths(1).minusDays(dateTime.getDayOfMonth()).toInstant());
    }

    public static Date toStartOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        return cal.getTime();
    }

    public static Date toStartOfYear(Date date) {
        LocalDateTime from = date.toInstant().atZone(LOCAL).toLocalDateTime();
        return new Date(from.with(TemporalAdjusters.firstDayOfYear()).toInstant(LOCAL).toEpochMilli());
    }

    public static Date toDate(Date date) {
        LocalDate localDate = date.toInstant().atZone(LOCAL).toLocalDate();
        return Date.from(localDate.atStartOfDay(LOCAL).toInstant());
    }

    public static Date toMonday(Date date) {
        LocalDateTime from = date.toInstant().atZone(LOCAL).toLocalDateTime();
        from = from.minusDays(from.getDayOfWeek().getValue() - 1);
        return new Date(from.atZone(LOCAL).toInstant().toEpochMilli());
    }

    public static Date subtractYears(Date date, int years) {
        LocalDateTime result = date.toInstant().atZone(LOCAL).toLocalDateTime().minusYears(years);
        return new Date(result.atZone(LOCAL).toInstant().toEpochMilli());
    }

    public static Date subtractDays(Date date, int days) {
        LocalDateTime result = date.toInstant().atZone(LOCAL).toLocalDateTime().minusDays(days);
        return new Date(result.atZone(LOCAL).toInstant().toEpochMilli());
    }

    public static Date addDays(Date date, int days) {
        LocalDateTime result = date.toInstant().atZone(LOCAL).toLocalDateTime().plusDays(days);
        return new Date(result.atZone(LOCAL).toInstant().toEpochMilli());
    }

    public static int toYear(Date date) {
        return date.toInstant().atZone(LOCAL).toLocalDateTime().getYear();
    }

    public static String lowerUTF8(String str) {
        return str == null ? null : str.toLowerCase();
    }

    public static boolean has(Object[] target, Object test) {
        return Arrays.asList(target).contains(test);
    }

    /**
     * Для примитивных типов должны быть использованы обёртки при передачи массива в качестве параметра в запросе
     */
    public static boolean hasAny(Object[] target, Object[] test) {
        return !Sets.intersection(Set.of(target), Set.of(test)).isEmpty();
    }

    public static double divide(double dividend, double divisor) {
        try {
            return dividend / divisor;
        } catch (ArithmeticException e) {
            return Double.POSITIVE_INFINITY;
        }
    }

    public static double ifNotFinite(double possibleInf, double defaultValue) {
        if (Double.isFinite(possibleInf)) {
            return possibleInf;
        }
        return defaultValue;
    }
}
