package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.CreateTransferFromPreparedPayload;

public class CreateTransferFromPreparedQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING = "{\"requestId\":123456}";

    @Autowired
    private CreateTransferFromPreparedQueueConsumer createTransferFromPreparedQueueConsumer;

    @Test
    public void testDeserializationWorks() {
        CreateTransferFromPreparedPayload payload = createTransferFromPreparedQueueConsumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(123456);
        assertions.assertThat(payload.getRequestId()).isEqualTo(123456);
    }
}
