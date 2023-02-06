package ru.yandex.market.mcrm.utils.date;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateFormattersTest {

    private static final String TORONTO_TZ = "America/Toronto";
    private static final String MOSCOW_TZ = "Europe/Moscow";
    private static final Instant NOW = ZonedDateTime.of(LocalDate.of(2018, 11, 28),
            LocalTime.of(12, 0),
            ZoneId.of(MOSCOW_TZ))
            .toInstant();

    @Test
    public void testFormatDuration() {
        Assertions.assertEquals("02:07:01",
                DateFormatters.formatDuration(Duration.ofSeconds(3600 * 2 + 60 * 7 + 1)));
        Assertions.assertEquals("132:48:59",
                DateFormatters.formatDuration(Duration.ofSeconds(3600 * 132 + 60 * 48 + 59)));
    }

    @Test
    public void zoneId() {
        String formatted = DateFormatters.formatZoneId(ZoneId.of(MOSCOW_TZ), NOW);
        Assertions.assertEquals("(GMT+03:00) Europe/Moscow", formatted);
    }

    @Test
    public void zoneIdNegativeZone() {
        String formatted = DateFormatters.formatZoneId(ZoneId.of(TORONTO_TZ), NOW);
        Assertions.assertEquals("(GMT-05:00) America/Toronto", formatted);
    }
}
