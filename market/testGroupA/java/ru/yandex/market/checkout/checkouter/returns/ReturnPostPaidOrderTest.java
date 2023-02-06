package ru.yandex.market.checkout.checkouter.returns;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.balance.model.NotificationMode;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.compensation.ReturnItemsUtils;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.trust.service.TrustPaymentService;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.ReceiptRepairHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.text.StringEscapeUtils.escapeJson;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_ORDERS_STUB;

/**
 * Тут будут тесты нового процесса возвратов через ЛК покупателя.
 */
public class ReturnPostPaidOrderTest extends AbstractReturnTestBase {

    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private TrustPaymentService trustPaymentService;
    @Autowired
    private ReceiptRepairHelper receiptRepairHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private RefundHelper refundHelper;
    private Order order;

    @BeforeEach
    public void createPostPaidOrder() {
        trustMockConfigurer.mockWholeTrust();
        Parameters params = postpaidBlueOrderParameters(123L);
        params.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Дополнительные параметры для чека возврата постоплатного заказа")
    public void postPaidOrderRefundReceiptContent() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, returnResp);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        returnService.createAndDoRefunds(returnResp, order);
        refundHelper.proceedAsyncRefunds(order.getId());
        returnService.processReturnPayments(order.getId(), returnResp.getId(), ClientInfo.SYSTEM);

