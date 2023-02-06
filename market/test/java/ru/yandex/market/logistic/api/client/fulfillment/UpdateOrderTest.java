package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.UpdateOrderResponse;
import ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateOrderTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "5927638";
    private static final String PARTNER_ID = "EXT101811250";
    private static final String FULFILLMENT_ID = "EXT101811250";

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    void testUpdateOrderSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_update_order", PARTNER_URL);

        UpdateOrderResponse response = fulfillmentClient.updateOrder(
            DtoFactory.createOrder(),
            getUpdateOrderRestrictedData(),
            getPartnerProperties()
        );

        assertEquals(getExpectedResponse(), response, "Asserting the response is correct");
    }


    @Test
    void testUpdateOrderWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_update_order",
            "ff_update_order_with_errors",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.updateOrder(
                DtoFactory.createOrder(),
                getUpdateOrderRestrictedData(),
                getPartnerProperties()
            )
        );
    }

    @Test
    void testCreateOrderValidationFailed() {
        Order order = DtoFactory.createOrder(null);
        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.createOrder(order, getCreateOrderRestrictedData(), getPartnerProperties())
        );
    }

    private UpdateOrderResponse getExpectedResponse() {
        return new UpdateOrderResponse(
            new ResourceId.ResourceIdBuilder()
                .setYandexId(YANDEX_ID)
                .setPartnerId(PARTNER_ID)
                .setFulfillmentId(FULFILLMENT_ID)
                .build());
    }
}
