package ru.yandex.market.core.delivery.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.delivery.tariff.model.CategoryId;
import ru.yandex.market.core.delivery.tariff.model.CategoryRule;
import ru.yandex.market.core.delivery.tariff.model.DeliveryOption;
import ru.yandex.market.core.delivery.tariff.model.DeliveryRuleId;
import ru.yandex.market.core.delivery.tariff.model.DeliveryTariff;
import ru.yandex.market.core.delivery.tariff.model.OptionGroup;
import ru.yandex.market.core.delivery.tariff.model.PriceRule;
import ru.yandex.market.core.delivery.tariff.model.RegionGroup;
import ru.yandex.market.core.delivery.tariff.model.RegionGroupService;
import ru.yandex.market.core.delivery.tariff.model.TariffType;
import ru.yandex.market.core.delivery.tariff.model.WeightRule;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ActionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ConditionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryCostConditionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryModifierDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.PercentValueLimiterDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueLimiterDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueModificationRuleDto;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.CourierTariffType;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.CategoryIdDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.CategoryRuleDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ComparisonOperationDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierActionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierConditionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryServiceCodeDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryServiceStrategyDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryTariffDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ModifyDeliveryServicesRequestDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OperationTypeDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OptionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OptionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.PriceRuleDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupCreateRequestDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupUpdateRequestDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.SelectedDeliveryServiceDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.WeightRuleDto;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.delivery.tariff.model.DeliveryServiceStrategy.AUTO_CALCULATED;
import static ru.yandex.market.core.delivery.tariff.model.DeliveryServiceStrategy.UNKNOWN_COST_TIME;
import static ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ActionDto.DeliveryServiceCode.WAIT_20;

@DisplayName("Тесты на маппер доставочных моделей в тарификаторные сущности")
public class TarifficatorDtoConverterTest {

    @Test
    @DisplayName("Тест маппинга запроса на обновление связей рег. группы с СД")
    void testUpdateLinksWithDeliveryServices() {
        assertThat(TarifficatorDtoConverter.convertToModifyDeliveryServicesRequest(
                createServiceLinks(), Set.of(1L)))
                .usingRecursiveComparison()
                .isEqualTo(new ModifyDeliveryServicesRequestDto()
                        .deliveryServicesToUpdate(createExpectedServiceLinks())
                        .deliveryServicesToDelete(List.of(1L)));
    }

    @Test
    @DisplayName("Тест маппинга региональной группы в запрос создания рег. группы")
    void testCreateRegionGroupMapping() {
        assertThat(TarifficatorDtoConverter.convertRegionGroupToCreateRequest(createLocalRegionGroup()))
                .usingRecursiveComparison()
                .isEqualTo(createExpectedRegionGroupCreateRequest());
    }

    @Test
    @DisplayName("Тест маппинга региональной группы в запрос обновления рег. группы")
    void testUpdateRegionGroupMapping() {
        assertThat(TarifficatorDtoConverter.convertRegionGroupToUpdateRequest(createLocalRegionGroup()))
                .usingRecursiveComparison()
                .isEqualTo(createExpectedRegionGroupUpdateRequest());
    }

    @Test
    @DisplayName("Тест маппинга тарифа полностью заполненного")
    void testTariffMapping_fullTariff() {
        assertThat(TarifficatorDtoConverter.convertTariff(createTestTariff()))
                .usingRecursiveComparison()
                .isEqualTo(createExpectedTariff());
    }

    @Test
    @DisplayName("Тест маппинга тарифа, в котором доставка берется из фида")
    void testTariffMappingFromFeed() {
        assertThat(TarifficatorDtoConverter.convertTariff(createFromFeedTariff()))
                .usingRecursiveComparison()
                .isEqualTo(createFromFeedExpectedTariff());
    }

    private RegionGroup createLocalRegionGroup() {
        RegionGroup regionGroup = new RegionGroup();
        regionGroup.setSelfRegion(true);
        regionGroup.setName("region1");
        regionGroup.setIncludes(Set.of(213L));

        return regionGroup;
    }

    private RegionGroupCreateRequestDto createExpectedRegionGroupCreateRequest() {
        return new RegionGroupCreateRequestDto()
                .localRegion(true)
                .groupName("region1")
                .regions(List.of(213L));
    }

    private RegionGroupUpdateRequestDto createExpectedRegionGroupUpdateRequest() {
        return new RegionGroupUpdateRequestDto()
                .groupName("region1")
                .regions(List.of(213L));
    }

    private DeliveryTariff createFromFeedTariff() {
        DeliveryTariff tariff = new DeliveryTariff();

        tariff.setTariffType(TariffType.UNIFORM);
        tariff.setUseYml(true);

        return tariff;
    }

    private DeliveryTariffDto createFromFeedExpectedTariff() {
        return new DeliveryTariffDto()
                .tariffType(CourierTariffType.FROM_FEED)
                .useYml(true)
                .categoryRules(new ArrayList<>())
                .priceRules(new ArrayList<>())
                .weightRules(new ArrayList<>())
                .optionsGroups(new ArrayList<>());
    }

