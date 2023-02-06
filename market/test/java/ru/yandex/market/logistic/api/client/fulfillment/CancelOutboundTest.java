package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.CancelOutboundResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест для {@link FulfillmentClient#cancelOutbound(ResourceId, PartnerProperties)}.
 */
class CancelOutboundTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "12345";
    private static final String PARTNER_ID = "Zakaz";

    @Test
    void testCancelInboundSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_cancel_outbound", PARTNER_URL);

        CancelOutboundResponse response =
            fulfillmentClient.cancelOutbound(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ CancelOutboundResponse"
        );
    }

    @Test

    void testCancelInboundWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_cancel_outbound",
            "ff_cancel_outbound_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.cancelOutbound(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties())
        );
    }

    private CancelOutboundResponse getExpectedResponse() {
        return new CancelOutboundResponse();
    }
}
