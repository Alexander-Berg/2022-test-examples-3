package ru.yandex.market.checkout.checkouter.pay;

import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.PaymentGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PaymentControllerGetPaymentTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentGetHelper paymentGetHelper;

    private Parameters parameters;

    @BeforeEach
    public void setUp() {
        parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
    }

    @Test
    public void shouldReturnNotFoundIfRequestedForNotPayedOrder() throws Exception {
        Order order = orderCreateHelper.createOrder(parameters);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, order.getBuyer().getUid());
        paymentGetHelper.getOrderPaymentForActions(order.getId(), clientInfo)
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnSuccessIfRequestedForPayedOrder() throws Exception {
        Order order = orderCreateHelper.createOrder(parameters);
        Payment payment = orderPayHelper.payForOrder(order);
        long orderId = order.getId();
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, order.getBuyer().getUid());
        paymentGetHelper.getOrderPaymentForActions(orderId, clientInfo)
                .andExpect(status().is2xxSuccessful());
        Payment payment1 = paymentGetHelper.getOrderPayment(orderId, clientInfo);
        Assertions.assertEquals(payment.getId(), payment1.getId());
    }

    @Test
    public void shouldReturnNotFoundIfRequestedForNotExistingOrder() throws Exception {
        Random random = new Random();
        long orderId = random.nextLong() % 1_000_000;
        paymentGetHelper.getOrderPaymentForActions(orderId, ClientInfo.SYSTEM)
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnNotFoundIfClientIdIsWrong() throws Exception {
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        long orderId = order.getId();
        paymentGetHelper.getOrderPaymentsForActions(orderId, new ClientInfo(ClientRole.USER,
                order.getBuyer().getUid() + 1))
                .andExpect(status().isNotFound());
    }

}
