package ru.yandex.market.deliverycalculator.indexer.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexerclient.model.CarrierInfo;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryOption;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRule;
import ru.yandex.market.deliverycalculator.indexerclient.model.ShopDeliveryInfo;
import ru.yandex.market.deliverycalculator.indexerclient.model.TariffInfoV1;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ActionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ConditionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryModifierDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueModificationRuleDto;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffStrategy;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.storage.model.ShopModifiersEntity;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Action;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Condition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifier;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Тест для {@link ShopModifiersService}.
 */
class ShopModifiersServiceTest extends FunctionalTest {

    @Autowired
    private ShopModifiersService tested;

    /**
     * Тест для {@link ShopModifiersService#deleteShopModifiers(Collection)}.
     * Случай: успешное удаление модификаторов
     */
    @Test
    @DbUnitDataSet(before = "deleteShopModifiers.before.csv", after = "deleteShopModifiers.after.csv")
    void testDeleteShopModifiers_successfullyDeleted() {
        tested.deleteShopModifiers(Sets.newHashSet(1L, 2L, 4L));
    }

    /**
     * Тест для {@link ShopModifiersService#deleteShopModifiers(Collection)}.
     * Случай: успешное удаление модификаторов
     */
    @Test
    @DbUnitDataSet(before = "deleteShopModifiers.before.csv", after = "deleteShopModifiers.before.csv")
    void testDeleteShopModifiers_emptyIdList() {
        tested.deleteShopModifiers(new HashSet<>());
    }

    /**
     * Тест для {@link ShopModifiersService#convertToShopModifiers(ShopDeliveryInfo)}.
     * Случай: успешная конвертация, несколько модификаоторов созданы
     */
    @Test
    void testConvertToShopModifiers_successfulConvert_severalAutoCalculatedTariffs() {
        ShopModifiersEntity actual = tested.convertToShopModifiers(
                createShopDeliveryInfo(1L,  createTestTariffs(), null)
        );

        assertNotNull(actual);
        assertEquals(1L, actual.getShopId());
        assertThat(actual.getModifiers(), containsInAnyOrder(getExpectedModifiers().toArray()));
    }

    /**
     * Тест для {@link ShopModifiersService#convertToShopModifiers(ShopDeliveryInfo)}.
     * Случай: успешная конвертация, 0 модификаторов создано, так нет настроенной автокалькуляции
     */
    @Test
    void testConvertToShopModifiers_successfulConvert_noModifiers_onlyRegularTariff() {
        TariffInfoV1 tariff = createTestCarrierTariff(asList(7, 8), new ArrayList<>(),
                Map.of(1, DeliveryTariffStrategy.UNKNOWN_COST_TIME),
                Map.of(1, DeliveryTariffStrategy.UNKNOWN_COST_TIME, 2, DeliveryTariffStrategy.UNKNOWN_COST_TIME));

        assertNull(tested.convertToShopModifiers(
                createShopDeliveryInfo(1L, List.of(tariff, createTestRegularTariff()), null)
        ));
    }

    /**
     * Тест для {@link ShopModifiersService#convertToShopModifiers(ShopDeliveryInfo)}.
     * Случай: успешная конвертация, нет настроенной автокалькуляции, есть модификаторы СиС
     */
    @Test
    void testConvertToShopModifiers_successfulConvert_noAutoCalculatedTariffs_withDeliveryModifiers() {
        TariffInfoV1 tariff = createTestCarrierTariff(asList(7, 8), new ArrayList<>(),
                Map.of(1, DeliveryTariffStrategy.UNKNOWN_COST_TIME),
                Map.of(1, DeliveryTariffStrategy.UNKNOWN_COST_TIME, 2, DeliveryTariffStrategy.UNKNOWN_COST_TIME));

        ShopModifiersEntity actual = tested.convertToShopModifiers(
                createShopDeliveryInfo(1L, List.of(tariff, createTestRegularTariff()), createDeliveryModifierDtos())
        );

        assertNotNull(actual);
        assertEquals(getExpectedDeliveryModifiers(), actual.getModifiers());
    }

