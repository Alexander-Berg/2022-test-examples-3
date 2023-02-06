package ru.yandex.market.jmf.module.metric.test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.module.metric.impl.MetricsTskvServiceImpl;

public class MetricLogDateTimeTest {

    private final OffsetDateTime SAMPLE_DATE_TIME
            = OffsetDateTime.of(
            2019, 6, 7,
            12, 13, 14,
            34560000, ZoneOffset.of("+03:00"));

    @Test
    public void jvm11DefaultFormat_logShatterAutoParserProblem() {
        Assertions.assertEquals(
                "2019-06-07T12:13:14.03456+03:00",
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(SAMPLE_DATE_TIME));
    }

    @Test
    public void writeMillisecondsWith3Digit() {
        Assertions.assertEquals("2019-06-07T12:13:14.034+03:00", MetricsTskvServiceImpl.formatDate(SAMPLE_DATE_TIME));
    }
}
