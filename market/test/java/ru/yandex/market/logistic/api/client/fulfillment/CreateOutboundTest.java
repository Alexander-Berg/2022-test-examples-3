package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.Outbound;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOutboundResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateOutboundTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "23456";
    private static final String PARTNER_ID = "Zakazik";

    @Test
    void testCreateOutboundSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_create_outbound", PARTNER_URL);
        CreateOutboundResponse response = fulfillmentClient.createOutbound(
            getObjectFromXml("fixture/entities/outbound.xml", Outbound.class), getPartnerProperties());
        assertEquals(getExpectedResponse(), response, "Должен вернуть корректный ответ CreateOutboundResponse");
    }

    @Test
    void testCreateOutboundWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_create_outbound", "ff_create_outbound_with_errors",
            PARTNER_URL);
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.createOutbound(
                getObjectFromXml("fixture/entities/outbound.xml", Outbound.class), getPartnerProperties()
            )
        );
    }

    private CreateOutboundResponse getExpectedResponse() {
        return new CreateOutboundResponse(new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setFulfillmentId(PARTNER_ID)
            .setPartnerId(PARTNER_ID)
            .build());
    }
}
