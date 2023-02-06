package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.CreateUtilizationOutboundsPayload;

public class CreateUtilizationOutboundsQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"warehouseId\":172}";

    @Autowired
    private CreateUtilizationOutboundsQueueProducer createUtilizationOutboundsQueueProducer;

    @Test
    public void testSerializeWorks() {
        CreateUtilizationOutboundsPayload payload = new CreateUtilizationOutboundsPayload(172);
        String payloadString = createUtilizationOutboundsQueueProducer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
