package ru.yandex.market.checkout.checkouter.storage.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PaymentReadingDaoTest extends AbstractServicesTestBase {

    @Autowired
    private PaymentReadingDao paymentReadingDao;
    @Autowired
    private PaymentService paymentService;

    @Test
    public void loadPaymentsByOrderIdForMissingOrder() {
        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(-1, PaymentGoal.MARKET_COMPENSATION);

        assertTrue(payments.isEmpty());
    }

    @Test
    public void getApproximatelyMinPaymentIdShouldReturnNullOnEmptyDatabase() {
        final Long result = paymentReadingDao.getApproximatelyMinPaymentId(LocalDateTime.now(getClock()));
        assertNull(result);
    }

    @Test
    public void getApproximatelyMinPaymentIdShouldReturnNotNullValue() {
        final Long paymentId = createPayment();
        assertNotNull(paymentId);

        final Long result = paymentReadingDao.getApproximatelyMinPaymentId(LocalDateTime.now(getClock()).minusDays(10));
        assertNotNull(result);
        assertEquals(paymentId, result);
    }

    @Test
    public void loadPaymentsNotSyncedWithBillingShouldNotFailOnEmptyDatabase() {
        final Collection<Payment> payments = paymentReadingDao.loadPaymentsNotSyncedWithBilling(
                List.of(PaymentStatus.INIT, PaymentStatus.CLEARED), 0, Long.MAX_VALUE, 10, null);
        assertNotNull(payments);
        assertEquals(0, payments.size());
    }

    @Test
    public void loadPaymentsNotSyncedWithBillingShouldReturnValue() {
        final Long paymentId = createPayment();
        assertNotNull(paymentId);

        final Collection<Payment> payments = paymentReadingDao.loadPaymentsNotSyncedWithBilling(
                List.of(PaymentStatus.INIT, PaymentStatus.CLEARED), 0, Long.MAX_VALUE, 10, null);
        assertNotNull(payments);
        assertEquals(1, payments.size());
        assertEquals(paymentId, payments.iterator().next().getId());
    }

    private Long createPayment() {
        final Payment payment = new Payment();
        final Date date = Date.from(Instant.now(getClock()));
        payment.setStatus(PaymentStatus.INIT);
        payment.setCreationDate(date);
        payment.setUpdateDate(date);
        payment.setStatusUpdateDate(date);
        payment.setUid(1L);
        payment.setCurrency(Currency.RUR);
        payment.setTotalAmount(BigDecimal.TEN);
        payment.setFake(false);
        payment.setType(PaymentGoal.ORDER_PREPAY);
        payment.setPrepayType(PrepayType.YANDEX_MARKET);
        payment.setBasketKey(new TrustBasketKey("basketId", "purchaseToken"));
        paymentService.insertPayment(payment, ClientInfo.SYSTEM, List.of());
        return payment.getId();
    }
}
