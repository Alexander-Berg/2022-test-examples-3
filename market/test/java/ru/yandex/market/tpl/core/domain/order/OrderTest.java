package ru.yandex.market.tpl.core.domain.order;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderTest {

    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;

    private PickupPoint pickupPoint;

    @BeforeEach
    void setUp() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 102461L, 1L));
    }

    @Test
    void calculateVolumeIfOneOfPlacesHasNullDimensions() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(List.of(
                        placeWithNullDimensions(),
                        placeWithDimensions(1, 2, 3), //0.000006
                        placeWithNullDimensions()
                ))
                .dimensions(new Dimensions(BigDecimal.ONE, 2, 3, 4)) //0.000024
                .build());
        assertThat(order.getOrderVolume()).isEqualTo(new BigDecimal("0.000024"));
    }

    @Test
    void calculateVolumeIfPlacesHasZeroDimensions() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(List.of(
                        placeWithDimensions(1, 2, 3), //0.000006
                        placeWithDimensions(0, 0, 0)
                ))
                .dimensions(new Dimensions(BigDecimal.ONE, 2, 3, 4)) //0.000024
                .build());
        assertThat(order.getOrderVolume()).isEqualTo(new BigDecimal("0.000024"));
    }

    @Test
    void calculateVolumeIfNoneOfPlacesHasNullDimensions() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(List.of(
                        placeWithDimensions(1, 2, 3), //0.000006
                        placeWithDimensions(1, 2, 4)  //0.000008
                ))
                .dimensions(new Dimensions(BigDecimal.ONE, 2, 3, 4)) //0.000024
                .build());
        assertThat(order.getOrderVolume()).isEqualTo(new BigDecimal("0.000014"));
    }

    @Test
    void calculateVolumeIfDimensionsIsEmpty() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(List.of())
                .dimensions(new Dimensions(BigDecimal.ONE, 2, 3, 4)) //0.000024
                .build());
        assertThat(order.getOrderVolume()).isEqualTo(new BigDecimal("0.000024"));
    }

    @Test
    void checkOrderTargetDsApiCheckpointIs44() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .flowStatus(OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP)
                .places(List.of())
                .build());
        assertThat(order.getTargetDsApiCheckpoint(OrderFlowStatus.SORTING_CENTER_PREPARED))
                .isEqualTo(44);
    }

    @Test
    void checkOrderTargetDsApiCheckpointIs45() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                .places(List.of())
                .build());
        assertThat(order.getTargetDsApiCheckpoint(OrderFlowStatus.SORTING_CENTER_PREPARED))
                .isEqualTo(45);
    }

    @Test
    void checkOrderTargetDsApiCheckpointIs47() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .flowStatus(OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_DELIVERY)
                .places(List.of())
                .build());
        assertThat(order.getTargetDsApiCheckpoint(OrderFlowStatus.SORTING_CENTER_PREPARED))
                .isEqualTo(47);
    }

    @Test
    void when_requesting_barCode_then_externalOrderId_is_returned() {
        Order order = orderGenerateService.createOrder("777");
        assertThat(order.getBarCode()).isEqualTo("777");
    }

    private OrderPlaceDto placeWithNullDimensions() {
        return OrderPlaceDto.builder().build();
    }

    private OrderPlaceDto placeWithDimensions(int length, int width, int height) {
        return OrderPlaceDto.builder()
                .dimensions(new Dimensions(BigDecimal.ONE, length, width, height))
                .build();
    }

}
