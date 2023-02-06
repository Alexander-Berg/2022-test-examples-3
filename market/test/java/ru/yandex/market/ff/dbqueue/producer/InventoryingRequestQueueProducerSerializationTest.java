package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.InventoryingRequestPayload;

public class InventoryingRequestQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private InventoryingRequestQueueProducer inventoryingRequestQueueProducer;

    @Test
    public void testSerializeWorks() {
        InventoryingRequestPayload payload = new InventoryingRequestPayload(123);
        String payloadString = inventoryingRequestQueueProducer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
