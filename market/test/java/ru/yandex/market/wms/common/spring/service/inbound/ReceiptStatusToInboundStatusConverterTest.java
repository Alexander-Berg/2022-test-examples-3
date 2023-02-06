package ru.yandex.market.wms.common.spring.service.inbound;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.StatusCode;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.model.enums.TrailerStatus;
import ru.yandex.market.wms.common.spring.dto.inbound.InboundStatus;
import ru.yandex.market.wms.common.spring.model.entity.ReceiptStatusEntity;
import ru.yandex.market.wms.common.spring.model.entity.TrailerStatusEntity;
import ru.yandex.market.wms.shared.libs.utils.time.DateTimeUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReceiptStatusToInboundStatusConverterTest {

    private final ReceiptStatusToInboundStatusConverter converter = new ReceiptStatusToInboundStatusConverter();
    private final DateTime dateTime = new DateTime("2021-05-01T01:02:03");

    @Test
    public void convertInbound() {
        //given
        ResourceId resourceId = resourceId();
        ReceiptStatusEntity receiptStatusEntity = getReceiptEntity(ReceiptStatus.NEW, dateTime);

        //when
        InboundStatus converted = converter.convert(resourceId, receiptStatusEntity);

        //then
        assertEquals(converted.getInboundId(), resourceId);
        assertEquals(converted.getStatus().getSetDate().toLocalDateTime(),
                dateTime.getOffsetDateTime().toLocalDateTime());
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.CREATED);
        assertEquals(converted.getStatus().getMessage(), "receiptKey");
    }

    @Test
    public void convertAllStatuses() {
        //given
        ResourceId resourceId = resourceId();
        ReceiptStatusEntity receiptUnknown = getReceiptEntity(ReceiptStatus.UNKNOWN, dateTime);
        ReceiptStatusEntity receiptInTransit = getReceiptEntity(ReceiptStatus.IN_TRANSIT, dateTime);
        ReceiptStatusEntity receiptScheduled = getReceiptEntity(ReceiptStatus.SCHEDULED, dateTime);
        ReceiptStatusEntity receiptInReceiving = getReceiptEntity(ReceiptStatus.IN_RECEIVING, dateTime);
        ReceiptStatusEntity receiptReceivedComplete = getReceiptEntity(ReceiptStatus.RECEIVED_COMPLETE, dateTime);
        ReceiptStatusEntity receiptClosed = getReceiptEntity(ReceiptStatus.CLOSED, dateTime);
        ReceiptStatusEntity receiptClosedWith = getReceiptEntity(ReceiptStatus.CLOSED_WITH_DISCREPANCIES, dateTime);
        ReceiptStatusEntity receiptPallet = getReceiptEntity(ReceiptStatus.PALLET_ACCEPTANCE, dateTime);
        ReceiptStatusEntity receiptVerified = getReceiptEntity(ReceiptStatus.VERIFIED_CLOSED, dateTime);
        ReceiptStatusEntity receiptCancelled = getReceiptEntity(ReceiptStatus.CANCELLED, dateTime);

        InboundStatus converted = converter.convert(resourceId, receiptUnknown);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.UNKNOWN);
        converted = converter.convert(resourceId, receiptInTransit);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.CREATED);
        converted = converter.convert(resourceId, receiptScheduled);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.CREATED);
        converted = converter.convert(resourceId, receiptInReceiving);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.ACCEPTANCE);
        converted = converter.convert(resourceId, receiptReceivedComplete);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.ACCEPTANCE);
        converted = converter.convert(resourceId, receiptClosed);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.ACCEPTANCE);
        converted = converter.convert(resourceId, receiptClosedWith);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.ACCEPTANCE);
        converted = converter.convert(resourceId, receiptPallet);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.ARRIVED);
        converted = converter.convert(resourceId, receiptVerified);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.ACCEPTED);
        converted = converter.convert(resourceId, receiptCancelled);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.CANCELLED);
    }

    @Test
    void convertTrailer() {
        //given
        ResourceId resourceId = resourceId();
        TrailerStatusEntity trailerStatusEntity = getTrailerEntity(TrailerStatus.COMPLETED, dateTime);

        //when
        InboundStatus converted = converter.convert(resourceId, trailerStatusEntity);

        //then
        assertEquals(converted.getInboundId(), resourceId);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.ARRIVED);
        assertEquals(converted.getStatus().getMessage(), "receiptKey");
        assertEquals(converted.getStatus().getSetDate().toLocalDateTime(),
                dateTime.getOffsetDateTime().toLocalDateTime());
    }

    @Test
    void unknownTrailerStatus() {
        //given
        ResourceId resourceId = resourceId();
        TrailerStatusEntity trailerStatusEntity = getTrailerEntity(TrailerStatus.CHECKED_IN, dateTime);

        //when
        InboundStatus converted = converter.convert(resourceId, trailerStatusEntity);

        //then
        assertEquals(converted.getInboundId(), resourceId);
        assertEquals(converted.getStatus().getStatusCode(), StatusCode.UNKNOWN);

    }

    private ReceiptStatusEntity getReceiptEntity(ReceiptStatus status, DateTime dateTime) {
        ReceiptStatusEntity receiptStatusEntity = new ReceiptStatusEntity();
        receiptStatusEntity.setStatus(status);
        receiptStatusEntity.setReceiptKey("receiptKey");
        receiptStatusEntity.setAddDate(DateTimeUtils.fromInstant(dateTime.getOffsetDateTime().toInstant()));
        return receiptStatusEntity;
    }

    private TrailerStatusEntity getTrailerEntity(TrailerStatus status, DateTime dateTime) {
        TrailerStatusEntity trailerStatusEntity = new TrailerStatusEntity();
        trailerStatusEntity.setStatus(status);
        trailerStatusEntity.setReceiptKey("receiptKey");
        trailerStatusEntity.setTrailerKey("trailerKey");
        trailerStatusEntity.setAddDate(DateTimeUtils.fromInstant(dateTime.getOffsetDateTime().toInstant()));
        return trailerStatusEntity;
    }

    private ResourceId resourceId() {
        return ResourceId.builder()
                .setYandexId("yaId")
                .setPartnerId("partnerId")
                .build();
    }
}
