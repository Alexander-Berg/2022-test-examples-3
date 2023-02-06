package ru.yandex.personal.mail.search.metrics.scraper.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class DateTimeUtils {
    public static Instant sameDayButCurrentYear(long timestamp) {
        LocalDateTime dt = LocalDateTime.ofEpochSecond(timestamp, 0, OffsetDateTime.now().getOffset());
        dt = dt.withYear(LocalDate.now().getYear());
        return dt.toInstant(OffsetDateTime.now().getOffset());
    }
}
