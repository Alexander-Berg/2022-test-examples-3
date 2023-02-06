package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.payment;

import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;

class TinkoffCancelPaymentProcessorTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;

    @Test
    public void shouldSkipTinkoffPaymentInHoldStatus() {
        var order = createOrder();
        var payment = order.getPayment();

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), equalTo(UNPAID));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildHoldCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildHoldCheckBasket(), null);
        orderPayHelper.notifyPayment(order.getPayment());
        transactionTemplate.execute(ts -> {
            IntStream.rangeClosed(1, 10).forEach(iteration ->
                    queuedCallService.addQueuedCallIfNotExist(CheckouterQCType.PAYMENT_CANCEL, payment.getId()));
            return null;
        });

        var queuedCalls = queuedCallService.existsQueuedCall(
                CheckouterQCType.PAYMENT_CANCEL,
                Set.of(payment.getId()));
        assertThat(queuedCalls, hasSize(1));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PAYMENT_CANCEL);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CANCEL, payment.getId()));
        assertThat(paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM).getStatus(),
                equalTo(PaymentStatus.HOLD));
    }

    private Order createOrder() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        var order = orderCreateHelper.createOrder(parameters);

        var payment = orderPayHelper.payForOrderWithoutNotification(order);
        orderPayHelper.notifyWaitingBankDecision(payment);

        return orderService.getOrder(order.getId());
    }
}
