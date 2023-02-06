package ru.yandex.market.antifraud.orders.service.loyalty.orderCount;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseItemDto;
import ru.yandex.market.crm.platform.models.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.filter.NotFilter.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrderCountRequestTest {

    List<Order> getOrders() {
        return List.of(Order.newBuilder().setId(1L).build());
    }

    @Test
    public void countOrdersIncomplete() {
        var from = Instant.now().minus(Duration.ofDays(2));
        var to = Instant.now();
        var incompleteCounter = mock(OrderCounter.class);
        when(incompleteCounter.canCountOrders(any(), any()))
                .thenReturn(false);
        var completeCounter = mock(OrderCounter.class, RETURNS_MOCKS);
        when(completeCounter.canCountOrders(any(), any()))
                .thenReturn(true);
        var request = OrderCountRequest.builder()
                .puid(1123L)
                .timeout(Duration.ofMillis(200))
                .from(from)
                .to(to)
                .items(Map.of("incomplete", incompleteCounter, "complete", completeCounter))
                .build();
        assertThat(request.countOrders(getOrders(), from, to.minus(Duration.ofDays(1)), 2).getResponseItems())
            .extractingFromEntries(Function.identity())
            .filteredOn("value", not(null))
            .extracting("key")
            .containsOnly("complete");
    }

    @Test
    public void countOrdersComplete() {
        var from = Instant.now().minus(Duration.ofDays(2));
        var to = Instant.now();
        var completeCounter = mock(OrderCounter.class);
        var responseItemDto = mock(OrderCountResponseItemDto.class);
        var orders = getOrders();
        when(completeCounter.canCountOrders(any(), any()))
                .thenReturn(true);
        when(completeCounter.countOrders(orders, 1123L))
                .thenReturn(responseItemDto);
        var request = OrderCountRequest.builder()
                .puid(1123L)
                .timeout(Duration.ofMillis(200))
                .from(from)
                .to(to)
                .items(Map.of("complete", completeCounter))
                .build();
        assertThat(request.countOrders(orders, from, to, 2))
                .isEqualTo(OrderCountResponseDtoV2.builder()
                        .puid(1123L)
                        .glueSize(2)
                        .responseItems(Map.of("complete", responseItemDto))
                        .build()
                );
    }
}
