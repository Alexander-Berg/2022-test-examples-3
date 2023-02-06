package ru.yandex.market.checkout.checkouter.b2b;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.B2B_ACCOUNT_PREPAYMENT;

public class B2bCustomersInvoiceTest extends AbstractWebTestBase {

    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private B2bCustomersService b2bCustomersService;
    @Autowired
    private QueuedCallService queuedCallService;

    @BeforeEach
    void init() {
        b2bCustomersMockConfigurer.mockGeneratePaymentInvoice();
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
        b2bCustomersMockConfigurer.mockGenerateMultiPaymentInvoice();
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, false);
    }

    @AfterEach
    void resetMocks() {
        b2bCustomersMockConfigurer.resetAll();
    }

    @ParameterizedTest(name = "{displayName} with multi = {0}")
    @MethodSource("useMultiOrSingleEndpoint")
    void generateInvoiceViaApi(Boolean enableMulti, String notUsed) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, enableMulti);
        Order b2bOrder = createOrder();

        PaymentInvoice paymentInvoice = client.payments().generatePaymentInvoice(b2bOrder.getId());

        assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), paymentInvoice.getPdfUrl());
    }

    public static Stream<Arguments> useMultiOrSingleEndpoint() {
        return Stream.of(
                Arguments.of(false, B2bCustomersMockConfigurer.POST_GENERATE_PAYMENT_INVOICE),
                Arguments.of(true, B2bCustomersMockConfigurer.POST_GENERATE_MULTI_PAYMENT_INVOICE)
        );
    }


    private Order createOrder() {
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        Order b2bOrder = orderCreateHelper.createOrder(parameters);
        assertEquals(OrderStatus.UNPAID, b2bOrder.getStatus());
        return b2bOrder;
    }

    @ParameterizedTest(name = "{displayName} with multi = {0}")
    @MethodSource("useMultiOrSingleEndpoint")
    void generateInvoiceShouldBeIdempotent(Boolean enableMulti, String eventName) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, enableMulti);
        Order b2bOrder = createOrder();

        b2bCustomersService.generatePaymentInvoiceUrl(b2bOrder.getId());
        b2bCustomersService.generatePaymentInvoiceUrl(b2bOrder.getId());

        assertGenerationExecutedOnce(eventName);
    }

    private void assertGenerationExecutedOnce(String eventName) {
        List<ServeEvent> generateInvoiceEvents = b2bCustomersMockConfigurer.findEventsByStubName(eventName);
        assertEquals(1, generateInvoiceEvents.size(), "Фактическая генерация счета была вызвана только один раз");
    }

    @ParameterizedTest(name = "{displayName} with multi = {0}")
    @MethodSource("useMultiOrSingleEndpoint")
    void generateInvoiceViaQC(Boolean enableMulti, String notUsed) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, enableMulti);
        Order b2bOrder = createOrder();
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.GENERATE_PAYMENT_INVOICE, b2bOrder.getId()));

        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.GENERATE_PAYMENT_INVOICE, b2bOrder.getId());

        String pdfUrl = orderService.getOrder(b2bOrder.getId()).getProperty(OrderPropertyType.INVOICE_PDF_URL);
        assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), pdfUrl);
    }

    @ParameterizedTest(name = "{displayName} with multi = {0}")
    @MethodSource("useMultiOrSingleEndpoint")
    void restrictToGenerateInvoiceForNonUnpaid(Boolean enableMulti, String notUsed) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, enableMulti);
        Order b2bOrder = createOrder();
        orderStatusHelper.proceedOrderToStatus(b2bOrder, OrderStatus.PENDING);
        assertEquals(OrderStatus.PENDING, orderService.getOrder(b2bOrder.getId()).getStatus());

        assertThrows(ErrorCodeException.class, () -> client.payments().generatePaymentInvoice(b2bOrder.getId()));
    }

    @ParameterizedTest(name = "{displayName} with multi = {0}")
    @MethodSource("useMultiOrSingleEndpoint")
    void restrictToGenerateInvoiceForNonBusiness(Boolean enableMulti, String notUsed) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, enableMulti);
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.prepaidBlueOrderParameters());
        assertEquals(OrderStatus.UNPAID, orderService.getOrder(order.getId()).getStatus());

        assertThrows(ErrorCodeException.class, () -> client.payments().generatePaymentInvoice(order.getId()));
    }

    @ParameterizedTest(name = "{displayName} with multi = {0}")
    @MethodSource("useMultiOrSingleEndpoint")
    void qcShouldSucceedIfInvoiceGeneratedViaApi(Boolean enableMulti, String eventName) {
        // Given
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, enableMulti);
        Order b2bOrder = createOrder();
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.GENERATE_PAYMENT_INVOICE, b2bOrder.getId()));

        // When
        PaymentInvoice paymentInvoice = client.payments().generatePaymentInvoice(b2bOrder.getId());

        // Then
        assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), paymentInvoice.getPdfUrl());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.GENERATE_PAYMENT_INVOICE, b2bOrder.getId());
        assertGenerationExecutedOnce(eventName);
    }

    @ParameterizedTest(name = "{displayName} with multi = {0}")
    @MethodSource("useMultiOrSingleEndpoint")
    void notifyBillPaidShouldMoveToPending() {
        long orderId1 = orderCreateHelper.createOrder(B2bCustomersTestProvider.defaultB2bParameters()).getId();
        long orderId2 = orderCreateHelper.createOrder(B2bCustomersTestProvider.defaultB2bParameters()).getId();

        client.payments().notifyBillPaid(new NotifyBillPaidRequest(List.of(orderId1, orderId2)));

        Order order1 = orderService.getOrder(orderId1);
        Order order2 = orderService.getOrder(orderId2);
        assertEquals(OrderStatus.PENDING, order1.getStatus());
        assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, order1.getSubstatus());
        assertEquals(OrderStatus.PENDING, order2.getStatus());
        assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, order2.getSubstatus());
    }

    @ParameterizedTest(name = "{displayName} with multi = {0}")
    @MethodSource("useMultiOrSingleEndpoint")
    void qcShouldSucceedWithoutInvoiceGenerationIfOrderIsCancelled(Boolean enableMulti, String eventName) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, enableMulti);
        Order b2bOrder = createOrder();
        orderStatusHelper.proceedOrderToStatus(b2bOrder, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.GENERATE_PAYMENT_INVOICE, b2bOrder.getId()));

        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.GENERATE_PAYMENT_INVOICE, b2bOrder.getId());

        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.GENERATE_PAYMENT_INVOICE, b2bOrder.getId()));
        assertGenerationNotExecuted(eventName);
    }

    private void assertGenerationNotExecuted(String eventName) {
        List<ServeEvent> generateInvoiceEvents = b2bCustomersMockConfigurer.findEventsByStubName(eventName);
        assertEquals(
                0,
                generateInvoiceEvents.size(),
                "Фактическая генерация счета не должна была быть вызвана");
    }

    @Test
    void generateMultiInvoiceViaApi() {
        // Given
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, true);
        Order b2bOrder = multiOrder();
        Set<Long> orderIds =
                orderService.getMultiOrderIds(b2bOrder.getProperty(OrderPropertyType.MULTI_ORDER_ID));
        Assertions.assertEquals(2, orderIds.size());

        // When
        PaymentInvoice paymentInvoice = client.payments().generatePaymentInvoice(b2bOrder.getId());

        // Then
        assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), paymentInvoice.getPdfUrl());
        for (Long orderId : orderIds) {
            String pdfUrl = orderService.getOrder(orderId).getProperty(OrderPropertyType.INVOICE_PDF_URL);
            assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), pdfUrl);
        }
    }

    private Order multiOrder() {
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        parameters.addOrder(B2bCustomersTestProvider.defaultB2bParameters());
        parameters.setPaymentMethod(B2B_ACCOUNT_PREPAYMENT);
        return orderCreateHelper.createOrder(parameters);
    }

    @Test
    void generateMultiInvoiceShouldBeIdempotent() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, true);
        Order b2bOrder = multiOrder();

        b2bCustomersService.generatePaymentInvoiceUrl(b2bOrder.getId());
        b2bCustomersService.generatePaymentInvoiceUrl(b2bOrder.getId());

        assertGenerationExecutedOnce(B2bCustomersMockConfigurer.POST_GENERATE_MULTI_PAYMENT_INVOICE);
    }

    @Test
    void generateMultiInvoiceViaQC() {
        // Given
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, true);
        Order b2bOrder = multiOrder();
        Set<Long> orderIds =
                orderService.getMultiOrderIds(b2bOrder.getProperty(OrderPropertyType.MULTI_ORDER_ID));
        Assertions.assertEquals(2, orderIds.size());

        // When
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.GENERATE_PAYMENT_INVOICE,
                orderIds.stream().findAny().get());

        // Then
        for (Long orderId : orderIds) {
            String pdfUrl = orderService.getOrder(orderId).getProperty(OrderPropertyType.INVOICE_PDF_URL);
            assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), pdfUrl);
        }
    }

    @Test
    void qcShouldSucceedIfMultiInvoiceGeneratedViaApi() {
        // Given
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_B2B_MULTI_INVOICE_API, true);
        Order b2bOrder = multiOrder();
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.GENERATE_PAYMENT_INVOICE, b2bOrder.getId()));
        Set<Long> orderIds =
                orderService.getMultiOrderIds(b2bOrder.getProperty(OrderPropertyType.MULTI_ORDER_ID));
        Assertions.assertEquals(2, orderIds.size());

        // When
        PaymentInvoice paymentInvoice = client.payments().generatePaymentInvoice(b2bOrder.getId());

        // Then
        assertEquals(B2bCustomersMockConfigurer.DEFAULT_PAYMENT_INVOICE.getUrl(), paymentInvoice.getPdfUrl());
        for (Long orderId : orderIds) {
            queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.GENERATE_PAYMENT_INVOICE, orderId);
        }
        assertGenerationExecutedOnce(B2bCustomersMockConfigurer.POST_GENERATE_MULTI_PAYMENT_INVOICE);
    }
}
