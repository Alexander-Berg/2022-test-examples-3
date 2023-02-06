package ru.yandex.market.checkout.checkouter.storage.payment;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.builders.PrepayPaymentBuilder;
import ru.yandex.market.checkouter.jooq.tables.records.PaymentHistoryRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkouter.jooq.Tables.PAYMENT_HISTORY;

public class PaymentHistoryDaoTest extends AbstractWebTestBase {

    private static final BigDecimal TOTAL_AMOUNT = BigDecimal.valueOf(100.02);

    @Autowired
    PaymentHistoryDao paymentHistoryDao;
    @Autowired
    PaymentWritingDao paymentWritingDao;
    @Autowired
    DSLContext dsl;
    private Long paymentId;

    @BeforeEach
    public void setUp() {
        Payment payment = createPayment();
        paymentId = transactionTemplate.execute(e ->
                paymentWritingDao.insertPayment(ClientInfo.SYSTEM, payment)
        );
    }

    @Test
    public void shouldFindPaymentHistoryByPaymentId() {
        List<PaymentHistoryRecord> historyEntities = findByPaymentId(paymentId);
        assertEquals(historyEntities.size(), 1, "Can't load payment history. ");
    }

    @Test
    public void shouldLoadTotalAmountFromPaymentHistory() {
        List<PaymentHistoryRecord> historyEntities = findByPaymentId(paymentId);
        assertEquals(historyEntities.size(), 1, "Can't load payment history. ");
        assertEquals(historyEntities.get(0).getTotalAmount(), TOTAL_AMOUNT);
    }

    @Test
    public void shouldDeletePaymentHistoryById() {

        List<PaymentHistoryRecord> historyEntities = findByPaymentId(paymentId);
        assertEquals(historyEntities.size(), 1, "Can't load payment history. ");
        transactionTemplate.execute(e -> {
                    paymentHistoryDao.deletePaymentHistoryEvents(paymentId);
                    return null;
                }
        );
        historyEntities = findByPaymentId(paymentId);
        assertEquals(historyEntities.size(), 0, "Can't delete payment history. ");
    }


    private Result<PaymentHistoryRecord> findByPaymentId(Long paymentId) {
        Select<PaymentHistoryRecord> query = dsl.selectFrom(PAYMENT_HISTORY)
                .where(PAYMENT_HISTORY.PAYMENT_ID.eq(paymentId));

        return query.fetch();
    }

    private Payment createPayment() {
        final long id = System.currentTimeMillis();
        final PrepayPaymentBuilder paymentBuilder =
                new PrepayPaymentBuilder();
        final Date insertDate = new Date(System.currentTimeMillis() - DateUtil.HOUR);
        paymentBuilder.setPaymentId(id);
        paymentBuilder.setNow(insertDate);
        paymentBuilder.setExpirationSeconds(60);
        paymentBuilder.setFake(true);
        paymentBuilder.setCurrency(Currency.RUR);
        paymentBuilder.setAmount(TOTAL_AMOUNT);
        return paymentBuilder.toPayment();
    }
}
