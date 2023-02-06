package ru.yandex.market.pvz.internal.domain.pickup_point;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.consumable.ConsumableQueryService;
import ru.yandex.market.pvz.core.domain.consumable.request.item.ConsumableRequestItemParams;
import ru.yandex.market.pvz.core.domain.consumable.type.ConsumableTypeParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointSimpleParams;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestConsumableTypeFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.consumable.dto.ConsumableCapacityDto;
import ru.yandex.market.pvz.internal.controller.pi.consumable.dto.ConsumableItemDto;
import ru.yandex.market.pvz.internal.controller.pi.consumable.dto.ConsumableRequestDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_FIRST_DEACTIVATION_REASON;
import static ru.yandex.market.pvz.core.test.factory.TestConsumableTypeFactory.ConsumableTypeTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointPartnerServiceTest {

    private final PickupPointPartnerService partnerService;
    private final PickupPointQueryService pickupPointQueryService;
    private final ConsumableQueryService consumableQueryService;
    private final PickupPointDeactivationCommandService pickupPointDeactivationCommandService;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestConsumableTypeFactory consumableTypeFactory;

    @Test
    void testGetCapacityWithEnhancedFlag() {
        ConsumableTypeParams consumableType = consumableTypeFactory.create();
        var pickupPoint = pickupPointQueryService.get(pickupPointFactory.createPickupPoint().getId());

        assertThat(partnerService.getConsumablesCapacity(pickupPoint.getPvzMarketId())).isEqualTo(
                ConsumableCapacityDto.builder()
                        .capacity(List.of(ConsumableItemDto.builder()
                                .name(consumableType.getName())
                                .count(consumableType.getCountPerPeriod())
                                .maxCount(consumableType.getCountPerPeriod())
                                .typeId(consumableType.getId())
                                .build()))
                        .build()
        );
    }

    @Test
    void testOrderSomeConsumables() {
        PickupPointSimpleParams pickupPoint = createPickupPoint();
        ConsumableTypeParams typeNotOrder = consumableTypeFactory.create();
        ConsumableTypeParams typeOrderPartially = consumableTypeFactory.create();
        ConsumableTypeParams typeOrderFully = consumableTypeFactory.create();

        partnerService.createConsumableOrder(pickupPoint.getPvzMarketId(), new ConsumableRequestDto(List.of(
                ConsumableItemDto.builder()
                        .typeId(typeOrderFully.getId())
                        .count(typeOrderFully.getCountPerPeriod())
                        .build(),

                ConsumableItemDto.builder()
                        .typeId(typeOrderPartially.getId())
                        .count(1)
                        .build()
        )));

        assertThat(partnerService.getConsumablesCapacity(pickupPoint.getPvzMarketId()).getCapacity())
                .containsExactlyInAnyOrder(
                        ConsumableItemDto.builder()
                                .typeId(typeNotOrder.getId())
                                .name(typeNotOrder.getName())
                                .count(typeNotOrder.getCountPerPeriod())
                                .maxCount(typeNotOrder.getCountPerPeriod())
                                .build(),

                        ConsumableItemDto.builder()
                                .typeId(typeOrderPartially.getId())
                                .name(typeOrderPartially.getName())
                                .count(typeOrderPartially.getCountPerPeriod() - 1)
                                .maxCount(typeOrderPartially.getCountPerPeriod())
                                .build()
                );
    }

    @Test
    void testShipWithFirstOrder() {
        PickupPointSimpleParams pickupPoint = createPickupPoint();
        ConsumableTypeParams typeToOrder = consumableTypeFactory.create();
        ConsumableTypeParams typeToShipWithFirstOrder = consumableTypeFactory.create(ConsumableTypeTestParams.builder()
                .shipWithFirstOrder(true)
                .build());

        partnerService.createConsumableOrder(pickupPoint.getPvzMarketId(), new ConsumableRequestDto(List.of(
                ConsumableItemDto.builder()
                        .typeId(typeToOrder.getId())
                        .count(typeToOrder.getCountPerPeriod())
                        .build()
        )));

        var requests = consumableQueryService.getPickupPointRequests(pickupPoint.getId());
        assertThat(requests).hasSize(1);

        long requestId = requests.get(0).getId();
        List<Long> actuallyOrderedTypeIds = consumableQueryService.getRequestItems(requestId).stream()
                .map(ConsumableRequestItemParams::getConsumableTypeId)
                .collect(Collectors.toList());

        assertThat(actuallyOrderedTypeIds).containsExactlyInAnyOrder(
                typeToOrder.getId(), typeToShipWithFirstOrder.getId());
    }

    @Test
    void testOrderMoreThanAvailable() {
        PickupPointSimpleParams pickupPoint = createPickupPoint();
        ConsumableTypeParams type = consumableTypeFactory.create();

        partnerService.createConsumableOrder(pickupPoint.getPvzMarketId(), new ConsumableRequestDto(List.of(
                ConsumableItemDto.builder()
                        .typeId(type.getId())
                        .count(type.getCountPerPeriod())
                        .build()
        )));

        assertThatThrownBy(() -> partnerService.createConsumableOrder(pickupPoint.getPvzMarketId(),
                new ConsumableRequestDto(List.of(ConsumableItemDto.builder()
                        .typeId(type.getId())
                        .count(type.getCountPerPeriod())
                        .build()
                ))
        )).hasMessageContaining("доступно не более");
    }

    private PickupPointSimpleParams createPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointWithCourierMapping(PickupPointTestParams.builder()
                .active(true)
                .build());

        pickupPointDeactivationCommandService.cancelDeactivation(
                pickupPoint.getId(), DEFAULT_FIRST_DEACTIVATION_REASON);

        return pickupPointQueryService.get(pickupPoint.getId());
    }

}
