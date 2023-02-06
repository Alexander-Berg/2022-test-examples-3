package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.CancelOrderResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelOrderTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "25";
    private static final String PARTNER_ID = "45";

    @Test
    void testCancelOrderSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_cancel_order", PARTNER_URL);

        CancelOrderResponse response = fulfillmentClient.cancelOrder(
            new ResourceId(YANDEX_ID, PARTNER_ID), getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualTo(new CancelOrderResponse());
    }

    @Test
    void testCancelOrderWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_cancel_order", "ff_cancel_order_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.cancelOrder(new ResourceId(YANDEX_ID, PARTNER_ID), getPartnerProperties())
        );
    }
}
