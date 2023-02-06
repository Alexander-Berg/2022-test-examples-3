package ru.yandex.market.core.order;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.order.payment.OrderTransaction;
import ru.yandex.market.billing.order.payment.OrderTransactionStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet()
class DbOrderDaoTest extends FunctionalTest {

    @Autowired
    private DbOrderDao orderDao;

    @Test
    @DbUnitDataSet(after = "mbiControlEnabledTest.after.csv")
    void mbiControlEnabledTest() {
        OrderTransaction transaction = OrderTransaction.builder()
                .setOrderId(1L)
                .setTrustTransactiontId("trust")
                .setStatus(OrderTransactionStatus.NEW)
                .setCreatedAt(Instant.now())
                .setTrantime(Instant.now())
                .setEventtime(Instant.now())
                .setType(PaymentGoal.ORDER_PREPAY)
                .setUid(1L)
                .setTransactionType("payment")
                .setTransactionId(123L)
                .setMbiControlEnabled(true)
                .build();
        orderDao.storeOrderTransaction(transaction);
    }

    @Test
    @DbUnitDataSet(after = "trustTransactionIdTest.after.csv")
    void trustTransactionIdTest() {
        OrderTransaction paymentTransaction = OrderTransaction.builder()
                .setOrderId(1L)
                .setTrustTransactiontId(null)
                .setStatus(OrderTransactionStatus.NEW)
                .setCreatedAt(Instant.now())
                .setTrantime(Instant.now())
                .setEventtime(Instant.now())
                .setType(PaymentGoal.ORDER_ACCOUNT_PAYMENT)
                .setUid(1L)
                .setTransactionType("payment")
                .setTransactionId(123L)
                .setMbiControlEnabled(true)
                .build();
        OrderTransaction refundTransaction = OrderTransaction.builder()
                .setOrderId(1L)
                .setTrustTransactiontId(null)
                .setStatus(OrderTransactionStatus.NEW)
                .setCreatedAt(Instant.now())
                .setTrantime(Instant.now())
                .setEventtime(Instant.now())
                .setType(PaymentGoal.ORDER_ACCOUNT_PAYMENT)
                .setUid(1L)
                .setTransactionType("refund")
                .setTransactionId(123L)
                .setMbiControlEnabled(true)
                .build();

        orderDao.storeOrderTransaction(paymentTransaction);
        orderDao.storeOrderTransaction(refundTransaction);
    }

    @Test
    @DbUnitDataSet(before = "mbiControlEnabledFindTest.before.csv")
    void mbiControlEnabledFindTest() {
        List<OrderTransaction> transactions = orderDao.findTransactionsByOrderIds(List.of(4L, 5L));
        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().anyMatch(OrderTransaction::getMbiControlEnabled));
        assertTrue(transactions.stream().anyMatch(it -> !it.getMbiControlEnabled()));
    }

    @Test
    @DbUnitDataSet(before = "cessionTest.before.csv")
    void cessionFindTest() {
        List<OrderTransaction> transactions = orderDao.findTransactionsByOrderIds(List.of(4L, 5L));
        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().anyMatch(OrderTransaction::isCession));
        assertTrue(transactions.stream().anyMatch(it -> !it.isCession()));
    }

    @Test
    @DbUnitDataSet(before = "cessionTest.before.csv", after = "cessionTest.after.csv")
    void cessionTest() {
        List<OrderTransaction> transactions = orderDao.findTransactionsByOrderIds(List.of(4L, 5L));
        transactions.forEach(it -> it.setCession(!it.isCession()));
        transactions.forEach(it -> orderDao.storeOrderTransaction(it));
    }

}
