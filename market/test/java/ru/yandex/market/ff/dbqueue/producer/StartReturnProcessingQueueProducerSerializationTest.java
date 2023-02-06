package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSortedSet;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.StartReturnProcessingPayload;

public class StartReturnProcessingQueueProducerSerializationTest extends IntegrationTest {
    private static final String PAYLOAD_STRING = "{\"orderIds\":[\"1\",\"2\"]}";

    @Autowired
    private StartReturnProcessingQueueProducer producer;

    @Test
    public void testSerializeWorks() {
        StartReturnProcessingPayload payload = new StartReturnProcessingPayload(ImmutableSortedSet.of("1", "2"));
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}

