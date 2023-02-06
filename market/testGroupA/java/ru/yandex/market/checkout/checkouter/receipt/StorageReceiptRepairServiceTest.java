package ru.yandex.market.checkout.checkouter.receipt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.storage.receipt.ReceiptDao;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class StorageReceiptRepairServiceTest extends AbstractWebTestBase {

    private static final int NOTIFICATION_TIMEOUT_HOURS = 48;
    private static final int RECEIPT_CHECK_DAYS = 90;

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ReceiptRepairService receiptRepairService;
    @Autowired
    private ReceiptDao receiptDao;
    @Value("${market.checkouter.payments.unhold.hours:48}")
    private long unholdTimeoutHours;
    @Autowired
    private OrderHistoryEventsTestHelper orderHistoryEventsTestHelper;

    private Order order;

    @BeforeEach
    public void createOrder() {
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderPayHelper.pay(order.getId());
        order = orderService.getOrder(order.getId());
    }

    @Test
    public void testNotThrow() {
        Receipt receiptClone = receiptService.findByOrder(order.getId()).get(0);
        Payment payment = order.getPayment();
        transactionTemplate.execute(ts -> {
            setPaymentStatusInProgress(payment.getId());
            receiptDao.insert(receiptClone);
            return null;
        });

        List<Receipt> inconsistentReceipts = new ArrayList<>(
                receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS));
        assertEquals(2, inconsistentReceipts.size());

        // Проверяем, что метод repairReceipt не выбрасывает исключение при наличии дубликатов чеков
        assertFalse(receiptRepairService.repairReceiptSilently(inconsistentReceipts.get(1).getId()));
        assertFalse(receiptRepairService.repairReceiptSilently(inconsistentReceipts.get(0).getId()));
    }

    @DisplayName("Проверяем, что генерируется событие RECEIPT_PRINTED при починке чека")
    @Test
    public void testReceiptPrintedFromRepair() {
        Long orderId = order.getId();
        Payment payment = order.getPayment();

        ReceiptItem receiptItem = new ReceiptItem(orderId);
        receiptItem.setAmount(BigDecimal.ONE);
        receiptItem.setCount(1);
        receiptItem.setItemId(12345L);
        receiptItem.setItemTitle("woop");
        receiptItem.setPrice(BigDecimal.ONE);
        Receipt receipt = new Receipt();
        receipt.setStatus(ReceiptStatus.NEW);
        receipt.setPaymentId(payment.getId());
        receipt.setType(ReceiptType.INCOME_RETURN);
        receipt.setItems(Collections.singletonList(receiptItem));
        long receiptId = receiptService.createReceipt(receipt, orderId);

        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildReversalConfig(), null);

        transactionTemplate.execute(ts -> {
            setPaymentStatus(payment.getId(), PaymentStatus.HOLD);
            return null;
        });

        var orderHistoryEventsBefore = orderHistoryEventsTestHelper.getEventsOfType(orderId,
                HistoryEventType.RECEIPT_PRINTED);
        assertEquals(0, orderHistoryEventsBefore.size());

        receiptRepairService.repairReceiptSilently(receiptId);

        var orderHistoryEventsAfter = orderHistoryEventsTestHelper.getEventsOfType(orderId,
                HistoryEventType.RECEIPT_PRINTED);
        assertEquals(1, orderHistoryEventsAfter.size());
        assertEquals(receiptId, orderHistoryEventsAfter.iterator().next().getReceipt().getId());


    }

    @Test
    public void testFailDuplicateReceipts() {
        Receipt receiptClone = receiptService.findByOrder(order.getId()).get(0);
        Payment payment = order.getPayment();
        transactionTemplate.execute(ts -> {
            setPaymentStatusInProgress(payment.getId());
            receiptDao.insert(receiptClone);
            setReceiptStatus(receiptClone.getId(), ReceiptStatus.PRINTED);
            return null;
        });

        List<Receipt> inconsistentReceipts = new ArrayList<>(
                receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS));
        assertEquals(1, inconsistentReceipts.size());

        long duplicateReceiptId = inconsistentReceipts.iterator().next().getId();
        receiptRepairService.repairReceiptSilently(duplicateReceiptId);
        Receipt duplicateReceipt = receiptService.getReceipt(duplicateReceiptId);
        assertEquals(ReceiptStatus.FAILED, duplicateReceipt.getStatus());

        Collection<Receipt> inconsistentReceiptsAfter =
                receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertEquals(0, inconsistentReceiptsAfter.size());
    }

    @Test
    public void testNotFailReceiptsFromDifferentOrders() {
        Receipt receiptClone = receiptService.findByOrder(order.getId()).get(0);
        List<ReceiptItem> receiptItems = receiptClone.getItems();
        Order order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        assertNotEquals(order.getId(), order2.getId());
        receiptItems.get(0).setOrderId(order2.getId());
        receiptClone.setItems(receiptItems);
        Payment payment = order.getPayment();
        transactionTemplate.execute(ts -> {
            setPaymentStatusInProgress(payment.getId());
            receiptDao.insert(receiptClone);
            setReceiptStatus(receiptClone.getId(), ReceiptStatus.PRINTED);
            return null;
        });

        List<Receipt> inconsistentReceipts = new ArrayList<>(
                receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS));
        assertEquals(1, inconsistentReceipts.size());

        long duplicateReceiptId = inconsistentReceipts.iterator().next().getId();
        receiptRepairService.repairReceiptSilently(duplicateReceiptId);
        Receipt duplicateReceipt = receiptService.getReceipt(duplicateReceiptId);
        assertEquals(ReceiptStatus.NEW, duplicateReceipt.getStatus());

        Collection<Receipt> inconsistentReceiptsAfter =
                receiptService.getInconsistentReceipts(NOTIFICATION_TIMEOUT_HOURS, RECEIPT_CHECK_DAYS);
        assertEquals(1, inconsistentReceiptsAfter.size());
    }

    private void setPaymentStatusInProgress(long paymentId) {
        masterJdbcTemplate.update(
                "UPDATE payment SET status = ? WHERE id = ?", PaymentStatus.CLEARED.getId(), paymentId);
    }

    private void setPaymentStatus(long paymentId, PaymentStatus status) {
        masterJdbcTemplate.update(
                "UPDATE payment SET status = ? WHERE id = ?", status.getId(), paymentId);
    }

    private void setReceiptStatus(long receiptId, ReceiptStatus status) {
        masterJdbcTemplate.update(
                "UPDATE receipt SET status = ? WHERE id = ?", status.getId(), receiptId);
    }
}
