package ru.yandex.market.checkout.checkouter.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

import javax.annotation.Nonnull;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.balance.model.NotificationMode;
import ru.yandex.market.checkout.checkouter.balance.model.notifications.TrustRefundNotification;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.ClientHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildCheckBasketWithCancelledRefund;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildCheckBasketWithConfirmedRefund;


/**
 * Тест повторного рефанда после фейла.
 * <p>
 * 1. Создаем заказ в DELIVERED
 * 2. Создаем возврат заказа
 * 3. Прогоняем таску ReturnProcessorTask, чтобы создать рефанды
 * 4. Присылаем нотификацию со статусом failed по рефанду возврата
 * 5. Дергаем ручку
 */
public class RefundRetryTest extends AbstractReturnTestBase {

    @Autowired
    private RefundService refundService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private RefundHelper refundHelper;
    private Order order;

    @BeforeEach
    public void createOrder() {
        setFixedTime(Instant.now());
        Parameters params = defaultBlueOrderParameters();
        params.getOrder().getItems().forEach(item -> item.setCount(10));
        order = orderCreateHelper.createOrder(params);
    }

    private Refund getFailedRefund(Return ret) {
        Collection<Refund> refunds;
        refunds = refundService.getReturnRefunds(ret);
        Refund failedRefund = getFirstRefund(refunds);
        assertThat(failedRefund.getStatus(), equalTo(RefundStatus.FAILED));
        return failedRefund;
    }

    private Return createReturn(Order order) {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        Return returnResp = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        request = ReturnHelper.copy(returnResp);
        returnResp = client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, request);
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();

        Collection<Refund> refunds = refundHelper.proceedAsyncRefunds(refundService.getReturnRefunds(returnResp));
        Refund refund = getFirstRefund(refunds);
        trustMockConfigurer.mockCheckBasket(buildCheckBasketWithCancelledRefund(refund.getTrustRefundId(), refund
                .getAmount()));
        trustMockConfigurer.mockStatusBasket(buildCheckBasketWithCancelledRefund(refund.getTrustRefundId(), refund
                .getAmount()), null);
        refundService.notifyRefund(new TrustRefundNotification(
                NotificationMode.refund_result, refund.getTrustRefundId(), "error", false)
        );

        return returnResp;
    }

    @AfterEach
    public void tearDown() {
        clearFixed();
    }

    @Test
    @Epic(Epics.REFUND)
    @Story(Stories.REFUND_RETRY)
    @DisplayName("Повтор рефанда после возврата")
    public void refundRetryOnFailure() {
        Return returnResp = createReturn(order);
        Refund failedRefund = getFailedRefund(returnResp);
        Refund retryRefund = refundHelper.proceedAsyncRefund(
                client.refunds().retryRefund(order.getId(), failedRefund.getId(), ClientRole.SYSTEM, 123));
        Collection<Refund> refunds = refundService.getReturnRefunds(returnResp);
        assertThat(refunds, hasSize(2));
        trustMockConfigurer.mockStatusBasket(buildCheckBasketWithConfirmedRefund(retryRefund.getTrustRefundId(),
                retryRefund
                .getAmount()), null);
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();
        refunds = refundService.getReturnRefunds(returnResp);
        assertThat(refunds, hasSize(2));
        assertThat(refunds, hasItem(allOf(
                hasProperty("trustRefundId", equalTo(retryRefund.getTrustRefundId())),
                hasProperty("status", equalTo(RefundStatus.SUCCESS))
        )));
        assertThat(refunds, hasItem(allOf(
                hasProperty("trustRefundId", equalTo(failedRefund.getTrustRefundId())),
                hasProperty("status", equalTo(RefundStatus.FAILED)),
                hasProperty("retryId", notNullValue())
        )));
    }

    @Test
    @Epic(Epics.REFUND)
    @Story(Stories.REFUND_RETRY)
    @DisplayName("Восстановление зафейленного возврата после успешного ретрая рефанда")
    public void recoverReturnAfterRefundRetry() {
        Return returnResp = createReturn(order);
        Refund failedRefund = getFailedRefund(returnResp);
        setFixedTime(Instant.now().plus(8, ChronoUnit.DAYS));
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();
        Return failedReturn = client.returns().getReturn(returnResp.getId(),
                false, ClientRole.SYSTEM, 1);
        assertThat(failedReturn.getStatus(), equalTo(ReturnStatus.FAILED));
        Refund retryRefund = refundHelper.proceedAsyncRefund(
                client.refunds().retryRefund(order.getId(), failedRefund.getId(), ClientRole.SYSTEM, 123));
        failedReturn = client.returns().getReturn(returnResp.getId(), false, ClientRole.SYSTEM, 1);
        assertThat(failedReturn.getStatus(), equalTo(ReturnStatus.REFUND_IN_PROGRESS));
        Collection<Refund> refunds = refundService.getReturnRefunds(returnResp);
        assertThat(refunds, hasSize(2));
        trustMockConfigurer.mockStatusBasket(buildCheckBasketWithConfirmedRefund(retryRefund.getTrustRefundId(),
                retryRefund
                .getAmount()), null);
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();
        failedReturn = client.returns().getReturn(returnResp.getId(), false, ClientRole.SYSTEM, 1);
        assertThat(failedReturn.getStatus(), equalTo(ReturnStatus.REFUNDED));
    }

    @Test
    @Epic(Epics.REFUND)
    @Story(Stories.REFUND_RETRY)
    @DisplayName("Повтор рефанда после отмены заказа")
    public void refundRetryOnCancel() {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        Refund failedRefund = orderPayHelper.refund(order, null, false);
        trustMockConfigurer.mockCheckBasket(buildCheckBasketWithCancelledRefund(failedRefund.getTrustRefundId(),
                failedRefund.getAmount()));
        trustMockConfigurer.mockStatusBasket(buildCheckBasketWithCancelledRefund(failedRefund.getTrustRefundId(),
                failedRefund
                .getAmount()), null);
        orderPayHelper.notifyRefund(failedRefund, BasketStatus.error);
        failedRefund = refundService.getRefund(failedRefund.getId());
        assertThat(failedRefund.getReturnId(), nullValue());
        assertThat(failedRefund.getStatus(), equalTo(RefundStatus.FAILED));

        Refund retryRefund = refundHelper.proceedAsyncRefund(
                client.refunds().retryRefund(order.getId(), failedRefund.getId(), ClientRole.SYSTEM, 123));
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertThat(refunds, hasSize(2));
        assertThat(refunds, hasItem(allOf(
                hasProperty("trustRefundId", equalTo(retryRefund.getTrustRefundId())),
                hasProperty("status", equalTo(RefundStatus.ACCEPTED))
        )));
        assertThat(refunds, hasItem(allOf(
                hasProperty("trustRefundId", equalTo(failedRefund.getTrustRefundId())),
                hasProperty("status", equalTo(RefundStatus.FAILED)),
                hasProperty("retryId", notNullValue())
        )));
    }

    @Nonnull
    private Refund getFirstRefund(Collection<Refund> refunds) {
        return refunds.stream().findFirst().orElseThrow(() -> new RuntimeException("No refunds were created"));
    }
}
