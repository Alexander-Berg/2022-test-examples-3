package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.CreateTransferFromPreparedPayload;

public class CreateTransferFromPreparedQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"requestId\":123456}";

    @Autowired
    private CreateTransferFromPreparedQueueProducer createTransferFromPreparedQueueProducer;

    @Test
    public void testSerializeWorks() {
        CreateTransferFromPreparedPayload payload = new CreateTransferFromPreparedPayload(123456);
        String payloadString = createTransferFromPreparedQueueProducer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
