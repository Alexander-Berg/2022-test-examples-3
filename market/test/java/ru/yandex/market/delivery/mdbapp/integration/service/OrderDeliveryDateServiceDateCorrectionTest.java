package ru.yandex.market.delivery.mdbapp.integration.service;


import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.delivery.mdbapp.components.logging.json.GetOrdersDeliveryDateErrorLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.json.GetOrdersDeliveryDateResultLogger;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order;
import ru.yandex.market.delivery.mdbclient.model.delivery.OrderDeliveryDate;
import ru.yandex.market.delivery.mdbclient.model.delivery.ResourceId;

public class OrderDeliveryDateServiceDateCorrectionTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    final int tzOffset = 8 * 3600;

    final LocalDate d0 = LocalDate.of(2020, 7, 15);
    final LocalDate d1 = d0.plusDays(1);
    final LocalDate d2 = d0.plusDays(2);
    final LocalDate d3 = d0.plusDays(3);

    final LocalTime morning = LocalTime.of(10, 0);
    final LocalTime noon = LocalTime.NOON;
    final LocalTime evening = LocalTime.of(20, 0);

    final OrderDeliveryDateService orderDeliveryDateService = new OrderDeliveryDateService(
        null,
        null,
        null,
        new GetOrdersDeliveryDateResultLogger(),
        new GetOrdersDeliveryDateErrorLogger(),
        new OrderDeliveryDateProcessIdProvider(() -> "")
    );

    @Test
    public void noCorrectionNeeded() {
        Order o1 = order(1L, d1, evening);
        OrderDeliveryDate dd = new OrderDeliveryDate(
            new ResourceId("1", "1"),
            toOffset(d1),
            toOffset(morning),
            toOffset(evening),
            "");

        Optional<Order> corrected = orderDeliveryDateService.correctDatesIfNecessary(o1, List.of(dd));

        softly.assertThat(corrected).isEmpty();
    }

    @Test
    public void correctTimeInRange() {
        Order o1 = order(1L, d1, evening);
        OrderDeliveryDate dd = new OrderDeliveryDate(
            new ResourceId("1", "1"),
            toOffset(d1),
            toOffset(morning),
            toOffset(noon),
            "");

        Optional<Order> corrected = orderDeliveryDateService.correctDatesIfNecessary(o1, List.of(dd));

        softly.assertThat(corrected).contains(order(1L, d1, noon));
    }


    @Test
    public void correctTimeNotInRange() {
        Order o1 = order(1L, d1, morning);
        OrderDeliveryDate dd = new OrderDeliveryDate(
            new ResourceId("1", "1"),
            toOffset(d1),
            toOffset(noon),
            toOffset(evening),
            "");

        Optional<Order> corrected = orderDeliveryDateService.correctDatesIfNecessary(o1, List.of(dd));

        softly.assertThat(corrected).contains(order(1L, d1, evening).setFromTime(noon));
    }

    @Test
    public void correctDateInRange() {
        Order o1 = order(1L, d3, evening);
        OrderDeliveryDate dd = new OrderDeliveryDate(
            new ResourceId("1", "1"),
            toOffset(d2),
            toOffset(morning),
            toOffset(evening),
            "");

        Optional<Order> corrected = orderDeliveryDateService.correctDatesIfNecessary(o1, List.of(dd));

        softly.assertThat(corrected).contains(order(1L, d2, evening).setFromDate(d2));
    }

    @Test
    public void correctDateNotInRange() {
        Order o1 = order(1L, d3, evening);
        OrderDeliveryDate dd = new OrderDeliveryDate(
            new ResourceId("1", "1"),
            toOffset(d0),
            toOffset(morning),
            toOffset(evening),
            "");

        Optional<Order> corrected = orderDeliveryDateService.correctDatesIfNecessary(o1, List.of(dd));

        softly.assertThat(corrected).contains(order(1L, d0, evening).setFromDate(d0));
    }

    @Test
    public void correctDateNullTime() {
        Order o1 = order(1L, d3, evening);
        OrderDeliveryDate dd = new OrderDeliveryDate(
            new ResourceId("1", "1"),
            toOffset(d0),
            null,
            null,
            "");

        Optional<Order> corrected = orderDeliveryDateService.correctDatesIfNecessary(o1, List.of(dd));

        Order expected = order(1L, d0, null)
            .setFromDate(d0)
            .setFromTime(null);
        softly.assertThat(corrected).contains(expected);
    }

    private OffsetDateTime toOffset(LocalDate localDate) {
        return localDate.atStartOfDay().atOffset(ZoneOffset.ofTotalSeconds(tzOffset));
    }

    private OffsetTime toOffset(LocalTime localTime) {
        return localTime.atOffset(ZoneOffset.ofTotalSeconds(tzOffset));
    }

    private Order order(long l, LocalDate toDate, LocalTime toTime) {
        return new Order()
            .setId(l)
            .setTimezoneOffset(tzOffset)
            .setFromDate(d1)
            .setToDate(toDate)
            .setFromTime(morning)
            .setToTime(toTime);
    }
}
