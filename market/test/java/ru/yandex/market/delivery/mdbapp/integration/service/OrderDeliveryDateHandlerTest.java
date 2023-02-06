package ru.yandex.market.delivery.mdbapp.integration.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Delivery;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.OrderRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.RequestCondition;

    public class OrderDeliveryDateHandlerTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    final LocalDate d0 = LocalDate.of(2020, 2, 21);
    final LocalDate d1 = d0.plusDays(1);
    final LocalDate d2 = d0.plusDays(2);
    final LocalDate d3 = d0.plusDays(3);

    final LocalTime morning = LocalTime.of(8, 0);
    final LocalTime evening = LocalTime.of(20, 0);

    final int timezoneOffsetSec = 3 * 3600;

    final LocalTime deliveryFromTime = LocalTime.of(10, 0);
    final LocalTime deliveryToTime = LocalTime.of(20, 0);

    final Order order = new Order()
        .setFromDate(d1)
        .setToDate(d2)
        .setFromTime(deliveryFromTime)
        .setToTime(deliveryToTime)
        .setTimezoneOffset(timezoneOffsetSec);

    @Test
    public void createOrderRequests() {
        RequestCondition c1 = new RequestCondition().setId(1L).setRequestTime(morning);
        RequestCondition c2 = new RequestCondition().setId(2L)
            .setRequestTime(morning).setFromDateOffset(1).setToDateOffset(1);
        RequestCondition c3 = new RequestCondition().setId(3L).setFromTimeOffset(-2);

        Delivery delivery = new Delivery() {
            @Override
            public Set<RequestCondition> getRequestConditions() {
                return Set.of(c1, c2, c3);
            }
        }.setEnabled(false);

        List<OrderRequest> orderRequests = new OrderDeliveryDateHandler(null, null, null, null)
            .makeRequests(order, delivery);

        softly.assertThat(new HashSet<>(orderRequests)).isEqualTo(Set.of(
            new OrderRequest().setOrder(order).setRequestCondition(c1).setProcessTime(OffsetDateTime.of(d1, morning,
                ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(c1).setProcessTime(OffsetDateTime.of(d2, morning,
                ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(c2).setProcessTime(OffsetDateTime.of(d2, morning,
                ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(c2).setProcessTime(OffsetDateTime.of(d3, morning,
                ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(c3).setProcessTime(OffsetDateTime.of(d1,
                deliveryFromTime.minusHours(2), ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(c3).setProcessTime(OffsetDateTime.of(d2,
                deliveryFromTime.minusHours(2), ZoneOffset.ofTotalSeconds(timezoneOffsetSec)))
        ));
    }
}
