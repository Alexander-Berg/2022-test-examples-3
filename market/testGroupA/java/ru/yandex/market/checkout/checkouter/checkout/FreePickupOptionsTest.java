package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters;

public class FreePickupOptionsTest extends AbstractWebTestBase {

    @BeforeEach
    public void setUpTests() {
        checkouterProperties.setEnableFreePickupForDbs(Boolean.TRUE);
    }

    @Test
    public void freePickupCartTest() throws Exception {
        Parameters parameters = defaultBlueNonFulfilmentOrderParameters();
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        parameters.setFreeDelivery(Boolean.TRUE);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertEquals(1, multiCart.getCarts().size());
        Order order = multiCart.getCarts().get(0);

        List<? extends Delivery> deliveries = order.getDeliveryOptions().stream()
                .filter(it -> it.getType() == DeliveryType.PICKUP)
                .filter(Delivery::isFree)
                .collect(Collectors.toList());

        assertEquals(3, deliveries.size());

        assertTrue(deliveries.stream().allMatch(it -> BigDecimal.ZERO.compareTo(it.getPrice()) == 0));

        parameters.setDeliveryServiceId(deliveries.get(0).getDeliveryServiceId());
        MultiOrder checkout = orderCreateHelper.checkout(multiCart, parameters);

        Delivery delivery = checkout.getOrders().get(0).getDelivery();
        assertEquals(0, BigDecimal.ZERO.compareTo(delivery.getPrice()));
    }

}
