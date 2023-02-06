package ru.yandex.market.tpl.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.core.CoreTestV2;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
@CoreTestV2
@Import(value = OrderPickupValidator.class)
class OrderPickupValidatorTest {

    private final OrderGenerateService orderGenerateService;
    private final OrderPickupValidator orderPickupValidator;

    @Test
    void validatePartiallyAcceptedOrder() {
        OrderGenerateService.OrderGenerateParam orderParam = OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(239L)
                        .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                        .places(List.of(
                                OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "place1")).build(),
                                OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "place2")).build()
                        ))
                .build();
        Order order = orderGenerateService.createOrder(orderParam);

        assertThatThrownBy(() -> orderPickupValidator.validatePartiallyAcceptedOrders(ScanRequest.builder()
                .partiallyAcceptedOrderIdToNotScannedPlaces(Map.of(order.getId(), Set.of("place1")))
                .build()
        )).hasMessageContaining(order.getExternalOrderId())
        .hasMessageContaining("is partially accepted");
    }
}
