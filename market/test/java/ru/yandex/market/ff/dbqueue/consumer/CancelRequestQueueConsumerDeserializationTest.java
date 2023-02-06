package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.CancelRequestPayload;

public class CancelRequestQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING = "{\"requestId\":600100}";

    @Autowired
    private CancelRequestQueueConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        CancelRequestPayload payload = consumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(600100);
    }
}
