package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.refund;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class RefundAsyncProcessorTest extends AbstractWebTestBase {

    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private RefundService refundService;

    @Test
    public void refundFakeOrder() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setSandbox(true);
        var order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order.getId()), OrderStatus.CANCELLED);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());

        var refunds = refundService.getRefunds(order.getId());
        assertThat(refunds.size()).isEqualTo(1);
        var refund = refunds.stream().findFirst().orElseThrow();

        assertThat(queuedCallService.existsQueuedCall(CheckouterQCType.PROCESS_REFUND, refund.getId())).isTrue();
        var servedEventsBeforeFakeRefund = trustMockConfigurer.servedEvents();
        refundHelper.proceedAsyncRefunds(order.getId());
        assertThat(queuedCallService.existsQueuedCall(CheckouterQCType.PROCESS_REFUND, order.getId())).isFalse();
        assertThat(trustMockConfigurer.servedEvents().size()).isEqualTo(servedEventsBeforeFakeRefund.size());
    }

    @Test
    public void refundWithTrustException() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        var order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order.getId()), OrderStatus.CANCELLED);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());

        var refunds = refundService.getRefunds(order.getId());
        assertThat(refunds.size()).isEqualTo(1);
        var refund = refunds.stream().findFirst().orElseThrow();

        assertThat(queuedCallService.existsQueuedCall(CheckouterQCType.PROCESS_REFUND, refund.getId())).isTrue();
        trustMockConfigurer.mockBadRequestRefund();

        Assertions.assertThrows(RuntimeException.class, () -> {
            refundHelper.proceedAsyncRefunds(order.getId());
        });

        assertThat(queuedCallService.existsQueuedCall(CheckouterQCType.PROCESS_REFUND, order.getId())).isTrue();
        var qc = queuedCallService.findQueuedCalls(CheckouterQCType.PROCESS_REFUND, refund.getId())
                .iterator().next();
        assertThat(qc.getLastTryErrorMessage()).startsWith("TrustException");
    }

    @Test
    public void tryToRefundNonExistedRefund() {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(true);

        var fakeRefundId = System.currentTimeMillis();
        transactionTemplate.execute(ts -> {
            queuedCallService.addQueuedCall(CheckouterQCType.PROCESS_REFUND, fakeRefundId);
            return null;
        });
        assertThatThrownBy(() ->
                queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.PROCESS_REFUND, fakeRefundId))
                .hasMessageContaining("Refund (id = " + fakeRefundId + ") is null! " +
                        "Refund processing will be interrupted!");
    }
}
