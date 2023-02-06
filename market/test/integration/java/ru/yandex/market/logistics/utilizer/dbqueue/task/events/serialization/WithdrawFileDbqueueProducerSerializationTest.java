package ru.yandex.market.logistics.utilizer.dbqueue.task.events.serialization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.withdraw.WithdrawFileDbqueuePayload;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.withdraw.WithdrawFileDbqueueProducer;

public class WithdrawFileDbqueueProducerSerializationTest extends DbqueueContextualTest {

    private static final String PAYLOAD_STRING = "{\"vendorId\":123}";

    @Autowired
    private WithdrawFileDbqueueProducer producer;

    @Test
    public void testSerializeWorks() {
        WithdrawFileDbqueuePayload payload = new WithdrawFileDbqueuePayload(123);
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        softly.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
