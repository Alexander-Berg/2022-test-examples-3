package ru.yandex.market.checkout.checkouter.pay;


import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector.InspectExpiredPaymentVirtualBnplTaskV2;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BnplTestProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

public class BnplVirtualPaymentTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private InspectExpiredPaymentVirtualBnplTaskV2 inspectExpiredPaymentVirtualBnplTaskV2;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
    }

    @Test
    void virtualBnplPaymentExpired() {
        var parameters = BnplTestProvider.defaultBnplParameters();
        var order = orderCreateHelper.createOrder(parameters);
        var payment = orderPayHelper.payForOrder(order);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.CREATE_VIRTUAL_PAYMENT, payment.getId());
        var virtual = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.VIRTUAL_BNPL).get(0);
        //authorized notify. Not cleared yet
        orderPayHelper.notifyPayment(virtual);
        Collection<Payment> expiredPayments = paymentService.loadPaymentsByGoal(PaymentGoal.VIRTUAL_BNPL,
                new PaymentStatus[]{PaymentStatus.HOLD}, 90, null, null);
        assertThat(expiredPayments, hasSize(1));
        virtual = expiredPayments.iterator().next();
        assertThat(virtual.getStatus(), is(PaymentStatus.HOLD));

        //В трасте виртуальный платеж поклирился. У нас все еще HOLD.
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        //Запускается paymentStatusInspector
        var result = inspectExpiredPaymentVirtualBnplTaskV2.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        virtual = paymentService.getPayment(virtual.getId(), ClientInfo.SYSTEM);
        assertThat(virtual.getStatus(), is(PaymentStatus.CLEARED));
    }

    @Test
    void skipVirtualBnplPaymentCreationAfterCancellation() {
        var parameters = BnplTestProvider.defaultBnplParameters();
        var order = orderCreateHelper.createOrder(parameters);
        var payment = orderPayHelper.payForOrder(order);
        orderPayHelper.updatePaymentStatus(payment.getId(), PaymentStatus.CANCELLED);
        payment = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.CREATE_VIRTUAL_PAYMENT, payment.getId());
        //Платеж не создался, так как основной уже отменен.
        var virtual = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.VIRTUAL_BNPL);
       assertTrue(virtual.isEmpty());
    }
}
