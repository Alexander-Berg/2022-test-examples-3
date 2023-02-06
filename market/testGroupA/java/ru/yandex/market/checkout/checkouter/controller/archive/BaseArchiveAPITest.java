package ru.yandex.market.checkout.checkouter.controller.archive;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.db.SortingInfo;
import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.EventsCountRequest;
import ru.yandex.market.checkout.checkouter.event.EventsRequest;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.HistorySortingField;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.BasicOrdersRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.OrderItemsHistory;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection;
import ru.yandex.market.checkout.checkouter.pay.PagedPayments;
import ru.yandex.market.checkout.checkouter.pay.PagedRefunds;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.RefundableItems;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.ItemsHistoryRequest;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.OrdersEventsRequest;
import ru.yandex.market.checkout.checkouter.request.PagedOrderEventsRequest;
import ru.yandex.market.checkout.checkouter.request.PagedPaymentsRequest;
import ru.yandex.market.checkout.checkouter.request.PagedRefundsRequest;
import ru.yandex.market.checkout.checkouter.request.PagedReturnsRequest;
import ru.yandex.market.checkout.checkouter.request.PaymentRequest;
import ru.yandex.market.checkout.checkouter.request.ReceiptPdfRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.request.ReturnPdfRequest;
import ru.yandex.market.checkout.checkouter.returns.PagedReturns;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnRequest;
import ru.yandex.market.checkout.checkouter.returns.ReturnableItemsResponse;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.ReceiptUploadPartitionTaskV2Factory;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderChangesViewModel;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.ReturnHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.ARCHIVED;

public class BaseArchiveAPITest extends AbstractArchiveWebTestBase {

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private ReceiptUploadPartitionTaskV2Factory receiptUploadPartitionTaskV2Factory;
    @Autowired
    private ReturnHelper returnHelper;

