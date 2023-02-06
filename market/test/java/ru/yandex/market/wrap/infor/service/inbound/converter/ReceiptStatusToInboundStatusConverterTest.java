package ru.yandex.market.wrap.infor.service.inbound.converter;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusType;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.entity.ReceiptStatus;
import ru.yandex.market.wrap.infor.entity.ReceiptStatusType;

class ReceiptStatusToInboundStatusConverterTest extends SoftAssertionSupport {

    private static final String RECEIPT_KEY = "RECEIPT_KEY";
    private static final LocalDateTime BACK_TO_FUTURE_DATETIME = LocalDateTime.of(1985, 10, 26, 1, 21);
    private static final LocalDateTime BACK_TO_FUTURE_DATETIME_PLUS_3 = LocalDateTime.of(1985, 10, 26, 4, 21);
    private static final ResourceId RESOURCE_ID = new ResourceId("yandexId", "partnerId");

    private final ReceiptStatusToInboundStatusConverter converter = new ReceiptStatusToInboundStatusConverter();

    /**
     * Сценарий №1: проверка корректности ковертации статусов
     */
    @Test
    void testConversionWithNewReceiptStatusType() {
        testStatusTypeConversion(ReceiptStatusType.UNKNOWN, InboundStatusType.UNKNOWN);

        testStatusTypeConversion(ReceiptStatusType.NEW, InboundStatusType.CREATED);
        testStatusTypeConversion(ReceiptStatusType.IN_TRANSIT, InboundStatusType.CREATED);
        testStatusTypeConversion(ReceiptStatusType.SCHEDULED, InboundStatusType.CREATED);

        testStatusTypeConversion(ReceiptStatusType.IN_RECEIVING, InboundStatusType.ACCEPTANCE);
        testStatusTypeConversion(ReceiptStatusType.RECEIVED_COMPLETE, InboundStatusType.ACCEPTANCE);
        testStatusTypeConversion(ReceiptStatusType.CLOSED, InboundStatusType.ACCEPTANCE);

        testStatusTypeConversion(ReceiptStatusType.VERIFIED_CLOSED, InboundStatusType.ACCEPTED);

        testStatusTypeConversion(ReceiptStatusType.CANCELLED, InboundStatusType.CANCELLED);
    }

    /**
     * Сценарий №2: проверка корректности переноса даты при смещении на +3 по времени
     */
    @Test
    void testConversionWithDayCarry() {

        LocalDateTime dateTime = LocalDateTime.of(2018, 9, 9, 22, 55);
        ReceiptStatus receiptStatus = new ReceiptStatus(RECEIPT_KEY, ReceiptStatusType.NEW, dateTime);

        LocalDateTime dateTimePlus3 = LocalDateTime.of(2018, 9, 10, 1, 55);
        InboundStatus expected = new InboundStatus(
            RESOURCE_ID,
            InboundStatusType.CREATED,
            DateTime.fromLocalDateTime(dateTimePlus3)
        );
        InboundStatus actual = converter.convert(RESOURCE_ID, receiptStatus);
        assertInboundStatus(actual, expected);
    }


    private ReceiptStatus createReceiptStatusByType(ReceiptStatusType type) {
        return new ReceiptStatus(RECEIPT_KEY, type, BACK_TO_FUTURE_DATETIME);
    }

    private InboundStatus createInboundStatusByType(InboundStatusType type) {
        return new InboundStatus(RESOURCE_ID, type, DateTime.fromLocalDateTime(BACK_TO_FUTURE_DATETIME_PLUS_3));
    }

    private void testStatusTypeConversion(ReceiptStatusType receiptStatusType, InboundStatusType inboundStatusType) {
        ReceiptStatus receiptStatus = createReceiptStatusByType(receiptStatusType);

        InboundStatus expected = createInboundStatusByType(inboundStatusType);
        InboundStatus actual = converter.convert(RESOURCE_ID, receiptStatus);

        assertInboundStatus(actual, expected);
    }

    private void assertInboundStatus(InboundStatus actual, InboundStatus expected) {
        softly
            .assertThat(actual.getStatusCode())
            .as("Asserting status code")
            .isEqualTo(expected.getStatusCode());

        softly
            .assertThat(actual.getDate().getOffsetDateTime())
            .as("Asserting date")
            .isEqualTo(expected.getDate().getOffsetDateTime());

        softly
            .assertThat(actual.getInboundId())
            .as("Asserting inbound id")
            .isEqualTo(expected.getInboundId());
    }
}
