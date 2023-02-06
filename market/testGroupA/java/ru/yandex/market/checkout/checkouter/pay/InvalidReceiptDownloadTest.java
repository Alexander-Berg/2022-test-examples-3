package ru.yandex.market.checkout.checkouter.pay;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.archive.StorageOrderArchiveService;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.helpers.ReceiptRepairHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.text.CharSequenceLength.hasLength;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType.MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT;
import static ru.yandex.market.checkout.util.GenericMockHelper.withUserRole;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CHECK_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CHECK_RECEIPT_PDF_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.LOAD_RECEIPT_PAYLOAD_STUB;
import static ru.yandex.market.checkout.util.matching.Matchers.matchesPattern;

public class InvalidReceiptDownloadTest extends AbstractPaymentTestBase {

    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private ReceiptRepairHelper receiptRepairHelper;
    @Autowired
    private StorageOrderArchiveService orderArchiveService;

    private Integer minReceiptPayloadLengthToRepairReceipt;

    @BeforeEach
    public void setUp() {
        minReceiptPayloadLengthToRepairReceipt =
                checkouterFeatureReader.getInteger(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT);
        checkouterFeatureWriter.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT, 5);
    }

    @AfterEach
    public void tearDown() {
        checkouterFeatureWriter.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT,
                minReceiptPayloadLengthToRepairReceipt);
    }

    //Флакает и ломает релизы, чиниться будет здесь MARKETCHECKOUT-20717
    @Test
    @Disabled
    public void checkTrustCalledForInvalidReceiptPayload() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));

        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                Iterables.getOnlyElement(order.getItems()).getBalanceOrderId()));
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                Iterables.getOnlyElement(order.getItems()).getBalanceOrderId()), null);
        trustMockConfigurer.mockEmptyTrustPaymentsReceipts();
        receiptRepairHelper.repairReceipts();

        Receipt receipt = Iterables.getOnlyElement(
                receiptService.findByOrder(order.getId(), ReceiptType.OFFSET_ADVANCE_ON_DELIVERED)
        );
        assertThat(receipt.getTrustPayload(), is("{}"));

        trustMockConfigurer.resetRequests();
        trustMockConfigurer.mockTrustPaymentsReceipts();
        byte[] pdfBytes = mockMvc.perform(
                withUserRole(get("/orders/{orderId}/receipts/{receiptId}/pdf?", order.getId(), receipt.getId()), order)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        matchesPattern("attachment; filename=receipt_.*[.]pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(
                trustMockConfigurer.servedEvents()
                        .stream()
                        .map(ServeEvent::getStubMapping)
                        .map(StubMapping::getName)
                        .collect(Collectors.toList()),
                contains(CHECK_BASKET_STUB, LOAD_RECEIPT_PAYLOAD_STUB, CHECK_RECEIPT_PDF_STUB)
        );
        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(
                order.getId(), ClientRole.SYSTEM, 0L, 1, 50
        );
        assertThat(events.getPager().getTotal(), lessThan(50));
        List<OrderHistoryEvent> receiptPrintedEvents = events.getItems().stream()
                .filter(e -> e.getType() == HistoryEventType.RECEIPT_PRINTED)
                .collect(Collectors.toList());
        assertThat(receiptPrintedEvents, hasSize(2)); // Income & Offset advance, no new receipt event on payload update
        receipt = Iterables.getOnlyElement(
                receiptService.findByOrder(order.getId(), ReceiptType.OFFSET_ADVANCE_ON_DELIVERED)
        );
        assertThat(receipt.getTrustPayload(), hasLength(greaterThan(25)));
    }

    @Test
    public void checkNoTrustCallsForValidPayload() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));

        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                Iterables.getOnlyElement(order.getItems()).getBalanceOrderId()));
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                Iterables.getOnlyElement(order.getItems()).getBalanceOrderId()), null);
        receiptRepairHelper.repairReceipts();

        Receipt receipt = Iterables.getOnlyElement(
                receiptService.findByOrder(order.getId(), ReceiptType.OFFSET_ADVANCE_ON_DELIVERED)
        );
        assertThat(receipt.getTrustPayload(), hasLength(greaterThan(25)));

        trustMockConfigurer.resetRequests();
        trustMockConfigurer.mockTrustPaymentsReceipts();
        byte[] pdfBytes = mockMvc.perform(
                withUserRole(get("/orders/{orderId}/receipts/{receiptId}/pdf?", order.getId(), receipt.getId()), order)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        matchesPattern("attachment; filename=receipt_.*[.]pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(
                trustMockConfigurer.servedEvents()
                        .stream()
                        .map(ServeEvent::getStubMapping)
                        .map(StubMapping::getName)
                        .collect(Collectors.toList()),
                contains(CHECK_RECEIPT_PDF_STUB)
        );
    }

    @Test
    public void shouldFailForArchivedOrder() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());

        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                Iterables.getOnlyElement(order.getItems()).getBalanceOrderId()));
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                Iterables.getOnlyElement(order.getItems()).getBalanceOrderId()), null);
        receiptRepairHelper.repairReceipts();

        Receipt receipt = Iterables.getOnlyElement(
                receiptService.findByOrder(order.getId(), ReceiptType.OFFSET_ADVANCE_ON_DELIVERED)
        );

        orderArchiveService.archiveOrders(Set.of(order.getId()), false);
        checkouterProperties.setArchiveAPIEnabled(true);

        transactionTemplate.execute(
                ts -> masterJdbcTemplate
                        .update("update receipt set pdf_url = null where id = ?", receipt.getId())
        );

        mockMvc.perform(
                withUserRole(get("/orders/{orderId}/receipts/{receiptId}/pdf?archived=true", order.getId(),
                        receipt.getId()), order)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        Matchers.equalTo("Could not generate receipt PDF for archived order")));
    }
}
