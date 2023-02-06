package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.UpdateCalendaringExternalIdPayload;

public class UpdateCalendaringExternalIdQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING = "{\"bookingId\":123,\"oldRequestId\":234,\"newRequestId\":345}";

    @Autowired
    private UpdateCalendaringExternalIdQueueConsumer updateCalendaringExternalIdQueueConsumer;

    @Test
    public void testDeserializationWorks() {
        UpdateCalendaringExternalIdPayload payload = updateCalendaringExternalIdQueueConsumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(123);
        assertions.assertThat(payload.getBookingId()).isEqualTo(123);
        assertions.assertThat(payload.getOldRequestId()).isEqualTo(234);
        assertions.assertThat(payload.getNewRequestId()).isEqualTo(345);
    }
}