    /**
     * Тест для {@link ShopModifiersService#convertToShopModifiers(ShopDeliveryInfo)}.
     * Случай: успешная конвертация, есть настроенная автокалькуляции, есть модификаторы СиС
     */
    @Test
    void testConvertToShopModifiers_successfulConvert_severalAutoCalculatedTariffs_withDeliveryModifiers() {
        ShopModifiersEntity actual = tested.convertToShopModifiers(
                createShopDeliveryInfo(1L, createTestTariffs(), createDeliveryModifierDtos())
        );

        assertNotNull(actual);
        assertThat(actual.getModifiers(), containsInAnyOrder(
                Stream.of(getExpectedModifiers(), getExpectedDeliveryModifiers())
                        .flatMap(Collection::stream)
                        .toArray()
        ));
    }

    private ShopDeliveryInfo createShopDeliveryInfo(long shopId,
                                                    List<TariffInfoV1> tariffs,
                                                    List<DeliveryModifierDto> modifierDtos) {
        ShopDeliveryInfo shopDeliveryInfo = new ShopDeliveryInfo();
        shopDeliveryInfo.setId(shopId);
        shopDeliveryInfo.setTariffInfos(tariffs);
        shopDeliveryInfo.setDeliveryModifiers(modifierDtos);
        return shopDeliveryInfo;
    }

    private List<DeliveryModifierDto> createDeliveryModifierDtos() {
        return asList(
                new DeliveryModifierDto.Builder()
                        .withId(0L)
                        .withTimestamp(1L)
                        .withAction(
                                new ActionDto.Builder()
                                        .withCostModificationRule(
                                                new ValueModificationRuleDto.Builder()
                                                        .withOperation(ValueModificationRuleDto.OperationEnum.ADD)
                                                        .withParameter(BigDecimal.ONE)
                                                        .build()
                                        )
                                        .build()
                        )
                        .build(),
                new DeliveryModifierDto.Builder()
                        .withId(0L)
                        .withTimestamp(1L)
                        .withAction(
                                new ActionDto.Builder()
                                        .withTimeModificationRule(
                                                new ValueModificationRuleDto.Builder()
                                                        .withOperation(ValueModificationRuleDto.OperationEnum.ADD)
                                                        .withParameter(BigDecimal.TEN)
                                                        .build()
                                        )
                                        .build()
                        )
                        .withCondition(
                                new ConditionDto.Builder()
                                        .withCarrierIds(Set.of(22L))
                                        .build()
                        )
                        .build()
        );
    }

    private List<DeliveryModifier> getExpectedDeliveryModifiers() {
        return asList(
                new DeliveryModifier.Builder()
                        .withId(0L)
                        .withTimestamp(1L)
                        .withAction(
                                new Action.Builder()
                                        .withCostModificationRule(
                                                new ValueModificationRule.Builder<Long>()
                                                        .withOperation(ValueModificationRule.OperationEnum.ADD)
                                                        .withParameter(1.0)
                                                        .build()
                                        )
                                        .build()
                        )
                        .build(),
                new DeliveryModifier.Builder()
                        .withId(0L)
                        .withTimestamp(1L)
                        .withAction(
                                new Action.Builder()
                                        .withTimeModificationRule(
                                                new ValueModificationRule.Builder<Integer>()
                                                        .withOperation(ValueModificationRule.OperationEnum.ADD)
                                                        .withParameter(10.0)
                                                        .build()
                                        )
                                        .build()
                        )
                        .withCondition(
                                new Condition.Builder()
                                        .withCarrierIds(Set.of(22L))
                                        .build()
                        )
                        .build()
        );
    }

    private List<TariffInfoV1> createTestTariffs() {
        return asList(
                createTestCarrierTariff(asList(1, 2), asList(3, 4),
                        Map.of(1, DeliveryTariffStrategy.AUTO_CALCULATED, 2, DeliveryTariffStrategy.UNKNOWN_COST_TIME),
                        new HashMap<>()),
                createTestCarrierTariff(asList(3, 4), new ArrayList<>(),
                        Map.of(1, DeliveryTariffStrategy.UNKNOWN_COST_TIME, 2, DeliveryTariffStrategy.UNKNOWN_COST_TIME),
                        new HashMap<>()),
                createTestCarrierTariff(asList(5), new ArrayList<>(),
                        Map.of(1, DeliveryTariffStrategy.AUTO_CALCULATED, 2, DeliveryTariffStrategy.AUTO_CALCULATED),
                        new HashMap<>()),
                createTestCarrierTariff(asList(7, 8), new ArrayList<>(), new HashMap<>(),
                        Map.of(1, DeliveryTariffStrategy.AUTO_CALCULATED, 2, DeliveryTariffStrategy.AUTO_CALCULATED)),
                createTestCarrierTariff(asList(9, 10), List.of(11),
                        Map.of(1, DeliveryTariffStrategy.AUTO_CALCULATED),
                        Map.of(1, DeliveryTariffStrategy.AUTO_CALCULATED, 2, DeliveryTariffStrategy.AUTO_CALCULATED)),
                createTestRegularTariff()
        );
    }

