package ru.yandex.market.antifraud.orders.service.loyalty.orderCount;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {

    @Test
    public void groupingMultiorders() {
        var results = Stream.of(
            new Order(12),
            new Order(13).withMultiOrderId("1"),
            new Order(14).withMultiOrderId("1"),
            new Order(15)
        ).collect(Utils.groupingMultiorders(x -> Utils.MultiOrderKey.builder()
            .puid(0)
            .multiorderId(x.getMultiOrderId())
            .createdAt(Instant.now())
            .build(), toList()));
        //noinspection unchecked
        assertThat(results)
            .extracting(Utils.OrderInfo::getInfo)
            .containsExactlyInAnyOrder(
                List.of(new Order(12)),
                List.of(new Order(15)),
                List.of(new Order(13).withMultiOrderId("1"), new Order(14).withMultiOrderId("1"))
            );
    }

    @Value
    @RequiredArgsConstructor
    static class Order {
        public Order(long orderId) {
            this.orderId = orderId;
            multiOrderId = null;
        }

        Long orderId;
        @With
        String multiOrderId;
    }
}