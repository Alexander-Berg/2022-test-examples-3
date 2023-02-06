package ru.yandex.market.tpl.core.domain.routing.util;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.code.util.ModelBuilderTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrderSplitGroupTypeSolverTest {

    @Test
    public void splitEmptyOrdersTest() {
        List<Order> emptyOrders = List.of();

        List<List<Order>> splitOrdersIntoGroups = OrderSplitGroupTypeSolver.splitOrdersIntoGroups(emptyOrders);

        assertThat(splitOrdersIntoGroups).allMatch(List::isEmpty);
    }

    @DisplayName("При разбиение b2b отделяются от b2c и каждый b2b разбивается по отдельности")
    @Test
    public void splitOrdersB2bAndB2c() {
        List<Order> orders = List.of(
                ModelBuilderTestUtil.buildB2bCustomersOrder(1L, "1111"),
                ModelBuilderTestUtil.buildB2bCustomersOrder(2L, "1111"),

                ModelBuilderTestUtil.buildOrder(3L),

                ModelBuilderTestUtil.buildOrder(4L)
        );

        List<List<Order>> splitOrdersIntoGroups = OrderSplitGroupTypeSolver.splitOrdersIntoGroups(orders);

        assertThat(splitOrdersIntoGroups).hasSize(3);
    }

    @DisplayName("Обычные заказы не разделяются из мультизаказа")
    @Test
    public void dontSplitOrdersB2c() {
        List<Order> orders = List.of(
                ModelBuilderTestUtil.buildOrder(1L),
                ModelBuilderTestUtil.buildOrder(2L),
                ModelBuilderTestUtil.buildOrder(3L),
                ModelBuilderTestUtil.buildOrder(4L)
        );

        List<List<Order>> splitOrdersIntoGroups = OrderSplitGroupTypeSolver.splitOrdersIntoGroups(orders);

        assertThat(splitOrdersIntoGroups).hasSize(1);
    }
}
