package ru.yandex.market.logistics.utilizer.dbqueue.task.events.serialization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.transfer.CreateTransferDbqueuePayload;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.transfer.CreateTransferDbqueueProducer;

public class CreateTransferQueueProducerSerializationTest extends DbqueueContextualTest {

    private static final String PAYLOAD_STRING = "{\"utilizationCycleId\":123}";

    @Autowired
    private CreateTransferDbqueueProducer producer;

    @Test
    public void testSerializeWorks() {
        CreateTransferDbqueuePayload payload = new CreateTransferDbqueuePayload(123);
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        softly.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
