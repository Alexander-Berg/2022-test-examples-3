package ru.yandex.market.logistics.tarifficator.repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.YaDeliveryTariffType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.ComparisonOperation;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.DeliveryServiceCode;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.DeliveryServiceStrategy;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.OperationType;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryCostCondition;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryModifier;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryModifierAction;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryModifierCondition;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryRegionGroupService;
import ru.yandex.market.logistics.tarifficator.model.shop.PercentValueLimiter;
import ru.yandex.market.logistics.tarifficator.model.shop.ValueLimiter;
import ru.yandex.market.logistics.tarifficator.model.shop.ValueModificationRule;
import ru.yandex.market.logistics.tarifficator.repository.shop.DeliveryRegionGroupServiceRepository;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/repository/delivery-region-group-service/deliveryRegionGroupServiceRepository.before.xml")
class DeliveryRegionGroupServiceRepositoryTest extends AbstractContextualTest {

    @Autowired
    DeliveryRegionGroupServiceRepository testing;

    @Test
    void getDeliveryService() {
        softly.assertThat(testing.getRegionGroupDeliveryService(102, 1001))
            .isNotNull()
            .isEqualTo(
                DeliveryRegionGroupService.builder()
                    .regionGroupId(102)
                    .deliveryServiceId(1001)
                    .courierDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                    .pickupDeliveryStrategy(DeliveryServiceStrategy.AUTO_CALCULATED)
                    .courierDeliveryModifiers(getDeliveryModifiers().get("full"))
                    .pickupDeliveryModifiers(getDeliveryModifiers().get("partial"))
                    .build()
            );
    }

    @Test
    void getNonExistentDeliveryService() {
        softly.assertThat(testing.getRegionGroupDeliveryService(101, 55555))
            .isNull();
    }

    @Test
    void getDeliveryServiceForNonExistentRegionGroup() {
        softly.assertThat(testing.getRegionGroupDeliveryService(77777, 1001))
            .isNull();
    }

    @Test
    void getDeliveryServices() {
        softly.assertThat(testing.getRegionGroupDeliveryServices(102, List.of(1002L, 1003L)))
            .isNotNull()
            .isNotEmpty()
            .isEqualTo(
                Map.of(
                    1002L, DeliveryRegionGroupService.builder()
                        .regionGroupId(102)
                        .deliveryServiceId(1002)
                        .courierDeliveryStrategy(DeliveryServiceStrategy.NO_DELIVERY)
                        .pickupDeliveryStrategy(DeliveryServiceStrategy.FIXED_COST_TIME)
                        .courierDeliveryModifiers(getDeliveryModifiers().get("partial"))
                        .pickupDeliveryModifiers(getDeliveryModifiers().get("full"))
                        .build(),
                    1003L, DeliveryRegionGroupService.builder()
                        .regionGroupId(102)
                        .deliveryServiceId(1003)
                        .courierDeliveryStrategy(DeliveryServiceStrategy.AUTO_CALCULATED)
                        .pickupDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                        .courierDeliveryModifiers(getDeliveryModifiers().get("partial"))
                        .pickupDeliveryModifiers(getDeliveryModifiers().get("partial"))
                        .build()
                )
            );
    }

    @Test
    void getDeliveryServicesForNonExistentRegionGroup() {
        softly.assertThat(testing.getRegionGroupDeliveryServices(77777, List.of(1002L, 1003L)))
            .isEmpty();
    }

    @Test
    void getNonExistentDeliveryServices() {
        softly.assertThat(testing.getRegionGroupDeliveryServices(102, List.of(22222L, 44444L)))
            .isEmpty();
    }

    @Test
    void getDeliveryServicesWithEmptyList() {
        softly.assertThat(testing.getRegionGroupDeliveryServices(102, Collections.emptyList()))
            .isEmpty();
    }

    @Test
    void getAllDeliveryServicesForRegion() {
        softly.assertThat(testing.getRegionGroupDeliveryServices(102))
            .isNotNull()
            .isNotEmpty()
            .isEqualTo(
                Map.of(
                    1001L, DeliveryRegionGroupService.builder()
                        .regionGroupId(102)
                        .deliveryServiceId(1001)
                        .courierDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                        .pickupDeliveryStrategy(DeliveryServiceStrategy.AUTO_CALCULATED)
                        .courierDeliveryModifiers(getDeliveryModifiers().get("full"))
                        .pickupDeliveryModifiers(getDeliveryModifiers().get("partial"))
                        .build(),
                    1002L, DeliveryRegionGroupService.builder()
                        .regionGroupId(102)
                        .deliveryServiceId(1002)
                        .courierDeliveryStrategy(DeliveryServiceStrategy.NO_DELIVERY)
                        .pickupDeliveryStrategy(DeliveryServiceStrategy.FIXED_COST_TIME)
                        .courierDeliveryModifiers(getDeliveryModifiers().get("partial"))
                        .pickupDeliveryModifiers(getDeliveryModifiers().get("full"))
                        .build(),
                    1003L, DeliveryRegionGroupService.builder()
                        .regionGroupId(102)
                        .deliveryServiceId(1003)
                        .courierDeliveryStrategy(DeliveryServiceStrategy.AUTO_CALCULATED)
                        .pickupDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                        .courierDeliveryModifiers(getDeliveryModifiers().get("partial"))
                        .pickupDeliveryModifiers(getDeliveryModifiers().get("partial"))
                        .build()
                )
            );
    }

