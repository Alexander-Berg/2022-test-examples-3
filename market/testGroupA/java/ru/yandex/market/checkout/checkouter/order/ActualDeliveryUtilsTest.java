package ru.yandex.market.checkout.checkouter.order;


import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.common.report.model.ActualDeliveryOption;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author dzvyagin
 */
public class ActualDeliveryUtilsTest {

    @Test
    public void optionsContainsDelivery() {
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(1L);
        Long deliveryServiceId = 123L;
        Delivery delivery = new Delivery();
        delivery.setDeliveryDates(new DeliveryDates(
                Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC)),
                Date.from(to.atStartOfDay().toInstant(ZoneOffset.UTC))
        ));
        delivery.setDeliveryServiceId(deliveryServiceId);

        ActualDeliveryOption deliveryOption = new ActualDeliveryOption();
        deliveryOption.setDayFrom(0);
        deliveryOption.setDayTo(1);
        deliveryOption.setDeliveryServiceId(deliveryServiceId);

        assertThat(ActualDeliveryUtils.optionsContainsDelivery(delivery, Set.of(deliveryOption),
                Clock.systemDefaultZone()), is(true));
    }

    @Test
    public void optionsNotContainsDelivery() {
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(1L);
        Long deliveryServiceId = 123L;
        Delivery delivery = new Delivery();
        delivery.setDeliveryDates(new DeliveryDates(
                Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC)),
                Date.from(to.atStartOfDay().toInstant(ZoneOffset.UTC))
        ));
        delivery.setDeliveryServiceId(deliveryServiceId);

        ActualDeliveryOption deliveryOption = new ActualDeliveryOption();
        deliveryOption.setDayFrom(1);
        deliveryOption.setDayTo(2);
        deliveryOption.setDeliveryServiceId(deliveryServiceId);

        assertThat(ActualDeliveryUtils.optionsContainsDelivery(delivery, Set.of(deliveryOption),
                Clock.systemDefaultZone()), is(false));
    }

    @Test
    public void shouldNotFailOnInvalidOptions() {
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(1L);
        Long deliveryServiceId = 123L;
        Delivery delivery = new Delivery();
        delivery.setDeliveryDates(new DeliveryDates(
                Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC)),
                Date.from(to.atStartOfDay().toInstant(ZoneOffset.UTC))
        ));
        delivery.setDeliveryServiceId(deliveryServiceId);

        ActualDeliveryOption deliveryOption = new ActualDeliveryOption();
        deliveryOption.setDayFrom(null);
        deliveryOption.setDayTo(null);
        deliveryOption.setDeliveryServiceId(deliveryServiceId);

        assertThat(ActualDeliveryUtils.optionsContainsDelivery(delivery, Set.of(deliveryOption),
                Clock.systemDefaultZone()), is(false));
    }
}
