package ru.yandex.market.checkout.checkouter.delivery;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;

public class DeliveryUpdateStatusValidatorTest {

    @Test
    public void shouldNotFailWhenFulfilmentIsNull() {
        final var order = OrderProvider.getBlueOrder();
        order.setFulfilment(null);

        final var statusValidator = new DeliveryUpdateStatusValidator<>(
                EnumSet.of(PROCESSING, DELIVERY, PICKUP), order, false);
        assertNotNull(statusValidator);
    }
}
