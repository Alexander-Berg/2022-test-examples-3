package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.InventoryingRequestPerSupplierPayload;

public class InventoryingRequestPerSupplierQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":123,\"supplierId\":124}";

    @Autowired
    private InventoryingRequestPerSupplierProducer inventoryingRequestPerSupplierProducer;

    @Test
    public void testSerializeWorks() {
        InventoryingRequestPerSupplierPayload payload = new InventoryingRequestPerSupplierPayload(123, 124);
        String payloadString = inventoryingRequestPerSupplierProducer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
