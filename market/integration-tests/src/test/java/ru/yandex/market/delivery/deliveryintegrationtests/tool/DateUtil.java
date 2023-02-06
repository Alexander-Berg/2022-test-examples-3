package ru.yandex.market.delivery.deliveryintegrationtests.tool;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static final DateTimeFormatter DEFAULT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter CALENDAR_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME_WITH_ZONE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    public static final DateTimeFormatter CURRENT_DATE_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DEFAULT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private DateUtil() { }

    public static LocalDate nextBusinessDay() {
        return LocalDate.now(ZoneId.of("Europe/Moscow")).plusDays(1);
    }

    public static String currentDateTime() {
        return now().format(DATE_TIME_WITH_ZONE);
    }

    public static String currentDate() {
        return now().format(DEFAULT_DATE);
    }

    public static String currentDateIso() {
        return now().format(CURRENT_DATE_ISO);
    }

    public static String afterTomorrowDateIso() {
        return now().plusDays(2).format(CURRENT_DATE_ISO);
    }

    public static String currentDateTimePlus(int seconds) {
        return now().plusSeconds(seconds).format(DATE_TIME_WITH_ZONE);
    }

    public static String currentDateTimePlusDays(int days) {
        return now().plusDays(days).format(DATE_TIME_WITH_ZONE);
    }

    public static String currentUtcDatePlusDays(int days) {
        return nowUtc().plusDays(days).format(DEFAULT_DATE);
    }

    public static String currentDatePlusDays(int days) {
        return now().plusDays(days).format(DEFAULT_DATE);
    }

    public static String currentDateMinusDays(int days) {
        return now().minusDays(days).format(DEFAULT_DATE);
    }

    public static String currentTimePlusHours(int hours) {
        return now().plusHours(hours).format(DEFAULT_TIME);
    }

    public static String calendarDatePlusDays(int days) {
        return now().plusDays(days).format(CALENDAR_DATE);
    }

    public static String tomorrowDateTime() {
        return now().plusDays(1).format(DATE_TIME_WITH_ZONE);
    }

    public static String todayPlusXDateTime(int days) {
        return now().plusDays(days).format(DATE_TIME_WITH_ZONE);
    }

    public static String todayPlusXDateZeroTime(int days) {
        return now().plusDays(days).withHour(0).withMinute(0).format(DATE_TIME_WITH_ZONE);
    }

    public static String fromDate(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.now(), ZoneId.of("Europe/Moscow")).format(DATE_TIME_WITH_ZONE);
    }

    private static ZonedDateTime now() {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
    }

    private static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(ZoneId.of("America/Montreal"));
    }
}
