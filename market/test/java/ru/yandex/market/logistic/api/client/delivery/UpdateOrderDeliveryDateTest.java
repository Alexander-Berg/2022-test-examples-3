package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateOrderDeliveryDateResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderDeliveryDate;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createOrderId;

class UpdateOrderDeliveryDateTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testUpdateOrderDeliveryDateSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_update_order_delivery_date", PARTNER_URL);

        UpdateOrderDeliveryDateResponse response =
            deliveryServiceClient.updateOrderDeliveryDate(DtoFactory.createOrderDeliveryDate(),
                getPartnerProperties());
        assertEquals(
            getRequestId(),
            response.getOrderId(),
            "Проверяем соответствие orderId"
        );
    }

    @Test
    void testUpdateOrderWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_update_order_delivery_date_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.updateOrderDeliveryDate(
                DtoFactory.createOrderDeliveryDate(),
                getPartnerProperties()
            )
        );
    }

    @Test
    void testCreateOrderValidationFailed() {
        OrderDeliveryDate orderDeliveryDate =
            new OrderDeliveryDate.OrderDeliveryDateBuilder(createOrderId(), null).build();

        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.updateOrderDeliveryDate(orderDeliveryDate, getPartnerProperties())
        );
    }

    private ResourceId getRequestId() {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId("12345")
            .build();
    }
}
