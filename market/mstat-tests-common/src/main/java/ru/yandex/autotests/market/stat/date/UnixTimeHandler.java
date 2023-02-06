package ru.yandex.autotests.market.stat.date;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Created by jkt on 10.04.14.
 * <p/>
 * BeanIO handler for parsing formatting UnixTimestamp from tsv to LocalDateTime
 */
class UnixTimeHandler implements TimeHandler {
    public LocalDateTime parse(String source) {
        if (source == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(source) * 1000), ZoneId.systemDefault());
    }

    public String format(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return Long.toString(ZonedDateTime.of(date, ZoneId.systemDefault()).toEpochSecond());
    }

    @Override
    public String toString() {
        return "Unix time handler. (seconds since 1970-01-01 00:00:00 UTC)";
    }
}
