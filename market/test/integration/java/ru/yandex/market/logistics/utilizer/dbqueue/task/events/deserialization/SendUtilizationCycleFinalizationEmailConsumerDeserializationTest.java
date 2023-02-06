package ru.yandex.market.logistics.utilizer.dbqueue.task.events.deserialization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.SendUtilizationCycleFinalizationEmailConsumer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.SendUtilizationCycleFinalizationEmailPayload;

public class SendUtilizationCycleFinalizationEmailConsumerDeserializationTest extends DbqueueContextualTest {

    private static final String PAYLOAD_STRING = "{\"utilizationCycleId\":123}";

    @Autowired
    private SendUtilizationCycleFinalizationEmailConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        SendUtilizationCycleFinalizationEmailPayload payload =
                consumer.getPayloadTransformer().toObject(PAYLOAD_STRING);
        softly.assertThat(payload).isNotNull();
        softly.assertThat(payload.getUtilizationCycleId()).isEqualTo(123);
    }
}
