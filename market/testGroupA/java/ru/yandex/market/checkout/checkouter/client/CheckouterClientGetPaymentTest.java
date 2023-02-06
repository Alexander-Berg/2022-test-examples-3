package ru.yandex.market.checkout.checkouter.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentWritingDao;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author ungomma
 */
public class CheckouterClientGetPaymentTest extends AbstractWebTestBase {

    @Autowired
    private PaymentWritingDao paymentWritingDao;
    @Autowired
    @Qualifier("transactionTemplate")
    private TransactionTemplate txTemplate;
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Test
    public void canGetPayment() {
        long orderId = orderServiceHelper.createGlobalOrder();
        Order order = orderService.getOrder(orderId);

        Payment payment = client.payments().getPayment(order.getPaymentId(), ClientRole.SYSTEM, order.getShopId());
        assertEquals(PaymentStatus.HOLD, payment.getStatus());
        assertEquals(order.getPayment().getId(), payment.getId());
        assertNull(payment.getFailReason());
        assertNull(payment.getCardNumber());
        assertNull(payment.getMaskedCardNumber());
    }

    @Test
    public void canGetPaymentWithCard() {
        long orderId = orderServiceHelper.createGlobalOrder();
        Order order = orderService.getOrder(orderId);

        Payment originalPayment = order.getPayment();
        originalPayment.setCardNumber("123456**3536");
        txTemplate.execute(txStatus -> {
            paymentWritingDao.updateStatus(originalPayment, ClientInfo.SYSTEM);
            return null;
        });

        Payment payment = client.payments().getPaymentWithCardNumber(order.getPaymentId(), ClientRole.SYSTEM,
                order.getShopId());
        assertEquals(PaymentStatus.HOLD, payment.getStatus());
        assertEquals(order.getPayment().getId(), payment.getId());
        assertNull(payment.getFailReason());
        assertNull(payment.getCardNumber());
        assertNotNull(payment.getMaskedCardNumber());
    }
}
