package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.model.notifications.TrustRefundNotification;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrder;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplOrderStatus;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplRefundRequestBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplRefundStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BnplTestProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.ITEMS_NOT_FOUND;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_BNPL_REFUNDS_BY_RECEIPTS;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplOrderParametersWithCashBackSpent;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.TRUST_URL;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

/**
 * @author : poluektov
 * date: 2021-07-05.
 */
public class BnplRefundTest extends AbstractWebTestBase {

    // region keys
    public static final BooleanFeatureType FEATURE_TOGGLE = ENABLE_BNPL_REFUNDS_BY_RECEIPTS;
    // endregion

    @Autowired
    private CheckouterFeatureReader featureReader;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReturnService returnService;
    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper checkouterAnnotationObjectMapper;
    @Autowired
    private BnplPaymentOperations bnplPaymentOperations;
    @Autowired
    private RefundHelper refundHelper;

    @BeforeEach
    public void mockBnpl() {
        bnplMockConfigurer.mockWholeBnpl();
        checkouterProperties.setEnableBnpl(true);
    }

    @Test
    void shouldCancelBnplOrderAndCreateRefund() throws IOException {
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        processVirtualPayment(order);
        if (Boolean.FALSE.equals(featureReader.getBoolean(FEATURE_TOGGLE))) {
            bnplMockConfigurer.mockGetBnplOrder(createMockForGetBnplOrder());
        }
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());
        refundHelper.proceedAsyncRefunds(order.getId());

        order = orderService.getOrder(order.getId());
        assertThat(order.getRefundPlanned(), equalTo(payment.getTotalAmount()));
        assertThat(order.getRefundActual(), nullValue());

        refundService.getRefunds(order.getId()).forEach(refund -> orderPayHelper.notifyRefund(refund));

        order = orderService.getOrder(order.getId());

