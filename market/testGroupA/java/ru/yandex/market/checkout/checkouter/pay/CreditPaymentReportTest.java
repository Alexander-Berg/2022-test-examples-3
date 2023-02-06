package ru.yandex.market.checkout.checkouter.pay;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.reports.PaymentReportsService;
import ru.yandex.market.checkout.checkouter.pay.reports.SberbankTransactionReportLine;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties.RefundStrategy;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats.DATE_FORMAT;

/**
 * @author : poluektov
 * date: 2019-06-27.
 */
public class CreditPaymentReportTest extends AbstractWebTestBase {

    private static final String LAST_REPORT_DATE_PATH = "/lastReportDate";
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
    @Autowired
    OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentReportsService paymentReportsService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private RefundHelper refundHelper;
    private Order order;
    private Instant fromDate;
    private Instant toDate;
    private Payment creditPayment;

    @BeforeEach
    public void createOrderWithCreditPayment() {
        trustMockConfigurer.resetRequests();
        Parameters parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(5000));
        parameters.setPaymentMethod(PaymentMethod.CREDIT);
        order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        fromDate = getClock().instant().minus(1, ChronoUnit.DAYS);
        toDate = getClock().instant().plus(1, ChronoUnit.DAYS);
        order = orderService.getOrder(order.getId());
        creditPayment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
    }

    @Test
    public void testExportPayments() {
        Payment supplierPayment = createClearedSupplierPayment();

        Collection<SberbankTransactionReportLine> finishedPayments =
                paymentReportsService.getAllFinishedPayments(fromDate, toDate);
        assertThat(finishedPayments.size(), greaterThanOrEqualTo(1));
        checkPaymentReportLine(finishedPayments.iterator().next(), creditPayment, supplierPayment);
    }

    @ParameterizedTest(name = "async refund: {0}")
    @ValueSource(booleans = {false, true})
    public void testExportRefunds(boolean async) {
        checkouterProperties.setAsyncRefundStrategies(async ? Set.of(RefundStrategy.SUPPLIER_REFUND_STRATEGY) :
                emptySet());
        Payment supplierPayment = createClearedSupplierPayment();
        Refund refund = createSupplierPaymentRefund(supplierPayment, async);
        Collection<SberbankTransactionReportLine> finishedRefunds =
                paymentReportsService.getAllFinishedRefunds(fromDate, toDate);
        assertThat(finishedRefunds.size(), greaterThanOrEqualTo(1));
        checkRefundReportLine(finishedRefunds.iterator().next(), refund);
    }

    @ParameterizedTest(name = "async refund: {0}")
    @ValueSource(booleans = {false, true})
    public void testMdsLinkGeneration(boolean async) {
        checkouterProperties.setAsyncRefundStrategies(async ? Set.of(RefundStrategy.SUPPLIER_REFUND_STRATEGY) :
                emptySet());
        Payment supplierPayment = createClearedSupplierPayment();
        createSupplierPaymentRefund(supplierPayment, async);
        String mdsUrl = paymentReportsService.createSberCreditTransactionsReport(fromDate, toDate);
        assertThat(mdsUrl, containsString("/report_"));
    }

    @ParameterizedTest(name = "async refund: {0}")
    @ValueSource(booleans = {false, true})
    public void testCsvContent(boolean async) {
        checkouterProperties.setAsyncRefundStrategies(async ? Set.of(RefundStrategy.SUPPLIER_REFUND_STRATEGY) :
                emptySet());
        Payment supplierPayment = createClearedSupplierPayment();
        createSupplierPaymentRefund(supplierPayment, async);
        ByteArrayOutputStream outputStream = paymentReportsService.generateReportBody(fromDate, toDate);
        String result = outputStream.toString();
        assertThat(result, startsWith("Номер заказа в Сбербанке"));
    }

    private void checkPaymentReportLine(SberbankTransactionReportLine reportLine, Payment creditPayment,
                                        Payment supplierPayment) {
        assertThat(reportLine.getSberbankOrderId(), equalTo(order.getPaymentId()));
        assertThat(reportLine.getClearingDate(), equalTo(creditPayment.getStatusUpdateDate()));
        assertThat(reportLine.getCreationDate(), equalTo(creditPayment.getCreationDate()));
        assertThat(reportLine.getTotalAmount(), equalTo(creditPayment.getTotalAmount()));
        assertThat(reportLine.getTrustId(), equalTo(supplierPayment.getBasketKey().getBasketId()));
        assertThat(reportLine.getType(), equalTo(SberbankTransactionReportLine.TransactionType.PAYMENT));
    }

    private void checkRefundReportLine(SberbankTransactionReportLine reportLine, Refund refund) {
        assertThat(reportLine.getSberbankOrderId(), equalTo(order.getPaymentId()));
        assertThat(reportLine.getClearingDate(), equalTo(refund.getStatusUpdateDate()));
        assertThat(reportLine.getCreationDate(), equalTo(refund.getCreationDate()));
        assertThat(reportLine.getTotalAmount(), equalTo(reportLine.getTotalAmount()));
        assertThat(reportLine.getTrustId(), equalTo(refund.getTrustRefundKey().getTrustRefundId()));
        assertThat(reportLine.getType(), equalTo(SberbankTransactionReportLine.TransactionType.REFUND));
    }

    private Payment createClearedSupplierPayment() {
        Payment supplierPayment = paymentService.createAndBindSupplierPayment(creditPayment.getId());
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPaymentClear(supplierPayment);
        order = orderService.getOrder(order.getId());
        supplierPayment = paymentService.getPayment(supplierPayment.getId(), ClientInfo.SYSTEM);
        assertThat(supplierPayment.getStatus(), equalTo(PaymentStatus.CLEARED));
        return supplierPayment;
    }

    private Refund createSupplierPaymentRefund(Payment supplierPayment, boolean async) {
        refundService.createAndDoReversalRefundCascade(supplierPayment, ClientInfo.SYSTEM, false);
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        if (async) {
            refunds = orderPayHelper.proceedAsyncRefunds(refunds);
        }
        orderPayHelper.notifyRefund(refunds.iterator().next());
        return refundService.getRefunds(order.getId()).iterator().next();
    }

}
