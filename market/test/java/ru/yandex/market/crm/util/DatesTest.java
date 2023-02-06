package ru.yandex.market.crm.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatesTest {

    @Test
    public void formatDuration() {
        assertFormattedDuration("0ms", Duration.ZERO);
        assertFormattedDuration("12ms", Duration.ofMillis(12));
        assertFormattedDuration("7s", Duration.ofSeconds(7));
        assertFormattedDuration("1h 20m 34s 567ms", Duration.ofMillis(4834567));
        assertFormattedDuration("2d 1h 20m 34s 567ms", Duration.ofMillis(177634567));
    }

    @Test
    public void formatShortGenitive_jan() {
        LocalDate date = LocalDate.of(2019, 1, 13);
        String result = Dates.formatShortGenitive(date);
        Assertions.assertEquals("13 января", result);
    }

    @Test
    public void formatShortGenitive_dec() {
        LocalDate date = LocalDate.of(2019, 12, 19);
        String result = Dates.formatShortGenitive(date);
        Assertions.assertEquals("19 декабря", result);
    }

    @Test
    public void formatShortGenitive_jul() {
        LocalDate date = LocalDate.of(2019, 7, 19);
        String result = Dates.formatShortGenitive(date);
        Assertions.assertEquals("19 июля", result);
    }

    @Test
    public void parseOffsetDateTime_withoutTz() {
        OffsetDateTime result = Dates.parseDateTime("2019-05-01 07:11:13");
        Assertions.assertEquals("2019-05-01T07:11:13+03:00", result.toString());
    }

    @Test
    public void parseOffsetDateTime_withoutTz2() {
        OffsetDateTime result = Dates.parseDateTime("30.11.2000 11:58:21");
        Assertions.assertEquals("2000-11-30T11:58:21+03:00", result.toString());
    }

    @Test
    public void parseOffsetDateTime_offset() {
        OffsetDateTime result = Dates.parseDateTime("2019-05-01T07:11:13+03:00");
        Assertions.assertEquals("2019-05-01T07:11:13+03:00", result.toString());
    }

    @Test
    public void parseOffsetDateTime_tz() {
        OffsetDateTime result = Dates.parseDateTime("2019-05-01T07:11:13+05:00[Asia/Yekaterinburg]");
        Assertions.assertEquals("2019-05-01T07:11:13+05:00", result.toString());
    }

    @Test
    public void parseOffsetDateTime_offset_basic() {
        OffsetDateTime result = Dates.parseDateTime("2019-05-01T07:11:13+0300");
        Assertions.assertEquals("2019-05-01T07:11:13+03:00", result.toString());
    }

    private void assertFormattedDuration(String expected, Duration actual) {
        Assertions.assertEquals(expected, Dates.formatDuration(actual));
    }


}
