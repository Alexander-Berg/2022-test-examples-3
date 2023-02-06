package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentWritingDao;
import ru.yandex.market.checkout.checkouter.storage.payment.RefundWritingDao;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @author : poluektov
 * date: 2019-12-12.
 */
public class ReceiptRestoreTest extends AbstractServicesTestBase {

    @Autowired
    public RefundWritingDao refundWritingDao;

    @Autowired
    public OrderInsertHelper orderInsertHelper;

    @Autowired
    public ReceiptRepairUtil receiptRepairUtil;

    @Autowired
    public PaymentWritingDao paymentWritingDao;

    @Autowired
    private Clock clock;

    @Test
    public void testRestoreFromFullRefund() {
        final long refundId = 4773L;

        long orderId = orderInsertHelper.insertOrder(OrderProvider.getBlueOrder());
        final Order order = orderService.getOrder(orderId);

        order.setId(orderId);


        Payment payment = insertPayment(order);
        insertRefund(refundId, order, payment);

        Receipt receipt = receiptRepairUtil.restoreRefundReceipt(refundId);
        List<ReceiptItem> items = receipt.getItems();
        assertThat(items, hasSize(2));
        assertThat(receipt.getRefundId(), notNullValue());
        assertThat(receipt.getType(), equalTo(ReceiptType.INCOME_RETURN));
        assertThat(receipt.getStatus(), equalTo(ReceiptStatus.GENERATED));
    }

    private Payment insertPayment(Order order) {
        Instant now = clock.instant();
        long newPaymentId = paymentWritingDao.getPaymentSequences().getNextPaymentId();
        Payment payment = new Payment();
        payment.setId(newPaymentId);
        payment.setOrderId(order.getId());
        payment.setTotalAmount(order.getBuyerTotal());
        payment.setCurrency(Currency.RUR);
        payment.setCreationDate(Date.from(now));
        payment.setUpdateDate(Date.from(now));
        payment.setStatusUpdateDate(Date.from(now));
        payment.setStatus(PaymentStatus.CLEARED);
        transactionTemplate.execute(ts -> {
            paymentWritingDao.insertPayment(ClientInfo.SYSTEM, payment);
            return null;
        });
        return payment;
    }

    private void insertRefund(long refundId, Order order, Payment payment) {
        Instant now = clock.instant();
        Refund refund = new Refund();
        refund.setId(refundId);
        refund.setAmount(order.getBuyerTotal());
        refund.setOrderId(order.getId());
        refund.setCurrency(Currency.RUR);
        refund.setOrderRemainder(BigDecimal.ZERO);
        refund.setCreationDate(Date.from(now));
        refund.setUpdateDate(Date.from(now));
        refund.setStatusUpdateDate(Date.from(now));
        refund.setPaymentId(payment.getId());
        refund.setStatus(RefundStatus.SUCCESS);
        transactionTemplate.execute(ts -> {
            refundWritingDao.insertRefund(refund, ClientInfo.SYSTEM);
            return null;
        });
    }
}
