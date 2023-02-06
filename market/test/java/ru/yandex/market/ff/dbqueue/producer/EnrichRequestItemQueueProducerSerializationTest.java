package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.EnrichRequestItemPayload;

public class EnrichRequestItemQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":600100,\"limit\":10,\"offset\":0}";

    @Autowired
    private EnrichRequestItemQueueProducer producer;

    @Test
    public void testSerializeWorks() {
        EnrichRequestItemPayload payload = new EnrichRequestItemPayload(600100, 10, 0);
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }


}
