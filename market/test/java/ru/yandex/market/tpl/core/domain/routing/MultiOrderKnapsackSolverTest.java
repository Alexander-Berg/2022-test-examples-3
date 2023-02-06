package ru.yandex.market.tpl.core.domain.routing;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class MultiOrderKnapsackSolverTest {

    private final OrderGenerateService orderGenerateService;

    @Test
    void splitOrdersIntoGroups() {
        List<List<BigDecimal>> lists = MultiOrderKnapsackSolver.splitOrdersIntoGroups(
                List.of(createOrderWithVolume(3),
                        createOrderWithVolume(3),
                        createOrderWithVolume(3),
                        createOrderWithVolume(3),
                        createOrderWithVolume(1),
                        createOrderWithVolume(6),
                        createOrderWithVolume(2),
                        createOrderWithVolume(100)),
                Order::getOrderVolume,
                BigDecimal.TEN

        ).stream()
                .map(l -> l.stream().map(Order::getOrderVolume).collect(Collectors.toList()))
                .collect(Collectors.toList());
        assertThat(lists).hasSize(4);
        List<Integer> volumeSums = lists.stream()
                .map(l -> l.stream().mapToInt(BigDecimal::intValue).sum())
                .collect(Collectors.toList());
        assertThat(volumeSums).contains(
                10,  // 3 + 3 + 3 + 1
                100, // 100
                9,   // 3 + 6
                2    // 2
        );
    }

    private Order createOrderWithVolume(Integer volumeInCubicMeters) {
        Dimensions dimensions = new Dimensions(BigDecimal.ONE, 100, 100, volumeInCubicMeters * 100);
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .dimensions(dimensions)
                .build());
    }
}