    @Test
    void getAllDeliveryServicesForNonExistentRegion() {
        softly.assertThat(testing.getRegionGroupDeliveryServices(11111))
            .isEmpty();
    }

    @Test
    void getShopDeliveryServices() {
        softly.assertThat(testing.getShopDeliveryServices(774))
            .isNotNull()
            .isNotEmpty()
            .isEqualTo(
                Map.of(
                    101L,
                    List.of(
                        DeliveryRegionGroupService.builder()
                            .regionGroupId(101)
                            .deliveryServiceId(1001)
                            .courierDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                            .pickupDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                            .build()
                    ),
                    102L,
                    List.of(
                        DeliveryRegionGroupService.builder()
                            .regionGroupId(102)
                            .deliveryServiceId(1001)
                            .courierDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                            .pickupDeliveryStrategy(DeliveryServiceStrategy.AUTO_CALCULATED)
                            .courierDeliveryModifiers(getDeliveryModifiers().get("full"))
                            .pickupDeliveryModifiers(getDeliveryModifiers().get("partial"))
                            .build(),
                        DeliveryRegionGroupService.builder()
                            .regionGroupId(102)
                            .deliveryServiceId(1002)
                            .courierDeliveryStrategy(DeliveryServiceStrategy.NO_DELIVERY)
                            .pickupDeliveryStrategy(DeliveryServiceStrategy.FIXED_COST_TIME)
                            .courierDeliveryModifiers(getDeliveryModifiers().get("partial"))
                            .pickupDeliveryModifiers(getDeliveryModifiers().get("full"))
                            .build(),
                        DeliveryRegionGroupService.builder()
                            .regionGroupId(102)
                            .deliveryServiceId(1003)
                            .courierDeliveryStrategy(DeliveryServiceStrategy.AUTO_CALCULATED)
                            .pickupDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                            .courierDeliveryModifiers(getDeliveryModifiers().get("partial"))
                            .pickupDeliveryModifiers(getDeliveryModifiers().get("partial"))
                            .build()
                    )
                )
            );
    }

    @Test
    void getDeliveryServicesForNonExistentShop() {
        softly.assertThat(testing.getShopDeliveryServices(99999))
            .isEmpty();
    }

    @Test
    void addDeliveryServices() {
        var deliveryServices = List.of(
            DeliveryRegionGroupService.builder()
                .regionGroupId(103)
                .deliveryServiceId(1001)
                .courierDeliveryStrategy(DeliveryServiceStrategy.AUTO_CALCULATED)
                .pickupDeliveryStrategy(DeliveryServiceStrategy.NO_DELIVERY)
                .courierDeliveryModifiers(getDeliveryModifiers().get("partial"))
                .build(),
            DeliveryRegionGroupService.builder()
                .regionGroupId(103)
                .deliveryServiceId(1003)
                .courierDeliveryStrategy(DeliveryServiceStrategy.FIXED_COST_TIME)
                .pickupDeliveryStrategy(DeliveryServiceStrategy.FIXED_COST_TIME)
                .courierDeliveryModifiers(getDeliveryModifiers().get("full"))
                .pickupDeliveryModifiers(getDeliveryModifiers().get("full"))
                .build()
        );
        testing.addRegionGroupDeliveryServices(deliveryServices);
        softly.assertThat(testing.getRegionGroupDeliveryServices(103))
            .isNotNull()
            .isNotEmpty()
            .hasSize(3)
            .isEqualTo(Map.of(
                1001L, deliveryServices.get(0),
                1002L, DeliveryRegionGroupService.builder()
                    .regionGroupId(103)
                    .deliveryServiceId(1002)
                    .courierDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                    .pickupDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
                    .build(),
                1003L, deliveryServices.get(1)
                )
            );
    }

