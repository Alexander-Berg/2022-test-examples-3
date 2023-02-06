package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.PaymentRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author : poluektov
 * date: 2021-05-14.
 */
public class GetPaymentsClientTest extends AbstractWebTestBase {

    Order order;
    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private OrderPayHelper orderPayHelper;

    @BeforeEach
    public void createOrder() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        order = orderCreateHelper.createOrder(parameters);

    }

    @Test
    void testPaymentStateSync() {
        //Оплачиваем заказ, но не высылаем нотифай.
        Payment payment = orderPayHelper.payForOrderWithoutNotification(order);
        assertEquals(PaymentStatus.INIT, payment.getStatus());
        PaymentRequest request = PaymentRequest.builder(payment.getId())
                .withForceTrustSync(true)
                .build();
        payment = checkouterClient.payments().getPayment(new RequestClientInfo(ClientRole.SYSTEM, 0L), request);
        //Платеж переходит в холд при вызове get-метода
        assertEquals(PaymentStatus.HOLD, payment.getStatus());
    }
}
