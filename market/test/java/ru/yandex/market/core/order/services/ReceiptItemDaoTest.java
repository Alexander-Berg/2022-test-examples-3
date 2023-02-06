package ru.yandex.market.core.order.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.order.payment.OrderTransaction;
import ru.yandex.market.billing.order.payment.OrderTransactionStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.ReceiptItemDao;
import ru.yandex.market.core.order.model.ReceiptItem;
import ru.yandex.market.core.payment.TransactionType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@DbUnitDataSet(before = "ReceiptItemDaoTest.before.csv")
public class ReceiptItemDaoTest extends FunctionalTest {
    @Autowired
    private ReceiptItemDao receiptItemDao;

    @Test
    @DbUnitDataSet(after = "ReceiptItemDaoTest.mergeReceiptItems.after.csv")
    void testMergeReceiptItems() {
        receiptItemDao.mergeReceiptItems(
                Arrays.asList(createTestPaymentReceiptItem(),
                createTestRefundReceiptItem(),
                createTestSpasiboReceiptItem(),
                createTestCashbackReceiptItem())
        );
    }

    private ReceiptItem createTestPaymentReceiptItem() {
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setItemId(200L);
        receiptItem.setTransactionId(100L);
        receiptItem.setTransactionType(TransactionType.PAYMENT.getId());
        receiptItem.setCount(3);
        receiptItem.setPrice(BigDecimal.valueOf(10.1));
        receiptItem.setAmount(BigDecimal.valueOf(30.3));

        return receiptItem;
    }

    private ReceiptItem createTestRefundReceiptItem() {
        ReceiptItem receiptItemRefund = new ReceiptItem();
        receiptItemRefund.setItemId(201L);
        receiptItemRefund.setTransactionId(104L);
        receiptItemRefund.setTransactionType(TransactionType.REFUND.getId());
        receiptItemRefund.setCount(2);
        receiptItemRefund.setPrice(BigDecimal.valueOf(10.1));
        receiptItemRefund.setAmount(BigDecimal.valueOf(20.2));

        return receiptItemRefund;
    }

    private ReceiptItem createTestRefundReceiptItem1() {
        ReceiptItem receiptItemRefund = new ReceiptItem();
        receiptItemRefund.setItemId(200L);
        receiptItemRefund.setOrderId(1L);
        receiptItemRefund.setTransactionId(100L);
        receiptItemRefund.setTransactionType(TransactionType.REFUND.getId());
        receiptItemRefund.setCount(3);
        receiptItemRefund.setPrice(BigDecimal.valueOf(20.1));
        receiptItemRefund.setAmount(BigDecimal.valueOf(60.3));

        return receiptItemRefund;
    }

    private ReceiptItem createTestSpasiboReceiptItem() {
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setItemId(205L);
        receiptItem.setOrderId(2L);
        receiptItem.setTransactionId(102L);
        receiptItem.setTransactionType(TransactionType.PAYMENT.getId());
        receiptItem.setCount(3);
        receiptItem.setPrice(BigDecimal.valueOf(10.1));
        receiptItem.setAmount(BigDecimal.valueOf(30.3));
        receiptItem.setSpasibo(BigDecimal.valueOf(10.1));

        return receiptItem;
    }

    private ReceiptItem createTestCashbackReceiptItem() {
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setItemId(206L);
        receiptItem.setOrderId(2L);
        receiptItem.setTransactionId(103L);
        receiptItem.setTransactionType(TransactionType.PAYMENT.getId());
        receiptItem.setCount(5);
        receiptItem.setPrice(BigDecimal.valueOf(15.1));
        receiptItem.setAmount(BigDecimal.valueOf(75.5));
        receiptItem.setCashback(BigDecimal.valueOf(15.1));

        return receiptItem;
    }

    private ReceiptItem createTestDeliveryReceiptItem() {
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setDeliveryId(301L);
        receiptItem.setOrderId(1L);
        receiptItem.setTransactionId(100L);
        receiptItem.setTransactionType(TransactionType.PAYMENT.getId());
        receiptItem.setCount(1);
        receiptItem.setPrice(BigDecimal.valueOf(599.0));
        receiptItem.setAmount(BigDecimal.valueOf(599.0));

        return receiptItem;
    }

