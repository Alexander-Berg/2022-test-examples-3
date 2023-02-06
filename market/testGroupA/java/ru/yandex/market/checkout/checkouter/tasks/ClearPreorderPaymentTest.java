package ru.yandex.market.checkout.checkouter.tasks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ClearPreorderPaymentTest extends AbstractWebTestBase {

    @Autowired
    private PaymentService paymentService;
    @Value("${market.checkouter.payments.clear.hours}")
    private int clearHours;

    @Test
    public void shouldClearPreorderOrders() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(OrderProvider.getBlueOrder(o -> {
            o.getItems().forEach(oi -> oi.setPreorder(true));
        }));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.isPreorder(), is(Boolean.TRUE));

        Order paidOrder = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PENDING);

        assertThat(paidOrder.getStatus(), is(OrderStatus.PENDING));

        setFixedTime(Instant.now().plus(clearHours, ChronoUnit.HOURS).plusSeconds(1));

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        Payment payment = paymentService.getPayment(paidOrder.getPaymentId(), ClientInfo.SYSTEM);

        assertThat(payment.getStatus(), is(PaymentStatus.CLEARED));
    }
}
