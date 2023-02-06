package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.CancelInboundResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест для {@link FulfillmentClient#cancelInbound(ResourceId, PartnerProperties)}.
 */
class CancelInboundTest extends CommonServiceClientTest {

    protected static final String YANDEX_ID = "12345";
    protected static final String PARTNER_ID = "Zakaz";

    @Test
    void testCancelInboundSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_cancel_inbound", PARTNER_URL);

        CancelInboundResponse response =
            fulfillmentClient.cancelInbound(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ CancelInboundResponse"
        );
    }

    @Test
    void testCancelInboundWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_cancel_inbound",
            "ff_cancel_inbound_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.cancelInbound(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties())
        );
    }


    private CancelInboundResponse getExpectedResponse() {
        return new CancelInboundResponse();
    }
}
