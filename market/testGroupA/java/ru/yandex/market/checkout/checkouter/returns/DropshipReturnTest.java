package ru.yandex.market.checkout.checkouter.returns;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

public class DropshipReturnTest extends AbstractReturnTestBase {

    private static final String PROMO_CODE = "PROMO-CODE";
    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReturnHelper returnHelper;

    private Order order;

    @BeforeEach
    public void init() {
        trustMockConfigurer.mockWholeTrust();
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
    }

    @Test
    public void returnPostPaidDropshipOrder() {
        order = dropshipDeliveryHelper.createDropshipPostpaidOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        Return ret = processReturnAndPayments(order, prepareDefaultReturnRequest(order, DeliveryType.DELIVERY));

        assertThat(ret, not(nullValue()));
        assertThat(ret.getStatus(), equalTo(ReturnStatus.REFUNDED));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void returnPartialPrePaidDropshipOrderWithSubsidy() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        increaseItemsCount(parameters, 10);
        parameters.setupPromo(PROMO_CODE);
        Order order = createOrderInDeliveredStatus(parameters);
        List<Payment> payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.ORDER_PREPAY, PaymentGoal.SUBSIDY);
        assertThat(payments, hasSize(2));

        Return retReq = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        retReq.getItems().forEach(i -> {
            i.setCount(5);
            i.setQuantity(BigDecimal.valueOf(5));
        });
        Return ret = processReturnAndPayments(order, retReq);

        assertThat(ret, not(nullValue()));
        assertThat(ret.getStatus(), equalTo(ReturnStatus.REFUNDED));
        Collection<Refund> refunds = refundService.getReturnRefunds(ret);
        assertThat(refunds, hasSize(payments.size()));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void returnPartialPostPaidDropshipOrderWithSubsidy() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPostpaidParameters();
        increaseItemsCount(parameters, 10);
        parameters.setupPostpaidPromo(PROMO_CODE);
        Order order = createOrderInDeliveredStatus(parameters);

        List<Payment> payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.ORDER_POSTPAY, PaymentGoal.SUBSIDY);
        assertThat(payments, hasSize(2));

        Return retReq = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        retReq.getItems().forEach(i -> {
            i.setCount(5);
            i.setQuantity(BigDecimal.valueOf(5));
        });
        Return ret = processReturnAndPayments(order, retReq);

        assertThat(ret, not(nullValue()));
        assertThat(ret.getStatus(), equalTo(ReturnStatus.REFUNDED));
        Collection<Refund> refunds = refundService.getReturnRefunds(ret);
        assertThat(refunds, hasSize(payments.size()));
    }

    @Nonnull
    private Return processReturnAndPayments(Order order, Return retReq) {
        trustMockConfigurer.mockWholeTrust();
        Return ret = returnHelper.initReturn(order.getId(), retReq);
        returnHelper.resumeReturn(order.getId(), ret.getId(), ret);
        // Create refunds
        returnService.processReturnPayments(order.getId(), ret.getId(), ClientInfo.SYSTEM);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        notifyRefundReceipts(ret);
        // Notify refunds
        returnService.processReturnPayments(order.getId(), ret.getId(), ClientInfo.SYSTEM);
        ret = getReturnById(ret.getId());
        return ret;
    }

    @Nonnull
    @SuppressWarnings("checkstyle:HiddenField")
    private Order createOrderInDeliveredStatus(Parameters parameters) {
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        if (order.getPaymentType() == PaymentType.PREPAID) {
            queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
        }
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.DELIVERED);
        if (order.getPaymentType() == PaymentType.POSTPAID) {
            queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);
            queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
        }
        return order;
    }

    private void increaseItemsCount(Parameters parameters, int count) {
        parameters.getOrder().getItems().stream()
                .findAny()
                .ifPresentOrElse(
                        item -> {
                            item.setCount(count);
                            item.setQuantity(BigDecimal.valueOf(count));
                        },
                        () -> {
                            throw new RuntimeException("Not found!");
                        }
                );
    }
}
