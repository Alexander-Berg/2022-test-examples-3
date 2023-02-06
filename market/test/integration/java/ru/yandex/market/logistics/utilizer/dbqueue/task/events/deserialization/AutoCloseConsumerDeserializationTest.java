package ru.yandex.market.logistics.utilizer.dbqueue.task.events.deserialization;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.callticket.CallTicketDbqueueConsumer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.callticket.CallTicketDbqueuePayload;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.closing.AutoCloseDbqueueConsumer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.closing.AutoCloseDbqueuePayload;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AutoCloseConsumerDeserializationTest extends DbqueueContextualTest {

    private static final String PAYLOAD_STRING = "{\"utilizationCycleId\":123}";

    @Autowired
    private AutoCloseDbqueueConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        AutoCloseDbqueuePayload payload =
                consumer.getPayloadTransformer().toObject(PAYLOAD_STRING);
        softly.assertThat(payload).isNotNull();
        softly.assertThat(payload.getUtilizationCycleId()).isEqualTo(123);
    }
}
