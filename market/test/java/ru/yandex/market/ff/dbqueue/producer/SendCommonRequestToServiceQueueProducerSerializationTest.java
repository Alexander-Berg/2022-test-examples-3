package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.SendRequestToServicePayload;

public class SendCommonRequestToServiceQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private SendCommonRequestToServiceQueueProducer sendCommonRequestToServiceQueueProducer;

    @Test
    public void testSerializeWorks() {
        SendRequestToServicePayload payload = new SendRequestToServicePayload(123);
        String payloadString = sendCommonRequestToServiceQueueProducer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
