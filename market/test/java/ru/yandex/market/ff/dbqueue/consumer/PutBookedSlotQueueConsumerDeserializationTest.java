package ru.yandex.market.ff.dbqueue.consumer;

import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.PutBookedSlotPayload;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

public class PutBookedSlotQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    public static final LocalDateTime REQUESTED_DATE =
            LocalDateTime.of(2021, 1, 1, 10, 10, 10);

    public static final LocalDateTime FROM_TIME =
            LocalDateTime.of(2021, 1, 1, 10, 10, 10);

    public static final LocalDateTime TO_TIME =
            LocalDateTime.of(2021, 1, 1, 11, 10, 10);

    private static final long GATE_ID = 1L;
    private static final long REQUEST_ID = 11L;


    @Autowired
    private PutBookedSlotQueueConsumer putBookedSlotQueueConsumer;

    @Test
    public void testDeserializationWorksActive() throws IOException {
        PutBookedSlotPayload payload = putBookedSlotQueueConsumer.getPayloadTransformer()
                .toObject(FileContentUtils.getFileContent(
                        "consumer/put_cs_booked_slot_payload_active_serialization.json"));

        Assertions.assertNotNull(payload);
        Assertions.assertEquals(payload.getRequestId(), REQUEST_ID);
        Assertions.assertEquals(payload.getBookingStatus(), BookingStatus.ACTIVE);
        Assertions.assertEquals(payload.getGateId(), GATE_ID);
        Assertions.assertEquals(payload.getFromTime(), FROM_TIME);
        Assertions.assertEquals(payload.getToTime(), TO_TIME);

    }

    @Test
    public void testDeserializationWorksCancel() throws IOException {
        PutBookedSlotPayload payload = putBookedSlotQueueConsumer.getPayloadTransformer()
                .toObject(FileContentUtils.getFileContent(
                        "consumer/put_cs_booked_slot_payload_cancel_serialization.json"));

        Assertions.assertNotNull(payload);
        Assertions.assertEquals(payload.getRequestId(), REQUEST_ID);
        Assertions.assertEquals(payload.getBookingStatus(), BookingStatus.CANCELLED);
    }
}