        order = client.getOrder(order.getId(), ClientRole.SYSTEM, null);
        ReturnItemsUtils.calculateServiceFeeAmounts(returnResp, order);
        checkBalanceCalls(order.getItems());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.PAYMENTS_NOTIFY_REFUNDS)
    @DisplayName("Уведомление о печати чека возврата постоплатного заказа")
    public void postPaidOrderRefundNotifyReceiptPrinted() throws Exception {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, returnResp);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        Collection<Refund> refunds = refundHelper.proceedAsyncRefunds(
                returnService.createAndDoRefunds(returnResp, order));
        returnService.processReturnPayments(order.getId(), returnResp.getId(), ClientInfo.SYSTEM);
        List<Receipt> receipts = getRefundReceipts(refunds);
        checkReceiptsStatus(receipts, ReceiptStatus.WAIT_FOR_NOTIFICATION);
        checkReceiptsPrintable(receipts, true);

        for (Refund refund : refunds) {
            notifyRefund(
                    BasketStatus.success,
                    refund.getTrustRefundId(),
                    NotificationMode.receipt,
                    trustPaymentService.getReceiptUrl(refund.getTrustRefundId(), refund.getTrustRefundId())
            );
        }
        receipts = getRefundReceipts(refunds);

        assertThat(receipts, not(empty()));
        checkReceiptsStatus(receipts, ReceiptStatus.PRINTED);
        checkCashReceiptPrintedEvent();
        assertTrue(receipts.stream()
                .allMatch(r -> StringUtils.isNotBlank(r.getTrustPayload())), "Not all receipts have payload");
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.PAYMENTS_NOTIFY_REFUNDS)
    @DisplayName("Уведомление о печати чека оффлайн возврата постоплатного заказа не триггерит событие юзеру")
    public void postPaidOrderRefundOffline() throws Exception {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        request.setPayOffline(true);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, returnResp);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        Collection<Refund> refunds = refundHelper.proceedAsyncRefunds(
                returnService.createAndDoRefunds(returnResp, order));
        returnService.processReturnPayments(order.getId(), returnResp.getId(), ClientInfo.SYSTEM);
        List<Receipt> receipts = getRefundReceipts(refunds);
        checkReceiptsStatus(receipts, ReceiptStatus.GENERATED);
        checkReceiptsPrintable(receipts, false);

        for (Refund refund : refunds) {
            notifyRefund(
                    BasketStatus.success,
                    refund.getTrustRefundId(),
                    NotificationMode.receipt,
                    trustPaymentService.getReceiptUrl(refund.getTrustRefundId(), refund.getTrustRefundId())
            );
        }
        receipts = getRefundReceipts(refunds);

        assertThat(receipts, not(empty()));
        checkReceiptsStatus(receipts, ReceiptStatus.GENERATED);
        checkReceiptsPrintable(receipts, false);
        checkNotCreatedCashReceiptPrintedEvent();
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RECEIPT_INSPECTOR)
    @DisplayName("Обработка чеков зависших в статусе WAIT_FOR_NOTIFICATION")
    public void postPaidOrderRefundReceiptInconsistencyCheck() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, returnResp);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        Collection<Refund> refunds = refundHelper.proceedAsyncRefunds(returnService.createAndDoRefunds(returnResp,
                order));
        returnService.processReturnPayments(order.getId(), returnResp.getId(), ClientInfo.SYSTEM);
        List<Receipt> receipts = getRefundReceipts(refunds);
        checkReceiptsStatus(receipts, ReceiptStatus.WAIT_FOR_NOTIFICATION);
        changeReceiptsCreateAt(receipts, 50);
        receiptRepairHelper.repairReceipts();
        receipts = getRefundReceipts(refunds);
        checkReceiptsStatus(receipts, ReceiptStatus.FAILED);
        Integer count = receiptService.getLastFailedReceiptsCount(PaymentGoal.ORDER_POSTPAY, true);
        assertThat(count, greaterThan(0));
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RECEIPT_INSPECTOR)
    @DisplayName("RECEIPT_FAILED событие для постоплатных рефандо-чеков")
    public void postPaidOrderRefundReceiptFailedEvent() throws Exception {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, returnResp);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        Collection<Refund> refunds = refundHelper.proceedAsyncRefunds(
                returnService.createAndDoRefunds(returnResp, order));
        returnService.processReturnPayments(order.getId(), returnResp.getId(), ClientInfo.SYSTEM);
        List<Receipt> receipts = getRefundReceipts(refunds);
        checkReceiptsStatus(receipts, ReceiptStatus.WAIT_FOR_NOTIFICATION);
        changeReceiptsCreateAt(receipts, 50);
        receiptRepairHelper.repairReceipts();
        checkReceiptFailedEvent();
    }

    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Возврат постоплатного заказа без реквизитов банка")
    @Test
    public void postPaidOrderRefundWithoutBankDetails() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
            request.setBankDetails(null);
            Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
            client.returns().resumeReturn(order.getId(),
                    returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, new Return());
        });
    }

    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Возврат постоплатного заказа с неполными реквизитами банка")
    @Test
    public void postPaidOrderRefundWithWrongBankDetails() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
            BankDetails bankDetails = request.getBankDetails();
            bankDetails.setCorraccount(null);
            request.setBankDetails(bankDetails);
            Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
            client.returns().resumeReturn(order.getId(),
                    returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, new Return());
        });
    }

    private void checkReceiptsStatus(List<Receipt> receipts, ReceiptStatus status) {
        assertTrue(receipts.stream()
                .peek(r -> log.debug("Receipt " + r))
                .allMatch(r -> r.getStatus() == status), "Not all receipts are " + status.name());
    }

    private void checkReceiptsPrintable(List<Receipt> receipts, boolean printable) {
        assertTrue(receipts.stream()
                .peek(r -> log.debug("Receipt " + r))
                .allMatch(r -> r.isPrintable() == printable), "Not all receipts are " + printable);
    }

    private List<Receipt> getRefundReceipts(Collection<Refund> refunds) {
        return refunds.stream()
                .map(refund -> receiptService.findByRefund(refund))
                .flatMap(Collection::stream)
                .collect(toList());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.PAYMENTS_NOTIFY_REFUNDS)
    @DisplayName("Уведомление о печати чека возврата постоплатного заказа без параметра receipt_data_url")
    public void postPaidOrderRefundNoReceiptUrlNotifyReceipt() throws Exception {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, returnResp);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        Collection<Refund> refunds = refundHelper.proceedAsyncRefunds(
                returnService.createAndDoRefunds(returnResp, order));
        returnService.processReturnPayments(order.getId(), returnResp.getId(), ClientInfo.SYSTEM);
        for (Refund refund : refunds) {
            notifyRefundAndExpectClientError(
                    refund.getPaymentId(),
                    BasketStatus.success,
                    refund.getTrustRefundId(),
                    NotificationMode.receipt,
                    null,
                    "{\"status\":400,\"code\":\"INVALID_REQUEST\"," +
                            "\"message\":\"Parameter receipt_data_url is missing!\"}"
            );
        }
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.PAYMENTS_NOTIFY_REFUNDS)
    @DisplayName("Уведомление о печати чека возврата постоплатного заказа с статусом !success")
    public void postPaidOrderRefundNotSuccessNotifyReceipt() throws Exception {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, returnResp);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        Collection<Refund> refunds = refundHelper.proceedAsyncRefunds(
                returnService.createAndDoRefunds(returnResp, order));
        returnService.processReturnPayments(order.getId(), returnResp.getId(), ClientInfo.SYSTEM);
        for (Refund refund : refunds) {
            notifyRefundAndExpectClientError(
                    refund.getPaymentId(),
                    BasketStatus.error,
                    refund.getTrustRefundId(),
                    NotificationMode.receipt,
                    trustPaymentService.getReceiptUrl(refund.getTrustRefundId(), refund.getTrustRefundId()),
                    "{\"status\":400,\"code\":\"INVALID_REQUEST\",\"message\":\"Illegal notification status error\"}"
            );
        }
    }

    private void checkBalanceCalls(Collection<OrderItem> orderItems) {
        List<String> createOrderRequests = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_ORDERS_STUB))
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .collect(toList());
        assertThat(createOrderRequests, not(empty()));
        assertThat(
                createOrderRequests,
                allOf(orderItems.stream()
                        .map(oi -> hasItem(hasSupplierType(oi.getSupplierType())))
                        .collect(toList()))
        );

        List<String> createBasketRequests = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .collect(toList());
        log.debug(String.join("\n", createBasketRequests));
        assertThat(createBasketRequests, not(empty()));
        assertThat(
                createBasketRequests,
                allOf(orderItems.stream()
                        .map(oi -> hasItem(hasNameAndVat(oi, order)))
                        .collect(toList()))
        );
    }

    @Nonnull
    private Matcher<String> hasSupplierType(SupplierType supplierType) {
        return allOf(
                containsString(escapeJson("\"external_id\":\"" + order.getId() + "\"")),
                containsString(escapeJson("\"supplier_type\":\"" + supplierType.getStringName() + "\""))
        );
    }

    private Matcher<String> hasNameAndVat(OrderItem oi, Order order) {
        return allOf(containsString(oi.getOfferName()),
                containsString(oi.getVat().getTrustId()),
                containsString("refunds/notify"),
                containsString("payload"),
                containsString(escapeJson("\"print_receipt\":true")),
                containsString(escapeJson("\"external_id\":\"" + order.getId() + "\"")),
                containsString(escapeJson("\"orderId\":\"" + order.getId() + "\""))
        );
    }

    private void notifyRefund(BasketStatus status, String trustId, NotificationMode mode,
                              String receiptUrl)
            throws Exception {
        mockMvc.perform(post("/refunds/notify?status={status}&trust_payment_id={basketId}&mode={mode}"
                + (receiptUrl != null ? "&receipt_data_url=" + receiptUrl : ""), status, trustId, mode))
                .andExpect(status().isOk());
    }

    private void notifyRefundAndExpectClientError(Long paymentId, BasketStatus status, String trustId,
                                                  NotificationMode mode, String receiptUrl, String errorMessage)
            throws Exception {
        mockMvc.perform(post("/refunds/notify?status={status}&trust_payment_id={basketId}&mode" +
                        "={mode}" + (receiptUrl != null ? "&receipt_data_url=" + receiptUrl : ""),
                status, trustId, mode))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(errorMessage));
    }

    private void changeReceiptsCreateAt(List<Receipt> receipts, int minusHours) {
        transactionTemplate.execute(status -> {
                    receipts.forEach(receipt -> {
                        masterJdbcTemplate.update(String.format("UPDATE receipt SET created_at = created_at " +
                                "- interval '%d hour' WHERE id = ?", minusHours), receipt.getId());
                    });
                    return null;
                }
        );
    }

    private void checkCashReceiptPrintedEvent() throws Exception {
        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        List<OrderHistoryEvent> receiptPrintedEvents = events.getItems().stream()
                .filter(e -> HistoryEventType.CASH_REFUND_RECEIPT_PRINTED == e.getType())
                .collect(toList());
        assertThat(receiptPrintedEvents, not(empty()));
        assertThat(receiptPrintedEvents.size(), is(1));
        OrderHistoryEvent e = receiptPrintedEvents.get(0);
        assertThat(e.getReceipt(), is(not(nullValue())));
        assertThat(e.getReceipt().getType(), is(ReceiptType.INCOME_RETURN));
    }

    private void checkNotCreatedCashReceiptPrintedEvent() throws Exception {
        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        List<OrderHistoryEvent> receiptPrintedEvents = events.getItems().stream()
                .filter(e -> HistoryEventType.CASH_REFUND_RECEIPT_PRINTED == e.getType())
                .collect(toList());
        assertThat(receiptPrintedEvents, empty());
    }

    private void checkReceiptFailedEvent() throws Exception {
        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        List<OrderHistoryEvent> receiptPrintedEvents = events.getItems().stream()
                .filter(e -> HistoryEventType.RECEIPT_FAILED == e.getType())
                .collect(toList());
        assertThat(receiptPrintedEvents, not(empty()));
        assertThat(receiptPrintedEvents.size(), is(1));
    }
}
