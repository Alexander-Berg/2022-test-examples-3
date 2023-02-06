package ru.yandex.market.mboc.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Test;

@SuppressWarnings("checkstyle:MagicNumber")
public class DateTimeFormatterUtilsTest {
    @Test
    public void testToSession() {
        Instant time = Instant.parse("2020-01-01T10:10:00.00Z");
        String sessionName = DateTimeFormatterUtils.getSessionName(time);

        Assertions.assertThat(sessionName).isEqualTo("20200101_1010");
    }

    @Test
    public void testToSessionAtLocalDateTime() {
        LocalDateTime time = LocalDateTime.parse("2020-01-01T10:10:00");
        String sessionName = DateTimeFormatterUtils.getSessionName(time);

        Assertions.assertThat(sessionName).isEqualTo("20200101_1010");
    }

    @Test
    public void testParse() {
        Instant time = Instant.parse("2020-01-01T10:10:00.00Z");
        Instant session = DateTimeFormatterUtils.parseSessionName("20200101_1010");

        Assertions.assertThat(session).isEqualTo(time);
    }

    @Test
    public void testParseAtLocalDateTime() {
        LocalDateTime time = LocalDateTime.parse("2020-01-01T10:10:00");
        LocalDateTime session = DateTimeFormatterUtils.parseSessionNameToLocal("20200101_1010");

        Assertions.assertThat(session).isEqualTo(time);
    }

    @Test
    public void testToSessionNow() {
        // just test no fails
        DateTimeFormatterUtils.getSessionName(Instant.now());
        DateTimeFormatterUtils.getSessionName(DateTimeUtils.dateTimeNow());
    }
}
