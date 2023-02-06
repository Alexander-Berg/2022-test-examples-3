package ru.yandex.market.checkout.checkouter.tasks.balance;

import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.storage.OrderEntityGroup;
import ru.yandex.market.checkout.storage.Storage;

import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

public class PaymentsStatusCheckerTaskIntegrationTest extends AbstractPaymentTestBase {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private Storage storage;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void shouldNotUpdatePaymentStatusIfNotConfirmed() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        orderUpdateService.updateOrderStatus(order().getId(), OrderStatus.DELIVERY);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        List<Payment> payments = paymentService.getPayments(
                order().getId(), ClientInfo.SYSTEM, PaymentGoal.ORDER_PREPAY);
        Payment payment = Iterables.getOnlyElement(payments);

        Assertions.assertEquals(payment.getStatus(), PaymentStatus.CLEARED);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(null);
        trustMockConfigurer.mockStatusBasket(null, null);

        makePaymentOlder(payment);

        tmsTaskHelper.runCheckPaymentStatusTaskV2();

        Payment payment2 = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);

        Assertions.assertNull(payment2.getFinalBalanceStatus());
    }

    @Test
    public void shouldUpdatePaymentStatusIfConfirmed() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        orderUpdateService.updateOrderStatus(order().getId(), OrderStatus.DELIVERY);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        List<Payment> payments = paymentService.getPayments(
                order().getId(), ClientInfo.SYSTEM, PaymentGoal.ORDER_PREPAY);
        Payment payment = Iterables.getOnlyElement(payments);

        Assertions.assertEquals(payment.getStatus(), PaymentStatus.CLEARED);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);

        makePaymentOlder(payment);

        tmsTaskHelper.runCheckPaymentStatusTaskV2();

        Payment payment2 = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);

        Assertions.assertEquals(PaymentStatus.CLEARED, payment2.getFinalBalanceStatus());
    }

    private void makePaymentOlder(Payment payment) {
        storage.updateEntityGroup(new OrderEntityGroup(order().getId()), (() -> {
            jdbcTemplate.update("update PAYMENT set " +
                    "updated_at = updated_at - interval '2' hour " +
                    "where id = ?", payment.getId());
            return null;
        }));
    }
}
