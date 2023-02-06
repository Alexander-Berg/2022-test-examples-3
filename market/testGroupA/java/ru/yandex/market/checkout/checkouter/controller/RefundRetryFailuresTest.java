package ru.yandex.market.checkout.checkouter.controller;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.balance.model.NotificationMode;
import ru.yandex.market.checkout.checkouter.balance.model.notifications.TrustRefundNotification;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildCheckBasketWithCancelledRefund;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildCheckBasketWithConfirmedRefund;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

/**
 *
 */
public class RefundRetryFailuresTest extends AbstractReturnTestBase {

    private static final String PROMO_CODE = "ololo";
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;

    private Order order;

    @BeforeEach
    public void createOrder() {
        setFixedTime(Instant.now());
        Parameters params = defaultBlueOrderParameters(OrderProvider.getBlueOrder((o) -> {
            o.getItems().forEach(i -> i.setCount(10));
        }));

        params.setupPromo(PROMO_CODE);
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
    }

    @AfterEach
    public void tearDown() {
        clearFixed();
    }

    private Return createReturn(Order order, PaymentGoal paymentGoalToFail) {
        Return request = prepareReturnRequestOfOneItem(order, DeliveryType.PICKUP);
        Return returnResp = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        request = ReturnHelper.copy(returnResp);
        returnResp = client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, request);
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        Collection<Refund> refunds = refundService.getReturnRefunds(returnResp);

        refunds.forEach(r -> {
            Payment payment = paymentService.findPayment(r.getPaymentId(), ClientInfo.SYSTEM);
            finishRefund(r, payment.getType() == paymentGoalToFail);
        });
        return returnResp;
    }

    private void finishRefund(Refund r, boolean fail) {
        if (fail) {
            trustMockConfigurer.mockCheckBasket(buildCheckBasketWithCancelledRefund(r.getTrustRefundId(),
                    r.getAmount()));
            trustMockConfigurer.mockStatusBasket(buildCheckBasketWithCancelledRefund(r.getTrustRefundId(),
                    r.getAmount()), null);
        } else {
            trustMockConfigurer.mockCheckBasket(buildCheckBasketWithConfirmedRefund(r.getTrustRefundId(),
                    r.getAmount()));
            trustMockConfigurer.mockStatusBasket(buildCheckBasketWithConfirmedRefund(r.getTrustRefundId(),
                    r.getAmount()), null);
        }

        String status = fail ? "error" : "success";
        refundService.notifyRefund(
                new TrustRefundNotification(NotificationMode.refund_result, r.getTrustRefundId(), status, false)
        );
    }

    @Disabled // сломался при обновлении PG 10->12
    @Test
    @Epic(Epics.REFUND)
    @Story(Stories.REFUND_RETRY)
    @DisplayName("При повторе рефанда заказа с субсидией не рефандится субсидия")
    public void prepayRefundWithSubsidyRetryOnFailure() {
        Return returnResp = createReturn(order, PaymentGoal.ORDER_PREPAY);
        Map<RefundStatus, List<Refund>> returnStatusMap = getRefundStatusMap(returnResp);
        Refund failed = returnStatusMap.get(RefundStatus.FAILED).get(0);
        // Проверяем, что сфейлился обычный рефанд, а не субсидийный
        assertThat(
                paymentService.findPayment(failed.getPaymentId(), ClientInfo.SYSTEM).getType(),
                equalTo(PaymentGoal.ORDER_PREPAY)
        );
        Assertions.assertEquals(Long.valueOf(1), refundService.countFailedRefunds());
        client.refunds().retryRefund(order.getId(), failed.getId(), ClientRole.SYSTEM, 123);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        Collection<Refund> refunds = refundService.getReturnRefunds(returnResp);
        assertThat(refunds, hasSize(3));

        refunds = refundService.getReturnRefunds(returnResp);
        Refund retriedRefund = refunds.stream().filter(r -> r.getStatus() == RefundStatus.ACCEPTED).findAny()
                .orElseThrow(() -> new RuntimeException("No refund in status ACCEPTED"));
        assertThat(retriedRefund.getRetryId(), nullValue());
        assertThat(retriedRefund.getPaymentId(), equalTo(failed.getPaymentId()));

        Refund failedRefund = refunds.stream().filter(r -> r.getStatus() == RefundStatus.FAILED).findAny()
                .orElseThrow(() -> new RuntimeException("No refund in status FAILED"));
        assertThat(failedRefund.getRetryId(), equalTo(retriedRefund.getId()));

        refunds.stream().filter(r -> r.getStatus() == RefundStatus.SUCCESS).findAny()
                .orElseThrow(() -> new RuntimeException("No refund in status ACCEPTED"));
    }

    @Test
    @Epic(Epics.REFUND)
    @Story(Stories.REFUND_RETRY)
    @DisplayName("Повтор рефанда с субсидией")
    public void subsidyRefundRetryOnFailure() {
        Return returnResp = createReturn(order, PaymentGoal.SUBSIDY);
        Map<RefundStatus, List<Refund>> returnStatusMap = getRefundStatusMap(returnResp);
        Refund failed = returnStatusMap.get(RefundStatus.FAILED).get(0);
        // Проверяем, что сфейлился именно рефанд субсидии
        assertThat(
                paymentService.findPayment(failed.getPaymentId(), ClientInfo.SYSTEM).getType(),
                equalTo(PaymentGoal.SUBSIDY)
        );
        client.refunds().retryRefund(order.getId(), failed.getId(), ClientRole.SYSTEM, 123);
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        Collection<Refund> refunds = refundService.getReturnRefunds(returnResp);
        assertThat(refunds, hasSize(3));

        refunds = refundService.getReturnRefunds(returnResp);
        Refund retriedRefund = refunds.stream().filter(r -> r.getStatus() == RefundStatus.ACCEPTED).findAny()
                .orElseThrow(() -> new RuntimeException("No refund in status ACCEPTED"));
        assertThat(retriedRefund.getRetryId(), nullValue());
        assertThat(retriedRefund.getPaymentId(), equalTo(failed.getPaymentId()));

        Refund failedRefund = refunds.stream().filter(r -> r.getStatus() == RefundStatus.FAILED).findAny()
                .orElseThrow(() -> new RuntimeException("No refund in status FAILED"));
        assertThat(failedRefund.getRetryId(), equalTo(retriedRefund.getId()));

        refunds.stream().filter(r -> r.getStatus() == RefundStatus.SUCCESS).findAny()
                .orElseThrow(() -> new RuntimeException("No refund in status RETURNED"));
    }

    private Map<RefundStatus, List<Refund>> getRefundStatusMap(Return ret) {
        Collection<Refund> refunds;
        refunds = refundService.getReturnRefunds(ret);
        return refunds.stream()
                .collect(Collectors.groupingBy(Refund::getStatus));
    }
}
