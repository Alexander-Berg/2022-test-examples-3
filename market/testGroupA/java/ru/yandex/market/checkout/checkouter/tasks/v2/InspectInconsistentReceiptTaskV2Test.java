package ru.yandex.market.checkout.checkouter.tasks.v2;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.tasks.AbstractQueueTaskTestBase;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.InspectInconsistentReceiptPartitionTaskV2Factory;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildOffsetAdvanceCheckBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildReversalWithRefundConfig;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildUnholdWithTimestamps;

public class InspectInconsistentReceiptTaskV2Test extends AbstractQueueTaskTestBase {

    private static final int NOTIFICATION_TIMEOUT_HOURS = 48;
    private static final int RECEIPT_CHECK_DAYS = 90;

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private InspectInconsistentReceiptPartitionTaskV2Factory inspectInconsistentReceiptPartitionTaskV2Factory;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Value("${market.checkouter.oms.service.tms.inspectInconsistentReceipt.timeout:48}")
    private long unholdTimeoutHours;

    private Order order;
    private Payment payment;

    @Test
    public void testUpdatesReceiptOnOrderCancel() throws Exception {
        //prepare
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payment = orderPayHelper.pay(order.getId());
        order = orderService.getOrder(order.getId());
        orderPayHelper.notifyPayment(payment);
        //do
        mockMvc.perform(post("/orders/{orderId}/status", order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.STATUS, OrderStatus.CANCELLED.name())
                        .param(CheckouterClientParams.SUBSTATUS, OrderSubstatus.USER_CHANGED_MIND.name()))
                .andExpect(status().isOk());
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));
        trustMockConfigurer.resetRequests();
        trustMockConfigurer.mockCheckBasket(buildUnholdWithTimestamps(null));
        trustMockConfigurer.mockStatusBasket(buildUnholdWithTimestamps(null), null);

        runInconsistentReceiptTaskOnce(inspectInconsistentReceiptPartitionTaskV2Factory.getTasks());

        inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, Matchers.empty());
    }

    @Test
    public void testUpdatesReceiptOnCancelledPaymentWithExpiredUnhold() throws Exception {
        //prepare
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payment = orderPayHelper.pay(order.getId());
        order = orderService.getOrder(order.getId());
        orderPayHelper.notifyPayment(payment);

        //do

        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));
        final long receiptId = inconsistentReceipts.iterator().next().getId();

        trustMockConfigurer.resetRequests();
        trustMockConfigurer.mockCheckBasket(buildReversalWithRefundConfig(null));
        trustMockConfigurer.mockStatusBasket(buildReversalWithRefundConfig(null), null);

        jumpToFuture(unholdTimeoutHours, ChronoUnit.HOURS);

        runInconsistentReceiptTaskOnce(inspectInconsistentReceiptPartitionTaskV2Factory.getTasks());

        clearFixed();

        final Receipt receipt = receiptService.getReceipt(order.getId(), receiptId);
        assertNotNull(receipt);
        assertEquals(ReceiptStatus.FAILED, receipt.getStatus());
        assertEquals("Error: unhold was not completed successfully", receipt.getTrustPayload());
        inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, Matchers.empty());
    }

    @Test
    public void updateOffsetAdvanceReceipts() throws Exception {
        //prepare
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payment = orderPayHelper.pay(order.getId());
        order = orderService.getOrder(order.getId());

        //do
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        // Сгенерируем чек на зачёт аванса
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));
        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, inconsistentReceipts.iterator().next().getType());

        trustMockConfigurer.resetRequests();
        assertThat(order.getItems(), hasSize(1));
        trustMockConfigurer.mockCheckBasket(
                buildOffsetAdvanceCheckBasket(order.getItems().iterator().next().getBalanceOrderId())
        );
        trustMockConfigurer.mockStatusBasket(buildOffsetAdvanceCheckBasket(order.getItems().iterator().next()
                .getBalanceOrderId()), null);

        // Act
        runInconsistentReceiptTaskOnce(inspectInconsistentReceiptPartitionTaskV2Factory.getTasks());

        // Assert
        inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(0));

        // Убедимся, что новые чеки будут подхвачены таской ReceiptUploadTask
        List<Receipt> receiptsForMdsUpload = receiptService.findReceiptsForMdsUpload(-1L, Integer.MAX_VALUE, null);
        assertThat(receiptsForMdsUpload, hasSize(2));
        receiptsForMdsUpload = receiptsForMdsUpload.stream()
                .filter(r -> r.getType() == ReceiptType.OFFSET_ADVANCE_ON_DELIVERED)
                .collect(toList());
        assertThat(receiptsForMdsUpload, hasSize(1));
        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, receiptsForMdsUpload.iterator().next().getType());
    }

    @Test
    public void updateOffsetAdvanceReceiptsForMultiOrder() throws Exception {
        //prepare
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payment = orderPayHelper.pay(order.getId());
        order = orderService.getOrder(order.getId());

        //do
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.getAnotherWarehouseOrderItem()));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order firstOrder = multiOrder.getOrders().get(0);
        Order secondOrder = multiOrder.getOrders().get(1);
        orderStatusHelper.proceedOrderToStatus(firstOrder, OrderStatus.DELIVERY);
        orderStatusHelper.proceedOrderToStatus(secondOrder, OrderStatus.DELIVERED);
        firstOrder = orderService.getOrder(firstOrder.getId());
        secondOrder = orderService.getOrder(secondOrder.getId());

        // Сгенерируем чек на зачёт аванса
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));
        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, inconsistentReceipts.iterator().next().getType());

        assertThat(firstOrder.getItems(), hasSize(1));
        assertThat(secondOrder.getItems(), hasSize(1));
        String firstOrderItemBalanceOrderId = firstOrder.getItems().iterator().next().getBalanceOrderId();
        String secondOrderItemBalanceOrderId = secondOrder.getItems().iterator().next().getBalanceOrderId();
        final Map<String, String> balanceOrderIdToDeliveryReceiptId = new LinkedHashMap<>();
        balanceOrderIdToDeliveryReceiptId.put(firstOrderItemBalanceOrderId, null);
        balanceOrderIdToDeliveryReceiptId.put(secondOrderItemBalanceOrderId, "123");
        trustMockConfigurer.mockCheckBasket(
                CheckBasketParams.buildOffsetAdvanceCheckBasket(balanceOrderIdToDeliveryReceiptId)
        );
        trustMockConfigurer.mockStatusBasket(buildOffsetAdvanceCheckBasket(balanceOrderIdToDeliveryReceiptId), null);

        // Act
        runInconsistentReceiptTaskOnce(inspectInconsistentReceiptPartitionTaskV2Factory.getTasks());

        // Assert
        inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(0));

        // Убедимся, что новые чеки будут подхвачены таской ReceiptUploadTask
        List<Receipt> receiptsForMdsUpload = receiptService.findReceiptsForMdsUpload(-1L, Integer.MAX_VALUE, null);
        assertThat(receiptsForMdsUpload, hasSize(3));
        receiptsForMdsUpload = receiptsForMdsUpload.stream()
                .filter(r -> r.getType() == ReceiptType.OFFSET_ADVANCE_ON_DELIVERED)
                .collect(toList());
        assertThat(receiptsForMdsUpload, hasSize(1));
        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, receiptsForMdsUpload.iterator().next().getType());
    }

    @Test
    void repairFailedReceiptInQC() {
        //prepare
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payment = orderPayHelper.pay(order.getId());
        order = orderService.getOrder(order.getId());

        //do
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        // Сгенерируем чек на зачёт аванса
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        trustMockConfigurer.resetRequests();
        trustMockConfigurer.mockCheckBasket(
                buildOffsetAdvanceCheckBasket(order.getItems().iterator().next().getBalanceOrderId())
        );
        trustMockConfigurer.mockStatusBasket(buildOffsetAdvanceCheckBasket(order.getItems().iterator().next()
                .getBalanceOrderId()), null);
        trustMockConfigurer.mockNotFoundReceipts();

        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));

        inspectInconsistentReceiptPartitionTaskV2Factory.getTasks().forEach((key, value) ->
                value.run(TaskRunType.ONCE));

        var receipt = inconsistentReceipts.iterator().next();
        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, receipt.getType());

        // И был взведен qc
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.RECEIPT_REPAIR, receipt.getId()));
        // Чек не попадает в обработку таски, после установки QC по нему
        Collection<Receipt> inconsistentReceiptsAfterQC =
                receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                        RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceiptsAfterQC, hasSize(0));

        trustMockConfigurer.mockTrustPaymentsReceipts();
        runInconsistentReceiptTaskOnce(inspectInconsistentReceiptPartitionTaskV2Factory.getTasks());
        assertEquals(ReceiptStatus.NEW, receiptService.getReceipt(receipt.getId()).getStatus(),
                "Таска должна пропускать такие чеки");
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.RECEIPT_REPAIR, receipt.getId());
        assertEquals(ReceiptStatus.PRINTED, receiptService.getReceipt(receipt.getId()).getStatus(),
                "QC его успешно обработал");

        // Убедимся, что новые чеки будут подхвачены таской ReceiptUploadTask
        List<Receipt> receiptsForMdsUpload = receiptService.findReceiptsForMdsUpload(-1L,
                Integer.MAX_VALUE, null);
        assertThat(receiptsForMdsUpload, hasSize(2));
        receiptsForMdsUpload = receiptsForMdsUpload.stream()
                .filter(r -> r.getType() == ReceiptType.OFFSET_ADVANCE_ON_DELIVERED)
                .collect(toList());
        assertThat(receiptsForMdsUpload, hasSize(1));
        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, receiptsForMdsUpload.iterator().next().getType());
    }

    private void runInconsistentReceiptTaskOnce(Map<Integer, AbstractTask<?>> tasks) {
        tasks.forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });
    }
}
