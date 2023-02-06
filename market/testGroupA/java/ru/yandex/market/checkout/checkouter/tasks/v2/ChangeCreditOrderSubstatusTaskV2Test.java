package ru.yandex.market.checkout.checkouter.tasks.v2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ChangeCreditOrderSubstatusTaskV2Test extends AbstractWebTestBase {

    @Autowired
    private ChangeCreditOrderSubstatusTaskV2 changeCreditOrderSubstatusTaskV2;
    @Value("${market.checkouter.oms.service.tms.changeCreditOrderSubstatus.awaitAfterTimeoutMinutes:30}")
    private Integer creditAwaitPaymentAfterTimeoutMinutes;

    @Test
    public void testChangeCreditSubstatus() {
        Instant fakeNow = getClock().instant().minus(creditAwaitPaymentAfterTimeoutMinutes + 1, ChronoUnit.MINUTES);
        setFixedTime(fakeNow);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.CREDIT);
        parameters.getOrder().setStatus(OrderStatus.UNPAID);
        parameters.getOrder().setSubstatus(OrderSubstatus.WAITING_USER_INPUT);
        Order order = orderCreateHelper.createOrder(parameters);
        orderCreateService.placeOrder(order);
        clearFixed();
        changeCreditOrderSubstatusTaskV2.run(TaskRunType.ONCE);
        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.getStatus(), is(OrderStatus.UNPAID));
        assertThat(orderFromDb.getSubstatus(), is(OrderSubstatus.WAITING_BANK_DECISION));
    }

}
