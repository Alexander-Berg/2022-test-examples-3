package ru.yandex.market.checkout.checkouter.pay;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.PaymentEditRequest;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BnplTestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;

public class BnplCancelTest extends AbstractWebTestBase {

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;

    @BeforeEach
    public void mockBnpl() {
        bnplMockConfigurer.mockWholeBnpl();
        checkouterProperties.setEnableBnpl(true);
    }

    @Test
    void orderCancelled() {
        //prepare
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        Payment virtualPayment = processVirtualPayment(order);
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        Payment bnplPayment = order.getPayment();
        assertTrue(virtualPayment.isCleared());
        assertTrue(bnplPayment.isHeld());

        //cancel order and run processHeldPaymentsTask
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        orderStatusHelper.processHeldPaymentsTask();

        //check
        virtualPayment = paymentService.getPayment(virtualPayment.getId(), ClientInfo.SYSTEM);
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        bnplPayment = order.getPayment();
        assertTrue(virtualPayment.isCleared(), "Виртуальный платеж был поклирен и поэтому остается в клире");
        assertTrue(bnplPayment.isCancelled(), "БНПЛ платеж отменяется полностью");

        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertEquals(1, refunds.size());
        assertEquals(virtualPayment.getId(), refunds.iterator().next().getPayment().getId(),
                "Создался единственный рефанд для поклиренного virtual");
        assertRefundStartRequestedOnce();

    }

    @Test
    void cancelExtraPayment() {
        //prepare
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        Payment bnplPayment = orderPayHelper.payForOrderWithoutNotification(order);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.CREATE_VIRTUAL_PAYMENT, order.getId()),
                "QC на создание виртуального платежа создается только при холде основного платежа");
        changePaymentMethodToYandex(order);
        Payment cardPayment = orderPayHelper.payForOrder(order);

        assertEquals(PaymentStatus.IN_PROGRESS,
                paymentService.getPayment(bnplPayment.getId(), ClientInfo.SYSTEM).getStatus(),
                "По Бнпл платежу не получили колбэк о холде");
        assertEquals(PaymentStatus.HOLD, paymentService.getPayment(cardPayment.getId(), ClientInfo.SYSTEM).getStatus(),
                "После смены способа оплаты, по карточному платежу холд прошел");
        assertEquals(OrderStatus.PROCESSING, orderService.getOrder(order.getId()).getStatus(),
                "Заказ с карточным платежом начали собирать");

        //do
        orderPayHelper.notifyBnplPayment(bnplPayment);

        //check
        assertEquals(PaymentStatus.CANCELLED,
                paymentService.getPayment(bnplPayment.getId(), ClientInfo.SYSTEM).getStatus(),
                "Отменили новый лишний платеж БНПЛ");
        List<Payment> virtualPayments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.VIRTUAL_BNPL);
        assertEquals(0, virtualPayments.size(), "Виртуальный платеж и не создавался");
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertEquals(0, refunds.size());
        assertRefundStartRequestedOnce();
    }

    @Test
    void multipleCancelledOrdersWithDisabledDivideItemsProperty() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                false);
        Parameters parameters = BnplTestProvider.defaultBnplParameters();
        parameters.addOrder(BnplTestProvider.defaultBnplParameters());
        var order1 = orderCreateHelper.createOrder(BnplTestProvider.defaultBnplParameters());
        var order2 = orderCreateHelper.createOrder(BnplTestProvider.defaultBnplParameters());
        var payment = orderPayHelper.payForOrders(List.of(order1, order2));

        processVirtualPayment(order1);
        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order1.getId()), OrderStatus.CANCELLED);
        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order2.getId()), OrderStatus.CANCELLED);

        orderStatusHelper.processHeldPaymentsTask();

        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order1.getId()));
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order2.getId()));

        assertEquals(PaymentStatus.CANCELLED,
                paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM).getStatus());
        assertRefundStartRequestedOnce();
    }

    private void assertRefundStartRequestedOnce() {
        List<String> requests = bnplMockConfigurer.findEventsByStubName(BnplMockConfigurer.POST_ORDER_REFUND_START)
                .stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .collect(Collectors.toList());
        assertEquals(1, requests.size(), "Вызвался рефанд платежа в БНПЛ");
    }

    private void changePaymentMethodToYandex(Order order) {
        OrderEditRequest editRequest = new OrderEditRequest();
        PaymentEditRequest paymentEdit = new PaymentEditRequest();
        paymentEdit.setPaymentMethod(PaymentMethod.YANDEX);
        editRequest.setPaymentEditRequest(paymentEdit);
        client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID, singletonList(BLUE), editRequest);
    }

    private Payment processVirtualPayment(Order order) {
        order = orderService.getOrder(order.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.CREATE_VIRTUAL_PAYMENT, order.getPaymentId());
        Payment virtual = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.VIRTUAL_BNPL).get(0);
        orderPayHelper.notifyPaymentClear(virtual);
        return paymentService.getPayment(virtual.getId(), ClientInfo.SYSTEM);
    }

}
