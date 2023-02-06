package ru.yandex.autotests.market.stat.date;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * Created by kateleb.
 * <p/>
 * BeanIO handler for parsing formatting UnixTimestamp from tsv to LocalDateTime
 */
class UnixTimeMsHandler implements TimeHandler {
    public LocalDateTime parse(String source) {
        return Optional.ofNullable(source).map(Long::parseLong).map(
            t -> LocalDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneId.systemDefault())).orElse(null);
    }

    public String format(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return Long.toString(ZonedDateTime.of(date, ZoneId.systemDefault()).toEpochSecond()) + "000";
    }

    @Override
    public String toString() {
        return "Unix time handler. (milliseconds since 1970-01-01 00:00:00 UTC)";
    }
}
