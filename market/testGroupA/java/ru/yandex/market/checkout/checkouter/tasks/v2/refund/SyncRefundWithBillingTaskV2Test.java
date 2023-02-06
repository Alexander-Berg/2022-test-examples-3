package ru.yandex.market.checkout.checkouter.tasks.v2.refund;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.refund.ItemsRefundStrategy;
import ru.yandex.market.checkout.checkouter.refund.AbstractRefundTestBase;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskResult;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.SyncRefundWithBillingPartitionTaskV2Factory;
import ru.yandex.market.checkout.checkouter.viewmodel.CreatePaymentResponse;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildWithBasketKeyConfig;

public class SyncRefundWithBillingTaskV2Test extends AbstractRefundTestBase {

    @Autowired
    private InspectExpiredRefundTaskV2 inspectExpiredRefundTaskV2;
    @Autowired
    private SyncRefundWithBillingPartitionTaskV2Factory syncRefundWithBillingPartitionTaskV2Factory;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ItemsRefundStrategy itemsRefundStrategy;

    @Test
    public void syncRefundWithBilling() throws Exception {
        var refund = prepareRefund();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.HOURS));

        var expiredRefunds = refundService.getRefundsWithExpiredStatus(LocalDateTime.now(getClock()));
        assertEquals(1, expiredRefunds.size());
        var refundWithExpiredStatus = expiredRefunds.iterator().next();
        Assertions.assertEquals(refund.getId(), refundWithExpiredStatus.getId());

        TaskResult result;
        trustMockConfigurer.mockCheckBasket(buildPostAuth(), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("First check passed");
        });
        trustMockConfigurer.mockCheckBasket(buildWithBasketKeyConfig(null), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs("First check passed");
        });
        if (refundHelper.isAsyncRefundStrategyEnabled(refund)) {
            for (Refund expiredRefund : refundHelper.proceedAsyncRefunds(expiredRefunds)) {
                refundHelper.processToSuccess(expiredRefund, order);
            }
        } else {
            result = inspectExpiredRefundTaskV2.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());
        }

        setFixedTime(getClock().instant().plus(1, ChronoUnit.HOURS));

        syncRefundWithBillingPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        refund = refundService.getRefund(refund.getId());
        Assertions.assertNotNull(refund.getFinalBalanceStatus());
    }

    @Test
    public void shouldFilterOutRefundsWithBnplPayments() throws Exception {
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();

        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order1 = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order1);
        assertTrue(paymentResponse.getBnpl());
        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        assertEquals(payment.getType(), PaymentGoal.BNPL);
        order1 = orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order1.getId()), OrderStatus.DELIVERY);

        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.CREATE_VIRTUAL_PAYMENT,
                order1.getPaymentId());
        Payment virtualBnplPayment = paymentService.getPayments(order1.getId(), ClientInfo.SYSTEM,
                PaymentGoal.VIRTUAL_BNPL).get(0);
        orderPayHelper.notifyPaymentCancel(virtualBnplPayment);

        Order order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order2 = orderStatusHelper.proceedOrderToStatus(order2, OrderStatus.DELIVERY);
        order2 = orderService.getOrder(order2.getId());
        assertNotEquals(order2.getPayment().getType(), PaymentGoal.BNPL);

        Refund refund1 = prepareRefund(order1);
        Refund refund2 = prepareRefund(order2);

        trustMockConfigurer.mockCheckBasket(buildPostAuth(), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("First check passed");
        });
        trustMockConfigurer.mockCheckBasket(buildWithBasketKeyConfig(null), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs("First check passed");
        });

        refund1 = refundHelper.proceedAsyncRefund(refund1);
        refund2 = refundHelper.proceedAsyncRefund(refund2);
        refundHelper.processToSuccess(refund1, order1);
        refundHelper.processToSuccess(refund2, order2);

        setFixedTime(getClock().instant().plus(2, ChronoUnit.HOURS));
        Method prepareBatch = SyncRefundWithBillingTaskV2.class.getDeclaredMethod("prepareBatch");
        Collection<Refund> refunds = new LinkedList<>();
        syncRefundWithBillingPartitionTaskV2Factory.getTasks().forEach((k, task) -> {
            try {
                refunds.addAll((Collection<Refund>) prepareBatch.invoke(task));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException();
            }
        });
        List<Long> foundRefunds = refunds.stream().map(Refund::getId).collect(Collectors.toList());
        assertThat(foundRefunds, hasSize(1));
        assertThat(foundRefunds, hasItem(refund2.getId()));
    }
}