    @Test
    @DbUnitDataSet(before = "ReceiptItemDaoTest.mergeReceiptItems.after.csv")
    void testGetOrderItemTransactionLinks() {
        assertThat(receiptItemDao.getReceiptItems(Collections.emptyList()), empty());

        List<ReceiptItem> links = receiptItemDao.getReceiptItems(
                Arrays.asList(
                        OrderTransaction.builder()
                                .setType(PaymentGoal.ORDER_PREPAY)
                                .setStatus(OrderTransactionStatus.DONE)
                                .setTrantime(Instant.now())
                                .setEventtime(Instant.now())
                                .setTransactionId(100L)
                                .setTransactionType(TransactionType.PAYMENT.getId())
                                .build(),
                        OrderTransaction.builder()
                                .setType(PaymentGoal.TINKOFF_CREDIT)
                                .setStatus(OrderTransactionStatus.DONE)
                                .setTrantime(Instant.now())
                                .setEventtime(Instant.now())
                                .setTransactionId(104L)
                                .setTransactionType(TransactionType.REFUND.getId())
                                .build(),
                        OrderTransaction.builder()
                                .setType(PaymentGoal.ORDER_POSTPAY)
                                .setStatus(OrderTransactionStatus.CANCELLED)
                                .setTrantime(Instant.now())
                                .setEventtime(Instant.now())
                                .setTransactionId(111L)
                                .setTransactionType(TransactionType.PAYMENT.getId())
                                .build())
        );

        ReceiptItem[] receiptItems = new ReceiptItem[2];
        receiptItems[0] = createTestPaymentReceiptItem();
        receiptItems[1] = createTestRefundReceiptItem();

        assertThat(links, containsInAnyOrder(receiptItems[0],
                receiptItems[1]));

        for (ReceiptItem receiptItem : receiptItems) {
            ReceiptItem receiptItemLink = links.get(links.indexOf(receiptItem));
            assertThat("Count check failed", receiptItem.getCount(), is(receiptItemLink.getCount()));
            assertThat("Price check failed", receiptItem.getPrice(), comparesEqualTo(receiptItemLink.getPrice()));
            assertThat("Amount check failed", receiptItem.getAmount(),
                    comparesEqualTo(receiptItemLink.getAmount()));
            assertThat("Spasibo check failed", receiptItem.getSpasibo(),
                    equalTo(receiptItemLink.getSpasibo()));
            assertThat("Cashback check failed", receiptItem.getCashback(),
                    equalTo(receiptItemLink.getCashback()));
            assertThat("Order id check failed", receiptItem.getOrderId(), is(receiptItemLink.getOrderId()));
        }
    }

    @Test
    @DbUnitDataSet(before = "ReceiptItemDaoTest.mergeReceiptItems.after.csv",
                   after = "ReceiptItemDaoTest.deleteReceiptItems.after.csv")
    void testDeleteReceiptItems() {
        receiptItemDao.deleteReceiptItems(
                100,
                TransactionType.PAYMENT,
                Collections.singletonList(200L),
                List.of()
        );
        receiptItemDao.deleteReceiptItems(
                104,
                TransactionType.REFUND,
                Collections.singletonList(201L),
                List.of()
        );
    }

    @Test
    @DbUnitDataSet(before = "ReceiptItemDaoTest.mergeReceiptItemsWithRefunds.before.csv",
                   after = "ReceiptItemDaoTest.mergeReceiptItemsWithRefunds.after.csv")
    void testMergeReceiptItemsWithRefunds() {
        receiptItemDao.mergeReceiptItems(Arrays.asList(createTestPaymentReceiptItem(),
                createTestRefundReceiptItem(), createTestRefundReceiptItem1()));
    }

    @Test
    @DbUnitDataSet(
            before = "ReceiptItemDaoTest.mergeReceiptItemsWithDifferentDeliveryId.before.csv",
            after = "ReceiptItemDaoTest.mergeReceiptItemsWithDifferentDeliveryId.after.csv"
    )
    void testMergeReceiptItemsWithDifferentDeliveryId() {
        receiptItemDao.mergeReceiptItems(List.of(createTestDeliveryReceiptItem()));
    }
}
