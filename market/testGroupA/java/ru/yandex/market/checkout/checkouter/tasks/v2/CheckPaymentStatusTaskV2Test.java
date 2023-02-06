package ru.yandex.market.checkout.checkouter.tasks.v2;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.checkouter.b2b.NotifyBillPaidRequest;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.storage.OrderEntityGroup;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.CheckPaymentStatusPartitionTaskV2Factory;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.CREATE_VIRTUAL_PAYMENT;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

public class CheckPaymentStatusTaskV2Test extends AbstractPaymentTestBase {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CheckPaymentStatusPartitionTaskV2Factory checkPaymentStatusTaskV2;
    @Autowired
    private Storage storage;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OrderHistoryEventsTestHelper historyEventsTestHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @Test
    public void shouldNotUpdatePaymentStatusIfNotConfirmed() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        orderUpdateService.updateOrderStatus(order().getId(), OrderStatus.DELIVERY);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        List<Payment> payments = paymentService.getPayments(
                order().getId(), ClientInfo.SYSTEM, PaymentGoal.ORDER_PREPAY);
        Payment payment = Iterables.getOnlyElement(payments);

        Assertions.assertEquals(payment.getStatus(), PaymentStatus.CLEARED);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(null);
        trustMockConfigurer.mockStatusBasket(null, null);

        makePaymentOlder(payment);

        checkPaymentStatusTaskV2.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        Payment payment2 = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);

        Assertions.assertNull(payment2.getFinalBalanceStatus());
    }

    @Test
    public void shouldNotUpdatePaymentWithGoalOrderAccountPayment() throws Exception {
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));
        createUnpaidB2bOrder();

        client.payments().generatePaymentInvoice(order().getId());
        client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(order().getId())));

        orderStatusHelper.proceedOrderToStatus(order(), OrderStatus.DELIVERY);

        List<Payment> payments = paymentService.getPayments(
                order().getId(), ClientInfo.SYSTEM, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        Assertions.assertEquals(1, payments.size());

        Payment payment = Iterables.getOnlyElement(payments);
        Assertions.assertEquals(payment.getStatus(), PaymentStatus.CLEARED);

        makePaymentOlder(payment);

        checkPaymentStatusTaskV2.getTasks().forEach((key, task) -> {
            Long countItemsToProcess = task.countItemsToProcess();
            Collection<?> batch = task.prepareBatch();
            Assertions.assertEquals(0, countItemsToProcess);
            Assertions.assertEquals(0, batch.size());
        });
    }

    @Test
    public void shouldUpdatePaymentStatusIfConfirmed() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        orderUpdateService.updateOrderStatus(order().getId(), OrderStatus.DELIVERY);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        List<Payment> payments = paymentService.getPayments(
                order().getId(), ClientInfo.SYSTEM, PaymentGoal.ORDER_PREPAY);
        Payment payment = Iterables.getOnlyElement(payments);

        Assertions.assertEquals(payment.getStatus(), PaymentStatus.CLEARED);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);

        makePaymentOlder(payment);

        checkPaymentStatusTaskV2.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        Payment payment2 = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);

        Assertions.assertEquals(PaymentStatus.CLEARED, payment2.getFinalBalanceStatus());

        var events = historyEventsTestHelper.getEventsOfType(order().getId(), HistoryEventType.PAYMENT_CLEARED);
        assertThat(events, IsCollectionWithSize.hasSize(1));
    }

    @Test
    @DisplayName("Проставить id поклиренного виртуального платежа в order_history")
    public void souldSetPaymentIdFromClearedPayment() throws IOException {
        //prepare
        bnplMockConfigurer.mockWholeBnpl();
        checkouterProperties.setEnableBnpl(true);
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        Payment basePayment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));
        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());

        Payment virtualBnplPayment = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.VIRTUAL_BNPL).get(0);
        orderPayHelper.notifyPaymentClear(virtualBnplPayment);
        assertThat(paymentService.getPayment(virtualBnplPayment.getId(), ClientInfo.SYSTEM).getStatus(),
                is(PaymentStatus.CLEARED));

        //do
        makePaymentOlder(virtualBnplPayment, order.getId());
        checkPaymentStatusTaskV2.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        //check
        var events = historyEventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.PAYMENT_CLEARED);
        assertThat(events, IsCollectionWithSize.hasSize(1));
        Long paymentId = jdbcTemplate.queryForObject("select payment_id from ORDER_HISTORY where order_id = ? and " +
                "event_type=?", Long.class, order.getId(), HistoryEventType.PAYMENT_CLEARED.getId());
        assertThat(paymentId, is(virtualBnplPayment.getId()));
    }

    @Test
    public void shouldUpdatePaymentStatusRelatedToMultiOrderIfConfirmed() throws Exception {
        Order blueOrder = orderCreateHelper.createOrder(defaultBlueOrderParameters());
        Order whiteOrder = orderCreateHelper.createOrder(WhiteParametersProvider.defaultWhiteParameters());
        var orders = Arrays.asList(blueOrder, whiteOrder);
        assertThat(orders.stream().map(Order::getRgb).collect(Collectors.toSet()), hasSize(2));
        Payment payment = orderPayHelper.payForOrders(orders);

        orders.forEach(order -> orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERY));

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);

        Assertions.assertEquals(payment.getStatus(), PaymentStatus.CLEARED);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);

        makePaymentOlder(payment, blueOrder.getId());

        checkPaymentStatusTaskV2.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        Payment payment2 = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);

        Assertions.assertEquals(PaymentStatus.CLEARED, payment2.getFinalBalanceStatus());

        var events = historyEventsTestHelper.getEventsOfType(blueOrder.getId(), HistoryEventType.PAYMENT_CLEARED);
        assertThat(events, IsCollectionWithSize.hasSize(1));
        events = historyEventsTestHelper.getEventsOfType(whiteOrder.getId(), HistoryEventType.PAYMENT_CLEARED);
        assertThat(events, IsCollectionWithSize.hasSize(1));
    }

    private void makePaymentOlder(Payment payment) {
        makePaymentOlder(payment, order().getId());
    }

    private void makePaymentOlder(Payment payment, Long orderId) {
        storage.updateEntityGroup(new OrderEntityGroup(orderId), (() -> {
            jdbcTemplate.update("update PAYMENT set " +
                    "updated_at = updated_at - interval '2' hour " +
                    "where id = ?", payment.getId());
            return null;
        }));
    }
}
