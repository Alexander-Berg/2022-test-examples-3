package ru.yandex.market.checkout.checkouter.trace;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.checkouter.trace.TraceLogHelper.awaitTraceLog;

public class ExperimentsTraceLoggingTest extends AbstractTraceLogTestBase {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExperimentsTraceLoggingTest.class);

    @Test
    public void shouldWriteExperimentsIntoTraceFromHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CheckouterClientParams.X_EXPERIMENTS, "some_experiment=5");

        testRestTemplate.exchange("/cart", HttpMethod.POST, new HttpEntity<>(null, headers), String.class);

        List<Map<String, String>> events = Collections.emptyList();

        events = awaitTraceLog(inMemoryAppender, events);
        if (events == null) {
            fail("No events received!");
        }

        LOGGER.info("{}", events);

        Map<String, String> inRecord = events.stream()
                .filter(it -> "IN".equals(it.get("type")))
                .findFirst()
                .get();

        assertEquals("some_experiment=5", inRecord.get("kv.experiments"));
    }
}
