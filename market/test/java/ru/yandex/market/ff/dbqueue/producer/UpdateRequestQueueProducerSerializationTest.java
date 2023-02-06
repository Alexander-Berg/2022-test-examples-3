package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.UpdateRequestPayload;

public class UpdateRequestQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private UpdateRequestQueueProducer updateRequestQueueProducer;

    @Test
    public void testSerializeWorks() {
        UpdateRequestPayload payload = new UpdateRequestPayload(123);
        String payloadString = updateRequestQueueProducer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
