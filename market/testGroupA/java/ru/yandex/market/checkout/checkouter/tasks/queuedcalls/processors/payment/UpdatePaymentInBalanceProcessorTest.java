package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.payment;

import java.time.Instant;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.TrustPaymentOperations;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildHoldCheckBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;
import static ru.yandex.market.queuedcalls.retry.RetryStrategies.IN_15_MIN_PROGRESSIVELY_TO_1_DAY;

public class UpdatePaymentInBalanceProcessorTest extends AbstractWebTestBase {

    @Autowired
    protected PaymentService paymentService;
    @Autowired
    protected OrderCreateHelper orderCreateHelper;
    @Autowired
    protected OrderPayHelper paymentHelper;
    @Autowired
    protected TrustPaymentOperations trustPaymentOperations;
    @Autowired
    protected QueuedCallService queuedCallService;
    @Autowired
    protected UpdatePaymentInBalanceProcessor updatePaymentInBalanceProcessor;
    private Order order;
    private Payment payment;
    private PaymentService paymentServiceSpy;
    private TrustPaymentOperations trustPaymentOperationsSpy;

    @BeforeEach
    void setUp() {
        paymentServiceSpy = mock(PaymentService.class, delegatesTo(paymentService));
        trustPaymentOperationsSpy = spy(trustPaymentOperations);

        updatePaymentInBalanceProcessor.setPaymentService(paymentServiceSpy);
        updatePaymentInBalanceProcessor.setTrustPaymentOperations(trustPaymentOperationsSpy);

        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payment = paymentHelper.payForOrder(order);
        paymentHelper.notifyPayment(payment);

        checkouterProperties.setEnableUpdatePaymentMode(true);

        assertThat(updatePaymentInBalanceProcessor.maxAgeInDays()).isGreaterThan(0);
        assertThat(updatePaymentInBalanceProcessor.getDefaultRetryStrategy())
                .isEqualTo(IN_15_MIN_PROGRESSIVELY_TO_1_DAY);
    }

    @Test
    public void paymentStatusIsCleared() {
        paymentHelper.notifyPaymentClear(payment);
        var expectedColorSet = Set.of(BLUE);

        var result = updatePaymentInBalanceProcessor.process(
                new QueuedCallProcessor.QueuedCallExecution(
                        payment.getId(),
                        null,
                        0, Instant.now(),
                        order.getId())
        );

        assertThat(result).isEqualTo(ExecutionResult.SUCCESS);
        verify(trustPaymentOperationsSpy, never())
                .getStateInPaymentSystem(any(Payment.class), eq(expectedColorSet));
        verify(paymentServiceSpy, times(1)).updatePaymentInBalance(eq(payment.getId()));
    }

    @Test
    public void paymentStatusHoldButItIsClearedInTrust() {
        var expectedColorSet = Set.of(BLUE);

        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);

        var result = updatePaymentInBalanceProcessor.process(
                new QueuedCallProcessor.QueuedCallExecution(
                        payment.getId(),
                        null,
                        0, Instant.now(),
                        order.getId())
        );

        assertThat(result).isEqualTo(ExecutionResult.SUCCESS);
        verify(trustPaymentOperationsSpy, times(1))
                .getStateInPaymentSystem(any(Payment.class), eq(expectedColorSet));
        verify(paymentServiceSpy, times(1)).updatePaymentInBalance(eq(payment.getId()));
    }

    @Test
    public void finalPaymentStatusIsClearedButPaymentStatusIsHold() {
        var expectedColorSet = Set.of(BLUE);

        paymentService.updateFinalBalanceStatus(
                paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM),
                PaymentStatus.CLEARED
        );

        var result = updatePaymentInBalanceProcessor.process(
                new QueuedCallProcessor.QueuedCallExecution(
                        payment.getId(),
                        null,
                        0, Instant.now(),
                        order.getId())
        );

        assertThat(result).isEqualTo(ExecutionResult.SUCCESS);
        verify(trustPaymentOperationsSpy, never())
                .getStateInPaymentSystem(any(Payment.class), eq(expectedColorSet));
        verify(paymentServiceSpy, times(1)).updatePaymentInBalance(eq(payment.getId()));
    }

    @Test
    public void paymentStatusIsHoldDelayTest() {
        paymentHelper.notifyPayment(payment);
        var expectedColorSet = Set.of(BLUE);

        trustMockConfigurer.mockCheckBasket(buildHoldCheckBasket());
        trustMockConfigurer.mockStatusBasket(buildHoldCheckBasket(), null);

        var result = updatePaymentInBalanceProcessor.process(
                new QueuedCallProcessor.QueuedCallExecution(
                        payment.getId(),
                        null,
                        0, Instant.now(),
                        order.getId())
        );

        verifyErrorResult(result);
        verify(trustPaymentOperationsSpy, times(1))
                .getStateInPaymentSystem(any(Payment.class), eq(expectedColorSet));
        verify(paymentServiceSpy, never()).updatePaymentInBalance(eq(payment.getId()));
    }

    @Test
    public void paymentFinalStatusIsHoldDelayTest() {
        paymentHelper.notifyPayment(payment);
        var expectedColorSet = Set.of(BLUE);

        paymentService.updateFinalBalanceStatus(
                paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM),
                PaymentStatus.HOLD
        );

        var result = updatePaymentInBalanceProcessor.process(
                new QueuedCallProcessor.QueuedCallExecution(
                        payment.getId(),
                        null,
                        0, Instant.now(),
                        order.getId())
        );

        verifyErrorResult(result);
        verify(trustPaymentOperationsSpy, never())
                .getStateInPaymentSystem(any(Payment.class), eq(expectedColorSet));
        verify(paymentServiceSpy, never()).updatePaymentInBalance(eq(payment.getId()));
    }


    private void verifyErrorResult(ExecutionResult executionResult) {
        var softly = new SoftAssertions();

        softly.assertThat(executionResult).isNotNull();
        softly.assertThat(executionResult.getMessage()).isEqualTo("Payment must be cleared before updating in the " +
                "balance service");
        softly.assertThat(executionResult.isError()).isTrue();
        softly.assertThat(executionResult.getRetryStrategy()).isEqualTo(IN_15_MIN_PROGRESSIVELY_TO_1_DAY);

        softly.assertAll();
    }

    private void executeQueuedCallSynchronously(Long paymentId) {
        assertTrue(queuedCallService.existsQueuedCall(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, paymentId));
        queuedCallService.executeQueuedCallSynchronously(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, paymentId);
        assertFalse(queuedCallService.existsQueuedCall(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, paymentId));
    }

    private void executeQueuedCallSynchronously(Long paymentId, String exceptionMessage) {
        assertTrue(queuedCallService.existsQueuedCall(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, paymentId));
        // payment status is HOLD, task will be executed later
        var exception = Assertions.assertThrows(
                RuntimeException.class,
                () -> queuedCallService.executeQueuedCallSynchronously(
                        PAYMENT_CALL_BALANCE_UPDATE_PAYMENT,
                        paymentId
                )
        );
        assertThat(exception.getMessage()).isEqualTo(exceptionMessage);
    }
}
