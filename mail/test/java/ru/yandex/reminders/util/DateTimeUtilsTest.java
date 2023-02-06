package ru.yandex.reminders.util;

import lombok.val;
import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

import static ru.yandex.reminders.util.DateTimeUtilsTestCommon.transitedDate;
import static ru.yandex.reminders.util.DateTimeUtilsTestCommon.tzFrom;
import static ru.yandex.reminders.util.DateTimeUtilsTestCommon.tzTo;

public class DateTimeUtilsTest {
    @Test
    public void tzMigrateInstant() {
        val local = transitedDate;
        val instant = local.toDateTime(tzFrom).toInstant();

        Assert.equals(instant, DateTimeUtils.tzMigrateInstant(instant, tzTo).plus(Duration.standardHours(3)));
        Assert.equals(local, DateTimeUtils.tzMigratedInstantLocalDateTime(instant, tzTo));
    }
}
