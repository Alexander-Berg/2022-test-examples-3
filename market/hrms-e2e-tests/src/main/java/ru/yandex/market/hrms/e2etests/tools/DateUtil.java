package ru.yandex.market.hrms.e2etests.tools;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static final DateTimeFormatter DEFAULT_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static ZonedDateTime now() {
        return ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
    }
    public static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(ZoneId.of("America/Montreal"));
    }
}
