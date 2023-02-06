package ru.yandex.market.checkout.checkouter.tasks;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.ReceiptTestHelper;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.ReceiptRepairHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.shop.DeliveryReceiptNeedType.DONT_CREATE_DELIVERY_RECEIPT_EXCLUDE_DELIVERY;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.getDefaultMeta;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildReversalWithRefundConfig;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildUnholdWithTimestamps;

public class ReceiptRepairTest extends AbstractPaymentTestBase {

    private static final int NOTIFICATION_TIMEOUT_HOURS = 48;
    private static final int RECEIPT_CHECK_DAYS = 90;

    @Autowired
    private ReceiptRepairHelper receiptRepairHelper;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderCompletionService orderCompletionService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReturnHelper returnHelper;

    @Value("${market.checkouter.payments.unhold.hours:48}")
    private long unholdTimeoutHours;

    @BeforeEach
    public void setUp() {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
    }

    @Test
    public void testUpdatesReceiptOnOrderCancel() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        mockMvc.perform(post("/orders/{orderId}/status", order().getId())
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

        ReceiptTestHelper receiptTestHelper = new ReceiptTestHelper(this, null, null);
        receiptTestHelper.repairReceipts();

        inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, Matchers.empty());
    }

    @Test
    public void testUpdatesReceiptOnCancelledPaymentWithExpiredUnhold() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        mockMvc.perform(post("/orders/{orderId}/status", order().getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.STATUS, OrderStatus.CANCELLED.name())
                        .param(CheckouterClientParams.SUBSTATUS, OrderSubstatus.USER_CHANGED_MIND.name()))
                .andExpect(status().isOk());
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));
        final long receiptId = inconsistentReceipts.iterator().next().getId();

        trustMockConfigurer.resetRequests();
        trustMockConfigurer.mockCheckBasket(buildReversalWithRefundConfig(null));
        trustMockConfigurer.mockStatusBasket(buildReversalWithRefundConfig(null), null);

        jumpToFuture(unholdTimeoutHours, ChronoUnit.HOURS);
        receiptRepairHelper.repairReceipts();
        clearFixed();

        final Receipt receipt = receiptService.getReceipt(order().getId(), receiptId);
        assertNotNull(receipt);
        assertEquals(ReceiptStatus.FAILED, receipt.getStatus());
        assertEquals("Error: unhold was not completed successfully", receipt.getTrustPayload());
        inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, Matchers.empty());
    }

    @Test
    public void testUpdatesReceiptOnCancelledPaymentWithNewReceipt() throws Exception {
        shopMetaData.set(ShopSettingsHelper.createShopSettings(shopService, OrderProvider.SHOP_ID, getDefaultMeta()));
        Order prepaidOrder = OrderProvider.getPrepaidOrder(o -> {
            o.setRgb(Color.BLUE);
            o.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
            o.getItems().forEach(i -> i.setSupplierId(o.getShopId()));
            o.getItems().forEach(i -> i.setSupplierType(SupplierType.THIRD_PARTY));
        });
        long orderId = orderCreateService.createOrder(prepaidOrder, ClientInfo.SYSTEM);
        Order reservedOrder = orderUpdateService.reserveOrder(orderId, String.valueOf(orderId),
                prepaidOrder.getDelivery());
        orderCompletionService.completeOrder(reservedOrder, ClientInfo.SYSTEM);

        this.order.set(orderService.getOrder(orderId, ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES)));
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.notifyPaymentFailed(receiptId);

        Receipt receipt = receiptService.getReceipt(orderId, receiptId);
        assertThat(receipt.getStatus(), is(ReceiptStatus.FAILED));
        assertThat(receipt.getType(), is(ReceiptType.INCOME));
        assertThat(
                paymentService.getPayment(order().getPaymentId(), ClientInfo.SYSTEM).getStatus(),
                is(PaymentStatus.CANCELLED)
        );
        Integer updatedRows = transactionTemplate.execute(tc ->
                getRandomWritableJdbcTemplate().update(
                        "UPDATE RECEIPT SET STATUS = 0 WHERE ID = ?",
                        receiptId
                ));
        assertThat(updatedRows, is(1));
        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));

        receiptRepairHelper.repairReceipts();

        inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, Matchers.empty());
    }

    @Test
    public void testUpdatesReceiptForRefundedOrder() throws Exception {
        checkouterProperties.setEnableServicesPrepay(true);
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        orderUpdateService.updateOrderStatus(order.get().getId(), OrderStatus.DELIVERY);
        long refundId = refundTestHelper.makeFullRefund();
        Refund refund = refundService.getRefund(refundId);
        assertThat(refund.getStatus(), is(RefundStatus.SUCCESS));
        List<Receipt> refundReceipts = receiptService.findByOrder(order().getId())
                .stream()
                .filter(r -> r.getRefundId() != null)
                .collect(toList());
        Receipt receipt = Iterables.getOnlyElement(refundReceipts);
        assertThat(receipt.getStatus(), is(ReceiptStatus.PRINTED));
        assertThat(receipt.getType(), is(ReceiptType.INCOME_RETURN));
        Integer updatedRows = transactionTemplate.execute(ts ->
                getRandomWritableJdbcTemplate().update(
                        "UPDATE RECEIPT SET STATUS = 0 WHERE ID = ?",
                        receipt.getId()
                ));
        assertThat(updatedRows, is(1));
        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));

        receiptRepairHelper.repairReceipts();

        inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, Matchers.empty());
    }

    @Test
    public void updateOffsetAdvanceReceipts() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());

        // Сгенерируем чек на зачёт аванса
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));
        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, inconsistentReceipts.iterator().next().getType());

        trustMockConfigurer.resetRequests();
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                Iterables.getOnlyElement(order.getItems()).getBalanceOrderId()));
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                Iterables.getOnlyElement(order.getItems()).getBalanceOrderId()), null);

        // Act
        receiptRepairHelper.repairReceipts();

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
    @DisplayName("Только позиция доставки в чеке")
    public void updateOffsetAdvanceReceiptsDeliveryOnly() {
        final int shopIdWithFlag2 = 668;
        final long businessId = 668L;

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.configureMultiCart(multiCart -> multiCart.getCarts().forEach(
                o -> {
                    o.setShopId((long) shopIdWithFlag2);
                }
        ));

        ShopMetaDataBuilder shopBuilder = ShopMetaDataBuilder.createCopy(
                ShopSettingsHelper.createCustomNotFulfilmentMeta(shopIdWithFlag2, businessId));
        shopBuilder.withDeliveryReceiptNeedType(DONT_CREATE_DELIVERY_RECEIPT_EXCLUDE_DELIVERY);
        parameters.addShopMetaData((long) shopIdWithFlag2, shopBuilder.build());

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());

        // Сгенерируем чек на зачёт аванса
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));
        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, inconsistentReceipts.iterator().next().getType());

        trustMockConfigurer.resetRequests();
        // Для позиции доставки формируется DeliveredBasketLine.orderId = order.id + "-delivery"
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                order.getDelivery().getBalanceOrderId()));
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildOffsetAdvanceCheckBasket(
                order.getDelivery().getBalanceOrderId()), null);

        // Act
        receiptRepairHelper.repairReceipts();

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

        // Убедимся, что в позициях чека лишь доставка и что чек напечатан
        Receipt deliveryReceipt = receiptService.findByOrder(order.getId()).stream()
                .filter(i -> i.getType() == ReceiptType.OFFSET_ADVANCE_ON_DELIVERED)
                .findFirst()
                .orElse(null);
        assertNotNull(deliveryReceipt);
        assertTrue(deliveryReceipt.getItems().stream().allMatch(ReceiptItem::isDelivery));
        assertSame(deliveryReceipt.getStatus(), ReceiptStatus.PRINTED);
    }
    // TODO: Написать тест на бесплатную доставку

    @Test
    public void updateOffsetAdvanceReceiptsForMultiOrder() {
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

        trustMockConfigurer.resetRequests();
        String firstOrderItemBalanceOrderId = Iterables.getOnlyElement(firstOrder.getItems()).getBalanceOrderId();
        String secondOrderItemBalanceOrderId = Iterables.getOnlyElement(secondOrder.getItems()).getBalanceOrderId();
        trustMockConfigurer.mockCheckBasket(
                CheckBasketParams.buildOffsetAdvanceCheckBasket(
                        new ListOrderedMap<String, String>() {{
                            put(firstOrderItemBalanceOrderId, null);
                            put(secondOrderItemBalanceOrderId, "123");
                        }})
        );
        trustMockConfigurer.mockStatusBasket(
                CheckBasketParams.buildOffsetAdvanceCheckBasket(
                        new ListOrderedMap<String, String>() {{
                            put(firstOrderItemBalanceOrderId, null);
                            put(secondOrderItemBalanceOrderId, "123");
                        }}), null
        );
        // Act
        receiptRepairHelper.repairReceipts();

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
    public void shouldSkipOldBrokenReceipts() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        mockMvc.perform(post("/orders/{orderId}/status", order().getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.STATUS, OrderStatus.CANCELLED.name())
                        .param(CheckouterClientParams.SUBSTATUS, OrderSubstatus.USER_CHANGED_MIND.name()))
                .andExpect(status().isOk());
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        Collection<Receipt> inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS,
                RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, hasSize(1));

        setFixedTime(getClock().instant().plus(91, ChronoUnit.DAYS));

        inconsistentReceipts = receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertThat(inconsistentReceipts, empty());
    }
}
