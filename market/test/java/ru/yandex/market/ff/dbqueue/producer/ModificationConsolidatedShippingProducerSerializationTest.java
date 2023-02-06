package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.ModificationConsolidatedShippingPayload;

public class ModificationConsolidatedShippingProducerSerializationTest extends IntegrationTest {
    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private ModificationConsolidatedShippingProducer modificationConsolidatedShippingProducer;

    @Test
    public void testSerializeWorks() {
        var payload = new ModificationConsolidatedShippingPayload(123L);
        String payloadString = modificationConsolidatedShippingProducer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
