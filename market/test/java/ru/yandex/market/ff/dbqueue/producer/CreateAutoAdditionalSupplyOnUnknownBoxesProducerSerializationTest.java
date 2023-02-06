package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.CreateAutoAdditionalSupplyOnUnknownBoxesPayload;

public class CreateAutoAdditionalSupplyOnUnknownBoxesProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private CreateAutoAdditionalSupplyOnUnknownBoxesQueueProducer producer;

    @Test
    public void testSerializeWorks() {
        CreateAutoAdditionalSupplyOnUnknownBoxesPayload payload =
                new CreateAutoAdditionalSupplyOnUnknownBoxesPayload(123);
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
