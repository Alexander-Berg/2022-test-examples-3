package ru.yandex.market.checkout.checkouter.pay;

import java.util.Date;

import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildHoldCheckBasket;

/**
 * @author : poluektov
 * date: 2021-11-29.
 */
public class FinalBalanceStatusTest extends AbstractWebTestBase {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;

    @Test
    public void testFillBalanceFinalStatus() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Payment payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        paymentService.fillFinalBalanceStatus(payment);
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getFinalBalanceStatus());
    }

    @Test
    public void forceSetFinalBalanceStatusToHoldWhenCancelledPaymentIsAuthorizedInTrust() {
        // setup
        checkouterFeatureWriter.writeValue(IntegerFeatureType.DELAY_IN_DAYS_BEFORE_HOLD_PAYMENT, 30);
        trustMockConfigurer.mockCheckBasket(buildHoldCheckBasket());
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Payment payment = orderPayHelper.payForOrderWithoutNotification(order);
        Date outdatedDate = LocalDate.now().minusDays(31).toDate();
        payment.setStatusUpdateDate(outdatedDate);
        paymentService.updatePaymentStatusToCancel(payment);
        payment = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());

        // act
        paymentService.fillFinalBalanceStatus(payment);
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);

        // verify
        assertEquals(PaymentStatus.HOLD, payment.getFinalBalanceStatus());
    }

    @Test
    public void doNotSetFinalBalanceStatusToHoldWhenTimeIsNotOut() {
        // setup
        checkouterFeatureWriter.writeValue(IntegerFeatureType.DELAY_IN_DAYS_BEFORE_HOLD_PAYMENT, 30);
        trustMockConfigurer.mockCheckBasket(buildHoldCheckBasket());
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Payment payment = orderPayHelper.payForOrderWithoutNotification(order);
        payment.setStatusUpdateDate(LocalDate.now().minusDays(30).toDate());
        paymentService.updatePaymentStatusToCancel(payment);
        payment = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());

        // act
        paymentService.fillFinalBalanceStatus(payment);
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);

        // verify
        assertNull(payment.getFinalBalanceStatus());
    }
}
