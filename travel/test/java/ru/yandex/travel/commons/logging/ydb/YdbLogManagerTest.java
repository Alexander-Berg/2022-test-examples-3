package ru.yandex.travel.commons.logging.ydb;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.logging.ydb.TOrderLogRecord;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.travel.commons.logging.CommonMdcParams.MDC_ENTITY_ID;

public class YdbLogManagerTest {
    @Test
    public void testEventsSentAndAsyncThreadClosed() throws Exception {
        Set<String> sentMessages = new ConcurrentSkipListSet<>();
        YdbLogTableClient client = Mockito.mock(YdbLogTableClient.class);
        doAnswer(invocation -> {
            List<TOrderLogRecord> messages = invocation.getArgument(0);
            // there could be re-tries
            sentMessages.addAll(messages.stream().map(TOrderLogRecord::getMessage).collect(toList()));
            Thread.sleep(150);
            return CompletableFuture.completedFuture(null);
        }).when(client).insertLogRecords(any());

        YdbLogProperties properties = YdbLogProperties.builder()
                .dbEndpoint("ep")
                .dbPath("db")
                .tablePath("table")
                .tvmSecret("secret")
                .queueSize(100)
                .batchSize(5)
                .maxAttempts(1)
                .clientTimeout(Duration.ofSeconds(30))
                .backoffSlot(Duration.ofMillis(10))
                .backoffCeiling(2)
                .shutdownTimeout(Duration.ofSeconds(30))
                .build();

        try (YdbLogManager ylm = new YdbLogManager("testYlm", client, properties)) {
            ylm.start();
            for (int i = 0; i < 10; i++) {
                ylm.write(new Log4jLogEvent(
                        "YdbLogManagerTest", null, "loggerFQCN", Level.INFO,
                        new MutableLogEvent(new StringBuilder("Test message " + i), null),
                        List.of(Property.createProperty(MDC_ENTITY_ID, "eid")), null
                ));
            }
        }

        // connection test message + 10 app messages
        assertThat(sentMessages.size()).isEqualTo(1 + 10);
    }
}
