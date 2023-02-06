package ru.yandex.autotests.market.stat.date;

import com.google.common.collect.Lists;
import ru.yandex.autotests.market.common.attacher.Attacher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by jkt on 13.05.14.
 */
public enum DatePatterns implements TimeHandler {
    HIVE_DAY_PARTITION(new DateTimeHandler("yyyy-MM-dd")),
    MONTH_PARTITION(new DateTimeHandler("yyyy-MM")),
    MYSQL_DAY(new DateTimeHandler("yyyy-MM-dd")),
    RAW_FILES_DATE(new DateTimeHandler("yyyy-MM-dd HH:mm:ss")),
    DATE_MS(new DateTimeHandler("yyyy-MM-dd HH:mm:ss.S")),
    DATE_MS2(new DateTimeHandler("yyyy-MM-dd HH:mm:ss.SS")),
    MYSQL_DATE(new DateTimeHandler("yyyy-MM-dd HH:mm:ss")),
    UNIX_DATE(new DateTimeHandler("yyyyMMddHHmmss")),
    UNIX_DAY(new DateTimeHandler("yyyyMMdd")),
    UNIX_TIMESTAMP(new UnixTimeHandler()),
    UNIX_TIMESTAMP_WITH_MS(new UnixTimeMsHandler()),
    DATE_WITH_MILLS(new DateTimeHandler("yyyy-MM-dd HH:mm:ss.SSS"));

    private TimeHandler handler;

    DatePatterns(TimeHandler handler) {
        this.handler = handler;
    }

    public static Set<TimeHandler> allHandlers() {
        return Stream.of(values()).map(DatePatterns::getHandler).collect(Collectors.toSet());
    }

    public static LocalDateTime parseByFirstMatchingPattern(String source) {
        if (source == null || source.toLowerCase().equals("null") || source.equals("")) {
            return null;
        }

        //too many millis
        if (source.matches(".*\\.\\d{3,10}$")) {
            source = source.substring(0, source.lastIndexOf(".") + 2);
        }
        for (TimeHandler timeHandler : allHandlers()) {
            try {
                return timeHandler.parse(source);
            } catch (Exception e) {
                Attacher.attachWarning(e.getMessage());
            }
        }
        throw new IllegalArgumentException("Can not parse date " + source + " with any given time handler");
    }

    public static List<String> formatPeriodsTo(List<LocalDateTime> dateTimes, final DatePatterns dp) {
        return Lists.transform(dateTimes, dp::format);
    }

    @Override
    public LocalDateTime parse(String source) {
        return getHandler().parse(source);
    }

    @Override
    public String format(LocalDateTime date) {
        return getHandler().format(date);
    }

    public TimeHandler getHandler() {
        return this.handler;
    }

}
