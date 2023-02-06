package ru.yandex.market.checkout.checkouter.order.status;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderProperty;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

public class AwaitConfirmationNewAntifraudSubstatusTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;

    @BeforeEach
    public void setUp() {
        checkouterProperties.setAntifraudSubstatusEnabled(true);


    }

    @Test
    void shouldMoveOrderToPendingAntifraudIfAsyncAntifraudOutlet() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().addProperty(
                new OrderProperty(null, OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD.getName(), "true")
        );
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());

        MatcherAssert.assertThat(order.getStatus(), CoreMatchers.is(OrderStatus.PENDING));
        MatcherAssert.assertThat(order.getSubstatus(), CoreMatchers.is(OrderSubstatus.ANTIFRAUD));
    }

    @Test
    void shouldMoveOrderToPendingAntifraudIfAsyncAntifraudOutletAndPostpaid() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().addProperty(
                new OrderProperty(null, OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD.getName(), "true")
        );
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderService.getOrder(order.getId());

        MatcherAssert.assertThat(order.getStatus(), CoreMatchers.is(OrderStatus.PENDING));
        MatcherAssert.assertThat(order.getSubstatus(), CoreMatchers.is(OrderSubstatus.ANTIFRAUD));
    }

    @Test
    void shouldProceedToProcessingAfterPendingAntifraud() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().addProperty(
                new OrderProperty(null, OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD.getName(), "true")
        );
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());

        MatcherAssert.assertThat(order.getStatus(), CoreMatchers.is(OrderStatus.PENDING));
        MatcherAssert.assertThat(order.getSubstatus(), CoreMatchers.is(OrderSubstatus.ANTIFRAUD));

        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION);
        order = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
    }

    @Test
    void shouldProceedToAntifraudIfPreorder() {
        Order cart = OrderProvider.getBlueOrder();
        cart.getItems().forEach(oi -> oi.setPreorder(true));

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(cart);
        parameters.getOrder().addProperty(
                new OrderProperty(null, OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD.getName(), "true")
        );

        Order order = orderCreateHelper.createOrder(parameters);

        MatcherAssert.assertThat(order.getStatus(), CoreMatchers.is(OrderStatus.UNPAID));

        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());

        MatcherAssert.assertThat(order.getStatus(), CoreMatchers.is(OrderStatus.PENDING));
        MatcherAssert.assertThat(order.getSubstatus(), CoreMatchers.is(OrderSubstatus.ANTIFRAUD));

        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PENDING, OrderSubstatus.PREORDER);
    }

    @Test
    void shouldCancelPendingAntifraudOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().addProperty(
                new OrderProperty(null, OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD.getName(), "true")
        );

        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());

        MatcherAssert.assertThat(order.getSubstatus(), CoreMatchers.is(OrderSubstatus.ANTIFRAUD));

        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_FRAUD);
    }
}
