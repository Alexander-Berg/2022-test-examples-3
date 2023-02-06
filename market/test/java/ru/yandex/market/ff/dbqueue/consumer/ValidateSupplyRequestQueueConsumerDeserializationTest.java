package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;

public class ValidateSupplyRequestQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private ValidateSupplyRequestQueueConsumer validateSupplyRequestQueueConsumer;

    @Test
    public void testDeserializationWorks() {
        ValidateRequestPayload payload = validateSupplyRequestQueueConsumer.getPayloadTransformer()
            .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(123);
    }
}
