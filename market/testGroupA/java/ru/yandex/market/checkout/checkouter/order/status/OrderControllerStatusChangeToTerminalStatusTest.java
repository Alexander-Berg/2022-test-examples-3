package ru.yandex.market.checkout.checkouter.order.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class OrderControllerStatusChangeToTerminalStatusTest extends AbstractWebTestBase {

    @Test
    @DisplayName("Можно перевести статус заказа из DELIVERY/DELIVERY_TO_STORE_STARTED " +
            "в DELIVERED")
    public void shouldAllowChangeStatusToDelivered() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        order = orderUpdateService.updateOrderStatus(
                order.getId(),
                StatusAndSubstatus.of(OrderStatus.DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED),
                ClientInfo.SYSTEM);
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_TO_STORE_STARTED));

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertThat(order.getStatus(), is(OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Можно перевести статус заказа из DELIVERY/DELIVERY_TO_STORE_STARTED " +
            "в CANCELLED")
    public void shouldAllowChangeStatusToCancelled() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        order = orderUpdateService.updateOrderStatus(
                order.getId(),
                StatusAndSubstatus.of(OrderStatus.DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED),
                ClientInfo.SYSTEM);
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_TO_STORE_STARTED));

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        assertThat(order.getStatus(), is(OrderStatus.CANCELLED));
    }
}
