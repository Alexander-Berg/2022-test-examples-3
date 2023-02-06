package ru.yandex.market.wms.shippingsorter.sorting.utils;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateTimeUtilsTest {

    @Test
    public void testDateTimePatterns() {
        Assertions.assertAll(
                () -> assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                        DateTimeUtils.getFormatPattern("2021-10-07T14:50:20.123654")),

                () -> assertEquals("yyyy-MM-dd HH:mm:ss.SSSSSSSSS",
                        DateTimeUtils.getFormatPattern("2021-10-07 14:50:20.123654789")),

                () -> assertEquals("yyyy-MM-dd'T'HH:mm:ss",
                        DateTimeUtils.getFormatPattern("2021-10-07T14:50:20")),

                () -> assertEquals("yyyy-MM-dd HH:mm:ss",
                        DateTimeUtils.getFormatPattern("2021-10-07 14:50:20")),

                () -> assertEquals("yyyy-MM-dd HH:mm",
                        DateTimeUtils.getFormatPattern("2021-10-07 14:50")));
    }

    @Test
    public void testNanoSecondsExceeded() {
        assertThrows(IllegalArgumentException.class,
                () -> DateTimeUtils.getFormatPattern("2021-10-07T14:50:20.1236549870"));
    }

    @Test
    public void testNanoSecondsParse() {
        Assertions.assertAll(
                () -> assertEquals(LocalDateTime.of(2021, 10, 7, 14, 50, 20, 123654000),
                        DateTimeUtils.parseDateTime("2021-10-07T14:50:20.123654")),

                () -> assertEquals(LocalDateTime.of(2021, 10, 7, 14, 50, 20, 123654000),
                        DateTimeUtils.parseDateTime("2021-10-07 14:50:20.123654"))
        );
    }

    @Test
    public void testDateTimeParse() {
        Assertions.assertAll(
                () -> assertEquals(LocalDateTime.of(2021, 10, 7, 8, 50, 20),
                        DateTimeUtils.parseDateTime("2021-10-07T08:50:20")),

                () -> assertEquals(LocalDateTime.of(2021, 10, 7, 8, 50, 20),
                        DateTimeUtils.parseDateTime("2021-10-07 08:50:20")),

                () -> assertEquals(LocalDateTime.of(2021, 10, 7, 8, 50),
                        DateTimeUtils.parseDateTime("2021-10-07 08:50")),

                () -> assertEquals(LocalDateTime.of(2021, 10, 7, 8, 5),
                        DateTimeUtils.parseDateTime("2021-10-07 08:05"))
        );
    }
}
