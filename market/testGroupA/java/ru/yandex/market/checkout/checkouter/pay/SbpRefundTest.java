package ru.yandex.market.checkout.checkouter.pay;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.CashbackTestProvider;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_REFUND_STUB;


public class SbpRefundTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private RefundService refundService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @BeforeEach
    public void prepare() {
        checkouterProperties.setEnableSbpPayment(true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_SBP_MARKUP_REPLACEMENT, true);
    }

    @Test
    void compositeRefundMarkup() {
        //prepare
        Parameters parameters = CashbackTestProvider.defaultCashbackParameters();
        parameters.setPaymentMethod(PaymentMethod.SBP);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.setShowSbp(true);
        var order = orderCreateHelper.createOrder(parameters);
        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());

        //do
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());
        Refund refund = refundService.getRefunds(payment).iterator().next();
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.PROCESS_REFUND, refund.getId());

        //check
        List<ServeEvent> createRefund = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_REFUND_STUB))
                .collect(Collectors.toList());
        assertThat(createRefund, hasSize(1));
        ServeEvent createRefundEvent = Iterables.getOnlyElement(createRefund);
        JsonTest.checkJsonMatcher(createRefundEvent.getRequest().getBodyAsString(), "$.paymethod_markup.*.sbp_qr",
                hasSize(2));
    }

    @Test
    public void testClearedEventToCancelledOrder() throws Exception {
        //prepare
        trustMockConfigurer.mockWholeTrust();
        stockStorageConfigurer.mockOkForUnfreeze();
        stockStorageConfigurer.mockOkForForceUnfreeze();

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.SBP);
        parameters.setShowSbp(true);

        var order1 = orderCreateHelper.createOrder(parameters);
        var order2 = orderCreateHelper.createOrder(parameters);
        var order3 = orderCreateHelper.createOrder(parameters);

        //do
        Payment payment = orderPayHelper.payForOrdersWithoutNotification(List.of(order1, order2, order3));

        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order1);
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order3);

        orderPayHelper.notifyPaymentClear(payment);

        // check
        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());
        order3 = orderService.getOrder(order3.getId());

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        Assertions.assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        Assertions.assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order1.getId()));
        Assertions.assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order2.getId()));
        Assertions.assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order3.getId()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testSecondClearedEventToCancelledOrder(boolean isSecondPaymentNotifiesFirst) throws Exception {
        //prepare
        trustMockConfigurer.mockWholeTrust();
        stockStorageConfigurer.mockOkForUnfreeze();
        stockStorageConfigurer.mockOkForForceUnfreeze();

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.SBP);
        parameters.setShowSbp(true);

        var order1 = orderCreateHelper.createOrder(parameters);
        var order2 = orderCreateHelper.createOrder(parameters);
        var order3 = orderCreateHelper.createOrder(parameters);

        //do
        Payment payment1 = orderPayHelper.payForOrdersWithoutNotification(List.of(order1, order2, order3));
        Payment payment2 = orderPayHelper.payForOrdersWithoutNotification(List.of(order1, order2, order3));

        if (isSecondPaymentNotifiesFirst) {
            var temp = payment1;
            payment1 = payment2;
            payment2 = temp;
        }

        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order1);
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order3);

        orderPayHelper.notifyPaymentClear(payment1);

        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());
        order3 = orderService.getOrder(order3.getId());

        Assertions.assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order1.getId()));
        Assertions.assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order2.getId()));
        Assertions.assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order3.getId()));

        Assertions.assertEquals(OrderStatus.CANCELLED, order1.getStatus());
        Assertions.assertEquals(OrderStatus.PROCESSING, order2.getStatus());
        Assertions.assertEquals(OrderStatus.CANCELLED, order3.getStatus());

        orderPayHelper.notifyPaymentClear(payment2);

        // check
        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());
        order3 = orderService.getOrder(order3.getId());

        payment1 = paymentService.getPayment(payment1.getId(), ClientInfo.SYSTEM);
        Assertions.assertEquals(PaymentStatus.CLEARED, payment1.getStatus());

        Assertions.assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order1.getId()));
        Assertions.assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order2.getId()));
        Assertions.assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order3.getId()));

        Assertions.assertEquals(OrderStatus.CANCELLED, order1.getStatus());
        Assertions.assertEquals(OrderStatus.PROCESSING, order2.getStatus());
        Assertions.assertEquals(OrderStatus.CANCELLED, order3.getStatus());

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);

        Collection<Refund> refunds1 = refundService.getRefunds(payment1);
        Collection<Refund> refunds2 = refundService.getRefunds(payment2);

        Assertions.assertEquals(2, refunds1.size());
        Assertions.assertEquals(3, refunds2.size());
    }
}
