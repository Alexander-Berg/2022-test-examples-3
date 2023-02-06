package ru.yandex.market.pvz.core.domain.consumable.type;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.consumable.ConsumableQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointSimpleParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestConsumableTypeFactory;
import ru.yandex.market.pvz.core.test.factory.TestConsumableTypeFactory.ConsumableTypeTestParams;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ConsumableQueryServiceTest {

    private final ConsumableQueryService consumableQueryService;
    private final PickupPointQueryService pickupPointQueryService;

    private final TestConsumableTypeFactory consumableTypeFactory;
    private final TestPickupPointFactory pickupPointFactory;

    @Test
    void testGetConsumableById() {
        ConsumableTypeParams created = consumableTypeFactory.create();
        assertThat(consumableQueryService.getTypeById(created.getId())).isEqualTo(created);
    }

    @Test
    void testGetConsumablesAvailableByPickupPointType() {
        ConsumableTypeParams anyPickupPointConsumable = consumableTypeFactory.create();
        ConsumableTypeParams brandedOnlyConsumable = consumableTypeFactory.create(ConsumableTypeTestParams.builder()
                .availableToPickupPointTypes(List.of(ConsumablePickupPointType.BRANDED))
                .build());

        ConsumableTypeParams nonBrandedOnlyConsumable = consumableTypeFactory.create(ConsumableTypeTestParams.builder()
                .availableToPickupPointTypes(List.of(ConsumablePickupPointType.NON_BRANDED))
                .build());

        PickupPoint brandedPickupPoint = pickupPointFactory.createPickupPoint(PickupPointTestParams.builder()
                .brandingType(PickupPointBrandingType.FULL)
                .build());

        PickupPoint nonBrandedPickupPoint = pickupPointFactory.createPickupPoint(PickupPointTestParams.builder()
                .brandingType(PickupPointBrandingType.NONE)
                .build());

        PickupPointSimpleParams brandedParams = pickupPointQueryService.get(brandedPickupPoint.getId());
        PickupPointSimpleParams nonBrandedParams = pickupPointQueryService.get(nonBrandedPickupPoint.getId());

        assertThat(consumableQueryService.getAvailableTypesToPickupPoint(brandedParams))
                .containsExactlyInAnyOrder(anyPickupPointConsumable, brandedOnlyConsumable);

        assertThat(consumableQueryService.getAvailableTypesToPickupPoint(nonBrandedParams))
                .containsExactlyInAnyOrder(anyPickupPointConsumable, nonBrandedOnlyConsumable);

    }

    @Test
    void testGetConsumablesAvailableByPickupPointId() {
        var pickupPoint = pickupPointQueryService.get(pickupPointFactory.createPickupPoint().getId());
        var consumableType = consumableTypeFactory.create(ConsumableTypeTestParams.builder()
                .availableToPickupPointTypes(List.of())
                .availableToPickupPointIds(List.of(pickupPoint.getId()))
                .build());

        assertThat(consumableQueryService.getAvailableTypesToPickupPoint(pickupPoint))
                .containsExactlyInAnyOrder(consumableType);
    }

}
