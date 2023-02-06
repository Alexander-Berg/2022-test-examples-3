package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.CancelOrderResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelOrderTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "25";
    private static final String PARTNER_ID = "45";

    @Test
    void testCancelOrderSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_cancel_order", PARTNER_URL);

        CancelOrderResponse response = deliveryServiceClient.cancelOrder(
            createOrderId(), getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(new CancelOrderResponse());
    }

    @Test
    void testCancelOrderWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_cancel_order", "ds_cancel_order_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.cancelOrder(createOrderId(), getPartnerProperties())
        );
    }

    private static ResourceId createOrderId() {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setPartnerId(PARTNER_ID)
            .setDeliveryId(PARTNER_ID)
            .build();
    }
}
