package ru.yandex.market.ff.dbqueue.producer;

import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.PutBookedSlotPayload;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

public class PutBookedSlotQueueProducerSerializationTest extends IntegrationTest {

    public static final LocalDateTime REQUESTED_DATE =
            LocalDateTime.of(2021, 1, 1, 10, 10, 10);

    public static final LocalDateTime FROM_TIME =
            LocalDateTime.of(2021, 1, 1, 10, 10, 10);

    public static final LocalDateTime TO_TIME =
            LocalDateTime.of(2021, 1, 1, 11, 10, 10);

    private static final long GATE_ID = 1L;
    private static final long REQUEST_ID = 11L;

    @Autowired
    private PutBookedSlotQueueProducer putBookedSlotQueueProducer;

    @Test
    public void testSerializationWorksActive() throws IOException {
        PutBookedSlotPayload payload = new PutBookedSlotPayload(
                REQUEST_ID,
                BookingStatus.ACTIVE,
                GATE_ID,
                FROM_TIME,
                TO_TIME
        );

        String payloadString = putBookedSlotQueueProducer.getPayloadTransformer().fromObject(payload);
        Assertions.assertNotNull(payloadString);
        JSONAssert.assertEquals(FileContentUtils.getFileContent(
                "consumer/put_cs_booked_slot_payload_active_serialization.json"),
                payloadString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializationWorksCancel() throws IOException {
        PutBookedSlotPayload payload = new PutBookedSlotPayload(REQUEST_ID, BookingStatus.CANCELLED);

        String payloadString = putBookedSlotQueueProducer.getPayloadTransformer().fromObject(payload);
        Assertions.assertNotNull(payloadString);
        JSONAssert.assertEquals(FileContentUtils.getFileContent(
                "consumer/put_cs_booked_slot_payload_cancel_serialization.json"),
                payloadString, JSONCompareMode.NON_EXTENSIBLE);

    }

    private Supplier createFirstPartySupplier() {
        Supplier supplier = new Supplier();
        supplier.setSupplierType(SupplierType.FIRST_PARTY);
        return supplier;
    }

}
