package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.InventoryingRequestPerSupplierPayload;

public class InventoryingRequestPerSupplierQueueProducerSerializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING = "{\"requestId\":123, \"supplierId\":124}";

    @Autowired
    private InventoryingRequestPerSupplierQueueConsumer inventoryingRequestPerSupplierQueueConsumer;

    @Test
    public void testDeserializationWorks() {
        InventoryingRequestPerSupplierPayload payload =
                inventoryingRequestPerSupplierQueueConsumer.getPayloadTransformer()
                        .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(123);
        assertions.assertThat(payload.getSupplierId()).isEqualTo(124);
    }
}
