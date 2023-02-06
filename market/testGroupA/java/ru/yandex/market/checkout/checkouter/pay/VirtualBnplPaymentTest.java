package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.checkout.multiware.CommonPaymentServiceTest;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.parcel.CancellationRequestStatus;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelNotFoundException;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.calculators.PaymentPartitionCalculator;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ParcelHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.MemCachedAgentMockFactory;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.CREATE_VIRTUAL_PAYMENT;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BnplTestProvider.CASHBACK_AMOUNT;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplOrderParametersWithCashBackSpent;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildUnholdWithTimestamps;

public class VirtualBnplPaymentTest extends AbstractWebTestBase {

    public static final List<String> SHOW_INFO_LIST = List.of(
            OrderItemProvider.SHOW_INFO,
            OrderItemProvider.ANOTHER_SHOW_INFO,
            OrderItemProvider.OTHER_SHOW_INFO
    );
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private ParcelHelper parcelHelper;
    @Autowired
    private RefundService refundService;
    @Autowired
    private OrderHistoryEventsTestHelper historyEventsTestHelper;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private MemCachedAgentMockFactory memCachedAgentMockFactory;
    @Autowired
    private MemCachedAgent memCachedAgent;

    @BeforeEach
    public void mockBnpl() {
        bnplMockConfigurer.mockWholeBnpl();
        checkouterProperties.setEnableBnpl(true);
    }

