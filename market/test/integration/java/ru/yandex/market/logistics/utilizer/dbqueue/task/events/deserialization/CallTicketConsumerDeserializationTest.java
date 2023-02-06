package ru.yandex.market.logistics.utilizer.dbqueue.task.events.deserialization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.callticket.CallTicketDbqueueConsumer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.callticket.CallTicketDbqueuePayload;

public class CallTicketConsumerDeserializationTest extends DbqueueContextualTest {

    private static final String PAYLOAD_STRING = "{\"utilizationCycleId\":123}";

    @Autowired
    private CallTicketDbqueueConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        CallTicketDbqueuePayload payload =
                consumer.getPayloadTransformer().toObject(PAYLOAD_STRING);
        softly.assertThat(payload).isNotNull();
        softly.assertThat(payload.getUtilizationCycleId()).isEqualTo(123);
    }
}
