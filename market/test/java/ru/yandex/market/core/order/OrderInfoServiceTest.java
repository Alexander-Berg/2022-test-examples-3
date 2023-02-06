package ru.yandex.market.core.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.core.fulfillment.model.StockType;
import ru.yandex.market.core.order.model.MbiOrderItem;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.order.model.OrderInfoStatus;
import ru.yandex.market.core.order.model.OrderItemExtendedStatus;
import ru.yandex.market.core.order.model.OrderItemStatusDetails;
import ru.yandex.market.core.order.model.OrderItemStockTypeDetails;
import ru.yandex.market.core.order.resupply.ResupplyOrderItem;
import ru.yandex.market.core.order.resupply.ResupplyType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

public class OrderInfoServiceTest {

    private static final LocalDateTime ORDER_DATE_TIME = LocalDateTime.of(2020, 12, 12, 0, 0, 0);

    @ParameterizedTest(name = "{0}")
    @MethodSource("args")
    @DisplayName("Проверка детализации по типам стоков и количеству возвратов и невыкупов для позиции в заказе")
    void testGetOrderItemStatusDetails(String description, MbiOrderItem orderItem,
                                       Collection<ResupplyOrderItem> resupplies,
                                       List<OrderItemStatusDetails> expected) {
        var statusHistoryMock = Mockito.mock(CpaOrderStatusHistoryDao.class);
        when(statusHistoryMock.getLastChangedStatusTime(anySet())).thenReturn(Map.of(1L, ORDER_DATE_TIME));
        var service = new OrdersInfoService(null, null, statusHistoryMock, null,
                null, null, null, null, null);
        assertThat(service.getOrderItemStatusDetails(orderItem, resupplies)).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(
                        "Без возратов и невыкупов",
                        MbiOrderItem.builder()
                                .setOrderId(1L)
                                .setCount(3)
                                .setShopSku("123")
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setExtendedStatus(new OrderItemExtendedStatus(
                                        OrderInfoStatus.PROCESSING, OrderInfoStatus.PROCESSING
                                ))
                                .build(),
                        Collections.emptyList(),
                        Collections.singletonList(OrderItemStatusDetails.builder()
                                .totalCount(3)
                                .itemStatus(OrderInfoStatus.PROCESSING)
                                .details(
                                        Collections.singletonList(
                                                OrderItemStockTypeDetails.builder()
                                                        .count(3)
                                                        .date(ORDER_DATE_TIME.toLocalDate())
                                                        .stockType(null)
                                                        .build()))
                                .build())
                ),
                Arguments.of(
                        "Частичный возврат",
                        MbiOrderItem.builder()
                                .setOrderId(1L)
                                .setCount(6)
                                .setShopSku("123")
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .build(),
                        List.of(ResupplyOrderItem.newBuilder()
                                        .setStockType(StockType.FIT)
                                        .setResupplyType(ResupplyType.RETURNED)
                                        .setCreatedAt(ORDER_DATE_TIME.plusDays(1))
                                        .setItemCount(2)
                                        .build(),
                                ResupplyOrderItem.newBuilder()
                                        .setStockType(StockType.DEFECT)
                                        .setResupplyType(ResupplyType.RETURNED)
                                        .setCreatedAt(ORDER_DATE_TIME.plusDays(2))
                                        .setItemCount(1)
                                        .build()
                        ),
                        List.of(OrderItemStatusDetails.builder()
                                        .totalCount(3)
                                        .itemStatus(OrderInfoStatus.RETURNED)
                                        .details(List.of(
                                                OrderItemStockTypeDetails.builder()
                                                        .count(2)
                                                        .date(LocalDate.of(2020, 12, 13))
                                                        .stockType(StockType.FIT)
                                                        .build(),
                                                OrderItemStockTypeDetails.builder()
                                                        .count(1)
                                                        .date(LocalDate.of(2020, 12, 14))
                                                        .stockType(StockType.DEFECT)
                                                        .build()
                                        ))
                                        .build(),
                                OrderItemStatusDetails.builder()
                                        .totalCount(3)
                                        .itemStatus(OrderInfoStatus.DELIVERED)
                                        .details(List.of(
                                                OrderItemStockTypeDetails.builder()
                                                        .count(3)
                                                        .date(ORDER_DATE_TIME.toLocalDate())
                                                        .stockType(null)
                                                        .build()
                                        ))
                                        .build()
                        )),
                Arguments.of(
                        "Невыкуп",
                        MbiOrderItem.builder()
                                .setOrderId(1L)
                                .setCount(10)
                                .setShopSku("123")
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .build(),
                        List.of(ResupplyOrderItem.newBuilder()
                                        .setStockType(StockType.FIT)
                                        .setResupplyType(ResupplyType.UNREDEEMED)
                                        .setCreatedAt(ORDER_DATE_TIME.plusDays(1))
                                        .setItemCount(4)
                                        .build(),
                                ResupplyOrderItem.newBuilder()
                                        .setStockType(StockType.DEFECT)
                                        .setResupplyType(ResupplyType.UNREDEEMED)
                                        .setCreatedAt(ORDER_DATE_TIME.plusDays(2))
                                        .setItemCount(4)
                                        .build()
                        ),
                        Collections.singletonList(OrderItemStatusDetails.builder()
                                .totalCount(10)
                                .itemStatus(OrderInfoStatus.UNREDEEMED)
                                .details(List.of(
                                        OrderItemStockTypeDetails.builder()
                                                .count(4)
                                                .date(LocalDate.of(2020, 12, 13))
                                                .stockType(StockType.FIT)
                                                .build(),
                                        OrderItemStockTypeDetails.builder()
                                                .count(4)
                                                .date(LocalDate.of(2020, 12, 14))
                                                .stockType(StockType.DEFECT)
                                                .build()
                                ))
                                .build()
                        )
                ));
    }
}
