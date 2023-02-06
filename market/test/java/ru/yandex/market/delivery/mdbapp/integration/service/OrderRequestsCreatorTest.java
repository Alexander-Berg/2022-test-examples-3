package ru.yandex.market.delivery.mdbapp.integration.service;


import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.Order;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.OrderRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.godd.RequestCondition;

public class OrderRequestsCreatorTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    final LocalDate d0 = LocalDate.of(2020, 2, 21);
    final LocalDate d1 = d0.plusDays(1);
    final LocalDate d2 = d0.plusDays(2);
    final LocalDate d3 = d0.plusDays(3);
    final LocalDate d4 = d0.plusDays(4);

    final LocalTime morning = LocalTime.of(8, 0);
    final LocalTime evening = LocalTime.of(20, 0);

    final int timezoneOffsetSec = 3 * 3600;

    final LocalTime deliveryFromTime = LocalTime.of(10, 0);
    final LocalTime deliveryToTime = LocalTime.of(20, 0);

    @Test
    public void testEveningBeforeDeliveryDate() {
        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setTimezoneOffset(timezoneOffsetSec);

        RequestCondition condition = new RequestCondition()
            .setRequestTime(evening)
            .setFromDateOffset(-1)
            .setToDateOffset(-1);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of(
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(d0.atTime(evening).atOffset(ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(d1.atTime(evening).atOffset(ZoneOffset.ofTotalSeconds(timezoneOffsetSec)))
        ));
    }

    @Test
    public void testDeliveryDateMorning() {

        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setTimezoneOffset(timezoneOffsetSec);

        RequestCondition condition = new RequestCondition()
            .setRequestTime(morning);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of(
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d1, morning, ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d2, morning, ZoneOffset.ofTotalSeconds(timezoneOffsetSec)))
        ));
    }

    @Test
    public void testDeliveryIntervalStart() {
        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setFromTime(deliveryFromTime)
            .setToTime(deliveryToTime)
            .setTimezoneOffset(timezoneOffsetSec);

        RequestCondition condition = new RequestCondition()
            .setFromTimeOffset(0);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of(
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d1, deliveryFromTime, ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d2, deliveryFromTime, ZoneOffset.ofTotalSeconds(timezoneOffsetSec)))
        ));
    }

    @Test
    public void testHourBeforeDeliveryEnd() {
        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setFromTime(deliveryFromTime)
            .setToTime(deliveryToTime)
            .setTimezoneOffset(timezoneOffsetSec);

        RequestCondition condition = new RequestCondition()
            .setToTimeOffset(-1);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of(
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(
                    d1,
                    deliveryToTime.minusHours(1),
                    ZoneOffset.ofTotalSeconds(timezoneOffsetSec)
                )),
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(
                    d2,
                    deliveryToTime.minusHours(1),
                    ZoneOffset.ofTotalSeconds(timezoneOffsetSec)
                ))
        ));
    }

    @Test
    public void testHourBeforeDeliveryEndNoTime() {
        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setTimezoneOffset(timezoneOffsetSec);

        RequestCondition condition = new RequestCondition()
            .setToTimeOffset(-1);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of());
    }

    @Test
    public void testDeliveryEndTime() {
        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setFromTime(deliveryFromTime)
            .setToTime(deliveryToTime)
            .setTimezoneOffset(timezoneOffsetSec);

        RequestCondition condition = new RequestCondition()
            .setToTimeOffset(0);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of(
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d1, deliveryToTime, ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d2, deliveryToTime, ZoneOffset.ofTotalSeconds(timezoneOffsetSec)))
        ));
    }

    @Test
    public void testDeliveryDateEvening() {
        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setTimezoneOffset(timezoneOffsetSec);


        RequestCondition condition = new RequestCondition()
            .setRequestTime(evening);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of(
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d1, evening, ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d2, evening, ZoneOffset.ofTotalSeconds(timezoneOffsetSec)))
        ));
    }

    @Test
    public void testDeliveryDateNextEvening() {
        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setTimezoneOffset(timezoneOffsetSec);


        RequestCondition condition = new RequestCondition()
            .setFromDateOffset(1)
            .setToDateOffset(1)
            .setRequestTime(evening);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of(
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d2, evening, ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d3, evening, ZoneOffset.ofTotalSeconds(timezoneOffsetSec)))
        ));
    }

    @Test
    public void testMorningPlus2() {
        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setTimezoneOffset(timezoneOffsetSec);


        RequestCondition condition = new RequestCondition()
            .setFromDateOffset(2)
            .setToDateOffset(2)
            .setRequestTime(morning);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of(
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d3, morning, ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d4, morning, ZoneOffset.ofTotalSeconds(timezoneOffsetSec)))
        ));
    }

    @Test
    public void testEveningPlus2() {
        Order order = new Order()
            .setFromDate(d1)
            .setToDate(d2)
            .setTimezoneOffset(timezoneOffsetSec);


        RequestCondition condition = new RequestCondition()
            .setFromDateOffset(2)
            .setToDateOffset(2)
            .setRequestTime(evening);

        List<OrderRequest> requests = new OrderRequestsCreator(order, condition).create().collect(Collectors.toList());

        softly.assertThat(requests).isEqualTo(List.of(
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d3, evening, ZoneOffset.ofTotalSeconds(timezoneOffsetSec))),
            new OrderRequest().setOrder(order).setRequestCondition(condition)
                .setProcessTime(OffsetDateTime.of(d4, evening, ZoneOffset.ofTotalSeconds(timezoneOffsetSec)))
        ));
    }
}