    @Test
    void shouldCreateVirtualPaymentWithIncomeReceipt() {
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        Payment basePayment = orderPayHelper.payForOrder(order);

        basePayment = paymentService.getPayment(basePayment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.HOLD, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));

        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());

        Payment virtualBnplPayment = getVirtualBnplPayment(order);

        List<OrderHistoryEvent> events =
                historyEventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.NEW_VIRTUAL_PAYMENT);
        assertThat(events, IsCollectionWithSize.hasSize(1));
        List<Receipt> printableReceipts = getPrintableReceipts(order);
        assertThat("Только один печатный INCOME чек", printableReceipts.size(), is(1));
        Receipt incomeReceipt = printableReceipts.get(0);
        assertThat("Печатный INCOME чек должен быть привязан к virtual_bnpl платежу",
                incomeReceipt.getPaymentId(), is(virtualBnplPayment.getId()));
        assertThat(incomeReceipt.getStatus(), is(ReceiptStatus.NEW));

        orderPayHelper.notifyPaymentClear(virtualBnplPayment);

        assertThat(receiptService.getReceipt(incomeReceipt.getId()).getStatus(), is(ReceiptStatus.PRINTED));
        assertThat(paymentService.getPayment(virtualBnplPayment.getId(), ClientInfo.SYSTEM).getStatus(),
                is(PaymentStatus.CLEARED));
    }

    @ParameterizedTest(name = "mockAlreadyClearedBasket: {0}")
    @ValueSource(booleans = {false, true})
    void shouldCreateVirtualPaymentWithCashbackAndIncomeReceipt(boolean mockAlreadyClearedBasket) throws IOException {
        Parameters parameters = defaultBnplOrderParametersWithCashBackSpent();
        Order order = orderCreateHelper.createOrder(parameters);
        Payment basePayment = orderPayHelper.payForOrder(order);

        basePayment = paymentService.getPayment(basePayment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.HOLD, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));
        if (mockAlreadyClearedBasket) {
            trustMockConfigurer.mockAlreadyClearedBasket();
        }
        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());

        Payment virtualBnplPayment = getVirtualBnplPayment(order);
        List<Receipt> printableReceipts = getPrintableReceipts(order);
        assertThat("Только один печатный INCOME чек", printableReceipts.size(), is(1));
        Receipt incomeReceipt = printableReceipts.get(0);
        assertThat("Печатный INCOME чек должен быть привязан к virtual_bnpl платежу",
                incomeReceipt.getPaymentId(), is(virtualBnplPayment.getId()));
        assertThat(incomeReceipt.getStatus(), is(ReceiptStatus.NEW));

        orderPayHelper.notifyPaymentClear(virtualBnplPayment);

        var payment = paymentService.getPayment(virtualBnplPayment.getId(), ClientInfo.SYSTEM);
        assertThat(receiptService.getReceipt(incomeReceipt.getId()).getStatus(), is(ReceiptStatus.PRINTED));
        assertThat(payment.getStatus(), is(PaymentStatus.CLEARED));
        checkSeparatePayment(orderService.getOrders(List.of(order.getId())).values());
    }

    @Test
    void shouldNotCreateVirtualPaymentWithoutCashbackAccount() throws Exception {
        trustMockConfigurer.mockListWalletBalanceResponse();
        Parameters parameters = defaultBnplOrderParametersWithCashBackSpent();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        var basePayment = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.BNPL).get(0);
        assertEquals(PaymentStatus.CLEARED, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));

        mockEmptyWalletResponse();

        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());

        Payment virtualBnplPayment = getVirtualBnplPayment(order);
        List<Receipt> printableReceipts = getPrintableReceipts(order);
        assertThat("Платеж отменен", virtualBnplPayment.getStatus(), is(PaymentStatus.CANCELLED));
        assertThat("Payment Basket не был создан", virtualBnplPayment.getBasketKey(), nullValue());
        assertThat("Печатный чек отсутствует", printableReceipts.size(), is(0));

        order = orderService.getOrder(order.getId());
        cancelParcel(order);

        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(OrderStatus.CANCELLED));
        assertThat(order.getSubstatus(), is(OrderSubstatus.SERVICE_FAULT));

        executeRefundQcAndValidate(order);
    }

    @Test
    void shouldCreateVirtualPaymentAndCancelItByTrustNotification() throws Exception {
        trustMockConfigurer.mockListWalletBalanceResponse();
        Parameters parameters = defaultBnplOrderParametersWithCashBackSpent();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        var basePayment = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.BNPL).get(0);
        assertEquals(PaymentStatus.CLEARED, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));

        trustMockConfigurer.mockPayBasketBadRequest();

        var exception = assertThrows(RuntimeException.class, () ->
                queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId())
        );
        // the bonuses were spent after we checked the yandex plus balance
        assertThat(exception.getMessage(), containsString("not_enough_funds"));

        Payment virtualBnplPayment = getVirtualBnplPayment(order);
        Payment finalVirtualBnplPayment = virtualBnplPayment;
        // check that multiple notifications will not break anything
        IntStream.range(1, 3).forEach(repeatCount -> orderPayHelper.notifyPaymentCancel(finalVirtualBnplPayment));

        virtualBnplPayment = getVirtualBnplPayment(order);
        List<Receipt> printableReceipts = getPrintableReceipts(order);
        assertThat("Платеж отменен", virtualBnplPayment.getStatus(), is(PaymentStatus.CANCELLED));
        assertThat("Payment Basket был создан", virtualBnplPayment.getBasketKey(), notNullValue());
        assertThat("Печатный чек отсутствует", printableReceipts.size(), is(0));

        order = orderService.getOrder(order.getId());
        cancelParcel(order);

        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(OrderStatus.CANCELLED));
        assertThat(order.getSubstatus(), is(OrderSubstatus.SERVICE_FAULT));

        executeRefundQcAndValidate(order);
    }

    @ParameterizedTest(name = "enableSeparateTotal: {0}")
    @ValueSource(booleans = {false, true})
    void shouldCreateVirtualPaymentWithCashbackAndIncomeReceiptMultiorder(boolean enableSeparateTotal) {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(enableSeparateTotal);

        Parameters parameters = defaultBnplParameters();
        parameters.addOrder(defaultBnplParameters());
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(
                new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "1")));
        var multiOrder = orderCreateHelper.createMultiOrder(parameters);

        var basePayment = orderPayHelper.payForOrders(multiOrder.getOrders());

        var orders = orderService.getOrderIdsByPayment(basePayment.getId());

        var order = orderService.getOrder(orders.get(0));

        basePayment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.HOLD, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));

        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());

        Payment virtualBnplPayment = getVirtualBnplPayment(order);
        List<Receipt> printableReceipts = getPrintableReceipts(order);
        assertThat("Только один печатный INCOME чек", printableReceipts.size(), is(1));
        Receipt incomeReceipt = printableReceipts.get(0);
        assertThat("Печатный INCOME чек должен быть привязан к virtual_bnpl платежу",
                incomeReceipt.getPaymentId(), is(virtualBnplPayment.getId()));
        assertThat(incomeReceipt.getStatus(), is(ReceiptStatus.NEW));

        orderPayHelper.notifyPaymentClear(virtualBnplPayment);

        var payment = paymentService.getPayment(virtualBnplPayment.getId(), ClientInfo.SYSTEM);
        assertThat(receiptService.getReceipt(incomeReceipt.getId()).getStatus(), is(ReceiptStatus.PRINTED));
        assertThat(payment.getStatus(), is(PaymentStatus.CLEARED));
        checkSeparatePayment(orderService.getOrders(orders).values());
    }

    @Test
    void shouldCreateVirtualPaymentWithCises() {
        Parameters parameters = defaultBnplParameters();
        parameters.getOrders().stream()
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .forEach(orderItems -> orderItems.setCargoTypes(Set.of(980)));
        Order order = orderCreateHelper.createOrder(parameters);
        Payment basePayment = orderPayHelper.payForOrder(order);

        basePayment = paymentService.getPayment(basePayment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.HOLD, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));

        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());

        Payment virtualBnplPayment = getVirtualBnplPayment(order);

        List<OrderHistoryEvent> events =
                historyEventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.NEW_VIRTUAL_PAYMENT);
        assertThat(events, IsCollectionWithSize.hasSize(1));
        List<Receipt> printableReceipts = getPrintableReceipts(order);
        assertThat("Только один печатный INCOME чек", printableReceipts.size(), is(1));
        Receipt incomeReceipt = printableReceipts.get(0);
        assertThat("Печатный INCOME чек должен быть привязан к virtual_bnpl платежу",
                incomeReceipt.getPaymentId(), is(virtualBnplPayment.getId()));
        assertThat(incomeReceipt.getStatus(), is(ReceiptStatus.NEW));

        orderPayHelper.notifyPaymentClear(virtualBnplPayment);

        assertThat(receiptService.getReceipt(incomeReceipt.getId()).getStatus(), is(ReceiptStatus.PRINTED));
        assertThat(paymentService.getPayment(virtualBnplPayment.getId(), ClientInfo.SYSTEM).getStatus(),
                is(PaymentStatus.CLEARED));

        var orderId = order.getId();
        var itemId = order.getItems().stream()
                .filter(item -> !item.getCargoTypes().isEmpty())
                .findFirst()
                .map(OrderItem::getId)
                .orElseThrow();
        var balanceServiceId = orderId + "-item-" + itemId + "-1";
        var requests = new HashSet<String>();
        trustMockConfigurer.servedEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(request -> request.getUrl().endsWith("orders_batch") || request.getUrl().endsWith("payments" +
                        "?show_trust_payment_id=true"))
                .map(LoggedRequest::getBodyAsString)
                .forEach(requestBody -> {
                            requests.add(requestBody);
                            jsonPath("$.orders[0].order_id").value(equalTo(balanceServiceId));
                        }
                );
        assertThat(requests.size(), equalTo(2));
    }

    @ParameterizedTest(name = "enableSeparateTotal: {0}")
    @ValueSource(booleans = {false, true})
    void shouldCreateVirtualPaymentWithCashbackCisesAndIncomeReceiptMultiorder(boolean enableSeparateTotal) {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(enableSeparateTotal);

        Parameters parameters = defaultBnplParameters();
        parameters.addOrder(defaultBnplParameters());
        parameters.getOrders().stream()
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .forEach(orderItems -> orderItems.setCargoTypes(Set.of(980)));
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(
                new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "1")));
        var multiOrder = orderCreateHelper.createMultiOrder(parameters);

        var basePayment = orderPayHelper.payForOrders(multiOrder.getOrders());

        var orders = orderService.getOrderIdsByPayment(basePayment.getId());

        var order = orderService.getOrder(orders.get(0));

        basePayment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.HOLD, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));

        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());

        Payment virtualBnplPayment = getVirtualBnplPayment(order);
        List<Receipt> printableReceipts = getPrintableReceipts(order);
        assertThat("Только один печатный INCOME чек", printableReceipts.size(), is(1));
        Receipt incomeReceipt = printableReceipts.get(0);
        assertThat("Печатный INCOME чек должен быть привязан к virtual_bnpl платежу",
                incomeReceipt.getPaymentId(), is(virtualBnplPayment.getId()));
        assertThat(incomeReceipt.getStatus(), is(ReceiptStatus.NEW));

        orderPayHelper.notifyPaymentClear(virtualBnplPayment);

        var payment = paymentService.getPayment(virtualBnplPayment.getId(), ClientInfo.SYSTEM);
        assertThat(receiptService.getReceipt(incomeReceipt.getId()).getStatus(), is(ReceiptStatus.PRINTED));
        assertThat(payment.getStatus(), is(PaymentStatus.CLEARED));
        checkSeparatePayment(orderService.getOrders(orders).values());
    }

    @Test
    public void walletCashbackMoreThanBnpl() throws IOException {
        trustMockConfigurer.mockListWalletBalanceResponse(1000.0);
        Parameters parameters = defaultBnplOrderParametersWithCashBackSpent();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        var basePayment = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.BNPL).get(0);
        assertEquals(PaymentStatus.CLEARED, basePayment.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_VIRTUAL_PAYMENT, basePayment.getId()));

        memCachedAgentMockFactory.resetMemCachedAgentMock(memCachedAgent);
        trustMockConfigurer.mockListWalletBalanceResponse(2000.0);

        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());

        Payment virtualBnplPayment = getVirtualBnplPayment(order);

        assertEquals(
                basePayment.amountByAgent(PaymentAgent.YANDEX_CASHBACK),
                virtualBnplPayment.amountByAgent(PaymentAgent.YANDEX_CASHBACK)
        );
    }

    @Test
    @DisplayName("Проверяем что чек для virtual bnpl создается даже при неудачном клире платежа.")
    public void existYandexCashbackAmountInVirtualBnplReceipt() throws IOException {
        //prepare
        Parameters parameters = defaultBnplOrderParametersWithCashBackSpent();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        var basePayment = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.BNPL).get(0);

        trustMockConfigurer.mockBadRequest();

        //do
        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());
        Payment virtualBnplPayment = getVirtualBnplPayment(order);
        orderPayHelper.notifyPayment(virtualBnplPayment);

        trustMockConfigurer.mockAlreadyClearedBasket();
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        //check
        var receipts = receiptService.findByOrder(order.getId());
        var paymentAgent = findYandexCashbackPaymentAgentByPaymentIdForReceipts(receipts, virtualBnplPayment.getId());

        assertThat(receipts.size(), is(2));
        assertNotNull(paymentAgent);
        assertThat(paymentService.getPayment(virtualBnplPayment.getId(), ClientInfo.SYSTEM).getStatus(),
                is(PaymentStatus.CLEARED));
    }

    @Test
    @DisplayName("Проверяем что отмененный платеж переходит в статус CANCELLED.")
    public void updateCancelledVirtualPaymentStatus() throws IOException {
        //prepare
        Parameters parameters = defaultBnplOrderParametersWithCashBackSpent();
        Order order = orderCreateHelper.createOrder(parameters);
        Payment basePayment = orderPayHelper.payForOrder(order);

        trustMockConfigurer.mockClearBasketAlreadyCancelled();
        trustMockConfigurer.mockStatusBasket(buildUnholdWithTimestamps(null), null);

        //do
        queuedCallService.executeQueuedCallSynchronously(CREATE_VIRTUAL_PAYMENT, basePayment.getId());
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);

        Payment virtualBnplPayment = getVirtualBnplPayment(order);
        orderPayHelper.notifyPaymentCancel(virtualBnplPayment);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        //check
        assertThat(paymentService.getPayment(virtualBnplPayment.getId(), ClientInfo.SYSTEM).getStatus(),
                is(PaymentStatus.CANCELLED));
    }

    private void executeRefundQcAndValidate(Order order) {
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());

        refundHelper.proceedAsyncRefunds(order.getId());

        var refunds = refundService.getRefunds(order.getId());
        assertEquals(1, refunds.size(), "Должен быть 1 рефанд. За основной платеж");
        refunds.forEach(r -> {
            var receipt = receiptService.findByRefund(r);
            assertEquals(1, receipt.size(), "Для каждого рефанда есть чек");
        });
    }

    private PaymentAgent findYandexCashbackPaymentAgentByPaymentIdForReceipts(List<Receipt> receipts, Long paymentId) {
        List<ReceiptItem> receiptItems = receipts.stream()
                .filter(receipt -> receipt.getPaymentId().equals(paymentId))
                .findFirst()
                .map(Receipt::getItems)
                .orElse(List.of());
        return receiptItems.stream()
                .findFirst()
                .filter(receiptItem ->
                        !receiptItem.isDelivery())
                .orElse(null)
                .getPartitions()
                .stream()
                .findFirst()
                .filter(receiptItemPartition ->
                        receiptItemPartition.getPaymentAgent() == PaymentAgent.YANDEX_CASHBACK)
                .orElse(null)
                .getPaymentAgent();
    }

    private void checkSeparatePayment(Collection<Order> orders) {
        orders.forEach(order -> {
            var receipt = receiptService.findByOrder(order.getId())
                    .stream()
                    .filter(receipt1 -> Objects.equals(receipt1.getPaymentId(), order.getPaymentId()))
                    .findFirst()
                    .orElseThrow();
            var receiptItems = receipt.getItems()
                    .stream()
                    .filter(receiptItem -> Objects.equals(receiptItem.getOrderId(), order.getId()))
                    .collect(Collectors.toList());
            Map<PaymentAgent, BigDecimal> totalByAgent = PaymentPartitionCalculator.getTotalByAgent(receiptItems);
            BigDecimal yandexCashbackAmount = totalByAgent.get(PaymentAgent.YANDEX_CASHBACK);

            BigDecimal bnplPaymentAmount = totalByAgent.get(PaymentAgent.DEFAULT);

            assertThat(yandexCashbackAmount, greaterThanOrEqualTo(BigDecimal.ONE));

            assertThat(bnplPaymentAmount, greaterThanOrEqualTo(BigDecimal.ONE));

            List<LoggedRequest> requests = trustMockConfigurer.trustMock().findRequestsMatching(
                    postRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL)).build()
            ).getRequests();

            var orderItem = order.getItems().stream().findFirst().orElseThrow();
            var balanceOrderId = Optional.of(orderItem)
                    .map(OrderItem::getInstances)
                    .stream()
                    .findFirst()
                    .map(instance -> instance.get(0))
                    .map(instance -> instance.get("balanceOrderId").asText())
                    .orElseGet(orderItem::getBalanceOrderId);
            requests.forEach(
                    r -> {
                        JsonTest.checkJsonMatcher(r.getBodyAsString(), "$.paymethod_id",
                                startsWith("yandex_account"));

                        JsonTest.checkJsonMatcher(r.getBodyAsString(),
                                "$.paymethod_markup." + balanceOrderId + ".yandex_account",
                                comparesEqualTo(yandexCashbackAmount),
                                BigDecimal.class);

                        JsonTest.checkJsonMatcher(r.getBodyAsString(),
                                "$.paymethod_markup.*.wrapper::bnpl",
                                // (delivery + order-item)*order_count
                                hasSize(2 * orders.size()));
                    }
            );
        });
    }

    private List<Receipt> getPrintableReceipts(Order order) {
        return receiptService.findByOrder(order.getId()).stream()
                .filter(Receipt::isPrintable)
                .filter(r -> r.getType() == ReceiptType.INCOME)
                .collect(Collectors.toList());
    }

    private Payment getVirtualBnplPayment(Order order) {
        return paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.VIRTUAL_BNPL).get(0);
    }

    private void mockEmptyWalletResponse() throws IOException {
        memCachedAgentMockFactory.resetMemCachedAgentMock(memCachedAgent);
        trustMockConfigurer.mockListPaymentMethodsWithoutCashbackAccount();
        trustMockConfigurer.mockEmptyListWalletBalanceResponse();
        trustMockConfigurer.mockPayBasketBadRequest();
    }

    private void cancelParcel(Order order) throws Exception {
        var cancellingParcel = Optional.ofNullable(order.getDelivery().getParcels())
                .orElseThrow(() -> new ParcelNotFoundException(0L))
                .iterator()
                .next();
        parcelHelper.updateCancellationRequestStatus(
                order.getId(),
                cancellingParcel.getId(),
                CancellationRequestStatus.CONFIRMED,
                ClientInfo.SYSTEM
        );
    }

    private Order createBnplOrderWithCashback() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(new Cashback(null,
                CashbackOptions.allowed(CommonPaymentServiceTest.CASHBACK_AMOUNT, "1")));

        return orderCreateHelper.createOrder(parameters);
    }

    private Order cancellOrder(Order order) {
        orderStatusHelper.updateOrderStatus(
                order.getId(),
                ClientInfo.SYSTEM,
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND
        );
        return orderService.getOrder(order.getId());
    }
}
