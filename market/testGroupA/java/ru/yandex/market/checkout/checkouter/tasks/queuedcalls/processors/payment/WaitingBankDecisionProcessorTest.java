package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.payment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

class WaitingBankDecisionProcessorTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;

    @BeforeEach
    public void setUp() {
        setFixedTime(Instant.now(getClock()));
    }

    @Test
    public void qcCallMustUpdatePaymentStatusTest() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        var order = orderCreateHelper.createOrder(parameters);

        var payment = orderPayHelper.payForOrderWithoutNotification(order);
        orderPayHelper.notifyWaitingBankDecision(payment);

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), equalTo(UNPAID));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        var checkPaymentStatusCount = getCheckStatusInvocationCount(payment);

        checkQcCall(payment.getId());
        setFixedTime(Instant.now(getClock()).plus(1, ChronoUnit.DAYS));
        queuedCallService.executeQueuedCallBatch(CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS);
        // payment still in WAITING_BANK_DECISION status
        assertTrue(queuedCallService.existsQueuedCall(CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS, payment.getId()));

        //set payment status to cleared in trust
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);

        checkQcCall(payment.getId());
        setFixedTime(Instant.now(getClock()).plus(1, ChronoUnit.DAYS));
        queuedCallService.executeQueuedCallBatch(CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS);
        assertFalse(queuedCallService.existsQueuedCall(CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS, payment.getId()));

        order = orderService.getOrder(order.getId());
        assertThat(order.getSubstatus(), not(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.CLEARED));
        assertThat(getCheckStatusInvocationCount(payment),
                equalTo(checkPaymentStatusCount
                        + 1 /* qc call failed: payment still in WAITING_BANK_DECISION status */
                        + 1 /* qc call succeed: payment status changed*/
                )
        );
    }

    @Test
    public void paymentStatusIsClearedNoUpdatesTest() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        var order = orderCreateHelper.createOrder(parameters);

        var payment = orderPayHelper.payForOrderWithoutNotification(order);
        orderPayHelper.notifyWaitingBankDecision(payment);

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), equalTo(UNPAID));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        orderPayHelper.notifyPaymentClear(payment);

        var checkPaymentStatusCount = getCheckStatusInvocationCount(payment);
        // A few moments later.. after 1 day
        setFixedTime(Instant.now(getClock()).plus(1, ChronoUnit.DAYS));
        assertTrue(queuedCallService.existsQueuedCall(CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS, payment.getId()));
        queuedCallService.executeQueuedCallBatch(CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS);
        assertFalse(queuedCallService.existsQueuedCall(CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS, payment.getId()));

        order = orderService.getOrder(order.getId());
        assertThat(order.getSubstatus(), not(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.CLEARED));
        assertThat(getCheckStatusInvocationCount(payment), equalTo(checkPaymentStatusCount));
    }

    private long getCheckStatusInvocationCount(Payment payment) {
        return trustMockConfigurer.trustMock().getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .filter(loggedRequest -> RequestMethod.GET.equals(loggedRequest.getMethod()))
                .map(LoggedRequest::getUrl)
                .filter(url -> url.contains("payment_status/" + payment.getBasketKey().getPurchaseToken() + "?"))
                .count();
    }

    private void checkQcCall(long objectId) {
        var qcCalls = queuedCallService.findQueuedCalls(CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS,
                objectId);
        assertThat(qcCalls.size(), equalTo(1));
        var qcCall = qcCalls.iterator().next();
        assertThat(qcCall.getNextTryAt(), equalTo(Instant.now(getClock()).plus(1, ChronoUnit.DAYS)));
    }
}