    @Test
    void updateDeliveryService() {
        var deliveryService = DeliveryRegionGroupService.builder()
            .regionGroupId(102)
            .deliveryServiceId(1003)
            .courierDeliveryStrategy(DeliveryServiceStrategy.AUTO_CALCULATED)
            .pickupDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
            .courierDeliveryModifiers(getDeliveryModifiers().get("full"))
            .pickupDeliveryModifiers(getDeliveryModifiers().get("full"))
            .build();
        testing.updateRegionGroupDeliveryServices(List.of(deliveryService));
        softly.assertThat(testing.getRegionGroupDeliveryService(102, 1003))
            .isNotNull()
            .isEqualTo(deliveryService);
    }

    @Test
    void updateDeliveryServiceWithNullModifiers() {
        var deliveryService = DeliveryRegionGroupService.builder()
            .regionGroupId(101)
            .deliveryServiceId(1001)
            .courierDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
            .pickupDeliveryStrategy(DeliveryServiceStrategy.NO_DELIVERY)
            .build();
        testing.updateRegionGroupDeliveryServices(List.of(deliveryService));
        softly.assertThat(testing.getRegionGroupDeliveryService(101, 1001))
            .isNotNull()
            .isEqualTo(deliveryService);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/delivery-region-group-service/deliveryRegionGroupServiceRepository.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateNonExistentDeliveryService() {
        var deliveryService = DeliveryRegionGroupService.builder()
            .regionGroupId(101)
            .deliveryServiceId(33333)
            .courierDeliveryStrategy(DeliveryServiceStrategy.UNKNOWN_COST_TIME)
            .pickupDeliveryStrategy(DeliveryServiceStrategy.NO_DELIVERY)
            .build();
        testing.updateRegionGroupDeliveryServices(List.of(deliveryService));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/delivery-region-group-service/deleteDeliveryRegionGroupService.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteDeliveryService() {
        testing.deleteRegionGroupDeliveryServices(102, Set.of(1001L));
        softly.assertThat(testing.getRegionGroupDeliveryService(102, 1001))
            .isNull();
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/delivery-region-group-service/deliveryRegionGroupServiceRepository.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteDeliveryServiceForNonExistentRegionGroup() {
        testing.deleteRegionGroupDeliveryServices(44444, Set.of(1001L));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/delivery-region-group-service/deliveryRegionGroupServiceRepository.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteNonExistentDeliveryService() {
        testing.deleteRegionGroupDeliveryServices(102, Set.of(22222L));
    }

    private Map<String, List<DeliveryModifier>> getDeliveryModifiers() {
        var valueLimiter = ValueLimiter.builder()
            .minValue(BigDecimal.ZERO)
            .maxValue(BigDecimal.TEN)
            .build();
        var valueModificationRule = ValueModificationRule.builder()
            .operation(OperationType.SUBTRACT)
            .parameter(BigDecimal.valueOf(12.97))
            .resultLimit(valueLimiter)
            .build();
        var action = DeliveryModifierAction.builder()
            .costModificationRule(valueModificationRule)
            .timeModificationRule(valueModificationRule)
            .paidByCustomerServices(Set.of(DeliveryServiceCode.INSURANCE))
            .isCarrierTurnedOn(true)
            .build();
        var condition = DeliveryModifierCondition.builder()
            .cost(
                PercentValueLimiter.builder()
                    .minValue(BigDecimal.ZERO)
                    .maxValue(BigDecimal.valueOf(98))
                    .percent(BigDecimal.valueOf(95.5))
                    .build()
            )
            .deliveryCost(
                DeliveryCostCondition.builder()
                    .percentFromOfferPrice(BigDecimal.valueOf(50))
                    .comparisonOperation(ComparisonOperation.MORE)
                    .build()
            )
            .weight(valueLimiter)
            .chargeableWeight(valueLimiter)
            .dimension(valueLimiter)
            .carrierIds(Set.of(1L))
            .deliveryDestinations(Set.of(213))
            .deliveryTypes(Set.of(YaDeliveryTariffType.PICKUP))
            .build();
        var fullDeliveryModifiers = List.of(
            DeliveryModifier.builder()
                .id(100L)
                .timestamp(1583325200238L)
                .action(action)
                .condition(condition)
                .build(),
            DeliveryModifier.builder()
                .id(101L)
                .timestamp(1583325200238L)
                .action(action)
                .condition(condition)
                .build()
        );
        var partialDeliveryModifiers = List.of(
            DeliveryModifier.builder()
                .id(200L)
                .timestamp(1583325200247L)
                .action(
                    DeliveryModifierAction.builder()
                        .costModificationRule(
                            ValueModificationRule.builder()
                                .operation(OperationType.FIX_VALUE)
                                .parameter(BigDecimal.valueOf(2000))
                                .build()
                        )
                        .build()
                )
                .build()
        );
        return Map.of("full", fullDeliveryModifiers, "partial", partialDeliveryModifiers);
    }

}
