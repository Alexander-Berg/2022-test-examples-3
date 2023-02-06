package ru.yandex.market.core.order;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.model.ReceiptItem;
import ru.yandex.market.core.order.payment.OrderTransaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.comparesEqualTo;

@DbUnitDataSet(before = {"db/datasource.csv", "db/supplier.csv", "db/deliveryTypes.csv"})
public class ReceiptItemDaoTest extends FunctionalTest {
    @Autowired
    private ReceiptItemDao receiptItemDao;

    @Test
    @DbUnitDataSet(before = "ReceiptItemDaoTest.before.csv",
            after = "ReceiptItemDaoTest.mergeReceiptItems.after.csv")
    void testMergeReceiptItems() {
        receiptItemDao.mergeReceiptItems(Arrays.asList(createTestPaymentReceiptItem(),
                createTestRefundReceiptItem(),
                createTestSpasiboReceiptItem(),
                createTestCashbackReceiptItem()));
    }

    private ReceiptItem createTestPaymentReceiptItem() {
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setItemId(200L);
        receiptItem.setPaymentId(100L);
        receiptItem.setCount(3);
        receiptItem.setPrice(BigDecimal.valueOf(10.1));
        receiptItem.setAmount(BigDecimal.valueOf(30.3));

        return receiptItem;
    }

    private ReceiptItem createTestRefundReceiptItem() {
        ReceiptItem receiptItemRefund = new ReceiptItem();
        receiptItemRefund.setItemId(201L);
        receiptItemRefund.setRefundId(100L);
        receiptItemRefund.setCount(2);
        receiptItemRefund.setPrice(BigDecimal.valueOf(10.1));
        receiptItemRefund.setAmount(BigDecimal.valueOf(20.2));

        return receiptItemRefund;
    }

    private ReceiptItem createTestRefundReceiptItem1() {
        ReceiptItem receiptItemRefund = new ReceiptItem();
        receiptItemRefund.setItemId(202L);
        receiptItemRefund.setOrderId(1L);
        receiptItemRefund.setRefundId(200L);
        receiptItemRefund.setCount(3);
        receiptItemRefund.setPrice(BigDecimal.valueOf(20.1));
        receiptItemRefund.setAmount(BigDecimal.valueOf(60.3));

        return receiptItemRefund;
    }

    private ReceiptItem createTestSpasiboReceiptItem() {
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setItemId(205L);
        receiptItem.setOrderId(2L);
        receiptItem.setPaymentId(102L);
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
        receiptItem.setPaymentId(103L);
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
        receiptItem.setPaymentId(100L);
        receiptItem.setCount(1);
        receiptItem.setPrice(BigDecimal.valueOf(599.0));
        receiptItem.setAmount(BigDecimal.valueOf(599.0));

        return receiptItem;
    }

    @Test
    @DbUnitDataSet(before = {"ReceiptItemDaoTest.before.csv",
            "ReceiptItemDaoTest.mergeReceiptItems.after.csv"})
    void testGetOrderItemTransactionLinks() {
        assertThat(receiptItemDao.getReceiptItems(Collections.emptyList()), empty());

        OrderTransaction paymentTransaction = new OrderTransaction();
        paymentTransaction.setPaymentId(100L);

        OrderTransaction refundTransaction = new OrderTransaction();
        refundTransaction.setRefundId(100L);

        List<ReceiptItem> links = receiptItemDao.getReceiptItems(
                Arrays.asList(paymentTransaction, refundTransaction, new OrderTransaction()));

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
            assertThat("Count check failed", receiptItem.getOrderId(), is(receiptItemLink.getOrderId()));
        }
    }

    @Test
    @DbUnitDataSet(before = {"ReceiptItemDaoTest.before.csv", "ReceiptItemDaoTest.deleteReceiptItems.before.csv"},
            after = "ReceiptItemDaoTest.deleteReceiptItems.after.csv")
    void testDeleteReceiptItems() {
        receiptItemDao.deleteReceiptItems(100, Collections.singletonList(200L), List.of());
        receiptItemDao.deleteReceiptItems(103, Collections.singletonList(206L), List.of());
        receiptItemDao.deleteReceiptItems(104, List.of(), Collections.singletonList(6L));
    }

    @Test
    @DbUnitDataSet(before = {"ReceiptItemDaoTest.before.csv", "ReceiptItemDaoTest.mergeReceiptItems.after.csv"},
            after = "ReceiptItemDaoTest.mergeReceiptItemsWithRefunds.after.csv")
    void testMergeReceiptItemsWithRefunds() {
        receiptItemDao.mergeReceiptItems(Arrays.asList(createTestPaymentReceiptItem(),
                createTestRefundReceiptItem(), createTestRefundReceiptItem1()));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "ReceiptItemDaoTest.before.csv",
                    "ReceiptItemDaoTest.mergeReceiptItemsWithDifferentDeliveryId.before.csv"
            },
            after = "ReceiptItemDaoTest.mergeReceiptItemsWithDifferentDeliveryId.after.csv"
    )
    void testMergeReceiptItemsWithDifferentDeliveryId() {
        receiptItemDao.mergeReceiptItems(List.of(createTestDeliveryReceiptItem()));
    }

    @Test
    @DbUnitDataSet(before = "ReceiptItemDaoTest.selectReceiptItems.before.csv")
    void testGetReceiptItemsByOrderItems() {
        ReceiptItem item1 = new ReceiptItem();
        item1.setItemId(201L);
        item1.setRefundId(100L);
        item1.setCount(2);
        item1.setPrice(BigDecimal.valueOf(10.1));
        item1.setAmount(BigDecimal.valueOf(20.2));

        ReceiptItem item2 = new ReceiptItem();
        item2.setItemId(206L);
        item2.setPaymentId(103L);
        item2.setCount(5);
        item2.setPrice(BigDecimal.valueOf(15.1));
        item2.setAmount(BigDecimal.valueOf(75.5));
        item2.setCashback(BigDecimal.valueOf(15.1));

        assertThat(receiptItemDao.getReceiptItemsByOrderItemIds(List.of(201L, 206L)), containsInAnyOrder(item1, item2));
    }
}
