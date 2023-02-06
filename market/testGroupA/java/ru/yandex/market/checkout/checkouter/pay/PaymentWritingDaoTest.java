package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.pay.builders.PrepayPaymentBuilder;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentReadingDao;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentWritingDao;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentWritingDaoTest extends AbstractServicesTestBase {

    @Autowired
    PaymentWritingDao paymentWritingDao;
    @Autowired
    PaymentReadingDao paymentReadingDao;

    @Test
    public void updateStatusTest() {
        ClientInfo author = ClientInfo.SYSTEM;
        Payment payment = insertPaymentInDb(author);

        //updatePayment
        Date updateDate = new Date();
        payment.setUpdateDate(updateDate);
        payment.setStatusUpdateDate(updateDate);
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setSubstatus(PaymentSubstatus.INIT_EXPIRED);
        payment.setStatusExpiryDate(null);
        payment.setFailReason("test updateDate");
        payment.setFailDescription("test updateDate");
        payment.setBalancePayMethodType("card");
        payment.setCardNumber("500000****0009");
        payment.setPaymentSystem("MASTERCARD");
        transactionTemplate.execute(ts -> {
            paymentWritingDao.updateStatus(payment, author);
            return null;
        });

        checkPayment(paymentReadingDao, payment);
    }

    @Test
    public void updatePaymentPartitionTest() {
        ClientInfo author = ClientInfo.SYSTEM;
        Payment payment = insertPaymentInDb(author);
        BigDecimal spasiboAmount = BigDecimal.TEN.subtract(BigDecimal.ONE);
        payment.addPartition(new PaymentPartition(PaymentAgent.SBER_SPASIBO, spasiboAmount));
        payment.addPartition(new PaymentPartition(
                PaymentAgent.DEFAULT,
                payment.getTotalAmount().subtract(spasiboAmount)
        ));

        transactionTemplate.execute(ts -> {
            paymentWritingDao.updatePaymentPartition(payment, author);
            return null;
        });

        checkPayment(paymentReadingDao, payment);
    }

    public Payment insertPaymentInDb(ClientInfo author) {
        //insertPayment
        final long paymentId = System.currentTimeMillis();
        final PrepayPaymentBuilder p =
                new PrepayPaymentBuilder();
        final Date insertDate = new Date(System.currentTimeMillis() - DateUtil.HOUR);
        p.setPaymentId(paymentId);
        p.setNow(insertDate);
        p.setUid(author.getId());
        p.setExpirationSeconds(60);
        p.setFake(true);
        p.setCurrency(Currency.RUR);
        p.setAmount(BigDecimal.TEN);
        Payment payment = p.toPayment();
        payment.setMbiControlEnabled(true);
        payment.setPaymentSystem("mastercard");
        transactionTemplate.execute(ts -> {
            paymentWritingDao.insertPayment(author, payment);
            return null;
        });
        checkPayment(paymentReadingDao, payment);
        return payment;
    }

    private void checkPayment(PaymentReadingDao paymentReadingDao, Payment payment) {
        Payment paymentfromDB = paymentReadingDao.loadPayment(payment.getId());
        assertEquals(paymentfromDB.getId(), payment.getId());
        assertEquals(paymentfromDB.getStatus(), payment.getStatus());
        assertEquals(paymentfromDB.getSubstatus(), payment.getSubstatus());
        assertEquals(paymentfromDB.getFailReason(), payment.getFailReason());
        assertEquals(paymentfromDB.getFailDescription(), payment.getFailDescription());
        assertEquals(paymentfromDB.getStatusUpdateDate().getTime() / 1000,
                payment.getStatusUpdateDate().getTime() / 1000);
        assertEquals(paymentfromDB.getUpdateDate().getTime() / 1000, payment.getUpdateDate().getTime() / 1000);
        assertEquals(paymentfromDB.getBalancePayMethodType(), payment.getBalancePayMethodType());
        assertEquals(paymentfromDB.getCardNumber(), payment.getCardNumber());
        assertEquals(paymentfromDB.getMbiControlEnabled(), payment.getMbiControlEnabled());
        assertEquals(paymentfromDB.getPaymentSystem(), payment.getPaymentSystem());
        BigDecimal spasiboAmount = payment.amountByAgent(PaymentAgent.SBER_SPASIBO);
        if (spasiboAmount != null) {
            assertEquals(
                    BigDecimal.ZERO,
                    paymentfromDB.amountByAgent(PaymentAgent.SBER_SPASIBO)
                            .subtract(spasiboAmount)
                            .setScale(0, BigDecimal.ROUND_HALF_EVEN)
            );
        }
    }
}
