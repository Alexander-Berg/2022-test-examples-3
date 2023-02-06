package ru.yandex.market.checkout.checkouter.checkout;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CreateOrderScheduleStringTest extends AbstractWebTestBase {

    @Test
    public void shouldCreateOrderWithSchedule() {
        Parameters parameters = new Parameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getDelivery().getOutlet().getScheduleString(), is("<WorkingTime>\n" +
                "                <WorkingDaysFrom>1</WorkingDaysFrom>\n" +
                "                <WorkingDaysTill>7</WorkingDaysTill>\n" +
                "                <WorkingHoursFrom>0:00</WorkingHoursFrom>\n" +
                "                <WorkingHoursTill>0:00</WorkingHoursTill>\n" +
                "            </WorkingTime>"));
    }
}
