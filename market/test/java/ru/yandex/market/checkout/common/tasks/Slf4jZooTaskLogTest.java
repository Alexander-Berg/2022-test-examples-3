package ru.yandex.market.checkout.common.tasks;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.carter.InMemoryAppender;

import static org.hamcrest.MatcherAssert.assertThat;

public class Slf4jZooTaskLogTest extends AbstractZooTaskTest {

    private InMemoryAppender inMemoryAppender;

    @BeforeEach
    public void setUp() {
        inMemoryAppender = new InMemoryAppender();
        inMemoryAppender.start();

        ((Logger) (LoggerFactory.getLogger("zoo-task.log"))).addAppender(inMemoryAppender);
    }

    @AfterEach
    public void tearDown() {
        ((Logger) (LoggerFactory.getLogger("zoo-task.log"))).detachAppender(inMemoryAppender);
    }

    @Test
    public void zooTask() {
        Instant instant = ZonedDateTime.of(2019, 7, 12, 12, 30, 0, 123000000, ZoneId.of("Europe/Moscow")).toInstant();
        Date date = Date.from(instant);
        testableClock.setFixed(instant, ZoneId.of("Europe/Moscow"));

        zooTask.runOnce();

        assertThat(inMemoryAppender.getRaw(), Matchers.hasSize(1));
        Map<String, String> tskvMap = inMemoryAppender.getTskvMaps().get(0);

        assertThat(tskvMap.get("taskName"), CoreMatchers.is("TestTask"));
        assertThat(tskvMap.get("startTime"), CoreMatchers.is(date.toString()));
        assertThat(tskvMap.get("startTimeMs"), CoreMatchers.is(String.valueOf(date.getTime())));
        assertThat(tskvMap.get("endTime"), CoreMatchers.is(date.toString()));
        assertThat(tskvMap.get("endTimeMs"), CoreMatchers.is(String.valueOf(date.getTime())));
        assertThat(tskvMap.get("kvKeys"), CoreMatchers.is("key,key2"));
        assertThat(tskvMap.get("kvValues"), CoreMatchers.is("value,value2"));
    }
}
