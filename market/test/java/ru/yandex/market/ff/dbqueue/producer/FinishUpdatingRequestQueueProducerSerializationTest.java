package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.FinishUpdatingRequestPayload;

public class FinishUpdatingRequestQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":12345}";

    @Autowired
    private FinishUpdatingRequestQueueProducer producer;

    @Test
    public void testSerializeWorks() {
        FinishUpdatingRequestPayload payload = new FinishUpdatingRequestPayload(12345);
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }


}