    private DeliveryTariffDto createExpectedTariff() {
        return new DeliveryTariffDto()
                .tariffType(CourierTariffType.WEIGHT_CATEGORY_PRICE)
                .notes("Notes")
                .useYml(false)
                .categoryRules(createExpectedCategoryRules())
                .priceRules(createExpectedPriceRules())
                .weightRules(createExpectedWeightRules())
                .optionsGroups(createExpectedOptionGroups());
    }

    private DeliveryTariff createTestTariff() {
        DeliveryTariff tariff = new DeliveryTariff();

        tariff.setTariffType(TariffType.WEIGHT_CATEGORY_PRICE);
        tariff.setNotes("Notes");
        tariff.setCategoryRules(createTestCategoryRules());
        tariff.setPriceRules(createTestPriceRules());
        tariff.setWeightRules(createTestWeightRules());
        tariff.setUseYml(false);
        tariff.setOptionsGroups(createOptionGroups());

        return tariff;
    }

    private List<OptionGroup> createOptionGroups() {
        return Arrays.asList(
                new OptionGroup(null, 1L, (short) 1, (short) 2, (short) 1, false, null),
                new OptionGroup(null, 1L, (short) 1, (short) 1, (short) 2, true, createOptions())
        );
    }

    private Collection<DeliveryOption> createOptions() {
        return singletonList(
                new DeliveryOption(0L, (short) 1, BigDecimal.valueOf(100), (short) 1, (short) 2, (byte) 13)
        );
    }

    private List<OptionGroupDto> createExpectedOptionGroups() {
        return Arrays.asList(
                new OptionGroupDto()
                        .categoryOrderNum(1)
                        .priceOrderNum(2)
                        .weightOrderNum(1)
                        .hasDelivery(false)
                        .options(new ArrayList<>()),
                new OptionGroupDto()
                        .categoryOrderNum(1)
                        .priceOrderNum(1)
                        .weightOrderNum(2)
                        .hasDelivery(true)
                        .options(singletonList(
                                new OptionDto()
                                        .orderNum(1)
                                        .orderBeforeHour(13)
                                        .daysFrom(1)
                                        .daysTo(2)
                                        .cost(BigDecimal.valueOf(100))
                        ))
        );
    }


    private WeightRule[] createTestWeightRules() {
        return new WeightRule[]{
                new WeightRule(new DeliveryRuleId(1L, 1), null, 5),
                new WeightRule(new DeliveryRuleId(1L, 2), 5, null)
        };
    }

    private List<WeightRuleDto> createExpectedWeightRules() {
        return Arrays.asList(
                new WeightRuleDto()
                        .weightTo(5)
                        .orderNum(1),
                new WeightRuleDto()
                        .weightFrom(5)
                        .orderNum(2)
        );
    }

    private PriceRule[] createTestPriceRules() {
        return new PriceRule[]{
                new PriceRule(new DeliveryRuleId(1L, 1), null, BigDecimal.valueOf(100.20)),
                new PriceRule(new DeliveryRuleId(1L, 2), BigDecimal.valueOf(100.20), null)
        };
    }

    private List<PriceRuleDto> createExpectedPriceRules() {
        return Arrays.asList(
                new PriceRuleDto()
                        .priceTo(BigDecimal.valueOf(100.20))
                        .orderNum(1),
                new PriceRuleDto()
                        .priceFrom(BigDecimal.valueOf(100.20))
                        .orderNum(2)
        );
    }

    private CategoryRule[] createTestCategoryRules() {
        CategoryRule categoryRuleWithMissedCategories = new CategoryRule(
                new DeliveryRuleId(1L, 2),
                new TreeSet<>(Set.of(
                        new CategoryId("cat1", 1L),
                        new CategoryId("cat2", 2L),
                        new CategoryId(null, 2L)
                        )
                )
        );
        categoryRuleWithMissedCategories.getMissing().add(new CategoryId("cat3", 2L));

        return new CategoryRule[]{
                new CategoryRule(new DeliveryRuleId(1L, 1), true),
                categoryRuleWithMissedCategories
        };
    }

    private List<CategoryRuleDto> createExpectedCategoryRules() {
        return Arrays.asList(
                new CategoryRuleDto()
                        .orderNum(1)
                        .includes(new ArrayList<>())
                        .others(true),
                new CategoryRuleDto()
                        .others(false)
                        .orderNum(2)
                        .includes(
                                List.of(
                                        new CategoryIdDto()
                                                .categoryId("cat1")
                                                .feedId(1L),
                                        new CategoryIdDto()
                                                .categoryId("cat2")
                                                .feedId(2L),
                                        new CategoryIdDto()
                                                .categoryId("cat3")
                                                .feedId(2L)
                                )
                        )
        );
    }

