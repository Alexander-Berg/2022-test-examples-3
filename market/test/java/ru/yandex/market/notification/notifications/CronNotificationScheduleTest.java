package ru.yandex.market.notification.notifications;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CronNotificationScheduleTest {

    @Test
    void getNextNotificationTimeAfter() throws ParseException {
        var schedule = new CronNotificationSchedule("0 0 9 ? *  MON", ZoneId.of("Europe/Moscow"));

        Instant time = ZonedDateTime.parse("2021-02-04T14:41:45+03:00").toInstant();

        Instant nextTime = schedule.getNextNotificationTimeAfter(time);
        assertEquals(ZonedDateTime.parse("2021-02-08T09:00+03:00").toInstant(), nextTime);

        Instant nextNextTime = schedule.getNextNotificationTimeAfter(nextTime);
        assertEquals(ZonedDateTime.parse("2021-02-15T09:00+03:00").toInstant(), nextNextTime);
    }
}
