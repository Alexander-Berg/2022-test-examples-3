package ru.yandex.market.solomon_pusher.scheduler.tms;

import java.time.format.DateTimeParseException;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.market.solomon_pusher.scheduler.tms.task.ShellTaskInfo;

@ParametersAreNonnullByDefault
public class ShellTaskValidationServiceTest {
    private static final ShellTaskValidationService SERVICE = new ShellTaskValidationService();

    @Test
    public void validCron() {
        SERVICE.validate(cron("* * * * *"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCron() {
        SERVICE.validate(cron("sdfgsdfg"));
    }

    @Test
    public void validPeriodHours() {
        SERVICE.validate(period("PT1H"));
    }

    @Test
    public void validPeriodSeconds() {
        SERVICE.validate(period("PT1S"));
    }

    @Test(expected = DateTimeParseException.class)
    public void invalidPeriod() {
        SERVICE.validate(period("1s"));
    }

    @Test(expected = IllegalStateException.class)
    public void invalidNone() {
        SERVICE.validate(none());
    }

    @Test(expected = IllegalStateException.class)
    public void invalidBoth() {
        SERVICE.validate(info("", ""));
    }

    private static ShellTaskInfo info(@Nullable String cron, @Nullable String period) {
        return new ShellTaskInfo(cron, period, null, null, null, null, null, null, null, null);
    }

    private static ShellTaskInfo cron(String cron) {
        return info(cron, null);
    }

    private static ShellTaskInfo period(String period) {
        return info(null, period);
    }

    private static ShellTaskInfo none() {
        return info(null, null);
    }
}
