package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.FinishUpdatingRequestPayload;

public class FinishUpdatingRequestQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING = "{\"requestId\":12345}";

    @Autowired
    private FinishUpdatingRequestQueueConsumer finishUpdatingRequestQueueConsumer;

    @Test
    public void testDeserializationWorks() {
        FinishUpdatingRequestPayload payload = finishUpdatingRequestQueueConsumer.getPayloadTransformer()
            .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(12345);
    }
}
