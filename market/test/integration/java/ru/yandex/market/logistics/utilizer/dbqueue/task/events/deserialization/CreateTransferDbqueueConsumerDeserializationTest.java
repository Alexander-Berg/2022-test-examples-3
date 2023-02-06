package ru.yandex.market.logistics.utilizer.dbqueue.task.events.deserialization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.transfer.CreateTransferDbqueueConsumer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.transfer.CreateTransferDbqueuePayload;

public class CreateTransferDbqueueConsumerDeserializationTest extends DbqueueContextualTest {

    private static final String PAYLOAD_STRING = "{\"utilizationCycleId\":123}";

    @Autowired
    private CreateTransferDbqueueConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        CreateTransferDbqueuePayload payload = consumer.getPayloadTransformer().toObject(PAYLOAD_STRING);
        softly.assertThat(payload).isNotNull();
        softly.assertThat(payload.getUtilizationCycleId()).isEqualTo(123);
    }
}
