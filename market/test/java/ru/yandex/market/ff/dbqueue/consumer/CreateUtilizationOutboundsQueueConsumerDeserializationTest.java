package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.CreateUtilizationOutboundsPayload;

public class CreateUtilizationOutboundsQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING = "{\"warehouseId\":172}";

    @Autowired
    private CreateUtilizationOutboundsQueueConsumer createUtilizationOutboundsQueueConsumer;

    @Test
    public void testDeserializationWorks() {
        CreateUtilizationOutboundsPayload payload = createUtilizationOutboundsQueueConsumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(172);
        assertions.assertThat(payload.getWarehouseId()).isEqualTo(172);
    }
}
