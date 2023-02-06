package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.UpdateCalendaringExternalIdPayload;

public class UpdateCalendaringExternalIdQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING = "{\"bookingId\":123,\"oldRequestId\":234,\"newRequestId\":345}";

    @Autowired
    private UpdateCalendaringExternalIdQueueProducer producer;

    @Test
    public void testSerializeWorks() {
        UpdateCalendaringExternalIdPayload payload = new UpdateCalendaringExternalIdPayload(
                123,
                234,
                345
        );
        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
