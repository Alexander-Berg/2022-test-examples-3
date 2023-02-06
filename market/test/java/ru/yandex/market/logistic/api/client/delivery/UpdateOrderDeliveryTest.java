package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateOrderDeliveryResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createUpdateOrderDelivery;

class UpdateOrderDeliveryTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testUpdateOrderDeliverySucceeded() throws Exception {
        prepareMockServiceNormalized("ds_update_order_delivery", PARTNER_URL);

        UpdateOrderDeliveryResponse response = deliveryServiceClient.updateOrderDelivery(
            createUpdateOrderDelivery(),
            getPartnerProperties());
        assertions.assertThat(response)
            .as("Asserting that response is not null")
            .isNotNull();
    }

    @Test
    void testUpdateOrderDeliveryFailed() throws Exception {
        prepareMockServiceNormalized("ds_update_order_delivery_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.updateOrderDelivery(
                createUpdateOrderDelivery(),
                getPartnerProperties()
            )
        );
    }
}
