package ru.yandex.market.checkout.checkouter.delivery.dropship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.checkouter.pay.PaymentType.POSTPAID;

public class DropshipPostPaidTest extends AbstractWebTestBase {

    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;
    @Autowired
    private QueuedCallService queuedCallService;

    @BeforeEach
    public void setUp() throws Exception {
        trustMockConfigurer.mockWholeTrust();
    }

    @Test
    public void testCashPayment() throws Exception {
        Parameters dropshipParams = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        dropshipParams.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(dropshipParams);
        assertThat(OrderTypeUtils.isFulfilment(order), is(false));
        assertThat(OrderTypeUtils.isMarketDelivery(order), is(true));
        assertThat(order.getPaymentType(), is(POSTPAID));

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertThat(order.getStatus(), is(OrderStatus.DELIVERED));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        Order freshOrder = orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertThat(freshOrder.getPayment(), notNullValue());
        assertThat(freshOrder.getPayment().getId(), notNullValue());
    }
}
