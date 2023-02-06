package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.EnrichRequestItemPayload;

public class EnrichRequestItemQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING = "{\"requestId\":600100,\"limit\":10,\"offset\":0}";

    @Autowired
    private EnrichRequestItemQueueConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        EnrichRequestItemPayload payload = consumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(600100);
        assertions.assertThat(payload.getLimit()).isEqualTo(10);
        assertions.assertThat(payload.getOffset()).isEqualTo(0);
    }

}
