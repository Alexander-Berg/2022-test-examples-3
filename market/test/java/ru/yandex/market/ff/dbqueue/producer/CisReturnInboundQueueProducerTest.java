package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.CisReturnInboundPayload;

public class CisReturnInboundQueueProducerTest extends IntegrationTest {
    private static final String PAYLOAD_STRING = "{\"id\":123}";

    @Autowired
    private CisReturnInboundQueueProducer producer;

    @Test
    public void testSerializeWorks() {
        CisReturnInboundPayload payload = new CisReturnInboundPayload(123);
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }

    @Test
    public void testDeserializeWorks() {
        CisReturnInboundPayload payload = new CisReturnInboundPayload(123);
        CisReturnInboundPayload cisReturnInboundPayload = producer.getPayloadTransformer().toObject(PAYLOAD_STRING);
        assertions.assertThat(cisReturnInboundPayload).isEqualTo(payload);
    }
}
