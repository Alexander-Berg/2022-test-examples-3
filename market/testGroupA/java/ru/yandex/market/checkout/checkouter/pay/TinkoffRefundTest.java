package ru.yandex.market.checkout.checkouter.pay;


import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.storage.payment.BalanceUserDataDao;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_LIFT_OPTIONS;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.pay.builders.AbstractPaymentBuilder.DELIVERY_TITLE;
import static ru.yandex.market.checkout.checkouter.receipt.ReceiptType.INCOME_RETURN;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_REFUND;
import static ru.yandex.market.checkout.common.util.BigDecimalUtils.isNullOrZero;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL;

public class TinkoffRefundTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private BalanceUserDataDao balanceUserDataDao;

    @Test
    public void shouldFullRefundSuccessfully() {
        Order order = createTinkoffCreditOrderInDelivery();
        Payment payment = order.getPayment();

        order = orderStatusHelper.proceedOrderToStatus(order, CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        refundHelper.proceedAsyncRefunds(order.getId());

        Refund refund = refundHelper.anyRefundFor(order, PaymentGoal.TINKOFF_CREDIT);
        assertSame(RefundStatus.ACCEPTED, refund.getStatus());
        assertEquals(payment.getTotalAmount(), refund.getAmount());
        assertTrue(refund.getUsingCashRefundService());
        Receipt receipt = receiptService.findByRefund(refund).iterator().next();
        assertEquals(ReceiptStatus.WAIT_FOR_NOTIFICATION, receipt.getStatus());
    }

    @Test
    public void shouldFullRefundSuccessfullyForCessionOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        Order order = orderCreateHelper.createOrder(parameters);

        orderPayHelper.payForOrderWithoutNotification(order);
        order = orderService.getOrder(order.getId());
        orderPayHelper.notifyTinkoffCessionClear(order.getPayment());
        order = orderService.getOrder(order.getId());

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_SUBSIDY_PAYMENT);
        Payment payment = order.getPayment();

        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERY));
        assertThat(payment.getType(), equalTo(PaymentGoal.TINKOFF_CREDIT));

        order = orderStatusHelper.proceedOrderToStatus(order, CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        refundHelper.proceedAsyncRefunds(order.getId());

        Refund refund = refundHelper.anyRefundFor(order, PaymentGoal.TINKOFF_CREDIT);
        assertSame(RefundStatus.ACCEPTED, refund.getStatus());
        assertEquals(payment.getTotalAmount(), refund.getAmount());
        Receipt receipt = receiptService.findByRefund(refund).iterator().next();
        assertEquals(ReceiptStatus.WAIT_FOR_NOTIFICATION, receipt.getStatus());
    }

    @Test
    public void shouldInvokeTinkoffBankPersonCreationInBalance() {
        Order order = createTinkoffCreditOrderInDelivery();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderStatusHelper.proceedOrderToStatus(order, CANCELLED);
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        refundHelper.proceedAsyncRefunds(order.getId());

        log.info("Balance calls:");
        trustMockConfigurer.balanceMock().getAllServeEvents().forEach(e -> log.info(e.getRequest().getBodyAsString()));
        checkBalanceRequests(order);
    }

    @Test
    public void shouldNotInvokeEntityCreationInBalanceIfAlreadyCreated() {
        Order order = createTinkoffCreditOrderInDelivery();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERED);
        insertBalanceIdsToDb(order);

        Return request = ReturnProvider.generateReturn(order);
        request.setUserCompensationSum(BigDecimal.ZERO);
        Return ret = returnService.initReturn(order.getId(), new ClientInfo(ClientRole.REFEREE, 123L), request,
                Experiments.empty());
        ret.setBankDetails(ReturnHelper.createDummyBankDetails());
        trustMockConfigurer.resetRequests();
        ret = returnService.resumeReturn(order.getId(), new ClientInfo(ClientRole.REFEREE, 123L), ret.getId(), ret,
                true);
        //assertNotNull(ret.getCompensationContractId());
        //assertNotNull(ret.getCompensationPersonId());
        //assertNotNull(ret.getCompensationClientId());

        //Нет вызовов баланса, т.к. есть запись в таблице balance_user_data
        assertThat(getRequestsByBalanceMethod("CreateOffer"), hasSize(0));
        assertThat(getRequestsByBalanceMethod("CreateClient"), hasSize(0));
        trustMockConfigurer.resetRequests();
        order = orderService.getOrder(order.getId());
        returnHelper.processReturnPayments(order, ret);
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertThat(refunds, hasSize(1));
        assertThat(refunds.iterator().next().getStatus(), equalTo(RefundStatus.ACCEPTED));
        //После процессинга рефанда произошел только один вызов апдейта плательщика, без создания новых контрактов и
        //клиента.
        assertThat(getRequestsByBalanceMethod("CreateOffer"), hasSize(0));
        assertThat(getRequestsByBalanceMethod("CreateClient"), hasSize(0));
        checkCreatePersonCall();

        List<BalanceUserData> balanceRecords = balanceUserDataDao.findByOrder(order.getId());
        assertThat(balanceRecords, hasSize(1));
        BalanceUserData balanceRecord = balanceRecords.iterator().next();
        assertNotNull(balanceRecord.getClientId());
        assertNotNull(balanceRecord.getContractId());
        assertNotNull(balanceRecord.getPersonId());
    }

    private void insertBalanceIdsToDb(Order order) {
        BalanceUserData balanceUserData = new BalanceUserData(order);
        balanceUserData.setClientId(123);
        balanceUserData.setPersonId(345);
        balanceUserData.setContractId(567);
        transactionTemplate.execute(ts -> {
            balanceUserDataDao.insert(balanceUserData);
            return null;
        });
    }

    @Test
    public void testCancelBeforeClear() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        Order order = orderCreateHelper.createOrder(parameters);
        Payment payment = orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());

        //при отмене взводится рефанд, а не анхолд.
        orderStatusHelper.proceedOrderToStatus(order, CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));

        //до клира, QC не обрабатывается
        //queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        //assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));

        //после клира рефанд проходит успешно.
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPaymentClear(payment);
        queuedCallService.executeQueuedCallSynchronously(ORDER_REFUND, order.getId());
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
    }

    @Test
    public void testCancelBeforePay() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        Order order = orderCreateHelper.createOrder(parameters);
        Payment payment = orderPayHelper.payForOrderWithoutNotification(order);

        //при отмене кредитного заказа взводится рефанд, а не анхолд.
        order = orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));

        //QC ожидает клира или кэнсела платежа
        assertEquals(PaymentStatus.INIT, order.getPayment().getStatus());
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));

        //после кэнсела QC проходит успешно и не создает рефанд.
        paymentService.updatePaymentStatusToCancel(payment);
        queuedCallService.executeQueuedCallSynchronously(ORDER_REFUND, order.getId());
        assertNull(refundHelper.anyRefundFor(order, PaymentGoal.TINKOFF_CREDIT));
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
    }

    @Test
    public void testCancelFailedReservation() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        pushApiConfigurer.mockAcceptFailure(parameters.getOrder());
        Order order = orderCreateHelper.createOrder(parameters);

        assertNull(order.getPaymentId());
        //при отмене кредитного заказа без платежа не должны создавать QC на рефанд
        order = orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
    }

    @Test
    public void refundReceiptDeliveryPriceShouldConformWithDeliveryPriceInTrustBasketCreationRequest() {
        Order order = createTinkoffCreditOrderWithLiftPriceInDelivery();
        assertFalse(isNullOrZero(order.getDelivery().getLiftPrice()));

        order = orderStatusHelper.proceedOrderToStatus(order, CANCELLED);

        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        refundHelper.proceedAsyncRefunds(order.getId());
        var trustReqBody = trustMockConfigurer.trustMock()
                .findRequestsMatching(postRequestedFor(urlEqualTo(TRUST_PAYMENTS_CREATE_BASKET_URL)).build())
                .getRequests()
                .get(0)
                .getBodyAsString();
        var deliveryReceipt = receiptService.findByOrder(order.getId(), INCOME_RETURN).get(0).getItems().stream()
                .filter(item -> item.getDeliveryId() != null)
                .findFirst()
                .orElseThrow();
        var deliveryPricePath = "$.orders[?(@.fiscal_title == '" + DELIVERY_TITLE + "')].price";
        var trustReqDeliveryPrice = JsonPath.compile(deliveryPricePath).<List<String>>read(trustReqBody).get(0);
        assertThat(new BigDecimal(trustReqDeliveryPrice), comparesEqualTo(deliveryReceipt.getAmount()));
    }

    private void checkBalanceRequests(Order order) {
        List<String> createClientRequests = getRequestsByBalanceMethod("FindClient");
        assertThat(createClientRequests, hasSize(1));
        assertThat(createClientRequests.get(0), containsString(order.getBuyer().getUid().toString()));

        List<String> getClientContractsRequests = getRequestsByBalanceMethod("GetClientContracts");
        assertThat(getClientContractsRequests, hasSize(1));

        List<String> createOfferRequests = getRequestsByBalanceMethod("CreateOffer");
        assertThat(createOfferRequests, hasSize(1));

        checkCreatePersonCall();
    }

    private void checkCreatePersonCall() {
        JurBankDetails bankDetails = JurBankDetails.tinkoffBankDetails("Возврат денежных средств по договору " +
                "0512345678, Иванов Иван Иванович. НДС не облагается.");
        List<String> createPartnerRequests = getRequestsByBalanceMethod("CreatePerson");
        assertThat(createPartnerRequests, hasSize(1));
        assertThat(createPartnerRequests.get(0), allOf(
                containsString(bankDetails.getName()),
                containsString(bankDetails.getLongName()),
                containsString(bankDetails.getAccount()),
                containsString(bankDetails.getCorraccount()),
                containsString(bankDetails.getBik()),
                containsString(bankDetails.getInn()),
                containsString(bankDetails.getKpp()),
                containsString(bankDetails.getLegalAddress()),
                containsString(bankDetails.getPostcode()),
                containsString(bankDetails.getPostAddress()),
                containsString(bankDetails.getPhone()),
                containsString(bankDetails.getEmail()),
                containsString(bankDetails.getPaymentPurpose())
        ));
    }

    private List<String> getRequestsByBalanceMethod(String methodName) {
        return trustMockConfigurer.balanceMock().getAllServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .filter(b -> b.contains(methodName)).collect(toList());
    }

    private Order createTinkoffCreditOrderWithLiftPriceInDelivery() {
        checkouterFeatureWriter.writeValue(ENABLE_LIFT_OPTIONS, true);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        parameters.getReportParameters().setLargeSize(true);
        parameters.addExperiment(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE);
        parameters.getOrder().getDelivery().setLiftPrice(BigDecimal.valueOf(50));
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);

        return createOrderAndProceedToDelivery(parameters);
    }

    private Order createTinkoffCreditOrderInDelivery() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        return createOrderAndProceedToDelivery(parameters);
    }

    private Order createOrderAndProceedToDelivery(Parameters parameters) {
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_SUBSIDY_PAYMENT);
        Payment payment = order.getPayment();

        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERY));
        assertThat(payment.getType(), equalTo(PaymentGoal.TINKOFF_CREDIT));

        return order;
    }
}
