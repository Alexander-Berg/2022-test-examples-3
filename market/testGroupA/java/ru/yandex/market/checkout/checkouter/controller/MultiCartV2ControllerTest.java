package ru.yandex.market.checkout.checkouter.controller;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.AvailableDeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CartItemResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.CartResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartResponse;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiCartV2ControllerTest extends AbstractWebTestBase {

    @Autowired
    private OrderCreateHelper orderCreateHelper;

    @Test
    public void serializationTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addAvailableDeliveryType(AvailableDeliveryType.COURIER)
                        .build()
        );
        parameters.getReportParameters().setAvailableDeliveryTypes(Set.of(AvailableDeliveryType.COURIER));
        MultiCartResponse multiCartResponse = orderCreateHelper.multiCartActualize(parameters);

        assertNotNull(multiCartResponse);
        List<CartResponse> cartsRs = multiCartResponse.getCarts();
        assertEquals(1, cartsRs.size());
        assertEquals(parameters.getOrders().get(0).getLabel(), cartsRs.get(0).getLabel());

        Collection<OrderItem> itemsRq = parameters.getOrders().get(0).getItems();
        List<CartItemResponse> itemsRs = cartsRs.get(0).getItems();
        assertEquals(1, itemsRs.size());
        assertEquals(itemsRq.iterator().next().getLabel(), itemsRs.get(0).getLabel());
        assertNotNull(multiCartResponse.getCarts().get(0).getAvailableDeliveryTypes());
        assertEquals(1, multiCartResponse.getCarts().get(0).getAvailableDeliveryTypes().size());
        assertTrue(
                multiCartResponse.getCarts()
                        .get(0)
                        .getAvailableDeliveryTypes()
                        .contains(AvailableDeliveryType.COURIER)
        );
    }
}
