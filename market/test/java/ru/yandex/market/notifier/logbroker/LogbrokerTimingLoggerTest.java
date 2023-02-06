package ru.yandex.market.notifier.logbroker;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.notifier.util.InMemoryAppender;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LogbrokerTimingLoggerTest {

    private Logger logger;
    private InMemoryAppender appender;

    @BeforeEach
    public void setUp() {
        appender = new InMemoryAppender();
        appender.start();

        logger = (Logger) LoggerFactory.getLogger("logbroker_timings");
        logger.addAppender(appender);
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    public void testLogbrokerTimingLogger() {
        Instant instant = ZonedDateTime.of(
                2019, 4, 3, 18, 50, 0, 0, ZoneId.systemDefault()
        ).toInstant();

        Date date = Date.from(instant);

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(1234L);
        Date tranDate = Date.from(instant.minusSeconds(30L));
        orderHistoryEvent.setTranDate(tranDate);
        Date publishTime = Date.from(instant.minusSeconds(15L));
        orderHistoryEvent.setPublishDate(publishTime);

        LogbrokerTimingLogger.logDelays(orderHistoryEvent, date, "standard", LogbrokerImportResult.SUCCESS);

        Map<String, String> map = appender.getTskvMaps().get(0);

        assertThat(map.get("id"), is("1234"));
        assertThat(map.get("read_time"), is(date.toString()));
        assertThat(map.get("tran_time"), is(tranDate.toString()));
        assertThat(map.get("publish_time"), is(publishTime.toString()));
        assertThat(map.get("consumer_index"), is("0"));
        assertThat(map.get("market_environment_profile"), is("standard"));
        assertThat(map.get("result"), is("SUCCESS"));
    }
}
