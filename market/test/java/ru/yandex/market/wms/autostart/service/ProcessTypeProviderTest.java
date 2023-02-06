package ru.yandex.market.wms.autostart.service;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.exception.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProcessTypeProviderTest {

    private final ProcessTypeProvider processTypeProvider = new ProcessTypeProvider();

    @ParameterizedTest
    @MethodSource("orderTypesArgs")
    void getBatchOrderTypeSucceedForOrdersOfMappedTypes(OrderType orderType, OrderType expectedBatchOrderType) {
        List<Order> orders = List.of(
                Order.builder().orderKey("KEY1").type(orderType.getCode()).build(),
                Order.builder().orderKey("KEY2").type(orderType.getCode()).build(),
                Order.builder().orderKey("KEY3").type(orderType.getCode()).build()
        );

        OrderType batchOrderType = processTypeProvider.getBatchOrderTypeByOrders(orders);

        assertThat(batchOrderType).isEqualTo(expectedBatchOrderType);
    }

    @Test
    void getBatchOrderTypeThrowsIfNoOrdersSpecified() {
        List<Order> orders = List.of();

        assertThrows(
                BadRequestException.class,
                () -> processTypeProvider.getBatchOrderTypeByOrders(orders),
                "Не найден тип заказа");
    }

    @Test
    void getBatchOrderTypeThrowsForOrdersOfUnmappedType() {
        List<Order> orders = List.of(
                Order.builder().orderKey("KEY1").type(OrderType.LOAD_TESTING.getCode()).build(),
                Order.builder().orderKey("KEY2").type(OrderType.LOAD_TESTING.getCode()).build(),
                Order.builder().orderKey("KEY3").type(OrderType.LOAD_TESTING.getCode()).build()
        );

        assertThrows(
                BadRequestException.class,
                () -> processTypeProvider.getBatchOrderTypeByOrders(orders),
                "Для заказов с типом LOAD_TESTING нет соответствующего типа пакетного заказа");
    }

    @Test
    void getBatchOrderTypeThrowsForOrdersOfDifferentMappedTypes() {
        List<Order> orders = List.of(
                Order.builder().orderKey("KEY1").type(OrderType.OUTBOUND_FIT.getCode()).build(),
                Order.builder().orderKey("KEY2").type(OrderType.OUTBOUND_SURPLUS.getCode()).build(),
                Order.builder().orderKey("KEY3").type(OrderType.OUTBOUND_FIT.getCode()).build()
        );

        assertThrows(
                BadRequestException.class,
                () -> processTypeProvider.getBatchOrderTypeByOrders(orders),
                "Заказы разных типов недопустимы");
    }

    private static Stream<Arguments> orderTypesArgs() {
        return Stream.of(
                Arguments.of(OrderType.STANDARD, OrderType.BATCH_ORDER),
                Arguments.of(OrderType.OUTBOUND_FIT, OrderType.BATCH_WITHDRAWAL),
                Arguments.of(OrderType.OUTBOUND_DEFECT, OrderType.BATCH_WITHDRAWAL_DAMAGE),
                Arguments.of(OrderType.OUTBOUND_EXPIRED, OrderType.BATCH_WITHDRAWAL_EXPIRED),
                Arguments.of(OrderType.OUTBOUND_SURPLUS, OrderType.BATCH_WITHDRAWAL)
        );
    }
}
