package ru.yandex.autotests.market.stat.date;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by entarrion on 18.05.15.
 */
public class DateTimeHandler implements TimeHandler {

    private String pattern;

    public DateTimeHandler(String pattern) {
        this.pattern = pattern;
    }

    public LocalDateTime parse(String source) {
        if (source == null) {
            return null;
        }
        LocalDateTime datetime;
        try {
            datetime = LocalDateTime.parse(source, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeException e) {
            LocalDate date = LocalDate.parse(source, DateTimeFormatter.ofPattern(pattern));
            datetime = LocalDateTime.of(date, LocalTime.of(0, 0, 0));
        }

        return datetime;
    }

    public String format(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return DateTimeFormatter.ofPattern(pattern).format(date);
    }

    @Override
    public String toString() {
        return String.format("Joda time handler for pattern: (%s)", pattern);
    }
}
