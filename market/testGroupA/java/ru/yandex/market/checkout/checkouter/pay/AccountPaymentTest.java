package ru.yandex.market.checkout.checkouter.pay;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.fintech.AccountPaymentFeatureToggle;
import ru.yandex.market.checkout.checkouter.b2b.NotifyBillPaidRequest;
import ru.yandex.market.checkout.checkouter.b2b.PaymentInvoice;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentReadingDao;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountPaymentTest extends AbstractWebTestBase {
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private AccountPaymentOperations accountPaymentOperations;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private PaymentReadingDao paymentReadingDao;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private CheckouterFeatureWriter featureWriter;
    @Autowired
    protected CheckouterAPI client;

    private Long orderId;

    @BeforeEach
    public void beforeEach() {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.ON);

        b2bCustomersMockConfigurer.mockGeneratePaymentInvoice();
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));

        var properties = B2bCustomersTestProvider.defaultB2bParameters();
        Order order = orderCreateHelper.createOrder(properties);
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        orderId = order.getId();
    }

    @AfterEach
    void afterEach() {
        b2bCustomersMockConfigurer.resetAll();
    }

    @Test
    public void testCreatePayment() throws Exception {
        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        Payment payment = accountPaymentOperations.createAndBindAccountPayment(orderId);
        validatePaymentAndReceiptAndEvents(payment);
    }

    @Test
    public void testCreatePaymentIdempotent() throws Exception {
        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        Payment payment1 = accountPaymentOperations.createAndBindAccountPayment(orderId);
        assertNotNull(payment1);
        validatePaymentAndReceiptAndEvents(payment1);

        Payment payment2 = accountPaymentOperations.createAndBindAccountPayment(orderId);
        assertNotNull(payment2);
        validatePaymentAndReceiptAndEvents(payment2);

        assertEquals(payment1.getId(), payment2.getId());

        payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(1, payments.size());
    }

    @Test
    void testCreatePaymentByQueuedCall() throws Exception {
        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.GENERATE_PAYMENT_INVOICE, orderId));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.GENERATE_PAYMENT_INVOICE, orderId);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.GENERATE_PAYMENT_INVOICE, orderId));

        Payment payment = getPaymentByOrder(orderId);
        validatePaymentAndReceiptAndEvents(payment);
    }

    @Test
    void testCreatePaymentByController() throws Exception {
        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        PaymentInvoice paymentInvoice = client.payments().generatePaymentInvoice(orderId);
        assertNotNull(paymentInvoice);
        assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), paymentInvoice.getPdfUrl());

        Payment payment = getPaymentByOrder(orderId);
        validatePaymentAndReceiptAndEvents(payment);
    }

    @Test
    void testCreatePaymentToggleOff() {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.OFF);

        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        PaymentInvoice paymentInvoice = client.payments().generatePaymentInvoice(orderId);
        assertNotNull(paymentInvoice);
        assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), paymentInvoice.getPdfUrl());

        payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());
    }

    @Test
    void testCreatePaymentToggleLogging() throws Exception {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.LOGGING);

        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        PaymentInvoice paymentInvoice = client.payments().generatePaymentInvoice(orderId);
        assertNotNull(paymentInvoice);
        assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), paymentInvoice.getPdfUrl());

        Payment payment = getPaymentByOrder(orderId);
        validatePaymentAndReceiptAndEvents(payment);
    }

    @Test
    public void testPaymentCleared() throws Exception {
        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        client.payments().generatePaymentInvoice(orderId);
        Payment paymentInProgress = getPaymentByOrder(orderId);
        assertEquals(PaymentStatus.IN_PROGRESS, paymentInProgress.getStatus());

        client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(orderId)));
        Payment paymentCleared = getPaymentByOrder(orderId);
        assertEquals(PaymentStatus.CLEARED, paymentCleared.getStatus());

        assertEquals(paymentCleared.getId(), paymentInProgress.getId());

        PagedEvents historyEventsPage = eventsGetHelper.getOrderHistoryEvents(orderId);
        Collection<OrderHistoryEvent> historyEvents = historyEventsPage.getItems();
        assertTrue(historyEvents.stream().anyMatch(e -> HistoryEventType.PAYMENT_CLEARED.equals(e.getType())));
    }

    @Test
    public void testPaymentClearedToggleLogging() throws Exception {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.LOGGING);

        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        client.payments().generatePaymentInvoice(orderId);
        Payment paymentInProgress = getPaymentByOrder(orderId);
        assertEquals(PaymentStatus.IN_PROGRESS, paymentInProgress.getStatus());

        client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(orderId)));
        Payment paymentCleared = getPaymentByOrder(orderId);
        assertEquals(PaymentStatus.CLEARED, paymentCleared.getStatus());

        assertEquals(paymentCleared.getId(), paymentInProgress.getId());

        PagedEvents historyEventsPage = eventsGetHelper.getOrderHistoryEvents(orderId);
        Collection<OrderHistoryEvent> historyEvents = historyEventsPage.getItems();
        assertTrue(historyEvents.stream().anyMatch(e -> HistoryEventType.PAYMENT_CLEARED.equals(e.getType())));
    }

    @Test
    public void testPaymentClearedToggleOff() throws Exception {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.OFF);

        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        client.payments().generatePaymentInvoice(orderId);
        payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(orderId)));
        payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        PagedEvents historyEventsPage = eventsGetHelper.getOrderHistoryEvents(orderId);
        Collection<OrderHistoryEvent> historyEvents = historyEventsPage.getItems();
        assertTrue(historyEvents.stream().noneMatch(e -> HistoryEventType.PAYMENT_CLEARED.equals(e.getType())));
    }

    @Test
    public void testPaymentClearedToggleOffToOn() throws Exception {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.OFF);

        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        client.payments().generatePaymentInvoice(orderId);
        payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.ON);
        assertThrows(ErrorCodeException.class, () ->
                client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(orderId))));

        PagedEvents historyEventsPage = eventsGetHelper.getOrderHistoryEvents(orderId);
        Collection<OrderHistoryEvent> historyEvents = historyEventsPage.getItems();
        assertTrue(historyEvents.stream().noneMatch(e -> HistoryEventType.PAYMENT_CLEARED.equals(e.getType())));
    }

    @Test
    public void testPaymentClearedToggleOffToLogging() throws Exception {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.OFF);

        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        client.payments().generatePaymentInvoice(orderId);
        payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(0, payments.size());

        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.LOGGING);
        client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(orderId)));

        PagedEvents historyEventsPage = eventsGetHelper.getOrderHistoryEvents(orderId);
        Collection<OrderHistoryEvent> historyEvents = historyEventsPage.getItems();
        assertTrue(historyEvents.stream().anyMatch(e -> HistoryEventType.PAYMENT_CLEARED.equals(e.getType())));
    }

    private void validatePaymentAndReceiptAndEvents(Payment payment) throws Exception {
        Order order = orderService.getOrder(orderId);

        // Создался нужный платеж
        assertNotNull(payment);
        assertEquals(payment.getId(), order.getPaymentId());
        assertEquals(PaymentStatus.IN_PROGRESS, payment.getStatus());
        assertEquals(PaymentGoal.ORDER_ACCOUNT_PAYMENT, payment.getType());

        // Создался нужный чек
        List<Receipt> receipts = receiptService.findByPayment(payment);
        assertEquals(1, receipts.size());
        assertFalse(receipts.get(0).isPrintable());

        // Сгенерированы нужные события
        PagedEvents historyEventsPage = eventsGetHelper.getOrderHistoryEvents(orderId);
        Collection<OrderHistoryEvent> historyEvents = historyEventsPage.getItems();
        assertTrue(historyEvents.stream().anyMatch(e -> HistoryEventType.NEW_PAYMENT.equals(e.getType())));
        assertTrue(historyEvents.stream().anyMatch(e -> HistoryEventType.RECEIPT_GENERATED.equals(e.getType())));
    }

    private Payment getPaymentByOrder(Long orderId) {
        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        assertEquals(1, payments.size());
        return payments.get(0);
    }
}
