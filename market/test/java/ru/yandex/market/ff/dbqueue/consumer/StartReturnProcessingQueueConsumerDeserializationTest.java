package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.StartReturnProcessingPayload;

public class StartReturnProcessingQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {
    private static final String PAYLOAD_STRING = "{\"orderIds\":[\"1\", \"2\"]}";

    @Autowired
    private StartReturnProcessingQueueConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        StartReturnProcessingPayload payload = consumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getOrderIds().size()).isEqualTo(2);
        assertions.assertThat(payload.getOrderIds().contains("1")).isEqualTo(true);
        assertions.assertThat(payload.getOrderIds().contains("2")).isEqualTo(true);
    }
}
