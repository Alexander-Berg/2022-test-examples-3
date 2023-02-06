package ru.yandex.market.jmf.time.test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.time.Now;

public class NowTest {

    @Test
    public void offsetDateTime_offset() {
        long offsetSeconds = 120;
        OffsetDateTime now = Now.offsetDateTime();

        OffsetDateTime result = Now.withOffset(Duration.ofSeconds(offsetSeconds), () -> Now.offsetDateTime());

        Long delta = result.toEpochSecond() - now.toEpochSecond();
        // тут такая логика проверки т.к. время "тикает" и с момента вызова методов может произоити переход на
        // следующую секунду.
        Assertions.assertTrue(Math.abs(delta - offsetSeconds) <= 1);
    }

    @Test
    public void offsetDateTime_moment() throws Exception {
        OffsetDateTime time = OffsetDateTime.of(2345, 1, 2, 3, 5, 7, 11, ZoneOffset.UTC);

        Thread.sleep(10);
        OffsetDateTime result = Now.withMoment(time.toInstant(), () -> Now.offsetDateTime());

        Assertions.assertTrue(time.isEqual(result));
    }

}
