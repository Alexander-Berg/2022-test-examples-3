package ru.yandex.market.antifraud.orders.service.processors;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class BundleEqualizerTest {

    @Test
    public void equalizeBundle() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName("rule1")
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).offerId("1").changes(Set.of(OrderItemChange.COUNT)).count(12).build(),
                                OrderItemResponseDto.builder().id(2L).offerId("2").changes(Set.of(OrderItemChange.COUNT)).bundleId("b1").count(6).build(),
                                OrderItemResponseDto.builder().id(4L).offerId("4").count(0).changes(Set.of(OrderItemChange.MISSING)).bundleId("b2").build()
                        )))
                        .build()
        );
        List<OrderItemResponseDto> items = detectorResults.stream()
                .map(OrderDetectorResult::getFixedOrder)
                .filter(Objects::nonNull)
                .map(OrderResponseDto::getItems)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        MultiCartRequestDto requestDto = MultiCartRequestDto.builder()
            .carts(List.of(
                CartRequestDto.builder()
                    .items(List.of(
                        OrderItemRequestDto.builder().id(1L).offerId("1").count(80).build(),
                        OrderItemRequestDto.builder().id(2L).offerId("2").count(80).bundleId("b1").build(),
                        OrderItemRequestDto.builder().id(3L).offerId("3").count(80).bundleId("b1").build(),
                        OrderItemRequestDto.builder().id(4L).offerId("4").count(80).bundleId("b2").build(),
                        OrderItemRequestDto.builder().id(5L).offerId("5").count(80).bundleId("b2").build()
                    ))
                    .build()))
            .build();
        OrderVerdict orderVerdict = OrderVerdict.builder()
                .checkResults(Set.of())
                .fixedOrder(new OrderResponseDto(items))
                .isDegradation(false)
                .build();
        OrderResponseDto fixedOrder =
                new BundleEqualizer().process(requestDto, detectorResults, orderVerdict).getFixedOrder();
        assertThat(fixedOrder.getItems()).hasSize(5);
        assertThat(fixedOrder.getItems()).contains(
                OrderItemResponseDto.builder().id(1L).offerId("1").count(12).changes(Set.of(OrderItemChange.COUNT)).build(),
                OrderItemResponseDto.builder().id(2L).offerId("2").count(6).bundleId("b1").changes(Set.of(OrderItemChange.COUNT)).build(),
                OrderItemResponseDto.builder().id(3L).offerId("3").count(6).bundleId("b1").changes(Set.of(OrderItemChange.COUNT)).build(),
                OrderItemResponseDto.builder().id(4L).offerId("4").count(0).bundleId("b2").changes(Set.of(OrderItemChange.MISSING)).build(),
                OrderItemResponseDto.builder().id(5L).offerId("5").count(0).bundleId("b2").changes(Set.of(OrderItemChange.MISSING)).build()
        );
    }


    @Test
    public void equalizeBrokenBundle() {
        List<OrderDetectorResult> detectorResults = List.of(
                OrderDetectorResult.builder()
                        .ruleName("rule1")
                        .fixedOrder(new OrderResponseDto(List.of(
                                OrderItemResponseDto.builder().id(1L).offerId("1").changes(Set.of(OrderItemChange.COUNT)).count(12).build(),
                                OrderItemResponseDto.builder().id(2L).offerId("2").changes(Set.of(OrderItemChange.COUNT)).bundleId("b1").count(6).build()
                        )))
                        .build()
        );
        List<OrderItemResponseDto> items = detectorResults.stream()
                .map(OrderDetectorResult::getFixedOrder)
                .filter(Objects::nonNull)
                .map(OrderResponseDto::getItems)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        MultiCartRequestDto requestDto = MultiCartRequestDto.builder()
            .carts(List.of(
                CartRequestDto.builder()
                    .items(List.of(
                        OrderItemRequestDto.builder().id(1L).offerId("1").count(80).build(),
                        OrderItemRequestDto.builder().id(2L).offerId("2").count(80).bundleId("b1").build(),
                        OrderItemRequestDto.builder().id(3L).offerId("3").count(80).bundleId("b1").build(),
                        OrderItemRequestDto.builder().id(4L).offerId("3").count(33).build()
                    ))
                    .build()))
            .build();
        OrderVerdict orderVerdict = OrderVerdict.builder()
                .checkResults(Set.of())
                .fixedOrder(new OrderResponseDto(items))
                .isDegradation(false)
                .build();
        OrderResponseDto fixedOrder =
                new BundleEqualizer().process(requestDto, detectorResults, orderVerdict).getFixedOrder();
        assertThat(fixedOrder.getItems()).hasSize(3);
        assertThat(fixedOrder.getItems()).contains(
                OrderItemResponseDto.builder().id(1L).offerId("1").count(12).changes(Set.of(OrderItemChange.COUNT)).build(),
                OrderItemResponseDto.builder().id(2L).offerId("2").count(6).bundleId("b1").changes(Set.of(OrderItemChange.COUNT)).build(),
                OrderItemResponseDto.builder().id(3L).offerId("3").count(6).bundleId("b1").changes(Set.of(OrderItemChange.COUNT)).build()
        );
    }

}
