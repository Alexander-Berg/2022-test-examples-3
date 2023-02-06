package ru.yandex.market.logistics.utilizer.dbqueue.task.events.serialization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.SendUtilizationCycleFinalizationEmailPayload;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.SendUtilizationCycleFinalizationEmailProducer;

public class SendUtilizationCycleFinalizationEmailProducerSerializationTest extends DbqueueContextualTest {

    private static final String PAYLOAD_STRING = "{\"utilizationCycleId\":123}";

    @Autowired
    private SendUtilizationCycleFinalizationEmailProducer producer;

    @Test
    public void testSerializeWorks() {
        SendUtilizationCycleFinalizationEmailPayload payload = new SendUtilizationCycleFinalizationEmailPayload(123);
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        softly.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
