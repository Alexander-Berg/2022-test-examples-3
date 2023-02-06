package ru.yandex.market.delivery;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.delivery.RetryableTarifficatorClient;
import ru.yandex.market.core.delivery.converter.DeliveryCalculatorModelConverter;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.deliverycalculator.indexerclient.DeliveryCalculatorIndexerClient;
import ru.yandex.market.deliverycalculator.indexerclient.DeliveryCalculatorIndexerClientXmlUtils;
import ru.yandex.market.deliverycalculator.indexerclient.model.CarrierInfo;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeleteShopRulesRequest;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRule;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRuleFeedCategory;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRuleFeedCategoryItem;
import ru.yandex.market.deliverycalculator.indexerclient.model.FeedInfo;
import ru.yandex.market.deliverycalculator.indexerclient.model.ShopDeliveryInfo;
import ru.yandex.market.deliverycalculator.indexerclient.model.ShopPlacementProgram;
import ru.yandex.market.deliverycalculator.indexerclient.model.TariffInfoV1;
import ru.yandex.market.deliverycalculator.indexerclient.model.UpdateShopRulesRequest;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.YaDeliveryTariffTypeDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ActionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ConditionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryCostConditionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryModifierDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.PercentValueLimiterDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueLimiterDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueModificationRuleDto;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffStrategy;
import ru.yandex.market.logistics.tarifficator.model.enums.YaDeliveryTariffType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.CourierTariffType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupStatus;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.CategoryIdDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.CategoryRuleDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ComparisonOperationDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierActionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierConditionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryServiceCodeDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryServiceStrategyDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryTariffDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OperationTypeDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OptionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OptionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.PriceRuleDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.SelectedDeliveryServiceDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopRegionGroupsDto;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link DeliveryTariffsToCalculatorExecutor}.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class DeliveryTariffsToCalculatorExecutorFunctionalTest extends FunctionalTest {

    @Autowired
    private DeliveryTariffsToCalculatorExecutor executor;

    @Autowired
    private DeliveryCalculatorIndexerClient indexerClient;

    @Autowired
    private RetryableTarifficatorClient retryableTarifficatorClient;

    @Autowired
    private EnvironmentService environmentService;

    private static List<DeliveryModifierDto> getModifiers() {
        final ValueLimiterDto zeroTenValueLimiter = new ValueLimiterDto.Builder()
                .withMinValue(BigDecimal.ZERO)
                .withMaxValue(BigDecimal.TEN).build();

        final ActionDto fullAction = new ActionDto.Builder()
                .withCostModificationRule(new ValueModificationRuleDto.Builder()
                        .withOperation(ValueModificationRuleDto.OperationEnum.SUBSTRACT)
                        .withParameter(BigDecimal.valueOf(12.97))
                        .withResultLimit(new ValueLimiterDto(BigDecimal.ZERO, BigDecimal.TEN))
                        .build())
                .withTimeModificationRule(new ValueModificationRuleDto.Builder()
                        .withOperation(ValueModificationRuleDto.OperationEnum.SUBSTRACT)
                        .withParameter(BigDecimal.valueOf(12.97))
                        .withResultLimit(new ValueLimiterDto(BigDecimal.ZERO, BigDecimal.TEN))
                        .build())
                .withPaidByCustomerServices(Set.of(ActionDto.DeliveryServiceCode.INSURANCE))
                .withIsCarrierTurnedOn(true)
                .build();

        final ConditionDto fullCondition = new ConditionDto.Builder()
                .withCost(new PercentValueLimiterDto.Builder().withMinValue(BigDecimal.ZERO)
                        .withMaxValue(BigDecimal.valueOf(98)).withPercent(BigDecimal.valueOf(95.5)).build())
                .withDeliveryCost(new DeliveryCostConditionDto.Builder().withPercentFromOfferPrice(50.0)
                        .withComparisonOperation(DeliveryCostConditionDto.ComparisonOperation.MORE).build())
                .withWeight(zeroTenValueLimiter)
                .withChargeableWeight(zeroTenValueLimiter)
                .withDimension(zeroTenValueLimiter)
                .withCarrierIds(Set.of(51L))
                .withDeliveryDestinations(Set.of(213))
                .withDeliveryTypes(Set.of(YaDeliveryTariffTypeDTO.COURIER)).build();

        final ActionDto simpleActionFixValue = new ActionDto.Builder()
                .withCostModificationRule(new ValueModificationRuleDto.Builder()
                        .withOperation(ValueModificationRuleDto.OperationEnum.FIX_VALUE)
                        .withParameter(BigDecimal.valueOf(2000L))
                        .build())
                .withPaidByCustomerServices(Collections.emptySet())
                .withIsCarrierTurnedOn(true)
                .build();

        final ActionDto simpleActionMultiply = new ActionDto.Builder()
                .withTimeModificationRule(new ValueModificationRuleDto.Builder()
                        .withOperation(ValueModificationRuleDto.OperationEnum.MULTIPLY)
                        .withParameter(BigDecimal.valueOf(2L))
                        .build())
                .withPaidByCustomerServices(Collections.emptySet())
                .withIsCarrierTurnedOn(true)
                .build();

        final ConditionDto carrierConditionPickup = new ConditionDto.Builder()
                .withCarrierIds(Set.of(48L))
                .withDeliveryDestinations(Set.of(2141, 21421, 21521, 2151, 216))
                .withDeliveryTypes(Set.of(YaDeliveryTariffTypeDTO.PICKUP))
                .build();

        final ConditionDto carrierConditionCourier = new ConditionDto.Builder()
                .withCarrierIds(Set.of(48L))
                .withDeliveryDestinations(Set.of(2141, 21421, 21521, 2151, 216))
                .withDeliveryTypes(Set.of(YaDeliveryTariffTypeDTO.COURIER))
                .build();

        return List.of(
                new DeliveryModifierDto.Builder()
                        .withAction(fullAction)
                        .withCondition(fullCondition)
                        .withId(100L)
                        .withTimestamp(1583325200238L)
                        .build(),
                new DeliveryModifierDto.Builder()
                        .withAction(fullAction)
                        .withCondition(fullCondition)
                        .withId(101L)
                        .withTimestamp(1583325200238L)
                        .build(),
                new DeliveryModifierDto.Builder()
                        .withAction(simpleActionFixValue)
                        .withCondition(carrierConditionCourier)
                        .withId(0L)
                        .withTimestamp(1583325200238L)
                        .build(),
                new DeliveryModifierDto.Builder()
                        .withAction(simpleActionMultiply)
                        .withCondition(carrierConditionPickup)
                        .withId(0L)
                        .withTimestamp(1583325200238L)
                        .build()
        );
    }

    /**
     * Тест проверяет, что при пустом списке авторасчетных служб функциональность авторасчета выключена.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportAutoCalculated.before.csv")
    void testExportAutoCalculatedInExcludeList() {
        RegionGroupDto regionGroupDto1 = buildRegionGroupDto(
                331000L,
                123L,
                false,
                true,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.NEW,
                Currency.BYR
        );
        RegionGroupDto regionGroupDto2 = buildRegionGroupDto(
                331001L,
                123L,
                true,
                true,
                List.of(225L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.NEW,
                Currency.BYR
        );
        ShopRegionGroupsDto shopRegionGroupsDto123 = new ShopRegionGroupsDto();
        shopRegionGroupsDto123.setRegionsGroups(List.of(regionGroupDto1, regionGroupDto2));
        ShopRegionGroupsDto shopRegionGroupsDto124 = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto123);
        when(retryableTarifficatorClient.getShopRegionGroups(124L)).thenReturn(shopRegionGroupsDto124);

        List<ShopDeliveryInfo> shopDeliveryInfoList = exportDataToDeliveryCalculator(2);

        ShopDeliveryInfo shopDeliveryInfo = shopDeliveryInfoList.get(0);
        assertThat(hasAutoCalculatedCourier(shopDeliveryInfo), equalTo(false));
        assertThat(shopDeliveryInfo.getFeeds(), containsInAnyOrder(
                hasProperty("id", equalTo(1L)),
                hasProperty("id", equalTo(2L)))
        );

        shopDeliveryInfo = shopDeliveryInfoList.get(1);
        assertThat(shopDeliveryInfo.getFeeds(), contains(
                hasProperty("id", equalTo(3L))
        ));
    }

    /**
     * Тест проверяет, что магазин участвует в авторасчете весогабаритов.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportAutoCalculated.before.csv")
    void testExportAutoCalculated() {
        RegionGroupDto regionGroupDto1 = buildRegionGroupDto(
                331000L,
                123L,
                false,
                false,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.RUR
        );
        RegionGroupDto regionGroupDto2 = buildRegionGroupDto(
                331001L,
                123L,
                true,
                false,
                List.of(225L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.RUR
        );
        ShopRegionGroupsDto shopRegionGroupsDto123 = new ShopRegionGroupsDto();
        shopRegionGroupsDto123.setRegionsGroups(List.of(regionGroupDto1, regionGroupDto2));
        ShopRegionGroupsDto shopRegionGroupsDto124 = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto123);
        when(retryableTarifficatorClient.getShopRegionGroups(124L)).thenReturn(shopRegionGroupsDto124);

        SelectedDeliveryServiceDto deliveryService511 = new SelectedDeliveryServiceDto();
        deliveryService511.setDeliveryServiceId(51L);
        deliveryService511.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED);
        deliveryService511.setPickupDeliveryStrategy(null);
        SelectedDeliveryServiceDto deliveryService512 = new SelectedDeliveryServiceDto();
        deliveryService512.setDeliveryServiceId(51L);
        deliveryService512.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME);
        deliveryService512.setPickupDeliveryStrategy(null);

        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331000L, 11L))
                .thenReturn(List.of(deliveryService511));
        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331001L, 11L))
                .thenReturn(List.of(deliveryService512));

        final ShopDeliveryInfo shopDeliveryInfo = exportDataToDeliveryCalculator(2).get(0);
        assertThat(hasAutoCalculatedCourier(shopDeliveryInfo), equalTo(true));
    }

    /**
     * Тест проверяет, что у магазина указывается тип кампании.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportAutoCalculated.before.csv")
    void testExportCampaignType() {
        RegionGroupDto regionGroupDto1 = buildRegionGroupDto(
                331000L,
                123L,
                false,
                true,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.NEW,
                Currency.BYR
        );
        RegionGroupDto regionGroupDto2 = buildRegionGroupDto(
                331001L,
                123L,
                true,
                true,
                List.of(225L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.NEW,
                Currency.BYR
        );
        ShopRegionGroupsDto shopRegionGroupsDto123 = new ShopRegionGroupsDto();
        shopRegionGroupsDto123.setRegionsGroups(List.of(regionGroupDto1, regionGroupDto2));
        ShopRegionGroupsDto shopRegionGroupsDto124 = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto123);
        when(retryableTarifficatorClient.getShopRegionGroups(124L)).thenReturn(shopRegionGroupsDto124);

        ShopDeliveryInfo shopDeliveryInfo = exportDataToDeliveryCalculator(2).get(0);
        assertThat(shopDeliveryInfo.getCampaignType(), is(CampaignType.SHOP.getId()));
    }

    /**
     * Тест проверяет, что магазин с не разрешенной СД не участвует в авторасчете весогабаритов.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportAutoCalculated.before.csv")
    void testExportAutoCalculatedNotEnabledCarrier() {
        RegionGroupDto regionGroupDto1 = buildRegionGroupDto(
                331000L,
                123L,
                false,
                true,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.NEW,
                Currency.BYR
        );
        RegionGroupDto regionGroupDto2 = buildRegionGroupDto(
                331001L,
                123L,
                true,
                true,
                List.of(225L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.NEW,
                Currency.BYR
        );
        ShopRegionGroupsDto shopRegionGroupsDto123 = new ShopRegionGroupsDto();
        shopRegionGroupsDto123.setRegionsGroups(List.of(regionGroupDto1, regionGroupDto2));
        ShopRegionGroupsDto shopRegionGroupsDto124 = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto123);
        when(retryableTarifficatorClient.getShopRegionGroups(124L)).thenReturn(shopRegionGroupsDto124);

        final ShopDeliveryInfo shopDeliveryInfo = exportDataToDeliveryCalculator(2).get(0);
        assertThat(hasAutoCalculatedCourier(shopDeliveryInfo), equalTo(false));
    }

    /**
     * Тест проверяет, что магазин с не разрешенным локальным регионом для СД не участвует в авторасчете весогабаритов.
     */
    @Test
    @DbUnitDataSet(before = {"data/testExportAutoCalculated.notEnabledRegion.before.csv"})
    void testExportAutoCalculatedNotEnabledLocalRegion() {
        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto);

        final ShopDeliveryInfo shopDeliveryInfo = exportDataToDeliveryCalculator(1).get(0);
        assertThat(hasAutoCalculatedCourier(shopDeliveryInfo), equalTo(false));
    }

    /**
     * Тест проверяет, что магазин не участвует в авторасчете весогабаритов.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportAutoCalculated.before.csv")
    void testExportNotAutoCalculated() {
        RegionGroupDto regionGroupDto1 = buildRegionGroupDto(
                331000L,
                123L,
                false,
                true,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.NEW,
                Currency.BYR
        );
        RegionGroupDto regionGroupDto2 = buildRegionGroupDto(
                331001L,
                123L,
                true,
                true,
                List.of(225L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.NEW,
                Currency.BYR
        );
        ShopRegionGroupsDto shopRegionGroupsDto123 = new ShopRegionGroupsDto();
        shopRegionGroupsDto123.setRegionsGroups(List.of(regionGroupDto1, regionGroupDto2));

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto123);
        when(retryableTarifficatorClient.getShopRegionGroups(124L)).thenReturn(shopRegionGroupsDto123);

        final ShopDeliveryInfo shopDeliveryInfo = exportDataToDeliveryCalculator(2).get(0);
        assertThat(hasAutoCalculatedCourier(shopDeliveryInfo), equalTo(false));
    }

    /**
     * Тест проверяет, что магазин участвует в авторасчете ПВЗ
     * и в тарифах ему проставляется стратегия авторасчета самовывоза для авторасчетных СД.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportAutoCalculatedPickup.before.csv")
    void testExportAutoCalculatedPickup() {

        RegionGroupDto regionGroupDto1 = buildRegionGroupDto(
                331000L,
                123L,
                false,
                false,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.RUR
        );
        RegionGroupDto regionGroupDto2 = buildRegionGroupDto(
                331001L,
                123L,
                true,
                false,
                List.of(225L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.RUR
        );
        RegionGroupDto regionGroupDto3 = buildRegionGroupDto(
                331002L,
                123L,
                false,
                false,
                List.of(225L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.RUR
        );

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        shopRegionGroupsDto.setRegionsGroups(List.of(regionGroupDto1, regionGroupDto2, regionGroupDto3));

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto);

        SelectedDeliveryServiceDto deliveryService106 = new SelectedDeliveryServiceDto();
        deliveryService106.setDeliveryServiceId(106L);
        deliveryService106.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME);
        deliveryService106.setPickupDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED);
        SelectedDeliveryServiceDto deliveryService601 = new SelectedDeliveryServiceDto();
        deliveryService601.setDeliveryServiceId(601L);
        deliveryService601.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME);
        deliveryService601.setPickupDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED);

        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331000L, 11L))
                .thenReturn(List.of(deliveryService106));
        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331001L, 11L))
                .thenReturn(List.of(deliveryService106));
        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331002L, 11L))
                .thenReturn(List.of(deliveryService601));

        final List<Integer> autoCalculatedCarriers = List.of(106, 601);

        final ShopDeliveryInfo shopDeliveryInfo = exportDataToDeliveryCalculator(1).get(0);
        Assertions.assertTrue(hasAutoCalculatedPickup(shopDeliveryInfo));

        final Set<CarrierInfo> carrierInfos = shopDeliveryInfo.getTariffInfos().stream()
                .flatMap(ti -> ti.getCarrierInfos().stream()).collect(Collectors.toSet());
        assertThat(carrierInfos, Matchers.not(Matchers.empty()));

        final Set<CarrierInfo> autoCalcCarrierInfos = carrierInfos.stream()
                .filter(carrier -> autoCalculatedCarriers.contains(carrier.getId()))
                .filter(carrier -> carrier.getPickupDeliveryStrategy() == DeliveryTariffStrategy.AUTO_CALCULATED)
                .collect(Collectors.toSet());

        assertEquals(autoCalculatedCarriers.size(), autoCalcCarrierInfos.size());
    }

    /**
     * Тест проверяет, что настройки ЯДо корректно выгружаются в калькулятор доставки.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportYaDeliverySettingsForShop.before.csv")
    void testExportYaDeliverySettingsForShop() {
        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(1110L)).thenReturn(shopRegionGroupsDto);

        final ShopDeliveryInfo shopDeliveryInfo = exportDataToDeliveryCalculator(1).get(0);
        assertThat(shopDeliveryInfo.isUseYaDeliverySettings(), equalTo(true));
        assertThat(shopDeliveryInfo.getSenderId(), equalTo(3456L));
    }

    /**
     * Тест проверяет, что правила доставки для дропшип выгружаются в индексатор.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportDropship.before.csv", after = "data/testExportDropship.after.csv")
    void testExportDropship() {
        ShopDeliveryInfo dropship = exportDataToDeliveryCalculator(1).get(0);

        assertThat(dropship.getId(), equalTo(99L));
        assertThat(dropship.getCampaignType(), is(CampaignType.SUPPLIER.getId()));
        assertEquals(RegionConstants.MOSCOW, dropship.getRegionId());
        assertEquals("RUR", dropship.getCourierCurrency());

        assertEquals(1, dropship.getFeeds().size());
        assertEquals(55, dropship.getFeeds().get(0).getId());

        assertFalse(hasAutoCalculatedCourier(dropship));
        assertFalse(hasAutoCalculatedPickup(dropship));
        assertFalse(dropship.isUseYmlOptions());
        assertFalse(dropship.isUnitedCatalog());
        assertEquals(0, dropship.getPlacementPrograms().size());
    }

    /**
     * Тест проверяет, что для магазина выгружаются СД.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportDeliveryServices.before.csv")
    void testExportDeliveryServices() {
        RegionGroupDto regionGroupDto1 = buildRegionGroupDto(
                331000L,
                123L,
                false,
                false,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.BYR
        );
        RegionGroupDto regionGroupDto2 = buildRegionGroupDto(
                331001L,
                123L,
                true,
                false,
                List.of(225L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.BYR
        );
        ShopRegionGroupsDto shopRegionGroupsDto123 = new ShopRegionGroupsDto();
        shopRegionGroupsDto123.setRegionsGroups(List.of(regionGroupDto1, regionGroupDto2));

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto123);

        SelectedDeliveryServiceDto deliveryService51 = new SelectedDeliveryServiceDto();
        deliveryService51.setDeliveryServiceId(51L);
        deliveryService51.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME);
        deliveryService51.setPickupDeliveryStrategy(null);
        SelectedDeliveryServiceDto deliveryService99 = new SelectedDeliveryServiceDto();
        deliveryService99.setDeliveryServiceId(99L);
        deliveryService99.setCourierDeliveryStrategy(null);
        deliveryService99.setPickupDeliveryStrategy(null);

        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331000L, 11L))
                .thenReturn(List.of(deliveryService51, deliveryService99));
        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331001L, 11L))
                .thenReturn(Collections.emptyList());

        final ShopDeliveryInfo shopDeliveryInfo = exportDataToDeliveryCalculator(1).get(0);

        final List<CarrierInfo> actual = shopDeliveryInfo.getTariffInfos().stream().map(TariffInfoV1::getCarrierInfos)
                .flatMap(List::stream).collect(Collectors.toList());
        final List<CarrierInfo> expected = Arrays.asList(
                DeliveryCalculatorModelConverter.createCarrierInfo(51, DeliveryTariffStrategy.UNKNOWN_COST_TIME, null)
        );
        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    /**
     * Тест проверяет, что для магазина выгружаются СД в локальном регионе.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportDeliveryServices.before.csv")
    void testExportDeliveryServicesWithLocalRegion() {
        RegionGroupDto regionGroupDto1 = buildRegionGroupDto(
                331000L,
                123L,
                false,
                false,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.BYR
        );
        RegionGroupDto regionGroupDto2 = buildRegionGroupDto(
                331001L,
                123L,
                true,
                false,
                List.of(225L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.BYR
        );
        ShopRegionGroupsDto shopRegionGroupsDto123 = new ShopRegionGroupsDto();
        shopRegionGroupsDto123.setRegionsGroups(List.of(regionGroupDto1, regionGroupDto2));

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto123);

        SelectedDeliveryServiceDto deliveryService511 = new SelectedDeliveryServiceDto();
        deliveryService511.setDeliveryServiceId(51L);
        deliveryService511.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME);
        deliveryService511.setPickupDeliveryStrategy(null);
        SelectedDeliveryServiceDto deliveryService512 = new SelectedDeliveryServiceDto();
        deliveryService512.setDeliveryServiceId(51L);
        deliveryService512.setCourierDeliveryStrategy(null);
        deliveryService512.setPickupDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED);
        SelectedDeliveryServiceDto deliveryService99 = new SelectedDeliveryServiceDto();
        deliveryService99.setDeliveryServiceId(99L);
        deliveryService99.setCourierDeliveryStrategy(null);
        deliveryService99.setPickupDeliveryStrategy(null);

        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331000L, 11L))
                .thenReturn(List.of(deliveryService511, deliveryService512, deliveryService99));
        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331001L, 11L))
                .thenReturn(Collections.emptyList());

        environmentService.setValue("use.all.region.groups", "true");

        final ShopDeliveryInfo shopDeliveryInfo = exportDataToDeliveryCalculator(1).get(0);

        final List<CarrierInfo> actual = shopDeliveryInfo.getTariffInfos().stream().map(TariffInfoV1::getCarrierInfos)
                .flatMap(List::stream).collect(Collectors.toList());
        final List<CarrierInfo> expected = Arrays.asList(
                DeliveryCalculatorModelConverter.createCarrierInfo(51, DeliveryTariffStrategy.UNKNOWN_COST_TIME, null),
                DeliveryCalculatorModelConverter.createCarrierInfo(51, null, DeliveryTariffStrategy.AUTO_CALCULATED)
        );
        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    /**
     * Тест проверяет, что для магазинов выгружается валюта курьерской доставки.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportCourierDeliveryCurrency.before.csv")
    void testExportCourierDeliveryCurrency() {
        RegionGroupDto regionGroupDto = buildRegionGroupDto(
                331000L,
                1111L,
                false,
                false,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.BYR
        );

        ShopRegionGroupsDto shopRegionGroupsDto1 = new ShopRegionGroupsDto();
        shopRegionGroupsDto1.setRegionsGroups(List.of(regionGroupDto));

        ShopRegionGroupsDto shopRegionGroupsDto2 = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(1111L)).thenReturn(shopRegionGroupsDto1);
        when(retryableTarifficatorClient.getShopRegionGroups(1112L)).thenReturn(shopRegionGroupsDto2);
        when(retryableTarifficatorClient.getShopRegionGroups(1113L)).thenReturn(shopRegionGroupsDto2);

        final List<ShopDeliveryInfo> shopDeliveryInfos = exportDataToDeliveryCalculator(3);

        // берём валюту из тарифа
        ShopDeliveryInfo shop = shopDeliveryInfos.get(0);
        assertEquals(1111, shop.getId());
        assertEquals("BYR", shop.getCourierCurrency());

        // берём валюту по-умолчанию
        shop = shopDeliveryInfos.get(1);
        assertEquals(1112, shop.getId());
        assertEquals("RUR", shop.getCourierCurrency());
    }

    /**
     * Тест проверяет, что для магазинов выгружается валюта курьерской доставки.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportSelfTariffWithNullPrice.before.csv")
    void testExportSelfTariffWithZeroDeliveryCost() {
        RegionGroupDto regionGroupDto = buildRegionGroupDto(
                331000L,
                123L,
                false,
                false,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.BYR
        );

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        shopRegionGroupsDto.setRegionsGroups(List.of(regionGroupDto));

        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto);

        SelectedDeliveryServiceDto deliveryService99 = new SelectedDeliveryServiceDto();
        deliveryService99.setDeliveryServiceId(99L);
        deliveryService99.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME);
        deliveryService99.setPickupDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME);

        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331000L, 11L))
                .thenReturn(List.of(deliveryService99));

        OptionDto optionDto = new OptionDto();
        optionDto.setCost(null);
        optionDto.setOrderBeforeHour(1);
        optionDto.setOrderNum(1);
        optionDto.setDaysFrom(1);
        optionDto.setDaysTo(2);

        OptionGroupDto optionGroupDto = new OptionGroupDto();
        optionGroupDto.setOptions(List.of(optionDto));
        optionGroupDto.setHasDelivery(false);

        DeliveryTariffDto deliveryTariffDto = new DeliveryTariffDto();
        deliveryTariffDto.setTariffType(CourierTariffType.UNIFORM);
        deliveryTariffDto.setOptionsGroups(List.of(optionGroupDto));
        when(retryableTarifficatorClient.getRegionGroupTariff(123L, 331000L, 11L))
                .thenReturn(deliveryTariffDto);

        final List<ShopDeliveryInfo> shopDeliveryInfos = exportDataToDeliveryCalculator(1);

        assertNotNull(shopDeliveryInfos);
        assertEquals(1, shopDeliveryInfos.size());
        assertNotNull(shopDeliveryInfos.get(0).getTariffInfos());
        assertEquals(1, shopDeliveryInfos.get(0).getTariffInfos().size());
        assertNotNull(shopDeliveryInfos.get(0).getTariffInfos().get(0).getRules());
        assertEquals(1, shopDeliveryInfos.get(0).getTariffInfos().get(0).getRules().size());

        DeliveryRule rule = shopDeliveryInfos.get(0).getTariffInfos().get(0).getRules().get(0);
        assertNotNull(rule.getOptions());
        assertEquals(1, rule.getOptions().size());
        assertNull(rule.getOptions().get(0).getDeliveryCost());
    }

    /**
     * Тест проверяет, что для магазинов выгружается валюта курьерской доставки.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportUcatDsbs.before.csv")
    void testExportUcatDsbs() {
        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(99L)).thenReturn(shopRegionGroupsDto);

        final List<ShopDeliveryInfo> shopDeliveryInfos = exportDataToDeliveryCalculator(1);

        ShopDeliveryInfo shop = shopDeliveryInfos.get(0);
        assertEquals(99, shop.getId());
        assertTrue(shop.isUnitedCatalog(), "isUnitedCatalog");
        assertThat(shop.getPlacementPrograms(), contains(ShopPlacementProgram.DROPSHIP_BY_SELLER));
    }

    /**
     * Тест проверяет, что ДБС магазины без фф линки не выгружаются в КД.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportDsbsWithoutFFlink.before.csv")
    void testDbsWithoutFfLinkNotExported() {
        executor.doJob(null);

        verify(indexerClient, Mockito.never()).updateShopDeliveryRules(any());
    }

    /**
     * Тест проверяет, что для магазинов корректно выгружается список модификаторов доставки.
     */
    @Test
    @DbUnitDataSet(before = "data/testExportDeliveryModifiers.before.csv")
    void testExportDeliveryModifiers() {
        RegionGroupDto regionGroupDto1 = buildRegionGroupDto(
                331000L,
                123L,
                false,
                false,
                List.of(213L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.BYR
        );
        RegionGroupDto regionGroupDto2 = buildRegionGroupDto(
                331001L,
                123L,
                true,
                false,
                List.of(225L),
                List.of(213L, 21422L, 21522L),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.BYR
        );
        RegionGroupDto regionGroupDto3 = buildRegionGroupDto(
                331002L,
                123L,
                true,
                false,
                List.of(21422L, 21522L),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.SUCCESS,
                Currency.BYR
        );
        RegionGroupDto regionGroupDto4 = buildRegionGroupDto(
                331003L,
                123L,
                false,
                false,
                List.of(),
                List.of(),
                CourierTariffType.DEFAULT,
                RegionGroupStatus.FAIL,
                Currency.BYR
        );

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        ShopRegionGroupsDto shopRegionGroupsDto123 = new ShopRegionGroupsDto();
        shopRegionGroupsDto123.setRegionsGroups(List.of(
                regionGroupDto1,
                regionGroupDto2,
                regionGroupDto3,
                regionGroupDto4
        ));

        when(retryableTarifficatorClient.getShopRegionGroups(10L)).thenReturn(shopRegionGroupsDto);
        when(retryableTarifficatorClient.getShopRegionGroups(11L)).thenReturn(shopRegionGroupsDto);
        when(retryableTarifficatorClient.getShopRegionGroups(123L)).thenReturn(shopRegionGroupsDto123);

        SelectedDeliveryServiceDto deliveryService9 = new SelectedDeliveryServiceDto();
        deliveryService9.setDeliveryServiceId(9L);
        deliveryService9.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME);
        deliveryService9.setPickupDeliveryStrategy(null);

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto valueLimiterDto =
                new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto();
        valueLimiterDto.setMinValue(BigDecimal.valueOf(0));
        valueLimiterDto.setMaxValue(BigDecimal.valueOf(10));

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueModificationRuleDto valueModificationRuleDto
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueModificationRuleDto();
        valueModificationRuleDto.setOperation(OperationTypeDto.SUBTRACT);
        valueModificationRuleDto.setParameter(BigDecimal.valueOf(12.97));
        valueModificationRuleDto.setResultLimit(valueLimiterDto);

        DeliveryModifierActionDto action = new DeliveryModifierActionDto();
        action.setCostModificationRule(valueModificationRuleDto);
        action.setTimeModificationRule(valueModificationRuleDto);
        action.setPaidByCustomerServices(List.of(DeliveryServiceCodeDto.INSURANCE));
        action.setIsCarrierTurnedOn(true);

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.PercentValueLimiterDto conditionCost
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.PercentValueLimiterDto();
        conditionCost.setMinValue(BigDecimal.valueOf(0));
        conditionCost.setMaxValue(BigDecimal.valueOf(98));
        conditionCost.setPercent(BigDecimal.valueOf(95.5));

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryCostConditionDto deliveryCostConditionDto
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryCostConditionDto();
        deliveryCostConditionDto.setPercentFromOfferPrice(BigDecimal.valueOf(50.0));
        deliveryCostConditionDto.setComparisonOperation(ComparisonOperationDto.MORE);

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto weightValueLimiter
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto();
        weightValueLimiter.setMinValue(BigDecimal.valueOf(0));
        weightValueLimiter.setMaxValue(BigDecimal.valueOf(10));

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto chargeableWeightValueLimiter
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto();
        chargeableWeightValueLimiter.setMinValue(BigDecimal.valueOf(0));
        chargeableWeightValueLimiter.setMaxValue(BigDecimal.valueOf(10));

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto dimensionValueLimiter
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto();
        dimensionValueLimiter.setMinValue(BigDecimal.valueOf(0));
        dimensionValueLimiter.setMaxValue(BigDecimal.valueOf(10));

        DeliveryModifierConditionDto condition = new DeliveryModifierConditionDto();
        condition.setCost(conditionCost);
        condition.setDeliveryCost(deliveryCostConditionDto);
        condition.setWeight(weightValueLimiter);
        condition.setChargeableWeight(chargeableWeightValueLimiter);
        condition.setDimension(dimensionValueLimiter);
        condition.setCarrierIds(List.of(1L));
        condition.setDeliveryDestinations(List.of(213));
        condition.setDeliveryTypes(List.of(YaDeliveryTariffType.PICKUP));

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto deliveryModifierDto100 =
                new ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto();
        deliveryModifierDto100.setId(100L);
        deliveryModifierDto100.setTimestamp(1583325200238L);
        deliveryModifierDto100.setAction(action);
        deliveryModifierDto100.setCondition(condition);

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto deliveryModifierDto101 =
                new ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto();
        deliveryModifierDto101.setId(101L);
        deliveryModifierDto101.setTimestamp(1583325200238L);
        deliveryModifierDto101.setAction(action);
        deliveryModifierDto101.setCondition(condition);

        SelectedDeliveryServiceDto deliveryService51 = new SelectedDeliveryServiceDto();
        deliveryService51.setDeliveryServiceId(51L);
        deliveryService51.setCourierDeliveryStrategy(null);
        deliveryService51.setPickupDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED);
        deliveryService51.setCourierDeliveryModifiers(List.of(deliveryModifierDto100, deliveryModifierDto101));

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueModificationRuleDto costModificationRule
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueModificationRuleDto();
        costModificationRule.setOperation(OperationTypeDto.FIX_VALUE);
        costModificationRule.setParameter(BigDecimal.valueOf(2000));

        DeliveryModifierActionDto courierDeliveryModifierActionDto = new DeliveryModifierActionDto();
        courierDeliveryModifierActionDto.setCostModificationRule(costModificationRule);

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto courierDeliveryModifierDto
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto();
        courierDeliveryModifierDto.setTimestamp(1583325200238L);
        courierDeliveryModifierDto.setAction(courierDeliveryModifierActionDto);

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueModificationRuleDto timeModificationRule
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueModificationRuleDto();
        timeModificationRule.setOperation(OperationTypeDto.MULTIPLY);
        timeModificationRule.setParameter(BigDecimal.valueOf(2));

        DeliveryModifierActionDto pickupDeliveryModifierActionDto = new DeliveryModifierActionDto();
        pickupDeliveryModifierActionDto.setTimeModificationRule(timeModificationRule);

        ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto pickupDeliveryModifierDto
                = new ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto();
        pickupDeliveryModifierDto.setTimestamp(1583325200238L);
        pickupDeliveryModifierDto.setAction(pickupDeliveryModifierActionDto);

        SelectedDeliveryServiceDto deliveryService48 = new SelectedDeliveryServiceDto();
        deliveryService48.setDeliveryServiceId(48L);
        deliveryService48.setCourierDeliveryStrategy(null);
        deliveryService48.setCourierDeliveryModifiers(List.of(courierDeliveryModifierDto));
        deliveryService48.setPickupDeliveryStrategy(null);
        deliveryService48.setPickupDeliveryModifiers(List.of(pickupDeliveryModifierDto));

        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331000L, 11L))
                .thenReturn(List.of(deliveryService9, deliveryService51));
        when(retryableTarifficatorClient.getSelectedDeliveryServices(123L, 331001L, 11L))
                .thenReturn(List.of(deliveryService48));

        final UpdateShopRulesRequest updateShopRulesRequest = exportDataToDeliveryCalculator();

        final List<DeliveryModifierDto> modifiers =
                updateShopRulesRequest.getShops().stream()
                        .flatMap(sdi -> sdi.getDeliveryModifiers().stream())
                        .collect(Collectors.toList());

        final List<DeliveryModifierDto> expectedModifiers = getModifiers();
        assertEquals(expectedModifiers.size(), modifiers.size());
        Assertions.assertTrue(modifiers.containsAll(expectedModifiers));

        final String s = DeliveryCalculatorIndexerClientXmlUtils.toXmlString(updateShopRulesRequest);
        final UpdateShopRulesRequest actual = DeliveryCalculatorIndexerClientXmlUtils.fromXmlString(s);

        assertEquals(modifiers,
                actual.getShops().stream()
                        .flatMap(sdi -> sdi.getDeliveryModifiers().stream()).collect(Collectors.toList()));
    }

    /**
     * Тест проверяет, что заархивированные магазины выгружаются как удаленные.
     */
    @Test
    @DbUnitDataSet(
            before = "data/testExportArchivedShopsAsDeleted.before.csv",
            after = "data/testExportArchivedShopsAsDeleted.after.csv"
    )
    void testExportArchivedShopsAsDeleted() {
        List<Long> deletedShopIds = exportDeletedDataToDeliveryCalculator();
        System.out.println(deletedShopIds);

        assertThat(deletedShopIds, hasSize(3));
        assertThat(deletedShopIds, containsInAnyOrder(2L, 3L, 4L));
    }

    /**
     * Тест проверяет, что для DBS выгружается виртуальный фид
     */
    @Test
    @DbUnitDataSet(before = "data/testExportMultiFeedDBS.before.csv")
    void testDsbsVirtFeed() {
        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(99L)).thenReturn(shopRegionGroupsDto);

        final List<ShopDeliveryInfo> shopDeliveryInfos = exportDataToDeliveryCalculator(1);

        ShopDeliveryInfo shop = shopDeliveryInfos.get(0);
        assertEquals(99, shop.getId());
        assertEquals(1, shop.getFeeds().size());
        assertThat(shop.getFeeds(), contains(new FeedInfo(99000)));
    }

    /**
     * Тест проверяет, что для CPC выгружаются все фиды
     */
    @Test
    @DbUnitDataSet(before = "data/testExportMultiFeedCPC.before.csv")
    void testCPCMultiFeeds() {
        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();

        when(retryableTarifficatorClient.getShopRegionGroups(99L)).thenReturn(shopRegionGroupsDto);

        final List<ShopDeliveryInfo> shopDeliveryInfos = exportDataToDeliveryCalculator(1);

        ShopDeliveryInfo shop = shopDeliveryInfos.get(0);
        assertEquals(99, shop.getId());
        assertEquals(2, shop.getFeeds().size());
        assertThat(shop.getFeeds(), containsInAnyOrder(new FeedInfo(77), new FeedInfo(78)));
    }

    /**
     * Тест проверяет исключение несуществующих в mbi фидов из правил тарификатора.
     */
    @Test
    @DbUnitDataSet(before = "data/testFilterAbsentFeedCPC.before.csv")
    void testFilterAbsentFeedCPC() {
        RegionGroupDto regionGroup1 = new RegionGroupDto().checkStatus(RegionGroupStatus.NEW)
                .currency(Currency.RUR)
                .datasourceId(434197L)
                .excludes(List.of())
                .hasDeliveryService(false)
                .id(9720L)
                .includes(List.of(213L))
                .selfRegion(true)
                .tariffType(CourierTariffType.FROM_FEED)
                .useYml(true);

        RegionGroupDto regionGroup2 = new RegionGroupDto().checkStatus(RegionGroupStatus.NEW)
                .currency(Currency.RUR)
                .datasourceId(434197L)
                .excludes(List.of())
                .hasDeliveryService(true)
                .id(91454L)
                .includes(List.of(119043L, 21635L, 118692L, 21642L, 10765L, 21646L, 21647L, 21619L, 21622L, 21656L, 20571L, 21627L, 10748L))
                .selfRegion(false)
                .tariffType(CourierTariffType.CATEGORY_PRICE)
                .useYml(false);

        RegionGroupDto regionGroup3 = new RegionGroupDto().checkStatus(RegionGroupStatus.NEW)
                .currency(Currency.RUR)
                .datasourceId(434197L)
                .excludes(List.of(2L, 119043L))
                .hasDeliveryService(true)
                .id(91457L)
                .includes(List.of(225L))
                .selfRegion(false)
                .tariffType(CourierTariffType.CATEGORY_PRICE)
                .useYml(false);

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto()
                .maxRegionsGroups(21)
                .regionsGroups(List.of(regionGroup1, regionGroup2, regionGroup3));
        when(retryableTarifficatorClient.getShopRegionGroups(434197L)).thenReturn(shopRegionGroupsDto);

        SelectedDeliveryServiceDto deliveryService511 = new SelectedDeliveryServiceDto();
        deliveryService511.setDeliveryServiceId(51L);
        deliveryService511.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED);
        deliveryService511.setPickupDeliveryStrategy(null);
        SelectedDeliveryServiceDto deliveryService512 = new SelectedDeliveryServiceDto();
        deliveryService512.setDeliveryServiceId(51L);
        deliveryService512.setCourierDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME);
        deliveryService512.setPickupDeliveryStrategy(null);

        when(retryableTarifficatorClient.getSelectedDeliveryServices(anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(deliveryService511, deliveryService512));

        DeliveryTariffDto deliveryTariff91457 = new DeliveryTariffDto()
                .categoryRules(List.of(new CategoryRuleDto()
                        .includes(List.of(new CategoryIdDto().categoryId("4").feedId(869627L)))
                        .orderNum(0)
                        .others(false),
                        new CategoryRuleDto()
                                .includes(List.of())
                                .orderNum(1)
                                .others(true)))
                .tariffType(CourierTariffType.CATEGORY_PRICE)
                .useYml(false)
                .weightRules(List.of())
                .priceRules(List.of(new PriceRuleDto().orderNum(0).priceTo(new BigDecimal(5000)),
                        new PriceRuleDto().orderNum(1).priceFrom(new BigDecimal(5000))
                ))
                .optionsGroups(List.of(
                        new OptionGroupDto().categoryOrderNum(1).hasDelivery(false).options(List.of()).priceOrderNum(0),
                        new OptionGroupDto().categoryOrderNum(0).hasDelivery(true).options(List.of(new OptionDto().cost(new BigDecimal(500)).daysFrom(61).daysTo(61).orderBeforeHour(13).orderNum(0))).priceOrderNum(0),
                        new OptionGroupDto().categoryOrderNum(0).hasDelivery(true).options(List.of(new OptionDto().cost(new BigDecimal(0)).daysFrom(61).daysTo(61).orderBeforeHour(13).orderNum(0))).priceOrderNum(1),
                        new OptionGroupDto().categoryOrderNum(1).hasDelivery(false).options(List.of()).priceOrderNum(1)

                ));

        when(retryableTarifficatorClient.getRegionGroupTariff(434197L, 91457L, 11L))
                .thenReturn(deliveryTariff91457);

        DeliveryTariffDto deliveryTariff91454 = new DeliveryTariffDto()
                .categoryRules(List.of(new CategoryRuleDto()
                                .includes(List.of(new CategoryIdDto().feedId(0L)))
                                .orderNum(0)
                                .others(false),
                        new CategoryRuleDto()
                                .includes(List.of(new CategoryIdDto().feedId(0L)))
                                .orderNum(1)
                                .others(false),
                        new CategoryRuleDto()
                                .includes(List.of())
                                .orderNum(2)
                                .others(true)))
                .tariffType(CourierTariffType.CATEGORY_PRICE)
                .useYml(false)
                .weightRules(List.of())
                .priceRules(List.of(new PriceRuleDto().orderNum(0).priceTo(new BigDecimal(5000)),
                        new PriceRuleDto().orderNum(1).priceFrom(new BigDecimal(5000))
                ))
                .optionsGroups(List.of(
                        new OptionGroupDto().categoryOrderNum(0).hasDelivery(true).options(List.of(new OptionDto().cost(new BigDecimal(500)).daysFrom(0).daysTo(0).orderBeforeHour(15).orderNum(0))).priceOrderNum(0),
                        new OptionGroupDto().categoryOrderNum(0).hasDelivery(true).options(List.of(new OptionDto().cost(new BigDecimal(0)).daysFrom(0).daysTo(0).orderBeforeHour(15).orderNum(0))).priceOrderNum(1),
                        new OptionGroupDto().categoryOrderNum(2).hasDelivery(false).options(List.of()).priceOrderNum(0),
                        new OptionGroupDto().categoryOrderNum(2).hasDelivery(false).options(List.of()).priceOrderNum(1),
                        new OptionGroupDto().categoryOrderNum(1).hasDelivery(true).options(List.of(new OptionDto().cost(new BigDecimal(0)).daysFrom(0).daysTo(0).orderBeforeHour(16).orderNum(0))).priceOrderNum(0),
                        new OptionGroupDto().categoryOrderNum(1).hasDelivery(true).options(List.of(new OptionDto().cost(new BigDecimal(0)).daysFrom(0).daysTo(0).orderBeforeHour(16).orderNum(0))).priceOrderNum(1),
                        new OptionGroupDto().categoryOrderNum(3).hasDelivery(false).options(List.of()).priceOrderNum(0),
                        new OptionGroupDto().categoryOrderNum(3).hasDelivery(false).options(List.of()).priceOrderNum(1)

                ));;

        when(retryableTarifficatorClient.getRegionGroupTariff(434197, 91454, 11))
                .thenReturn(deliveryTariff91454);

        environmentService.setValue("TarifficatorToCalculatorExporter.exclude.unexisting.feeds.partners", "434197");
        executor.doJob(null);

        verify(indexerClient).updateShopDeliveryRules(argThat(this::isValid));
    }

    private boolean isValid(UpdateShopRulesRequest request) {
        Long badFeedId = 869627L;

        ShopDeliveryInfo shopDeliveryInfo = request.getShops().get(0);
        List<TariffInfoV1> tariffInfos = shopDeliveryInfo.getTariffInfos();
        List<DeliveryRule> deliveryRuleStream = tariffInfos.stream()
                .map(TariffInfoV1::getRules)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        List<DeliveryRule> deliveryRuleStream1 = deliveryRuleStream.stream()
                .map(DeliveryRule::getChildren)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<DeliveryRuleFeedCategory> collect =
                deliveryRuleStream1.stream().map(DeliveryRule::getFeedCategory).collect(Collectors.toList());


        List<DeliveryRuleFeedCategoryItem> items =
                collect.stream()
                        .map(category -> CollectionUtils.union(
                                CollectionUtils.emptyIfNull(category.getIncludeItems()),
                                CollectionUtils.emptyIfNull(category.getExcludeItems())))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

        return request.getShops().size() == 1 &&
                items.stream()
                        .map(DeliveryRuleFeedCategoryItem::getFeedId)
                        .noneMatch(badFeedId::equals);
    }

    private List<Long> exportDeletedDataToDeliveryCalculator() {
        executor.doJob(null);

        ArgumentCaptor<DeleteShopRulesRequest> captor = ArgumentCaptor.forClass(DeleteShopRulesRequest.class);
        verify(indexerClient, times(1)).deleteShopDeliveryRules(captor.capture());
        verifyNoMoreInteractions(indexerClient);

        List<DeleteShopRulesRequest> requests = captor.getAllValues();
        assertThat(requests, hasSize(1));

        return requests.get(0).getDeletedShopIds();
    }

    private List<ShopDeliveryInfo> exportDataToDeliveryCalculator(int shopsCount) {
        final List<UpdateShopRulesRequest> requests = exportData();

        final List<ShopDeliveryInfo> shops = requests.stream()
                .flatMap(request -> request.getShops().stream())
                .collect(Collectors.toList());
        assertThat(shops, hasSize(shopsCount));
        shops.sort((o1, o2) -> (int) (o1.getId() - o2.getId()));
        return shops;
    }

    private UpdateShopRulesRequest exportDataToDeliveryCalculator() {
        final List<UpdateShopRulesRequest> requests = exportData();
        return requests.get(0);
    }

    @Nonnull
    private List<UpdateShopRulesRequest> exportData() {
        executor.doJob(null);

        final ArgumentCaptor<UpdateShopRulesRequest> captor = ArgumentCaptor.forClass(UpdateShopRulesRequest.class);
        verify(indexerClient, times(1)).updateShopDeliveryRules(captor.capture());
        verifyNoMoreInteractions(indexerClient);

        final List<UpdateShopRulesRequest> requests = captor.getAllValues();
        assertThat(requests, hasSize(1));
        return requests;
    }

    private boolean hasAutoCalculatedCourier(ShopDeliveryInfo shopDeliveryInfo) {
        return hasAutoCalculated(shopDeliveryInfo, CarrierInfo::isAutoCalculatedCourier);
    }

    private boolean hasAutoCalculatedPickup(ShopDeliveryInfo shopDeliveryInfo) {
        return hasAutoCalculated(shopDeliveryInfo, CarrierInfo::isAutoCalculatedPickup);
    }

    private boolean hasAutoCalculated(ShopDeliveryInfo shopDeliveryInfo, Predicate<CarrierInfo> filter) {
        return Optional.ofNullable(shopDeliveryInfo.getTariffInfos()).orElse(Collections.emptyList()).stream()
                .map(TariffInfoV1::getCarrierInfos)
                .flatMap(Collection::stream)
                .filter(filter)
                .map(CarrierInfo::getId)
                .count() > 0;
    }

    private RegionGroupDto buildRegionGroupDto(
            long groupId,
            long datasourceId,
            boolean isSelfRegion,
            boolean isUseYml,
            List<Long> includes,
            List<Long> excludes,
            CourierTariffType tariffType,
            RegionGroupStatus status,
            Currency currency) {

        RegionGroupDto regionGroupDto = new RegionGroupDto();
        regionGroupDto.setId(groupId);
        regionGroupDto.setDatasourceId(datasourceId);
        regionGroupDto.setName("test");
        regionGroupDto.setSelfRegion(isSelfRegion);
        regionGroupDto.setTariffType(tariffType);
        regionGroupDto.setUseYml(isUseYml);
        regionGroupDto.setIncludes(includes);
        regionGroupDto.setExcludes(excludes);
        regionGroupDto.setModifiedBy(1L);
        regionGroupDto.setCurrency(currency);
        regionGroupDto.setHasDeliveryService(null);
        regionGroupDto.setCheckStatus(status);
        regionGroupDto.setCheckStatusModifiedAt(new Date());
        return regionGroupDto;
    }

}
