package ru.yandex.market.logistics.utilizer.dbqueue.task.events.serialization;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.callticket.CallTicketDbqueuePayload;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.callticket.CallTicketDbqueueProducer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CallTicketProducerSerializationTest extends DbqueueContextualTest {

    private static final String PAYLOAD_STRING = "{\"utilizationCycleId\":123}";

    @Autowired
    private CallTicketDbqueueProducer producer;

    @Test
    public void testSerializeWorks() {
        CallTicketDbqueuePayload payload = new CallTicketDbqueuePayload(123);
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        softly.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
