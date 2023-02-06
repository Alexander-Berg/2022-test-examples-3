package ru.yandex.market.logistic.api.client.delivery;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.Item;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateOrderItemsResponse;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotEmptyErrorMessage;

class UpdateOrderItemsTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testUpdateOrderItemsSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ds_update_order_items",
            PARTNER_URL
        );

        UpdateOrderItemsResponse response =
            deliveryServiceClient.updateOrderItems(
                DtoFactory.createOrderId(),
                BigDecimal.valueOf(1100),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1),
                120,
                70,
                30,
                DtoFactory.createItems(),
                getPartnerProperties()
            );
        assertEquals(
            getRequestId(),
            response.getOrderId(),
            "Проверяем соответствие orderId"
        );
    }

    @Test
    void testUpdateOrderWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ds_update_order_items_with_errors",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.updateOrderItems(
                DtoFactory.createOrderId(),
                BigDecimal.valueOf(1100),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1),
                120,
                70,
                30,
                DtoFactory.createItems(),
                getPartnerProperties()
            )
        );
    }

    @Test
    void testCreateOrderValidationFailed() {
        List<Item> items = Collections.emptyList();

        assertions.assertThatThrownBy(
            () -> deliveryServiceClient.updateOrderItems(
                DtoFactory.createOrderId(),
                BigDecimal.valueOf(1100),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1),
                120,
                70,
                30,
                items,
                getPartnerProperties()
            )
        )
        .isInstanceOf(ValidationException.class)
        .hasMessage(getNotEmptyErrorMessage("items"));
    }

    private ResourceId getRequestId() {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId("12345")
            .build();
    }
}
