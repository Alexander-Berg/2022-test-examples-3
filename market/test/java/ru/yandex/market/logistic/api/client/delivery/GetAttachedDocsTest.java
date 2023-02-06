package ru.yandex.market.logistic.api.client.delivery;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.DocumentFormat;
import ru.yandex.market.logistic.api.model.delivery.OrientationType;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.request.entities.ShipmentType;
import ru.yandex.market.logistic.api.model.delivery.response.GetAttachedDocsResponse;
import ru.yandex.market.logistic.api.utils.DateTime;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createSender;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createWarehouse;

class GetAttachedDocsTest extends CommonServiceClientTest {

    private static final ResourceId ORDER_ID = new ResourceId.ResourceIdBuilder().setYandexId("12345").build();
    private static final ShipmentType SHIPMENT_TYPE = ShipmentType.ACCEPTANCE;
    private static final DateTime SHIPMENT_DATE = DateTime.fromLocalDateTime(LocalDateTime.of(2019, 8, 7, 12, 11, 10));
    private static final ResourceId REGISTER_ID = new ResourceId.ResourceIdBuilder().setYandexId("123").build();

    @Test
    void testGetAttachedDocsSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_get_attached_docs", PARTNER_URL);

        GetAttachedDocsResponse response = callApiMethod();
        assertEquals(getExpectedResponse(), response, "Должен вернуть корректный ответ GetOrderResponse");
    }

    @Test
    void testGetAttachedDocsWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_attached_docs",
            "ds_get_attached_docs_with_errors",
            PARTNER_URL);
        assertThrows(RequestStateErrorException.class, this::callApiMethod);
    }

    @Test
    void testGetAttachedDocsValidationFailed() {
        assertThrows(ValidationException.class, () ->
            deliveryServiceClient.getAttachedDocs(
                emptyList(),
                SHIPMENT_TYPE,
                SHIPMENT_DATE,
                createSender(),
                createWarehouse(),
                REGISTER_ID,
                getPartnerProperties())
        );
    }

    private GetAttachedDocsResponse callApiMethod() {
        return deliveryServiceClient.getAttachedDocs(
            singletonList(ORDER_ID),
            SHIPMENT_TYPE,
            SHIPMENT_DATE,
            createSender(),
            createWarehouse(),
            REGISTER_ID,
            getPartnerProperties());
    }

    private GetAttachedDocsResponse getExpectedResponse() {
        return new GetAttachedDocsResponse.GetAttachedDocsResponseBuilder()
            .setPdf("test pdf")
            .setFormat(DocumentFormat.PDF)
            .setOrientation(OrientationType.ALBUM)
            .build();
    }
}