    private List<DeliveryModifier> getExpectedModifiers() {
        return asList(
                createCarrierAvailabilityModifier(true, 1L, YaDeliveryTariffType.COURIER,
                        Sets.newHashSet(1, 2, 5, 9, 10)),
                createCarrierAvailabilityModifier(false, 1L, YaDeliveryTariffType.COURIER,
                        Sets.newHashSet(3, 4, 11, 10_000)),
                createCarrierAvailabilityModifier(true, 2L, YaDeliveryTariffType.COURIER, Sets.newHashSet(5)),
                createCarrierAvailabilityModifier(false, 2L, YaDeliveryTariffType.COURIER, Sets.newHashSet(10_000)),

                createCarrierAvailabilityModifier(true, 1L, YaDeliveryTariffType.PICKUP,
                        Sets.newHashSet(7, 8, 9, 10)),
                createCarrierAvailabilityModifier(false, 1L, YaDeliveryTariffType.PICKUP,
                        Sets.newHashSet(11, 10_000)),
                createCarrierAvailabilityModifier(true, 2L, YaDeliveryTariffType.PICKUP,
                        Sets.newHashSet(7, 8, 9, 10)),
                createCarrierAvailabilityModifier(false, 2L, YaDeliveryTariffType.PICKUP,
                        Sets.newHashSet(11, 10_000)));
    }

    private TariffInfoV1 createTestCarrierTariff(List<Integer> regionIds,
                                                 List<Integer> excludedRegions,
                                                 Map<Integer, DeliveryTariffStrategy> courierCarriers,
                                                 Map<Integer, DeliveryTariffStrategy> pickupCarriers) {
        TariffInfoV1 tariff = new TariffInfoV1();
        tariff.setModificationTime(new Date());
        tariff.setCurrency("RUR");
        tariff.setRules(singletonList(new DeliveryRule()));
        tariff.getRules().get(0).setIncludedRegions(regionIds);
        tariff.getRules().get(0).setExcludedRegions(excludedRegions);
        tariff.getRules().get(0).setOptions(singletonList(new DeliveryOption()));
        tariff.setStrategy(DeliveryTariffStrategy.UNKNOWN_COST_TIME);

        List<CarrierInfo> carriers = Stream.concat(
                courierCarriers.keySet().stream(),
                pickupCarriers.keySet().stream()
        ).distinct().map(carrierId -> {
            CarrierInfo carrierInfo = new CarrierInfo();
            carrierInfo.setId(carrierId);
            carrierInfo.setCourierDeliveryStrategy(courierCarriers.get(carrierId));
            carrierInfo.setPickupDeliveryStrategy(pickupCarriers.get(carrierId));
            return carrierInfo;
        }).collect(Collectors.toList());
        tariff.setCarrierInfos(carriers);

        return tariff;
    }

    private TariffInfoV1 createTestRegularTariff() {
        TariffInfoV1 tariff = new TariffInfoV1();
        tariff.setModificationTime(new Date());
        tariff.setCurrency("RUR");
        tariff.setRules(singletonList(new DeliveryRule()));
        tariff.getRules().get(0).setIncludedRegions(asList(1, 2));
        tariff.getRules().get(0).setOptions(singletonList(new DeliveryOption()));
        tariff.setStrategy(DeliveryTariffStrategy.UNKNOWN_COST_TIME);

        return tariff;
    }

    private DeliveryModifier createCarrierAvailabilityModifier(boolean isAvailable,
                                                               long carrierId,
                                                               YaDeliveryTariffType type,
                                                               @Nonnull Set<Integer> regions) {
        return new DeliveryModifier.Builder()
                .withId(0L)
                .withTimestamp(1575214500000L)
                .withAction(new Action.Builder()
                        .withIsCarrierTurnedOn(isAvailable)
                        .build())
                .withCondition(new Condition.Builder()
                        .withDeliveryTypes(Sets.newHashSet(type))
                        .withCarrierIds(Sets.newHashSet(carrierId))
                        .withDeliveryDestinations(regions)
                        .build())
                .build();
    }
}
