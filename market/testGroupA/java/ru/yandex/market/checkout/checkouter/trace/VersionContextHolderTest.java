package ru.yandex.market.checkout.checkouter.trace;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.checkouter.trace.TraceLogHelper.awaitTraceLog;

public class VersionContextHolderTest extends AbstractTraceLogTestBase {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(VersionContextHolderTest.class);

    @Test
    public void shouldWriteVersionIntoTraceFromHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Checkouter-Client-Version", "1.234");

        testRestTemplate.exchange("/ping", HttpMethod.GET, new HttpEntity<>(null, headers), String.class);

        List<Map<String, String>> events = Collections.emptyList();

        events = awaitTraceLog(inMemoryAppender, events);
        if (events == null) {
            fail("No events received!");
        }

        logger.info("{}", events);

        Map<String, String> inRecord = events.stream()
                .filter(it -> "IN".equals(it.get("type")))
                .findFirst()
                .get();

        Assertions.assertEquals("1.234", inRecord.get("kv.clientVersion"));
    }


    @Test
    public void shouldWriteVersionViaClient() {
        checkouterAPI.ping();

        List<Map<String, String>> events = Collections.emptyList();

        events = awaitTraceLog(inMemoryAppender, events);
        if (events == null) {
            fail("No events received!");
        }

        logger.info("{}", events);

        Map<String, String> inRecord = events.stream()
                .filter(it -> "IN".equals(it.get("type")))
                .findFirst()
                .get();

        assertThat(inRecord.get("kv.clientVersion"), CoreMatchers.allOf(
                CoreMatchers.startsWith("1.1."),
                CoreMatchers.endsWith("-SNAPSHOT")
        ));
    }
}
