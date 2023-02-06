package ru.yandex.mail.micronaut.http_logger;

import lombok.experimental.UtilityClass;
import one.util.streamex.StreamEx;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

import java.util.List;

@UtilityClass
class LogUtils {
    static List<Appender> findAppenders(Logger log) {
        return findAppenders(log, Appender.class);
    }

    static <T extends Appender> List<T> findAppenders(Logger log, Class<T> appenderType) {
        return StreamEx.of(((org.apache.logging.log4j.core.Logger) log).getAppenders().values())
            .select(appenderType)
            .toImmutableList();
    }
}
