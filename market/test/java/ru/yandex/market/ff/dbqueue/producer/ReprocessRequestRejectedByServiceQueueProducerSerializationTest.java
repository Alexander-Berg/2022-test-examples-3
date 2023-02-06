package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.ReprocessRequestRejectedByServicePayload;

public class ReprocessRequestRejectedByServiceQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private ReprocessRequestRejectedByServiceQueueProducer producer;

    @Test
    public void testSerializeWorks() {
        ReprocessRequestRejectedByServicePayload payload = new ReprocessRequestRejectedByServicePayload(123, null);
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
