package ru.yandex.market.logistics.utilizer.dbqueue.task.events.deserialization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.withdraw.WithdrawFileDbqueueConsumer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.withdraw.WithdrawFileDbqueuePayload;

public class WithdrawFileDbqueueConsumerDeserializationTest extends DbqueueContextualTest {

    private static final String PAYLOAD_STRING = "{\"vendorId\":123}";

    @Autowired
    private WithdrawFileDbqueueConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        WithdrawFileDbqueuePayload payload = consumer.getPayloadTransformer().toObject(PAYLOAD_STRING);
        softly.assertThat(payload).isNotNull();
        softly.assertThat(payload.getVendorId()).isEqualTo(123);
    }
}
