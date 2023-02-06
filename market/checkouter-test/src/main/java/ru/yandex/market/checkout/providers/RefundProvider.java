package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;

import static ru.yandex.common.util.date.DateUtil.now;

public final class RefundProvider {

    public static final long CLIENT_ID = 12345L;
    private static final long DEFAULT_PAYMENT_ID = 100L;
    private static final long DEFAULT_ORDER_ID = 1L;
    private static final AtomicLong REFUND_ID_SEQ = new AtomicLong(20000);

    private RefundProvider() {
        throw new UnsupportedOperationException();
    }

    public static Refund createRefund() {
        Refund refund = new Refund();
        refund.setFake(true);
        refund.setId(REFUND_ID_SEQ.incrementAndGet());
        refund.setOrderId(DEFAULT_ORDER_ID);
        refund.setPaymentId(DEFAULT_PAYMENT_ID);
        refund.setStatus(RefundStatus.SUCCESS);
        refund.setAmount(BigDecimal.valueOf(100500L));
        refund.setOrderRemainder(BigDecimal.valueOf(10L));
        refund.setCreatedBy(CLIENT_ID);
        refund.setCreatedByRole(ClientRole.SHOP_USER);
        refund.setCurrency(Currency.RUR);
        refund.setCreationDate(now());
        refund.setUpdateDate(now());
        refund.setStatusUpdateDate(now());
        refund.setReason(RefundReason.ORDER_CANCELLED);
        refund.setOrderItemsChanged(false);
        return refund;
    }

    public static Refund createRefundWithExpiryDate(LocalDateTime expiry) {
        Refund refund = createRefund();
        refund.setStatusExpiryDate(
                Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant())
        );
        return refund;
    }
}
