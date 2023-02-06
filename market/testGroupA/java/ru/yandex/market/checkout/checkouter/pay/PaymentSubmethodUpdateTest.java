package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;

class PaymentSubmethodUpdateTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
    }

    @Test
    void paymentSubmethodMustUpdate() throws Exception {
        checkouterProperties.setEnableInstallments(true);
        Order order = orderCreateHelper.createOrder(defaultBnplParameters());
        order = orderService.getOrder(order.getId());
        assertThat(order.getPaymentSubmethod()).isEqualTo(PaymentSubmethod.BNPL);

        Payment payment = orderPayHelper.pay(order.getId());
        orderPayHelper.pay(order.getId());
        orderPayHelper.notifyPayment(payment);

        order = orderService.getOrder(order.getId());

        payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);

        assertEquals(PaymentGoal.BNPL, order.getPayment().getType());
        assertEquals(PaymentSubmethod.BNPL, order.getPaymentSubmethod());
        assertEquals(PaymentSubmethod.BNPL, payment.getProperties().getPaymentSubmethod());
    }
}
