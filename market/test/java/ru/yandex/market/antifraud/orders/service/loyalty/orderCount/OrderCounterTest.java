package ru.yandex.market.antifraud.orders.service.loyalty.orderCount;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.dto.OrderCountDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseItemDto;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrderCounterTest {

    List<Order> getOrders(long puid, Instant now) {
        return List.of(
                Order.newBuilder()
                        .setId(1L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build()).setStatus("CANCELLED")
                        .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo1").build())
                        .setCreationDate(now.toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(2L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build()).setStatus("DELIVERY")
                        .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo2").build())
                        .setCreationDate(now.toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(3L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build()).setStatus("DELIVERED")
                        .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo3").build())
                        .setCreationDate(now.minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(4L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build()).setStatus("DELIVERY")
                        .setMultiOrderId("1").setCreationDate(now.minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(5L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(puid).build()).setStatus("DELIVERED")
                        .setMultiOrderId("1").setCreationDate(now.minus(2L, ChronoUnit.DAYS).toEpochMilli()).build()
        );
    }

    @Test
    public void canCountOrders() {
        var from = Instant.now().minus(Duration.ofDays(2));
        var to = Instant.now();
        var orderCounter = OrderCounter.builder()
                .from(from)
                .to(to)
                .filters(List.of())
                .build();
        assertThat(orderCounter.canCountOrders(from.plus(Duration.ofSeconds(1)), to))
                .isFalse();
        assertThat(orderCounter.canCountOrders(from, to))
                .isTrue();
        assertThat(orderCounter.canCountOrders(from.minus(Duration.ofSeconds(1)), to.plus(Duration.ofSeconds(1))))
                .isTrue();
        assertThat(orderCounter.canCountOrders(Instant.MIN, Instant.MAX))
                .isTrue();
        assertThat(orderCounter.canCountOrders(from, null))
                .isTrue();
    }

    @Test
    public void countOrders() {
        var now = Instant.ofEpochMilli(System.currentTimeMillis());
        var orderCountFilter = mock(OrderCountFilter.class);
        when(orderCountFilter.test(any()))
                .thenReturn(true);
        var orderCounter = OrderCounter.builder()
                .filters(List.of(orderCountFilter))
                .from(Instant.now())
                .to(Instant.now())
                .build();
        assertThat(orderCounter.countOrders(getOrders(1123L, now), 1123L))
                .isEqualTo(OrderCountResponseItemDto.builder()
                        .from(now.minus(Duration.ofDays(2)))
                        .to(now)
                        .glueOrderCount(new OrderCountDto(2, 1, 1, 4))
                        .userOrderCount(new OrderCountDto(1, 1, 1, 3))
                        .build());
    }

    @Test
    public void countOrdersFiltered() {
        var now = Instant.ofEpochMilli(System.currentTimeMillis());
        var orderCountFilter = mock(OrderCountFilter.class);
        when(orderCountFilter.test(any()))
                .thenReturn(true, true, false, false, false);
        var orderCounter = OrderCounter.builder()
                .filters(List.of(orderCountFilter))
                .from(Instant.now())
                .to(Instant.now())
                .build();
        assertThat(orderCounter.countOrders(getOrders(1123L, now), 1123L))
                .isEqualTo(OrderCountResponseItemDto.builder()
                        .from(now)
                        .to(now)
                        .glueOrderCount(new OrderCountDto(1, 0, 1, 2))
                        .userOrderCount(new OrderCountDto(0, 0, 1, 1))
                        .build());
    }
}
