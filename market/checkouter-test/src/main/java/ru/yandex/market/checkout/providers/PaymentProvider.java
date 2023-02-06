package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;

import static ru.yandex.common.util.date.DateUtil.now;

public final class PaymentProvider {

    private static final long CLIENT_UID = 12345L;
    private static final AtomicLong PAYMENT_ID_SEQ = new AtomicLong(10000);

    private PaymentProvider() {
        throw new UnsupportedOperationException();
    }

    public static Payment createPayment(final long orderId) {
        Payment payment = new Payment();
        payment.setFake(true);
        payment.setId(PAYMENT_ID_SEQ.incrementAndGet());
        payment.setUid(CLIENT_UID);
        payment.setTotalAmount(BigDecimal.valueOf(100500L));
        payment.setOrderId(orderId);
        payment.setStatus(PaymentStatus.HOLD);
        payment.setCurrency(Currency.RUR);
        payment.setType(PaymentGoal.SUBSIDY);
        payment.setPrepayType(PrepayType.YANDEX_MARKET);
        payment.setCreationDate(now());
        payment.setUpdateDate(now());
        payment.setStatusUpdateDate(now());
        return payment;
    }

    public static Payment createPaymentWithExpiryDate(final long orderId, LocalDateTime expiry) {
        Payment payment = createPayment(orderId);
        payment.setStatusExpiryDate(
                Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant())
        );
        return payment;
    }

    public static Payment createCancelledPaymentWithExpiryDate(final long orderId, LocalDateTime expiry) {
        Payment payment = createPaymentWithExpiryDate(orderId, expiry);
        payment.setStatus(PaymentStatus.CANCELLED);
        return payment;
    }
}
