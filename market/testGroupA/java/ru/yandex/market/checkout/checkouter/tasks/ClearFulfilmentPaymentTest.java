package ru.yandex.market.checkout.checkouter.tasks;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ClearFulfilmentPaymentTest extends AbstractWebTestBase {

    @Autowired
    private PaymentService paymentService;
    @Value("${market.checkouter.payments.clear.hours}")
    private int clearHours;

    @Test
    public void shouldClearFulfilmentOrders() {
        Instant createdAt = ZonedDateTime.of(
                2028, 7, 2,
                14, 20, 0, 0,
                ZoneId.systemDefault()
        ).toInstant();
        setFixedTime(createdAt);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.getBuyer().setDontCall(true);

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.isFulfilment(), is(Boolean.TRUE));
        Order paidOrder = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        assertThat(paidOrder.getStatus(), is(OrderStatus.PROCESSING));
        assertThat(paidOrder.getPayment().getStatus(), is(PaymentStatus.HOLD));

        setFixedTime(createdAt.plus(clearHours, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES));

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        Payment payment = paymentService.getPayment(paidOrder.getPaymentId(), ClientInfo.SYSTEM);

        assertThat(payment.getStatus(), is(PaymentStatus.CLEARED));
    }
}
