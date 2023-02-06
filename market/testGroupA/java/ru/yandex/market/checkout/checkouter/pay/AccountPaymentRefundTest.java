package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.fintech.AccountPaymentFeatureToggle;
import ru.yandex.market.checkout.checkouter.b2b.NotifyBillPaidRequest;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.util.axapta.AxaptaApiMockConfigurer;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountPaymentRefundTest extends AbstractWebTestBase {
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private WireMockServer axaptaMock;
    @Autowired
    private AxaptaApiMockConfigurer axaptaApiMockConfigurer;

    @BeforeEach
    public void beforeEach() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.ON);

        b2bCustomersMockConfigurer.mockGeneratePaymentInvoice();
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));
        axaptaApiMockConfigurer.mockAcceptRefund();
    }

    @AfterEach
    public void afterEach() {
        b2bCustomersMockConfigurer.resetAll();
        axaptaApiMockConfigurer.resetAll();
    }

    public static Stream<Arguments> testParametersSource() {
        return Stream.of(
                //Order: UNPAID -> PENDING -> DELIVERY; Payment: IN_PROGRESS -> CLEARED; Refund: ACCEPTED -> SUCCESS
                Arguments.of(true, true, true),
                //Order: UNPAID -> PENDING -> DELIVERY; Payment: IN_PROGRESS -> CLEARED; Refund: ACCEPTED -> FAILED
                Arguments.of(true, true, false),
                //Order: UNPAID -> DELIVERY;            Payment: IN_PROGRESS;            Refund: ACCEPTED -> SUCCESS
                Arguments.of(true, false, true),
                //Order: UNPAID -> DELIVERY;            Payment: IN_PROGRESS;            Refund: ACCEPTED -> FAILED
                Arguments.of(true, false, false),
                //Order: UNPAID -> PENDING;             Payment: IN_PROGRESS -> CLEARED; Refund: ACCEPTED -> SUCCESS
                Arguments.of(false, true, true),
                //Order: UNPAID -> PENDING;             Payment: IN_PROGRESS -> CLEARED; Refund: ACCEPTED -> FAILED
                Arguments.of(false, true, false),
                //Order: UNPAID;                        Payment: IN_PROGRESS;            Refund: ACCEPTED -> SUCCESS
                Arguments.of(false, false, true),
                //Order: UNPAID;                        Payment: IN_PROGRESS;            Refund: ACCEPTED -> FAILED
                Arguments.of(false, false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("testParametersSource")
    public void testCancelOrderAndCreateRefund(boolean orderToDelivery,
                                               boolean notifyBillPaid,
                                               boolean refundToSuccess) throws Exception {
        Order order = createOrder();

        client.payments().generatePaymentInvoice(order.getId());
        if (notifyBillPaid) {
            client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(order.getId())));
        }

        order = orderService.getOrder(order.getId());
        if (orderToDelivery) {
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
            queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT,
                    order.getId());

            order = orderService.getOrder(order.getId());
            Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
            assertEquals(OrderStatus.DELIVERY, order.getStatus());
            assertEquals(PaymentStatus.CLEARED, payment.getStatus());
        } else {
            Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
            if (notifyBillPaid) {
                assertEquals(OrderStatus.PENDING, order.getStatus());
                assertEquals(PaymentStatus.CLEARED, payment.getStatus());
            } else {
                assertEquals(OrderStatus.UNPAID, order.getStatus());
                assertEquals(PaymentStatus.IN_PROGRESS, payment.getStatus());
            }
        }

        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertEquals(0, refunds.size());

        assertRefundStrategy(order);

        Refund refund = getRefundBy(order);
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        if (refundToSuccess) {
            client.refunds()
                    .approveManualRefund(order.getId(), refund.getId(), ClientRole.SYSTEM, ClientRole.SYSTEM.getId(),
                            null);

            order = orderService.getOrder(order.getId());
            assertEquals(payment.getTotalAmount(), order.getRefundActual());
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), order.getRefundPlanned());

            refund = getRefundBy(order);
            assertEquals(RefundStatus.SUCCESS, refund.getStatus());
        } else {
            client.refunds()
                    .failManualRefund(order.getId(), refund.getId(), ClientRole.SYSTEM, ClientRole.SYSTEM.getId(),
                            null);

            order = orderService.getOrder(order.getId());
            assertEquals(payment.getTotalAmount(), order.getRefundPlanned());
            assertNull(order.getRefundActual());

            refund = getRefundBy(order);
            assertEquals(RefundStatus.FAILED, refund.getStatus());
        }
    }

    @Test
    public void testCancelOrderThenNotifyBillPaid() throws Exception {
        Order order = createOrder();

        client.payments().generatePaymentInvoice(order.getId());
        order = orderService.getOrder(order.getId());

        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));

        client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(order.getId())));
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));

        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertEquals(0, refunds.size());

        assertRefundStrategy(order);
    }

    @Test
    public void testCancelOrderThenCreatePaymentFail() {
        final Order order = createOrder();

        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));

        assertThrows(ErrorCodeException.class, () -> client.payments().generatePaymentInvoice(order.getId()));

        List<Payment> payments =
                paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());
    }

    @Test
    public void testCancelOrderThenNotifyBillPaidWithPaymentToggleOff() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.OFF);

        Order order = createOrder();

        client.payments().generatePaymentInvoice(order.getId());
        order = orderService.getOrder(order.getId());

        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));

        client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(order.getId())));
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
    }

    private Order createOrder() {
        var parameters = B2bCustomersTestProvider.defaultB2bParameters();
        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));
        return orderCreateHelper.createOrder(parameters);
    }

    private void assertRefundStrategy(Order order) throws Exception {
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());

        Refund refund = getRefundBy(order);
        assertNotNull(refund);
        assertEquals(RefundStatus.DRAFT, refund.getStatus());

        refundHelper.proceedAsyncRefunds(order.getId());
        refund = getRefundBy(order);
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(RefundStatus.ACCEPTED, refund.getStatus());
        assertEquals(payment.getTotalAmount(), refund.getAmount());
        assertNotNull(refund.getTrustRefundKey());
        assertThat(refund.getPayment().getStatus(),
                anyOf(equalTo(PaymentStatus.CLEARED), equalTo(PaymentStatus.CANCELLED)));

        order = orderService.getOrder(order.getId());
        assertEquals(order.getRefundPlanned(), payment.getTotalAmount());
        assertNull(order.getRefundActual());

        PagedEvents historyEventsPage = eventsGetHelper.getOrderHistoryEvents(order.getId());
        Collection<OrderHistoryEvent> historyEvents = historyEventsPage.getItems();
        assertTrue(historyEvents.stream().anyMatch(e -> HistoryEventType.REFUND.equals(e.getType())));
        assertTrue(historyEvents.stream().anyMatch(e -> HistoryEventType.RECEIPT_GENERATED.equals(e.getType())));

        Receipt receipt = getReceiptBy(refund);
        assertEquals(ReceiptStatus.GENERATED, receipt.getStatus());

        assertEquals(1, order.getItems().size());

        axaptaMock.verify(1, postRequestedFor(
                urlPathEqualTo(AxaptaApiMockConfigurer.REFUND_ACCEPT_URL))
                .withRequestBody(matchingJsonPath("$.id",
                        WireMock.equalToJson(String.valueOf(refund.getId()))))
                .withRequestBody(matchingJsonPath("$.orderId",
                        WireMock.equalToJson(String.valueOf(order.getId()))))
                .withRequestBody(matchingJsonPath("$.refundStatus",
                        WireMock.equalTo(RefundStatus.IN_PROGRESS.name())))
                .withRequestBody(matchingJsonPath("$.items[0].itemId",
                        WireMock.equalToJson(String.valueOf(order.getItems().iterator().next().getId()))))
                .withRequestBody(matchingJsonPath("$.items[0].price",
                        WireMock.equalToJson(String.valueOf(order.getItems().iterator().next().getPrice()))))
                .withRequestBody(matchingJsonPath("$.items[1].itemId",
                        WireMock.absent()))
                .withRequestBody(matchingJsonPath("$.items[1].price",
                        WireMock.equalToJson(String.valueOf(order.getDelivery().getPriceWithLift()))))
                .withRequestBody(matchingJsonPath("$.items[2]",
                        WireMock.absent()))
        );
    }

    private Refund getRefundBy(Order order) {
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertEquals(1, refunds.size());
        return refunds.iterator().next();
    }

    private Receipt getReceiptBy(Refund refund) {
        var receipt = receiptService.findByRefund(refund);
        assertEquals(1, receipt.size());
        return receipt.get(0);
    }
}