    private DeliveryModifierDto createTestModifier() {
        return new DeliveryModifierDto.Builder()
                .withAction(new ActionDto.Builder()
                        .withTimeModificationRule(new ValueModificationRuleDto.Builder()
                                .withOperation(ValueModificationRuleDto.OperationEnum.ADD)
                                .withParameter(BigDecimal.valueOf(2))
                                .build())
                        .withCostModificationRule(new ValueModificationRuleDto.Builder()
                                .withOperation(ValueModificationRuleDto.OperationEnum.SUBSTRACT)
                                .build())
                        .withIsCarrierTurnedOn(true)
                        .withPaidByCustomerServices(Set.of(WAIT_20))
                        .build())
                .withId(100L)
                .withTimestamp(1583325200238L)
                .withCondition(new ConditionDto.Builder()
                        .withCarrierIds(Set.of(223L))
                        .withCost(new PercentValueLimiterDto.Builder()
                                .withMinValue(BigDecimal.ONE)
                                .withMaxValue(BigDecimal.valueOf(200))
                                .withPercent(BigDecimal.valueOf(20))
                                .build())
                        .withDeliveryCost(
                                new DeliveryCostConditionDto.Builder()
                                        .withPercentFromOfferPrice(10.0)
                                        .withComparisonOperation(DeliveryCostConditionDto.ComparisonOperation.LESS)
                                        .build())
                        .withWeight(new ValueLimiterDto.Builder()
                                .withMinValue(BigDecimal.valueOf(10))
                                .withMaxValue(BigDecimal.valueOf(20))
                                .build())
                        .withChargeableWeight(new ValueLimiterDto.Builder()
                                .withMinValue(BigDecimal.valueOf(20))
                                .withMaxValue(BigDecimal.valueOf(30))
                                .build())
                        .withDimension(new ValueLimiterDto.Builder()
                                .withMinValue(BigDecimal.valueOf(30))
                                .withMaxValue(BigDecimal.valueOf(40))
                                .build())
                        .withDeliveryDestinations(Set.of(213))
                        .build()
                )
                .build();
    }

    private ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto createExpectedModifier() {
        return new ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto()
                .action(new DeliveryModifierActionDto()
                        .timeModificationRule(
                                new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueModificationRuleDto()
                                        .operation(OperationTypeDto.ADD)
                                        .parameter(BigDecimal.valueOf(2)))
                        .costModificationRule(
                                new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueModificationRuleDto()
                                        .operation(OperationTypeDto.SUBTRACT))
                        .isCarrierTurnedOn(true)
                        .paidByCustomerServices(List.of(DeliveryServiceCodeDto.WAIT_20)))
                .id(100L)
                .timestamp(1583325200238L)
                .condition(new DeliveryModifierConditionDto()
                        .carrierIds(List.of(223L))
                        .cost(new ru.yandex.market.logistics.tarificator.open.api.client.api.model.PercentValueLimiterDto()
                                .minValue(BigDecimal.ONE)
                                .maxValue(BigDecimal.valueOf(200))
                                .percent(BigDecimal.valueOf(20)))
                        .deliveryCost(
                                new ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryCostConditionDto()
                                        .percentFromOfferPrice(BigDecimal.valueOf(10.0))
                                        .comparisonOperation(ComparisonOperationDto.LESS))
                        .weight(new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto()
                                .minValue(BigDecimal.valueOf(10))
                                .maxValue(BigDecimal.valueOf(20)))
                        .chargeableWeight(new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto()
                                .minValue(BigDecimal.valueOf(20))
                                .maxValue(BigDecimal.valueOf(30)))
                        .dimension(new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto()
                                .minValue(BigDecimal.valueOf(30))
                                .maxValue(BigDecimal.valueOf(40)))
                        .deliveryDestinations(List.of(213))
                        .deliveryTypes(new ArrayList<>())
                );
    }

    private List<RegionGroupService> createServiceLinks() {
        return Arrays.asList(
                new RegionGroupService(1L, 99L, AUTO_CALCULATED,
                        UNKNOWN_COST_TIME, singletonList(createTestModifier()), null),
                new RegionGroupService(1L, 99L, AUTO_CALCULATED,
                        UNKNOWN_COST_TIME, null, singletonList(createTestModifier()))
        );
    }

    private List<SelectedDeliveryServiceDto> createExpectedServiceLinks() {
        return Arrays.asList(
                new SelectedDeliveryServiceDto()
                        .deliveryServiceId(99L)
                        .courierDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED)
                        .addCourierDeliveryModifiersItem(createExpectedModifier())
                        .pickupDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME)
                        .pickupDeliveryModifiers(new ArrayList<>()),
                new SelectedDeliveryServiceDto()
                        .deliveryServiceId(99L)
                        .courierDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED)
                        .courierDeliveryModifiers(new ArrayList<>())
                        .pickupDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME)
                        .addPickupDeliveryModifiersItem(createExpectedModifier())
        );
    }
}