        assertThat(order.getRefundActual(), equalTo(payment.getTotalAmount()));
        assertThat(order.getRefundPlanned(), equalTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN)));

        validateRefunds(refundService.getRefunds(order.getId()), BnplRefundStatus.APPROVED);
    }

    @Test
    void shouldCancelBnplOrderAndFailOnSecondAttempt() throws IOException {
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        processVirtualPayment(order);
        if (Boolean.FALSE.equals(featureReader.getBoolean(FEATURE_TOGGLE))) {
            bnplMockConfigurer.mockGetBnplOrder(createMockForGetBnplOrder());
        }
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());
        refundHelper.proceedAsyncRefunds(order.getId());

        order = orderService.getOrder(order.getId());
        refundService.getRefunds(order.getId()).forEach(refund -> orderPayHelper.notifyRefund(refund));

        RefundableItems refundalbeItems = refundService.getRefundableItems(order);
        assertTrue(refundalbeItems.getItems().isEmpty());
        assertFalse(refundalbeItems.getDelivery().isRefundable());
    }

    @Test
    void incomeReturnByVirtualPayment() throws IOException {
        // prepare
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        Payment virtual = processVirtualPayment(order);
        if (Boolean.FALSE.equals(featureReader.getBoolean(FEATURE_TOGGLE))) {
            bnplMockConfigurer.mockGetBnplOrder(createMockForGetBnplOrder());
        }
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());
        assertEquals(PaymentStatus.CLEARED, virtual.getStatus());

        // do refund
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());
        refundHelper.proceedAsyncRefunds(order.getId());

        // check income return receipts and refunds
        List<Receipt> incomeReturnReceipts = receiptService.findByOrder(order.getId(), ReceiptType.INCOME_RETURN);
        assertEquals(2, incomeReturnReceipts.size(), "Должно быть два возвратных чека. За основной и виртуальный " +
                "платеж");
        List<Receipt> printableReturnReceipts =
                incomeReturnReceipts.stream().filter(Receipt::isPrintable).collect(Collectors.toList());
        assertEquals(1, printableReturnReceipts.size(), "Один печатный возвратный чек");
        Collection<Refund> refundsByVirtual = refundService.getRefunds(virtual);
        assertEquals(1, refundsByVirtual.size(), "Один рефанд по виртуальному платежу");
        var virtualRefund = refundsByVirtual.iterator().next();
        var incomeReturnReceipt = printableReturnReceipts.get(0);
        assertEquals(virtualRefund.getId(), incomeReturnReceipt.getRefundId(), "Печатный возвратный чек привязан к " +
                "рефанду по виртуальному платежу");

        orderPayHelper.notifyRefund(virtualRefund);
        virtualRefund = refundService.getRefund(virtualRefund.getId());
        assertEquals(RefundStatus.SUCCESS, virtualRefund.getStatus(), "Успешный рефанд");
    }

    @Test
    void shouldCancelBnplOrderAndCreateRefundWithPlus() throws IOException {
        Parameters parameters = defaultBnplOrderParametersWithCashBackSpent();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        processVirtualPayment(order);
        if (Boolean.FALSE.equals(featureReader.getBoolean(FEATURE_TOGGLE))) {
            bnplMockConfigurer.mockGetBnplOrder(createMockForGetBnplOrder());
        }
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());
        refundHelper.proceedAsyncRefunds(order.getId());

        var refunds = refundService.getRefunds(order.getId());
        assertEquals(2, refunds.size(), "Должно быть два рефанда. За основной и виртуальный платеж");
        refunds.forEach(r -> {
            var receipt = receiptService.findByRefund(r);
            assertEquals(1, receipt.size(), "Для каждого рефанда есть чек");
            var yandexCashbackAmount = r.getPayment().amountByAgent(PaymentAgent.YANDEX_CASHBACK);
            assertNotNull(yandexCashbackAmount);
            assertTrue(
                    yandexCashbackAmount.compareTo(BigDecimal.ZERO) > 0,
                    "Сумма YANDEX_CASHBACK должна быть больше 0");
            validateRequests(r.getPayment());
        });
    }

    @Test
    public void shouldCancelBnplOrderWithHoldPaymentButClearedInBnpl() throws IOException {
        Parameters parameters = defaultBnplParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        Long orderId = order.getId();

        var payment = orderPayHelper.payForOrder(order);
        Long bnplPaymentId = payment.getId();

        order = orderService.getOrder(orderId, ClientInfo.SYSTEM);
        var virtualBnplPayment = processVirtualPayment(order);
        Long virtualBnplPaymentId = virtualBnplPayment.getId();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        order = orderService.getOrder(orderId, ClientInfo.SYSTEM);
        payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        virtualBnplPayment = paymentService.getPayment(virtualBnplPaymentId, ClientInfo.SYSTEM);

        assertEquals(PaymentStatus.HOLD, payment.getStatus());
        assertEquals(PaymentStatus.CLEARED, virtualBnplPayment.getStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        assertEquals(0, refundService.getRefunds(orderId).size());
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, orderId));
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CANCEL, bnplPaymentId));
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CANCEL, virtualBnplPaymentId));

        var bnplOrder = createMockForGetBnplOrder();
        bnplOrder.setOrderStatus(BnplOrderStatus.COMMITED);
        if (Boolean.FALSE.equals(featureReader.getBoolean(FEATURE_TOGGLE))) {
            bnplMockConfigurer.mockGetBnplOrder(bnplOrder);
        }

        IntStream.range(1, 3).forEach(iteration -> {
            orderStatusHelper.processHeldPaymentsTask();
            assertEquals(0, refundService.getRefunds(orderId).size());
            assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, orderId));
            assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CANCEL, bnplPaymentId));
            assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CANCEL, virtualBnplPaymentId));
        });

        orderPayHelper.notifyPayment(payment);
        payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CANCEL, bnplPaymentId));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.PAYMENT_CANCEL, bnplPaymentId);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CANCEL, bnplPaymentId));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, orderId);
        refundHelper.proceedAsyncRefunds(orderId);
        refundService.getRefunds(orderId).forEach(refund -> orderPayHelper.notifyRefund(refund));

        validateRefunds(refundService.getRefunds(orderId), BnplRefundStatus.APPROVED);
    }

    @Test
    void checkFractionsWhenRefundWithPlus() throws IOException {
        // См. https://st.yandex-team.ru/MARKETCHECKOUT-24084
        // При делении баллов на число предметов может получиться бесконечная дробь - копейки не сходятся
        // Например, при делении 11 на 3 или 6, 7 и т.д. сумма рефанда получится неверной (не 400 а 399.99)
        if (Boolean.FALSE.equals(featureReader.getBoolean(FEATURE_TOGGLE))) {
            return;
        }
        Parameters parameters = BnplTestProvider.defaultBnplParameters(
                OrderItemProvider.buildOrderItem("124", BigDecimal.valueOf(100), 3));
        parameters.getBuiltMultiCart().setSelectedCashbackOption(CashbackOption.SPEND);
        parameters.getBuiltMultiCart().setCashback(
                new Cashback(null, CashbackOptions.allowed(BigDecimal.valueOf(11), "1")));
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        processVirtualPayment(order);
        if (Boolean.FALSE.equals(featureReader.getBoolean(FEATURE_TOGGLE))) {
            bnplMockConfigurer.mockGetBnplOrder(createMockForGetBnplOrder());
        }
        bnplMockConfigurer.mockGetBnplOrder(createMockForGetBnplOrder());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());
        refundHelper.proceedAsyncRefunds(order.getId());

        var refunds = refundService.getRefunds(order.getId());
        assertEquals(2, refunds.size(), "Должно быть два рефанда. За основной и виртуальный платеж");
        refunds.forEach(r -> {
            var receipt = receiptService.findByRefund(r);
            assertEquals(1, receipt.size(), "Для каждого рефанда есть чек");
            var yandexCashbackAmount = r.getPayment().amountByAgent(PaymentAgent.YANDEX_CASHBACK);
            assertNotNull(yandexCashbackAmount);
            assertTrue(
                    yandexCashbackAmount.compareTo(BigDecimal.ZERO) > 0,
                    "Сумма YANDEX_CASHBACK должна быть больше 0");
            validateRequests(r.getPayment());
        });
    }

    private void validateRefunds(Collection<Refund> refunds, BnplRefundStatus bnplRefundStatus) {
        assertEquals(2, refunds.size(), "Должно быть два рефанда. За основной и виртуальный платеж");
        refunds.forEach(r -> {
            if (r.getPayment().getType() == PaymentGoal.BNPL) {
                assertThat(r.getStatus(), equalTo(convertBnplRefundStatus(bnplRefundStatus)));
            } else {
                assertThat(r.getStatus(), equalTo(RefundStatus.SUCCESS));
                assertFalse(r.getUsingCashRefundService());
            }
            var receipt = receiptService.findByRefund(r);
            assertThat(
                    "Статус платежа должен быть CLEARED или CANCELLED",
                    r.getPayment().getStatus(),
                    anyOf(equalTo(PaymentStatus.CLEARED), equalTo(PaymentStatus.CANCELLED))
            );
            assertEquals(1, receipt.size(), "Для каждого рефанда есть чек");
            assertNotNull(r.getTrustRefundKey());
        });

        var bnplRefund = getRequestBody(BnplMockConfigurer.POST_ORDER_REFUND, BnplRefundRequestBody.class)
                .orElseThrow();
        assertThat(
                "callback url заполнен",
                bnplRefund.getCallbackUrl(),
                containsString("/refunds/notify")
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldPartialRefund(boolean partialUnholdInBnpl) {
        var order1 = orderCreateHelper.createOrder(BnplTestProvider.defaultBnplParameters());
        var order2 = orderCreateHelper.createOrder(BnplTestProvider.defaultBnplParameters());
        var payment = orderPayHelper.payForOrders(List.of(order1, order2));

        processVirtualPayment(order1);
        if (partialUnholdInBnpl) {
            // отмена до перехода в delivery одного из заказов. BNPL платеж в холде, у них будет анхолд и клир.
            orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order2.getId()), OrderStatus.CANCELLED);
            orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order1.getId()), OrderStatus.DELIVERY);
            // такой случай обрабатывается processHeldPaymentsTask
            tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        } else {
            // отмена после перехода в delivery одного из заказов. BNPL платеж уже поклирен, у них будет рефанд.
            orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order1.getId()), OrderStatus.DELIVERY);
            orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order2.getId()), OrderStatus.CANCELLED);
            // в этом случае создается QC на рефанд
            assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order2.getId()));
            queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order2.getId());
        }
        refundHelper.proceedAsyncRefunds(order1.getId(), order2.getId());

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());

        assertEquals(0, refundService.getRefunds(order1.getId()).size());
        assertEquals(payment.getStatus(), PaymentStatus.CLEARED, "при частичных отменах всегда клирим бнпл платеж");

        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order2.getId());
        refundService.getRefunds(order2.getId()).stream()
                .filter(refund -> refund.getStatus() == RefundStatus.DRAFT)
                .forEach(refundHelper::proceedAsyncRefund);

        Collection<Refund> refunds = refundService.getRefunds(order2.getId());
        assertEquals(2, refunds.size());
        assertThat(refunds, everyItem(hasProperty("status", is(RefundStatus.ACCEPTED))));
    }

    @Test
    void failoverDuringSyncRefundsCreation() {
        var order1 = orderCreateHelper.createOrder(BnplTestProvider.defaultBnplParameters());
        var order2 = orderCreateHelper.createOrder(BnplTestProvider.defaultBnplParameters());
        var order3 = orderCreateHelper.createOrder(BnplTestProvider.defaultBnplParameters());
        var payment = orderPayHelper.payForOrders(List.of(order1, order2, order3));
        processVirtualPayment(order1);

        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order1.getId()), OrderStatus.CANCELLED);
        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order2.getId()), OrderStatus.CANCELLED);
        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order3.getId()), OrderStatus.DELIVERY);

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        Collection<Refund> refunds = refundService.getRefunds(payment);
        assertThat(refunds, hasSize(0));

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertThat(payment.getStatus(), is(PaymentStatus.CLEARED));

        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order1.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order2.getId());
        refundHelper.proceedAsyncRefunds(order1.getId(), order2.getId());

        refunds = refundService.getRefunds(payment);
        assertThat(refunds, hasSize(2));
        assertThat(refunds, everyItem(hasProperty("status", is(RefundStatus.ACCEPTED))));

        // TODO: можно ли как-то замокать bnplMockConfigurer.mockGetBnplOrder для этих ордеров
        //       или старой версии это неизлечимо как дробные копейки?
        if (Boolean.FALSE.equals(featureReader.getBoolean(FEATURE_TOGGLE))) {
            return;
        }

        var bnplRefunds = getRequestsBody(BnplMockConfigurer.POST_ORDER_REFUND, BnplRefundRequestBody.class);
        assertThat(bnplRefunds, hasSize(2));
        assertThat(bnplRefunds.get(0).getOrderServices(), hasSize(1));
        assertThat(bnplRefunds.get(1).getOrderServices(), hasSize(1));
        assertThat(
                bnplRefunds.get(0).getOrderServices().get(0).getAmount(),
                Matchers.comparesEqualTo(order1.getBuyerTotal())
        );
        assertThat(
                bnplRefunds.get(1).getOrderServices().get(0).getAmount(),
                Matchers.comparesEqualTo(order2.getBuyerTotal())
        );
    }

    @Test
    void returnBnplOrder() {
        Parameters parameters = BnplTestProvider.defaultBnplParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        var order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        processVirtualPayment(order);
        order = orderService.getOrder(order.getId());
        Return ret = returnHelper.createReturn(order.getId(),
                ReturnProvider.generateReturnWithDelivery(order, order.getDelivery().getDeliveryServiceId()));
        assertEquals(ReturnStatus.REFUND_IN_PROGRESS, ret.getStatus());
        returnService.processReturnPayments(order.getId(), ret.getId(), ClientInfo.SYSTEM);

        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertEquals(2, refunds.size());
        assertThat(refunds, everyItem(hasProperty("amount", is(order.getBuyerTotal()))));
    }

    @Test
    void itemRemovalFromBnplOrder() {
        Parameters parameters = BnplTestProvider.defaultBnplParameters(
                OrderItemProvider.buildOrderItem("123", BigDecimal.valueOf(5000L), 1),
                OrderItemProvider.buildOrderItem("124", BigDecimal.valueOf(100), 5));
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        processVirtualPayment(order);

        OrderItem maxCountItem = order.getItems().stream().max(Comparator.comparingInt(OfferItem::getCount)).get();
        // MDB уведомляет Чекаутер о ненайденном товаре, создается ChangeRequest на удаление одной штуки товара из
        // заказа
        var editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true,
                List.of(new ItemInfo(maxCountItem.getId(), 4, null)), ITEMS_NOT_FOUND));
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                List.of(Color.BLUE), editRequest);

        // MDB успешно подтверждает ChangeRequest
        client.updateChangeRequestStatus(order.getId(), changeRequests.iterator().next().getId(), ClientRole.SYSTEM,
                null,
                new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null));

        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertEquals(2, refunds.size());
        assertThat(refunds, everyItem(hasProperty("amount", numberEqualsTo(maxCountItem.getBuyerPrice()))));
    }

    @Test
    void shouldPostponeRefundIfVirtualNotCleared() {
        var order1 = orderCreateHelper.createOrder(BnplTestProvider.defaultBnplParameters());
        var order2 = orderCreateHelper.createOrder(BnplTestProvider.defaultBnplParameters());
        var payment = orderPayHelper.payForOrders(List.of(order1, order2));

        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.CREATE_VIRTUAL_PAYMENT, payment.getId());
        var virtual = paymentService.getPayments(order1.getId(), ClientInfo.SYSTEM, PaymentGoal.VIRTUAL_BNPL).get(0);
        //HOLD
        orderPayHelper.notifyPayment(virtual);

        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order1.getId()), OrderStatus.CANCELLED);
        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order2.getId()), OrderStatus.DELIVERY);

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order1.getId());
        assertThrows(RuntimeException.class,
                () -> queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order1.getId()),
                "Пока virtual в холде, order_refund qc пройти не должен");
        orderPayHelper.notifyPaymentClear(virtual);
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order1.getId());
        refundHelper.proceedAsyncRefunds(order1.getId(), order2.getId());

        Collection<Refund> bnplRefunds = refundService.getRefunds(order1.getId());
        assertEquals(2, bnplRefunds.size());
        assertThat(bnplRefunds, everyItem(allOf(
                hasProperty("status", is(RefundStatus.ACCEPTED)),
                hasProperty("amount", numberEqualsTo(order1.getBuyerTotal())))));
    }

    // MARKETFINTECH-150
    @Test
    void shouldFailRetryOverLimit() throws IOException {
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));

        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        processVirtualPayment(order);
        if (Boolean.FALSE.equals(featureReader.getBoolean(FEATURE_TOGGLE))) {
            bnplMockConfigurer.mockGetBnplOrder(createMockForGetBnplOrder());
        }
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());


        var refunds = refundService.getRefunds(order.getId());
        Refund bnplRefund = refunds.stream()
                .filter(r -> r.getPayment().getType() == PaymentGoal.BNPL)
                .findFirst()
                .get();
        createFakeRefundReceipt(order, bnplRefund);
        QueuedCall refundQc = queuedCallService.findQueuedCallsByOrderId(order.getId())
                .stream()
                .filter(qc ->
                        qc.getCallType() == CheckouterQCType.PROCESS_REFUND &&
                        qc.getObjectId().equals(bnplRefund.getId()))
                .findFirst()
                .get();
        // 10 tries for qc and IN_PROGRESS STATUS for refund to imitate bnpl fail
        transactionTemplate.execute(ts -> {
                    jdbcTemplate.update("" +
                            "update queued_calls " +
                            "set tries_count = 11 " +
                            "where id = " + refundQc.getId());
                    jdbcTemplate.update("" +
                            "update refund " +
                            "set status = 7 " +
                            "where id = " + bnplRefund.getId()
                    );
                    return null;
                }
        );


        refundHelper.proceedAsyncRefund(bnplRefund);

        long qcCount = queuedCallService.findQueuedCallsByOrderId(order.getId())
                .stream()
                .filter(qc ->
                        qc.getCallType() == CheckouterQCType.PROCESS_REFUND)
                .count();

        // still 2 unprocessed refund qc
        assertEquals(2L, qcCount);

        // old qc is inactive
        assertFalse(
                queuedCallService.findQueuedCallsByOrderId(order.getId()).stream()
                        .anyMatch(qc -> qc.getCallType() == CheckouterQCType.PROCESS_REFUND &&
                                qc.getObjectId().equals(bnplRefund.getId()))
        );

        refunds = refundService.getRefunds(order.getId());
        assertEquals(3, refunds.size());
        // have both failed and drafted
        assertTrue(refunds.stream().anyMatch(r -> r.getStatus() == RefundStatus.FAILED &&
                r.getPayment().getType() == PaymentGoal.BNPL));
        assertTrue(refunds.stream().anyMatch(r -> r.getStatus() == RefundStatus.DRAFT &&
                r.getPayment().getType() == PaymentGoal.BNPL));

    }

    @Test
    public void secondSplitClearedCase() {
        // создаем заказ
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        // делаем оплату
        Payment firstBnplPayment = orderPayHelper.payForOrderWithoutNotification(order);

        // делаем дооплату
        Payment secondBnplPayment = orderPayHelper.payForOrderWithoutNotification(order);

        // заказ еще в статусе UNPAID
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertEquals(OrderStatus.UNPAID, order.getStatus());

        // приходит HOLD по первой оплате
        orderPayHelper.notifyPayment(firstBnplPayment);
        firstBnplPayment = paymentService.getPayment(firstBnplPayment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.HOLD, firstBnplPayment.getStatus());

        // заказ ушел в PROCESSING
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        // создаются виртуальные оплаты, но создастся только одна
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.CREATE_VIRTUAL_PAYMENT);
        List<Payment> virtualPayments = paymentService.getPayments(
                order.getId(), ClientInfo.SYSTEM, PaymentGoal.VIRTUAL_BNPL);
        assertEquals(1, virtualPayments.size());
        Payment virtualPayment = virtualPayments.get(0);
        orderPayHelper.notifyPaymentClear(virtualPayment);
        virtualPayment = paymentService.getPayment(virtualPayment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, virtualPayment.getStatus());

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        // прилетает холд по второй оплате, она сразу уходит в CANCELLED
        orderPayHelper.notifyPayment(secondBnplPayment);
        secondBnplPayment = paymentService.getPayment(secondBnplPayment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CANCELLED, secondBnplPayment.getStatus());

        // создаются рефанды
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);

        // заказ все еще в PROCESSING
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        // выполняются рефанды
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        // заказ уходит в доставку
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertEquals(OrderStatus.DELIVERY, order.getStatus());
        firstBnplPayment = paymentService.getPayment(firstBnplPayment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, firstBnplPayment.getStatus());

        // заказ доставлен
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertEquals(OrderStatus.DELIVERED, order.getStatus());

        // не создано ни одного рефанда
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertEquals(0, refunds.size());

        // заказ ушел в отмену
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        // создается субсидия ?
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        // проводим рефанды
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        // созданы рефанды
        refunds = refundService.getRefunds(order.getId());
        assertEquals(2, refunds.size());
        refunds.forEach(it -> assertEquals(RefundStatus.ACCEPTED, it.getStatus()));
        Set<Long> paymentIds = refunds.stream()
                .map(it -> it.getPayment().getId())
                .collect(Collectors.toUnmodifiableSet());
        assertTrue(paymentIds.contains(firstBnplPayment.getId()));
        assertTrue(paymentIds.contains(virtualPayment.getId()));
    }

    private void createFakeRefundReceipt(Order order, Refund refund) {
        Receipt fakeRecipt = new Receipt();
        fakeRecipt.setRefundId(refund.getId());
        fakeRecipt.setPaymentId(refund.getPaymentId());
        fakeRecipt.setType(ReceiptType.INCOME_RETURN);
        fakeRecipt.setStatus(ReceiptStatus.WAIT_FOR_NOTIFICATION);
        List<ReceiptItem> rItems = new ArrayList<>();
        for (var oItem : order.getItems()) {
            ReceiptItem ri = new ReceiptItem(order.getId());
            ri.setItemId(oItem.getId());
            ri.setCount(oItem.getCount());
            ri.setPrice(oItem.getPrice());
            ri.setAmount(oItem.getBuyerPrice());
            rItems.add(ri);
        }
        fakeRecipt.setItems(rItems);
        receiptService.createReceipt(fakeRecipt);
    }

    private void validateRequests(Payment payment) {
        PaymentPartition yandexCashbackPart = payment.getPartitions().stream()
                .filter(part -> part.getPaymentAgent() == PaymentAgent.YANDEX_CASHBACK)
                .findAny()
                .orElseThrow();

        PaymentPartition bnplPaymentPart = payment.getPartitions().stream()
                .filter(part -> part.getPaymentAgent() == PaymentAgent.DEFAULT)
                .findAny()
                .orElseThrow();

        assertThat(yandexCashbackPart.getAmount(), greaterThanOrEqualTo(BigDecimal.ONE));
        assertThat(bnplPaymentPart.getAmount(), greaterThanOrEqualTo(BigDecimal.ONE));
        assertThat(
                yandexCashbackPart.getAmount()
                        .add(bnplPaymentPart.getAmount()),
                Matchers.comparesEqualTo(payment.getTotalAmount())
        );
        List<LoggedRequest> requests = trustMockConfigurer.trustMock().findRequestsMatching(
                postRequestedFor(urlEqualTo(TRUST_URL + "/refunds")).build()
        ).getRequests();
        requests.forEach(
                r -> {
                    JsonTest.checkJsonMatcher(r.getBodyAsString(), "$.paymethod_markup.*.yandex_account",
                            containsInRelativeOrder(yandexCashbackPart.getAmount().toString()));
                    JsonTest.checkJsonMatcher(r.getBodyAsString(), "$.paymethod_markup.*.wrapper::bnpl",
                            hasSize(2));
                }
        );

        var bnplRefundList = bnplMockConfigurer.findEventsByStubName(BnplMockConfigurer.POST_ORDER_REFUND)
                .stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .collect(Collectors.toList());
        assertThat(bnplRefundList, hasSize(1));
        bnplRefundList.forEach(requestBody -> {
            var expectedAmount = bnplPaymentPart.getAmount().setScale(4, RoundingMode.HALF_DOWN).toString();
            JsonTest.checkJsonMatcher(requestBody, "$.services.*.amount", containsInRelativeOrder(expectedAmount));
        });
    }

    private BnplOrder createMockForGetBnplOrder() throws IOException {
        var mockBnplOrder = bnplMockConfigurer.getDefaultBnplOrderResponse();
        return getRequestBody(BnplMockConfigurer.POST_ORDER_CREATE, BnplOrder.class)
                .map(request -> {
                            mockBnplOrder.setOrderServices(request.getOrderServices());
                            mockBnplOrder.setUserId(request.getUserId());
                            return mockBnplOrder;
                        }
                ).orElseThrow();
    }

    @Test
    public void checkRefundStatusWithEmptyTrustRefundKey() {
        var refund = new Refund();
        refund.setId(1L);
        refund.setTrustRefundKey(new TrustRefundKey(""));
        var exception = assertThrows(IllegalStateException.class, () ->
                bnplPaymentOperations.fetchRefundStatusFromPaymentSystem(
                        Mockito.mock(Payment.class),
                        Mockito.mock(Order.class),
                        refund,
                        Mockito.mock(TrustRefundNotification.class)
                ));
        assertThat(exception.getMessage(), equalTo("Unable to fetch refund(1) status in external system. " +
                "Reason: externalRefundId is null"));
        assertThat(bnplMockConfigurer.servedEvents(), hasSize(0));
    }

    @NotNull
    private <T> Optional<T> getRequestBody(String stubName, Class<T> clazz) {
        return bnplMockConfigurer
                .findEventsByStubName(stubName)
                .stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .findFirst()
                .map(request -> parse(request, clazz));
    }

    private <T> ArrayList<T> getRequestsBody(String stubName, Class<T> clazz) {
        return bnplMockConfigurer
                .findEventsByStubName(stubName)
                .stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .map(request -> parse(request, clazz))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Nullable
    private <T> T parse(String request, Class<T> clazz) {
        try {
            return checkouterAnnotationObjectMapper.readValue(request, clazz);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e);
            return null;
        }
    }

    private Payment processVirtualPayment(Order order) {
        order = orderService.getOrder(order.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.CREATE_VIRTUAL_PAYMENT, order.getPaymentId());
        Payment virtual = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.VIRTUAL_BNPL).get(0);
        orderPayHelper.notifyPaymentClear(virtual);
        return paymentService.getPayment(virtual.getId(), ClientInfo.SYSTEM);
    }

    private RefundStatus convertBnplRefundStatus(BnplRefundStatus refundBnplStatus) {
        switch (refundBnplStatus) {
            case DRAFT:
                return RefundStatus.DRAFT;
            case PROCESSING:
                return RefundStatus.ACCEPTED;
            case APPROVED:
                return RefundStatus.SUCCESS;
            case FAILED:
                return RefundStatus.FAILED;
            default:
                log.error("Couldn't resolve refund status for: status = {}",
                        refundBnplStatus);
                throw new IllegalArgumentException(
                        "Couldn't resolve refund status for refund.id = {}: status = " + refundBnplStatus);
        }
    }
}