    @DisplayName("Не архивированные заказы доступны через основное АПИ")
    @Test
    void shouldNotArchivedOrderIsAccessibleViaBasicAPI() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        Order order = createBlueOrder();
        long receiptId = receiptService.findByOrder(order.getId()).get(0).getId();
        long returnId = createReturn(order);
        Long eventId = getAnyEventId(order.getId());
        checkReturnResult(order, receiptId, returnId, eventId, false);
    }

    @DisplayName("Не архивированные заказы не доступны через архивное АПИ")
    @Test
    void shouldNotArchivedOrderIsNotAccessibleViaArchiveAPI() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        Order order = createBlueOrder();
        long receiptId = receiptService.findByOrder(order.getId()).get(0).getId();
        long returnId = createReturn(order);
        Long eventId = getAnyEventId(order.getId());
        checkReturnError(order, receiptId, returnId, eventId, true);
    }

    @DisplayName("Архивированные заказы не доступны через основное АПИ")
    @Test
    void shouldArchivedOrderIsNotAccessibleViaBasicAPI() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        Order order = createBlueOrder();
        long receiptId = receiptService.findByOrder(order.getId()).get(0).getId();
        long returnId = createReturn(order);
        Long eventId = getAnyEventId(order.getId());
        archiveOrder(order);
        checkReturnError(order, receiptId, returnId, eventId, false);
    }

    @DisplayName("Архивированные заказы не доступны через архивное АПИ, если архивное АПИ выключено")
    @Test
    void shouldArchivedOrderIsNotAccessibleViaArchiveAPIIfArchiveAPIDisabled() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(false);
        Order order = createBlueOrder();
        long receiptId = receiptService.findByOrder(order.getId()).get(0).getId();
        long returnId = createReturn(order);
        Long eventId = getAnyEventId(order.getId());
        archiveOrder(order);
        checkReturnError(order, receiptId, returnId, eventId, true);
    }

    @DisplayName("Архивированные заказы, находящиеся в основном хранилище, доступны через архивное АПИ")
    @Test
    void shouldArchivedOrderIsAccessibleViaArchiveAPIFromBasicStorage() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        Order order = createBlueOrder();
        receiptUploadPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });
        long receiptId = receiptService.findByOrder(order.getId()).get(0).getId();
        long returnId = createReturn(order);
        Long eventId = getAnyEventId(order.getId());
        archiveOrder(order);

        checkReturnResult(order, receiptId, returnId, eventId, true);
    }

    @DisplayName("Архивированные заказы, находящиеся и в основном хранилище, и в архивном, доступны через архивное АПИ")
    @Test
    void shouldArchivedOrderIsAccessibleViaArchiveAPIFromMultipleStorage() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        Order order = createBlueOrder();
        receiptUploadPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });
        Receipt receipt = receiptService.findByOrder(order.getId()).get(0);
        long returnId = createReturn(order);
        Long eventId = getAnyEventId(order.getId());
        archiveOrder(order);
        copyArchivingData(OrderMovingDirection.BASIC_TO_ARCHIVE);

        checkReturnResult(order, receipt.getId(), returnId, eventId, true);
    }

    @DisplayName("Архивированные заказы, находящиеся в архивном хранилище, доступны через архивное АПИ")
    @Test
    void shouldArchivedOrderIsAccessibleViaArchiveAPIFromArchiveStorage() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        Order order = createBlueOrder();
        receiptUploadPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });
        long receiptId = receiptService.findByOrder(order.getId()).get(0).getId();
        long returnId = createReturn(order);
        Long eventId = getAnyEventId(order.getId());
        clearPaymentAndUpdateIncomeReceiptToPrintedStatus();
        archiveOrder(order);
        moveArchivingData(OrderMovingDirection.BASIC_TO_ARCHIVE);

        checkReturnResult(order, receiptId, returnId, eventId, true);
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private void checkReturnResult(
            Order order, long receiptId, long returnId, long eventId, boolean archived
    ) throws Exception {
        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
        BasicOrderRequest request = BasicOrderRequest.builder(order.getId())
                .withArchived(archived)
                .build();

        //EndPoint.ORDERS_ID
        Order returnedOrder = client.getOrder(clientInfo,
                OrderRequest.builder(order.getId()).withArchived(archived).build());
        assertNotNull(returnedOrder);

        //EndPoint.ORDERS_ID_ITEMS
        OrderItems orderItems = client.getOrderItems(clientInfo, request);
        assertNotNull(orderItems);
        assertThat(orderItems.getContent(), hasSize(1));

        //EndPoint.ORDERS_ID_WARRANTY
        InputStream warrantyIs = client.getWarrantyPdf(clientInfo, request);
        assertNotNull(warrantyIs);

        //EndPoint.ORDERS_ID_PAYMENT
        Payment orderPayment = client.payments().getActivePayment(clientInfo, request);
        assertNotNull(orderPayment);

        //EndPoint.PAYMENTS_ID
        Payment payment = client.payments()
                .getPayment(clientInfo, PaymentRequest.builder(order.getPaymentId()).withArchived(archived).build());
        assertNotNull(payment);

        //EndPoint.PAYMENTS_ID_BASKET
        mockMvc.perform(get("/payments/{paymentId}/basket", order.getPaymentId())
                        .param(ARCHIVED, String.valueOf(archived)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trust_payment_id").isNotEmpty());

        //EndPoint.ORDERS_ID_PAYMENTS
        PagedPayments pagedPayments = client.payments()
                .getPayments(clientInfo, PagedPaymentsRequest.builder(order.getId()).withArchived(archived).build());
        assertNotNull(pagedPayments);
        assertThat(pagedPayments.getItems(), hasSize(1));

        //EndPoint.ORDERS_ID_RECEIPTS
        Receipts receipts = client.getOrderReceipts(clientInfo, request);
        assertNotNull(receipts);
        assertThat(receipts.getContent(), hasSize(1));

        //EndPoint.ORDERS_ID_RECEIPTS_ID_PDF
        InputStream receiptIs = client.getOrderReceiptPdf(
                clientInfo, ReceiptPdfRequest.builder(order.getId(), receiptId).withArchived(archived).build());
        assertNotNull(receiptIs);

        //EndPoint.ORDERS_RECEIPTS
        Receipts orderIdsReceipts = client.getOrdersReceipts(
                clientInfo, BasicOrdersRequest.builder(List.of(order.getId())).withArchived(archived).build());
        assertNotNull(orderIdsReceipts);
        assertThat(orderIdsReceipts.getContent(), hasSize(1));

        //EndPoint.GET_ORDERS
        OrderSearchRequest orderSearchRequest = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE}).withArchived(archived).build();
        PagedOrders pagedOrders = client.getOrders(clientInfo, orderSearchRequest);
        assertNotNull(pagedOrders);
        assertThat(pagedOrders.getItems(), hasSize(1));

        Pager expectedPager = Pager.build(Pager.atPage(1, 10), 1);
        assertEquals(expectedPager, pagedOrders.getPager());

        //EndPoint.ORDERS_COUNT
        int count = client.getOrdersCount(clientInfo, orderSearchRequest);
        assertEquals(1, count);

        //EndPoint.RETURNS_ID
        Return ret = client.returns()
                .getReturn(clientInfo, ReturnRequest.builder(returnId).withArchived(archived).build());
        assertNotNull(ret);

        //EndPoint.ORDERS_ID_RETURNS_ID
        Return retByOrderId = client.returns()
                .getReturn(clientInfo, ReturnRequest.builder(returnId, order.getId()).withArchived(archived).build());
        assertNotNull(retByOrderId);

        //EndPoint.ORDERS_ID_RETURNS_ID_PDF
        InputStream returnIs = client.returns()
                .getReturnApplicationPdf(
                        clientInfo,
                        ReturnPdfRequest.builder(order.getId(), returnId)
                                .withArchived(archived)
                                .build()
                );
        assertNotNull(returnIs);

        //EndPoint.ORDERS_ID_RETURNS
        PagedReturns pagedReturns = client.returns()
                .getOrderReturns(clientInfo, PagedReturnsRequest.builder(order.getId()).withArchived(archived).build());

        assertNotNull(pagedReturns);
        assertThat(pagedReturns.getItems(), hasSize(1));

        Pager expectedReturnsPager = Pager.build(Pager.atPage(1, 10), 1);
        assertEquals(expectedReturnsPager, pagedReturns.getPager());

        //EndPoint.ORDERS_ID_RETURNS_ITEMS
        ReturnableItemsResponse returnableItems = client.returns().getReturnableItems(clientInfo, request);
        assertNotNull(returnableItems);
        assertFalse(returnableItems.getReturnableItems().isEmpty());

        //EndPoint.ORDERS_ID_REFUNDS
        //TODO: поправить тест после починки бага в копировании заказа (создать рефанд)
        PagedRefunds pagedRefunds = client.refunds()
                .getRefunds(clientInfo, PagedRefundsRequest.builder(order.getId()).withArchived(archived).build());

        assertNotNull(pagedRefunds);
        assertThat(pagedRefunds.getItems(), hasSize(0));

        Pager expectedRefundPager = Pager.build(Pager.atPage(1, 10), 0);
        assertEquals(expectedRefundPager, pagedRefunds.getPager());

        //EndPoint.ORDERS_ID_REFUNDABLE_ITEMS
        RefundableItems refundableItems = client.refunds().getRefundableItems(clientInfo, request);
        assertNotNull(refundableItems);
        assertFalse(refundableItems.getItems().isEmpty());

        //EndPoint.ORDERS_EVENTS_ID
        OrderHistoryEvent event = client.orderHistoryEvents().getOrderHistoryEvent(eventId, archived);
        assertNotNull(event);

        //EndPoint.ORDERS_ID_EVENTS
        PagedEvents pagedEvents = client.orderHistoryEvents().getOrderHistoryEvents(
                clientInfo,
                PagedOrderEventsRequest.builder(order.getId()).withArchived(archived).build()
        );
        assertNotNull(pagedEvents);
        assertThat(pagedEvents.getItems(), hasSize(10));

        //EndPoint.ORDERS_EVENTS_BY_ORDER_ID
        OrderHistoryEvents ordersEvents = client.orderHistoryEvents()
                .getOrdersHistoryEvents(
                        OrdersEventsRequest.builder(new long[]{order.getId()})
                                .withArchived(archived)
                                .build()
                );

        assertNotNull(ordersEvents);
        assertThat(ordersEvents.getContent(), hasSize(12));

        //EndPoint.ORDERS_EVENTS
        OrderHistoryEvents events = client.orderHistoryEvents()
                .getHistoryEvents(
                        clientInfo,
                        EventsRequest.builder(0)
                                .withBatchSize(9)
                                .withOrderFilter(OrderFilter.builder().setRgb(Color.BLUE).build())
                                .withArchived(archived)
                                .build()
                );
        assertNotNull(events);
        assertThat(events.getContent(), hasSize(9));

        //EndPoint.ORDERS_EVENTS_COUNT
        Integer eventsCount = client.orderHistoryEvents()
                .getOrderHistoryEventsCount(EventsCountRequest.builder(0, Long.MAX_VALUE)
                        .withArchived(archived).build());
        assertEquals(12, eventsCount);

        //EndPoint.RETURNS_HISTORY
        Return returnHistoryByEventId = client.returns().getReturnHistoryByEventId(eventId, archived);
        assertNotNull(returnHistoryByEventId);

        //EndPoint.ORDERS_ID_CHANGES
        OrderChangesViewModel orderChanges = client.getOrderChanges(
                clientInfo, BasicOrderRequest.builder(order.getId()).withArchived(archived).build());
        assertNotNull(orderChanges);

        //EndPoint.BULK_ORDERS_ID_CHANGES
        List<OrderChangesViewModel> bulkOrderChanges = client.bulkGetOrderChanges(
                clientInfo, BasicOrdersRequest.builder(List.of(order.getId())).withArchived(archived).build());
        assertEquals(1, bulkOrderChanges.size());

        //EndPoint.ORDERS_ID_ITEMS_HISTORY_ID
        OrderItemsHistory itemsHistory = client.getOrderItemsHistory(
                clientInfo, ItemsHistoryRequest.builder(order.getId(), eventId).withArchived(archived).build());
        assertNotNull(itemsHistory);
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private void checkReturnError(
            Order order, long receiptId, long returnId, long eventId, boolean archived
    ) throws Exception {
        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
        BasicOrderRequest request = BasicOrderRequest.builder(order.getId())
                .withArchived(archived)
                .build();

        //EndPoint.ORDERS_ID
        performAndExpectOrderNotFoundException(order.getId(), () -> client.getOrder(
                clientInfo, OrderRequest.builder(order.getId()).withArchived(archived).build())
        );

        //EndPoint.ORDERS_ID_ITEMS
        performAndExpectOrderNotFoundException(order.getId(), () -> client.getOrderItems(clientInfo, request));

        //EndPoint.ORDERS_ID_WARRANTY
        performAndExpectOrderNotFoundException(order.getId(), () -> client.getWarrantyPdf(clientInfo, request));

        //EndPoint.ORDERS_ID_PAYMENT
        performAndExpectOrderNotFoundException(order.getId(), () -> client.payments()
                .getActivePayment(clientInfo, request));

        //EndPoint.PAYMENTS_ID
        performGetPaymentAndCheckNotFoundException("/payments/{paymentId}", order.getPaymentId(), archived);

        //EndPoint.PAYMENTS_ID_BASKET
        performGetPaymentAndCheckNotFoundException("/payments/{paymentId}/basket", order.getPaymentId(), archived);

        //EndPoint.ORDERS_ID_PAYMENTS
        performAndExpectOrderNotFoundException(order.getId(), () -> client.payments()
                .getPayments(clientInfo, PagedPaymentsRequest.builder(order.getId()).withArchived(archived).build()));

        //EndPoint.ORDERS_ID_RECEIPTS
        performAndExpectOrderNotFoundException(order.getId(), () -> client.getOrderReceipts(clientInfo, request));

        //EndPoint.ORDERS_ID_RECEIPTS_ID_PDF
        performAndExpectOrderNotFoundException(order.getId(), () -> client.getOrderReceiptPdf(
                clientInfo, ReceiptPdfRequest.builder(order.getId(), receiptId).withArchived(archived).build()));

        //EndPoint.ORDERS_RECEIPTS
        Receipts receipts = client.getOrdersReceipts(
                clientInfo, BasicOrdersRequest.builder(List.of(order.getId())).withArchived(archived).build());
        assertNotNull(receipts);
        assertThat(receipts.getContent(), empty());

        //EndPoint.GET_ORDERS
        OrderSearchRequest orderSearchRequest = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE}).withArchived(archived).build();
        PagedOrders pagedOrders = client.getOrders(clientInfo, orderSearchRequest);
        assertNotNull(pagedOrders);
        assertThat(pagedOrders.getItems(), hasSize(0));

        Pager expectedPager = Pager.build(Pager.atPage(1, 10), 0);
        assertEquals(expectedPager, pagedOrders.getPager());

        //EndPoint.ORDERS_COUNT
        int count = client.getOrdersCount(clientInfo, orderSearchRequest);
        assertEquals(0, count);

        //EndPoint.RETURNS_ID
        if (!archived || checkouterProperties.getArchiveAPIEnabled()) {
            performAndExpectOrderNotFoundException(order.getId(), () -> client.returns()
                    .getReturn(clientInfo, ReturnRequest.builder(returnId).withArchived(archived).build()));
        } else {
            performAndExpectReturnNotFoundException(returnId, () -> client.returns()
                    .getReturn(clientInfo, ReturnRequest.builder(returnId).withArchived(archived).build()));
        }

        //EndPoint.ORDERS_ID_RETURNS_ID
        performAndExpectOrderNotFoundException(order.getId(), () -> client.returns()
                .getReturn(clientInfo, ReturnRequest.builder(returnId, order.getId()).withArchived(archived).build()));

        //EndPoint.ORDERS_ID_RETURNS_ID_PDF
        performAndExpectOrderNotFoundException(order.getId(), () -> client.returns()
                .getReturnApplicationPdf(
                        clientInfo,
                        ReturnPdfRequest.builder(order.getId(), returnId).withArchived(archived).build()
                ));

        //EndPoint.ORDERS_ID_RETURNS
        performAndExpectOrderNotFoundException(order.getId(), () -> client.returns()
                .getOrderReturns(clientInfo,
                        PagedReturnsRequest.builder(order.getId()).withArchived(archived).build()));

        //EndPoint.ORDERS_ID_RETURNS_ITEMS
        performAndExpectOrderNotFoundException(order.getId(), () -> client.returns()
                .getReturnableItems(clientInfo, request));

        //EndPoint.ORDERS_ID_REFUNDS
        performAndExpectOrderNotFoundException(order.getId(), () -> client.refunds()
                .getRefunds(clientInfo, PagedRefundsRequest.builder(order.getId()).withArchived(archived).build()));

        //EndPoint.ORDERS_ID_REFUNDABLE_ITEMS
        performAndExpectOrderNotFoundException(order.getId(), () -> client.refunds()
                .getRefundableItems(clientInfo, request));


        //EndPoint.ORDERS_EVENTS_ID
        performAndExpectEventNotFoundException(eventId, () -> client.orderHistoryEvents()
                .getOrderHistoryEvent(eventId, archived));

        //EndPoint.ORDERS_ID_EVENTS
        performAndExpectOrderNotFoundException(order.getId(), () -> client.orderHistoryEvents().getOrderHistoryEvents(
                clientInfo,
                PagedOrderEventsRequest.builder(order.getId()).withArchived(archived).build()
        ));

        //EndPoint.ORDERS_EVENTS_BY_ORDER_ID
        OrderHistoryEvents ordersEvents = client.orderHistoryEvents()
                .getOrdersHistoryEvents(
                        OrdersEventsRequest.builder(new long[]{order.getId()})
                                .withArchived(archived)
                                .build()
                );

        assertNotNull(ordersEvents);
        assertThat(ordersEvents.getContent(), hasSize(0));

        //EndPoint.ORDERS_EVENTS
        OrderHistoryEvents events = client.orderHistoryEvents()
                .getHistoryEvents(
                        clientInfo,
                        EventsRequest.builder(0)
                                .withBatchSize(9)
                                .withOrderFilter(OrderFilter.builder().setRgb(Color.BLUE).build())
                                .withArchived(archived)
                                .build()
                );
        assertNotNull(events);
        assertThat(events.getContent(), hasSize(0));

        //EndPoint.ORDERS_EVENTS_COUNT
        Integer eventsCount = client.orderHistoryEvents()
                .getOrderHistoryEventsCount(EventsCountRequest.builder(0, Long.MAX_VALUE)
                        .withArchived(archived).build());
        assertEquals(0, eventsCount);

        //EndPoint.RETURNS_HISTORY
        performAndExpectEventNotFoundException(eventId, () -> client.returns()
                .getReturnHistoryByEventId(eventId, archived));

        //EndPoint.ORDERS_ID_CHANGES
        performAndExpectOrderNotFoundException(order.getId(), () -> client.getOrderChanges(
                clientInfo, BasicOrderRequest.builder(order.getId()).withArchived(archived).build()));

        //EndPoint.BULK_ORDERS_ID_CHANGES
        List<OrderChangesViewModel> bulkOrderChanges = client.bulkGetOrderChanges(
                clientInfo, BasicOrdersRequest.builder(List.of(order.getId())).withArchived(archived).build());
        assertEquals(0, bulkOrderChanges.size());

        //EndPoint.ORDERS_ID_ITEMS_HISTORY_ID
        performAndExpectOrderNotFoundException(order.getId(), () -> client.getOrderItemsHistory(
                clientInfo, ItemsHistoryRequest.builder(order.getId(), eventId).withArchived(archived).build()));
    }

    private void performGetPaymentAndCheckNotFoundException(
            String url, long paymentId, boolean archived) throws Exception {
        ResultActions resultActions;
        resultActions = mockMvc.perform(get(url, paymentId)
                        .param(ARCHIVED, String.valueOf(archived)))
                .andExpect(status().isNotFound());
        if (!archived || checkouterProperties.getArchiveAPIEnabled()) {
            resultActions.andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        } else {
            resultActions.andExpect(jsonPath("$.code").value("payment_not_found"))
                    .andExpect(jsonPath("$.message").value("Payment not found: " + paymentId));
        }
    }

    private void performAndExpectOrderNotFoundException(long orderId, Executable executable) {
        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, executable);
        assertEquals("ORDER_NOT_FOUND", exception.getCode());
        assertEquals("Order not found: " + orderId, exception.getMessage());
    }

    private void performAndExpectReturnNotFoundException(long returnId, Executable executable) {
        ErrorCodeException exception = assertThrows(ErrorCodeException.class, executable);
        assertEquals("RETURN_NOT_FOUND", exception.getCode());
        assertEquals("Return not found: " + returnId, exception.getMessage());
    }

    private void performAndExpectEventNotFoundException(long eventId, Executable executable) {
        ErrorCodeException exception = assertThrows(ErrorCodeException.class, executable);
        assertEquals("EVENT_NOT_FOUND", exception.getCode());
        assertEquals("Event with id: " + eventId + " was not found", exception.getMessage());
    }

    private long createReturn(Order order) {
        ReturnDelivery rd = new ReturnDelivery();
        rd.setDeliveryServiceId(1L);
        rd.setType(DeliveryType.DELIVERY);
        Return ret = new Return();
        ret.setBankDetails(ReturnHelper.createDummyBankDetails());
        ret.setDelivery(rd);
        ret.setItems(order.getItems().stream()
                .map(i -> ReturnItem
                        .initReturnOrderItem(i.getId(), ReturnReasonType.BAD_QUALITY, i.getCount(), i.getQuantity()))
                .collect(Collectors.toList()));
        ret = returnHelper.initReturn(order.getId(), ret);
        returnHelper.resumeReturn(order.getId(), ret.getId(), ret);
        return ret.getId();
    }

    private Long getAnyEventId(long orderId) {
        return eventService
                .getPagedOrderHistoryEvents(
                        orderId,
                        Pager.atPage(1, 1),
                        new SortingInfo<>(HistorySortingField.ID, SortingOrder.ASC),
                        null,
                        Set.of(HistoryEventType.ORDER_RETURN_CREATED),
                        false,
                        ClientInfo.SYSTEM,
                        null)
                .getItems()
                .iterator().next().getId();
    }
}
