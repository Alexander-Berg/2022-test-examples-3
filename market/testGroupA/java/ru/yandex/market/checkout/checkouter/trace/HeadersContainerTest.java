package ru.yandex.market.checkout.checkouter.trace;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import ru.yandex.market.checkout.application.AbstractContainerTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.log.Loggers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.checkouter.trace.TraceLogHelper.awaitTraceLog;

public class HeadersContainerTest extends AbstractContainerTestBase {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExperimentsTraceLoggingTest.class);
    public static final Logger TRACE_LOGGER = ((Logger) LoggerFactory.getLogger(Loggers.REQUEST_TRACE));

    private InMemoryAppender traceAppender;
    private Level oldLevel;

    @BeforeEach
    public void setUp() {
        traceAppender = new InMemoryAppender();
        traceAppender.start();

        TRACE_LOGGER.addAppender(traceAppender);
        oldLevel = TRACE_LOGGER.getLevel();
        TRACE_LOGGER.setLevel(Level.TRACE);
    }

    @AfterEach
    public void tearDown() {
        TRACE_LOGGER.detachAppender(traceAppender);
        TRACE_LOGGER.setLevel(oldLevel);
    }

    @Test
    public void shouldWriteExperimentsIntoTraceFromHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CheckouterClientParams.X_EXPERIMENTS, "some_experiment=5");

        testRestTemplate.exchange("/cart", HttpMethod.POST, new HttpEntity<>(null, headers), String.class);

        List<Map<String, String>> events = Collections.emptyList();

        events = awaitTraceLog(traceAppender, events);
        if (events == null) {
            fail("No events received!");
        }

        LOGGER.info("{}", events);

        Map<String, String> inRecord = events.stream()
                .filter(it -> "IN".equals(it.get("type")))
                .findFirst()
                .get();

        assertEquals("some_experiment=5", inRecord.get("kv.header." + CheckouterClientParams.X_EXPERIMENTS));
    }
}
