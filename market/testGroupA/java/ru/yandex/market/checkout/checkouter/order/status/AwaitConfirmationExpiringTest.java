package ru.yandex.market.checkout.checkouter.order.status;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.expiration.OrderExpirationService;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.order.OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD;

public class AwaitConfirmationExpiringTest extends AbstractWebTestBase {

    @Autowired
    OrderExpirationService orderExpirationService;

    @Test
    void shouldExpireFromAwaitConfirmationSubstatusWhenAntifraudFlag() {
        Order updatedOrder = createOrderInSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        setAsyncOutletAntifraudProperty(updatedOrder, true);

        jumpToFuture(3, ChronoUnit.HOURS);
        var expiredOrders = orderExpirationService.findOrdersForExpiration(0, 10);
        assertThat(expiredOrders, hasSize(1));
    }

    @Test
    void shouldExpireFromAwaitConfirmationSubstatusWhenOtherCases() {
        Order updatedOrder = createOrderInSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        setAsyncOutletAntifraudProperty(updatedOrder, false);

        jumpToFuture(3, ChronoUnit.HOURS);
        var expiredOrders = orderExpirationService.findOrdersForExpiration(0, 10);
        assertThat(expiredOrders, hasSize(1));
    }

    @Test
    void shouldExpireFromOtherSubstatuses() {
        Order updatedOrder = createOrderInSubstatus(OrderSubstatus.ASYNC_PROCESSING);
        setAsyncOutletAntifraudProperty(updatedOrder, true);

        jumpToFuture(3, ChronoUnit.HOURS);
        var expiredOrders = orderExpirationService.findOrdersForExpiration(0, 10);
        assertThat(expiredOrders, hasSize(1));
    }

    private Order createOrderInSubstatus(OrderSubstatus substatus) {
        Order blueOrder = OrderProvider.getBlueOrder();
        blueOrder.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        blueOrder.setDelivery(DeliveryProvider.shopSelfDelivery().build());
        long orderId = orderCreateService.createOrder(blueOrder, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, "666", blueOrder.getDelivery());
        return orderUpdateService.updateOrderStatus(
                orderId, OrderStatus.PENDING, substatus
        );
    }

    private void setAsyncOutletAntifraudProperty(Order updatedOrder, boolean value) {
        transactionTemplate.execute(ts -> masterJdbcTemplate.update(
                "insert into order_property(order_id, name, text_value) values(?, ?, ?)",
                updatedOrder.getId(),
                ASYNC_OUTLET_ANTIFRAUD.getName(),
                value
        ));
    }
}
