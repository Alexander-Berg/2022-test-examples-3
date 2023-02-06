package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;

public class ValidateShadowSupplyRequestQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private ValidateShadowSupplyRequestQueueProducer validateShadowSupplyRequestQueueProducer;

    @Test
    public void testSerializeWorks() {
        ValidateRequestPayload payload = new ValidateRequestPayload(123);
        String payloadString = validateShadowSupplyRequestQueueProducer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
