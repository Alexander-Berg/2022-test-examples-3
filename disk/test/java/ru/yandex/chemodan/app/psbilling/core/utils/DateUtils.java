package ru.yandex.chemodan.app.psbilling.core.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;

public class DateUtils {
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    public static Instant futureDate() {
        return Instant.now().plus(Duration.standardDays(1));
    }

    public static LocalDate futureDateLd() {
        return futureDate().toDateTime().toLocalDate();
    }

    public static Option<Instant> futureDateO() {
        return Option.of(futureDate());
    }

    public static Instant farFutureDate() {
        return futureDate().plus(Duration.standardDays(10));
    }

    public static LocalDate farFutureDateLd() {
        return farFutureDate().toDateTime().toLocalDate();
    }

    public static Option<Instant> farFutureDateO() {
        return Option.of(farFutureDate());
    }

    public static Instant pastDate() {
        return Instant.now().minus(Duration.standardDays(1));
    }

    public static LocalDate pastDateLd() {
        return pastDate().toDateTime().toLocalDate();
    }

    public static Option<Instant> pastDateO() {
        return Option.of(pastDate());
    }

    public static Instant farPastDate() {
        return pastDate().minus(Duration.standardDays(10));
    }

    public static LocalDate farPastDateLd() {
        return farPastDate().toDateTime().toLocalDate();
    }

    public static Option<Instant> farPastDateO() {
        return Option.of(farPastDate());
    }

    public static Instant freezeTime() {
        freezeTime(Instant.now());
        return Instant.now();
    }

    public static Instant shiftTime(Duration duration) {
        return freezeTime(Instant.now().plus(duration));
    }

    public static Instant shiftTimeBack(Duration duration) {
        return freezeTime(Instant.now().minus(duration));
    }

    public static Instant freezeTime(Instant time) {
        logger.info("freeze time at {}", time);
        DateTimeUtils.setCurrentMillisFixed(time.toDate().getTime());
        return Instant.now();
    }

    public static Instant freezeTime(LocalDate date) {
        return freezeTime(ru.yandex.chemodan.util.date.DateTimeUtils.toInstant(date));
    }

    public static Instant freezeTime(DateTime date) {
        return freezeTime(date.toInstant());
    }

    public static Instant freezeTime(String date) {
        return freezeTime(Instant.parse(date));
    }

    public static Instant unfreezeTime() {
        DateTimeUtils.setCurrentMillisSystem();
        logger.info("unfreeze time. now is {}", Instant.now());
        return Instant.now();
    }
}
