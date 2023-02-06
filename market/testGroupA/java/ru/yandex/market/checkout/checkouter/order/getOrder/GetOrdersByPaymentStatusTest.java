package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByPaymentStatusTest extends AbstractWebTestBase {

    private Order heldOrder;
    private Order clearedOrder;

    @BeforeAll
    public void setUp() {
        super.setUpBase();
        Parameters heldOrderParams = new Parameters();
        heldOrderParams.setPaymentMethod(PaymentMethod.YANDEX);

        heldOrder = orderCreateHelper.createOrder(heldOrderParams);
        orderStatusHelper.proceedOrderToStatus(heldOrder, OrderStatus.PROCESSING);

        clearedOrder = orderCreateHelper.createOrder(heldOrderParams);
        orderStatusHelper.proceedOrderToStatus(clearedOrder, OrderStatus.DELIVERY);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @Test
    public void shouldFilterByPaymentStatus() {
        OrderSearchRequest request = new OrderSearchRequest();
        request.paymentStatus = EnumSet.of(PaymentStatus.HOLD);

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        PagedOrders heldOrders = orderService.getOrders(request, ClientInfo.SYSTEM);
        List<Long> orderIds = heldOrders.getItems().stream().map(Order::getId).collect(Collectors.toList());
        assertThat(
                orderIds,
                CoreMatchers.hasItem(heldOrder.getId())
        );
        assertThat(
                orderIds,
                CoreMatchers.not(CoreMatchers.hasItem(clearedOrder.getId()))
        );
    }

    @Test
    public void shouldFilterByPaymentStatusCleared() {
        OrderSearchRequest request = new OrderSearchRequest();
        request.paymentStatus = EnumSet.of(PaymentStatus.CLEARED);

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        PagedOrders heldOrders = orderService.getOrders(request, ClientInfo.SYSTEM);
        List<Long> orderIds = heldOrders.getItems().stream().map(Order::getId).collect(Collectors.toList());
        assertThat(
                orderIds,
                CoreMatchers.hasItem(clearedOrder.getId())
        );
        assertThat(
                orderIds,
                CoreMatchers.not(CoreMatchers.hasItem(heldOrder.getId()))
        );
    }
}
