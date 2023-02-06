package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.ModificationConsolidatedShippingPayload;

public class ModificationConsolidatedShippingConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {
    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private ModificationConsolidatedShippingConsumer consumer;

    @Test
    void testDeserializationWorks() {
        ModificationConsolidatedShippingPayload payload = new ModificationConsolidatedShippingPayload(
                123L
        );
        var actual = consumer.getPayloadTransformer().toObject(PAYLOAD_STRING);
        assertions.assertThat(actual).isEqualTo(payload);
    }
}
