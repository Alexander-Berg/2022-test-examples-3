package ru.yandex.market.delivery;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.delivery.RetryableTarifficatorClient;
import ru.yandex.market.core.delivery.tariff.model.CategoryId;
import ru.yandex.market.core.delivery.tariff.model.CategoryRule;
import ru.yandex.market.core.delivery.tariff.model.DeliveryOption;
import ru.yandex.market.core.delivery.tariff.model.DeliveryRuleId;
import ru.yandex.market.core.delivery.tariff.model.OptionGroup;
import ru.yandex.market.core.delivery.tariff.model.PriceRule;
import ru.yandex.market.core.delivery.tariff.model.WeightRule;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.salesnotes.SalesNotesService;
import ru.yandex.market.core.supplier.dao.PartnerFulfillmentLinkDao;
import ru.yandex.market.deliverycalculator.indexerclient.model.CarrierInfo;
import ru.yandex.market.deliverycalculator.indexerclient.model.DeliveryRule;
import ru.yandex.market.deliverycalculator.indexerclient.model.ShopDeliveryInfo;
import ru.yandex.market.deliverycalculator.indexerclient.model.TariffInfoV1;
import ru.yandex.market.deliverycalculator.indexerclient.model.TreeCriteriaDefaultPolicy;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffStrategy;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.CourierTariffType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupStatus;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.CategoryIdDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.CategoryRuleDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryTariffDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OptionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OptionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.PriceRuleDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopRegionGroupsDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.WeightRuleDto;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static ru.yandex.common.util.collections.CollectionFactory.newTreeSet;
import static ru.yandex.common.util.collections.CollectionFactory.set;

public class TarifficatorToCalculatorExporterTest {

    private TarifficatorToCalculatorExporter exporter;

    private RetryableTarifficatorClient retryableTarifficatorClient;

    @BeforeEach
    void beforeEach() {

        retryableTarifficatorClient = Mockito.mock(RetryableTarifficatorClient.class);

        exporter = new TarifficatorToCalculatorExporter(
                Mockito.mock(PartnerTypeAwareService.class, withSettings().defaultAnswer(Answers.RETURNS_MOCKS)),
                Mockito.mock(PartnerPlacementProgramService.class, withSettings().defaultAnswer(Answers.RETURNS_MOCKS)),
                Mockito.mock(FeedService.class),
                Mockito.mock(PartnerFulfillmentLinkDao.class),
                retryableTarifficatorClient,
                Mockito.mock(RegionService.class),
                Mockito.mock(SalesNotesService.class),
                Mockito.mock(EnvironmentService.class)
        );
    }


    @Test
    void createTariff() {
        RegionGroupDto regionGroupDto = buildRegionGroupDto(false, false, List.of(1L), CourierTariffType.UNIFORM,
                RegionGroupStatus.SUCCESS);
        OptionDto optionDto = buildOptionDto(0, 100, 0, 0);
        OptionGroupDto optionGroupDto = buildOptionGroupDto(List.of(optionDto), 1, null, null, true);

        DeliveryTariffDto deliveryTariffDto = buildDeliveryTariffDto(
                CourierTariffType.UNIFORM,
                null,
                null,
                null,
                List.of(optionGroupDto)
        );

        Optional<TariffInfoV1> actual = exporter.createTariff(regionGroupDto, deliveryTariffDto);

        TariffInfoV1 expected = new TariffInfoV1();
        expected.setCurrency("BYR");

        DeliveryRule root = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withCost(10000).withMaxDaysCount(0)
                .withOrderBeforeHour(12)
                .build();

        expected.setRules(Collections.singletonList(root));
        expected.setStrategy(DeliveryTariffStrategy.FIX);
        CarrierInfo carrierInfo = new CarrierInfo();
        carrierInfo.setId(99);
        expected.setCarrierInfos(Collections.singletonList(carrierInfo));
        assertEquals(expected, actual.orElse(null));
    }

    @Test
    void createYMLTariff() {
        RegionGroupDto regionGroupDto = buildRegionGroupDto(true, true, List.of(1L), CourierTariffType.DEFAULT,
                RegionGroupStatus.NEW);

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        shopRegionGroupsDto.setRegionsGroups(List.of(regionGroupDto));

        when(retryableTarifficatorClient.getShopRegionGroups(774L)).thenReturn(shopRegionGroupsDto);

        List<TariffInfoV1> actual = getTariffs(774L);

        assertEquals(0, actual.size());
    }

    @Test
    void buildRuleTreeNone() {
        OptionDto optionDto = buildOptionDto(0, 1, 0, 0);

        OptionGroupDto optionGroupDto = buildOptionGroupDto(List.of(optionDto), null, null, null, true);

        DeliveryRule root = new DeliveryRule();
        root.setIncludedRegions(Collections.singletonList(1));
        root.setChildren(exporter.buildRuleTree(
                null,
                new LinkedList<>(),
                root,
                Collections.singletonMap(new TarifficatorToCalculatorExporter.RuleKey(), optionGroupDto)
        ));
        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withCost(100)
                .withOrderBeforeHour(12).withMaxDaysCount(0)
                .build();
        assertEquals(expected, root);
    }


    @Test
    void buildRuleTreeMultiOptions() {
        OptionDto optionDto1 = buildOptionDto(0, 1, 0, 0);
        OptionDto optionDto2 = buildOptionDto(0, 0, 1, 7);

        OptionGroupDto optionGroupDto = buildOptionGroupDto(List.of(optionDto1, optionDto2), null, null, null, true);

        DeliveryRule root = new DeliveryRule();
        root.setIncludedRegions(Collections.singletonList(1));
        root.setChildren(exporter.buildRuleTree(
                null,
                new LinkedList<>(),
                root,
                Collections.singletonMap(new TarifficatorToCalculatorExporter.RuleKey(), optionGroupDto)
        ));
        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withCost(100).withMaxDaysCount(0)
                .withOrderBeforeHour(12)
                .build();
        assertEquals(expected, root);
    }

    @Test
    void buildRuleTreeOne() {
        OptionDto optionDto1 = buildOptionDto(0, 1, 0, 0);
        OptionDto optionDto2 = buildOptionDto(0, 2, 0, 0);

        OptionGroupDto optionGroupDto1 = buildOptionGroupDto(List.of(optionDto1), null, null, null, true);
        OptionGroupDto optionGroupDto2 = buildOptionGroupDto(List.of(optionDto2), null, null, null, true);

        CategoryRule catRule1 = new CategoryRule(new DeliveryRuleId(1L, 0), newTreeSet(set(new CategoryId("1", 1L))));
        CategoryRule catRule2 = new CategoryRule(new DeliveryRuleId(1L, 1), newTreeSet(set(new CategoryId("2", 1L))));

        DeliveryRule root = new DeliveryRule();
        root.setIncludedRegions(Collections.singletonList(1));
        root.setChildren(exporter.buildRuleTree(
                Collections.singletonList(new ru.yandex.market.core.delivery.tariff.model.DeliveryRule[]{
                        catRule1,
                        catRule2
                }),
                new LinkedList<>(),
                root,
                ImmutableMap.<TarifficatorToCalculatorExporter.RuleKey, OptionGroupDto>builder()
                        .put(new TarifficatorToCalculatorExporter.RuleKey(catRule1), optionGroupDto1)
                        .put(new TarifficatorToCalculatorExporter.RuleKey(catRule2), optionGroupDto2)
                        .build()
        ));
        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withChildren(
                        new DeliveryRuleBuilder()
                                .withCost(100)
                                .withOrderBeforeHour(12).withMaxDaysCount(0)
                                .withCategories(set(new TestCategoryId("1", 1))).build(),
                        new DeliveryRuleBuilder()
                                .withCost(200)
                                .withOrderBeforeHour(12).withMaxDaysCount(0)
                                .withCategories(set(new TestCategoryId("2", 1))).build()
                ).build();
        assertEquals(expected, root);
    }

    @Test
    void buildRuleTreeTwo() {
        OptionDto optionDto1 = buildOptionDto(0, 1, 0, 0);
        OptionDto optionDto2 = buildOptionDto(0, 2, 0, 0);
        OptionDto optionDto3 = buildOptionDto(0, 3, 0, 0);
        OptionDto optionDto4 = buildOptionDto(0, 4, 0, 0);

        OptionGroupDto optionGroupDto1 = buildOptionGroupDto(List.of(optionDto1), null, null, null, true);
        OptionGroupDto optionGroupDto2 = buildOptionGroupDto(List.of(optionDto2), null, null, null, true);
        OptionGroupDto optionGroupDto3 = buildOptionGroupDto(List.of(optionDto3), null, null, null, true);
        OptionGroupDto optionGroupDto4 = buildOptionGroupDto(List.of(optionDto4), null, null, null, true);

        CategoryRule catRule1 = new CategoryRule(new DeliveryRuleId(1L, 0), newTreeSet(set(new CategoryId("1", 1L))));
        CategoryRule catRule2 = new CategoryRule(new DeliveryRuleId(1L, 1), newTreeSet(set(new CategoryId("2", 1L))));
        PriceRule priceRule1 = new PriceRule(new DeliveryRuleId(1L, 0), BigDecimal.ZERO, BigDecimal.valueOf(1000L));
        PriceRule priceRule2 = new PriceRule(new DeliveryRuleId(1L, 0), BigDecimal.valueOf(1000L), null);

        DeliveryRule root = new DeliveryRule();
        root.setIncludedRegions(Collections.singletonList(1));
        root.setChildren(exporter.buildRuleTree(
                Arrays.asList(new ru.yandex.market.core.delivery.tariff.model.DeliveryRule[]{
                        catRule1,
                        catRule2
                }, new ru.yandex.market.core.delivery.tariff.model.DeliveryRule[]{
                        priceRule1,
                        priceRule2
                }),
                new LinkedList<>(),
                root,
                ImmutableMap.<TarifficatorToCalculatorExporter.RuleKey, OptionGroupDto>builder()
                        .put(new TarifficatorToCalculatorExporter.RuleKey(catRule1, priceRule1), optionGroupDto1)
                        .put(new TarifficatorToCalculatorExporter.RuleKey(catRule1, priceRule2), optionGroupDto2)
                        .put(new TarifficatorToCalculatorExporter.RuleKey(catRule2, priceRule1), optionGroupDto3)
                        .put(new TarifficatorToCalculatorExporter.RuleKey(catRule2, priceRule2), optionGroupDto4)
                        .build()
        ));
        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withChildren(
                        new DeliveryRuleBuilder()
                                .withCategories(set(new TestCategoryId("1", 1)))
                                .withChildren(
                                        new DeliveryRuleBuilder().withPrice(0, 1000)
                                                .withCost(100).withMaxDaysCount(0)
                                                .withOrderBeforeHour(12).build(),
                                        new DeliveryRuleBuilder().withPrice(1000, null)
                                                .withCost(200).withMaxDaysCount(0)
                                                .withOrderBeforeHour(12).build()
                                ).build(),
                        new DeliveryRuleBuilder()
                                .withCategories(set(new TestCategoryId("2", 1)))
                                .withChildren(
                                        new DeliveryRuleBuilder().withPrice(0, 1000)
                                                .withCost(300).withMaxDaysCount(0).withMaxDaysCount(0)
                                                .withOrderBeforeHour(12).build(),
                                        new DeliveryRuleBuilder().withPrice(1000, null)
                                                .withCost(400).withMaxDaysCount(0).withMaxDaysCount(0)
                                                .withOrderBeforeHour(12).build()
                                ).build()
                ).build();
        assertEquals(expected, root);
    }

    @Test
    void buildRuleTreePriceWeight() {
        OptionDto optionDto1 = buildOptionDto(0, 1, 0, 0);
        OptionDto optionDto2 = buildOptionDto(0, 2, 0, 0);
        OptionDto optionDto3 = buildOptionDto(0, 3, 0, 0);
        OptionDto optionDto4 = buildOptionDto(0, 4, 0, 0);

        OptionGroupDto optionGroupDto1 = buildOptionGroupDto(List.of(optionDto1), null, null, null, true);
        OptionGroupDto optionGroupDto2 = buildOptionGroupDto(List.of(optionDto2), null, null, null, true);
        OptionGroupDto optionGroupDto3 = buildOptionGroupDto(List.of(optionDto3), null, null, null, true);
        OptionGroupDto optionGroupDto4 = buildOptionGroupDto(List.of(optionDto4), null, null, null, true);

        PriceRule priceRule1 = new PriceRule(new DeliveryRuleId(1L, 0), BigDecimal.ZERO, BigDecimal.valueOf(1000L));
        PriceRule priceRule2 = new PriceRule(new DeliveryRuleId(1L, 0), BigDecimal.valueOf(1000L), null);

        WeightRule weightRule1 = new WeightRule(new DeliveryRuleId(1L, 0), 0, 1);
        WeightRule weightRule2 = new WeightRule(new DeliveryRuleId(1L, 0), 1, null);

        DeliveryRule root = new DeliveryRule();
        root.setIncludedRegions(Collections.singletonList(1));
        root.setChildren(exporter.buildRuleTree(
                Arrays.asList(new ru.yandex.market.core.delivery.tariff.model.DeliveryRule[]{
                        priceRule1,
                        priceRule2
                }, new ru.yandex.market.core.delivery.tariff.model.DeliveryRule[]{
                        weightRule1,
                        weightRule2
                }),
                new LinkedList<>(),
                root,
                ImmutableMap.<TarifficatorToCalculatorExporter.RuleKey, OptionGroupDto>builder()
                        .put(new TarifficatorToCalculatorExporter.RuleKey(priceRule1, weightRule1), optionGroupDto1)
                        .put(new TarifficatorToCalculatorExporter.RuleKey(priceRule1, weightRule2), optionGroupDto2)
                        .put(new TarifficatorToCalculatorExporter.RuleKey(priceRule2, weightRule1), optionGroupDto3)
                        .put(new TarifficatorToCalculatorExporter.RuleKey(priceRule2, weightRule2), optionGroupDto4)
                        .build()
        ));

        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withChildren(
                        new DeliveryRuleBuilder()
                                .withPrice(0, 1000)
                                .withChildren(
                                        new DeliveryRuleBuilder().withWeight(0, 1)
                                                .withCost(100).withMaxDaysCount(0)
                                                .withOrderBeforeHour(12).build(),
                                        new DeliveryRuleBuilder().withWeight(1, null)
                                                .withCost(200).withMaxDaysCount(0)
                                                .withOrderBeforeHour(12).build()
                                ).build(),
                        new DeliveryRuleBuilder()
                                .withPrice(1000, null)
                                .withChildren(
                                        new DeliveryRuleBuilder().withWeight(0, 1)
                                                .withCost(300).withMaxDaysCount(0)
                                                .withOrderBeforeHour(12).build(),
                                        new DeliveryRuleBuilder().withWeight(1, null)
                                                .withCost(400).withMaxDaysCount(0)
                                                .withOrderBeforeHour(12).build()
                                ).build()
                ).build();
        assertEquals(expected, root);
    }

    @Test
    void buildPriceRuleTreeWithoutDelivery() {
        OptionDto optionDto1 = buildOptionDto(0, 1, 0, 0);
        OptionGroupDto optionGroupDto1 = buildOptionGroupDto(List.of(optionDto1), null, 0, null, false);

        OptionDto optionDto2 = buildOptionDto(0, 100, 0, 0);
        OptionGroupDto optionGroupDto2 = buildOptionGroupDto(List.of(optionDto2), null, 1, null, true);

        RegionGroupDto regionGroupDto = buildRegionGroupDto(false, false, List.of(1L), CourierTariffType.PRICE,
                RegionGroupStatus.SUCCESS);

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        shopRegionGroupsDto.setRegionsGroups(List.of(regionGroupDto));

        DeliveryTariffDto deliveryTariffDto = buildDeliveryTariffDto(
                CourierTariffType.PRICE,
                null,
                List.of(
                        buildPriceRuleDto(0, null, BigDecimal.valueOf(1000L)),
                        buildPriceRuleDto(1, BigDecimal.valueOf(1000L), null)
                ),
                null,
                List.of(optionGroupDto1, optionGroupDto2));

        when(retryableTarifficatorClient.getShopRegionGroups(774L)).thenReturn(shopRegionGroupsDto);
        when(retryableTarifficatorClient.getRegionGroupTariff(774L, 1L, 11L))
                .thenReturn(deliveryTariffDto);


        final List<TariffInfoV1> actual = getTariffs(774);

        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withChildren(
                        new DeliveryRuleBuilder().withMaxDaysCount(0)
                                .withPrice(null, 1000).withNoDelivery().build(),
                        new DeliveryRuleBuilder()
                                .withCost(10000).withMaxDaysCount(0)
                                .withOrderBeforeHour(12)
                                .withPrice(1000, null).build()
                ).build();
        assertEquals(1, actual.size());
        assertEquals(1, actual.get(0).getRules().size());
        assertEquals(2, actual.get(0).getRules().get(0).getChildren().size());
        assertFalse(actual.get(0).getRules().get(0).getChildren().get(0).getOptions().get(0).getDelivery());
        assertEquals(expected, actual.get(0).getRules().get(0));
    }


    @Test
    void buildRuleTreeWithoutDelivery() {
        OptionGroupDto optionGroupDto1 = buildOptionGroupDto(null, null, null, null, false);

        OptionDto optionDto2 = buildOptionDto(0, 2, 0, 0);
        OptionGroupDto optionGroupDto2 = buildOptionGroupDto(List.of(optionDto2), null, null, null, true);

        RegionGroupDto regionGroupDto = buildRegionGroupDto(false, false, List.of(1L), CourierTariffType.CATEGORY,
                RegionGroupStatus.SUCCESS);

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        shopRegionGroupsDto.setRegionsGroups(List.of(regionGroupDto));

        DeliveryTariffDto deliveryTariffDto = buildDeliveryTariffDto(
                CourierTariffType.UNIFORM,
                List.of(
                        buildCategoryRuleDto(List.of(buildCategoryIdDto("1", 1L)), 0, false),
                        buildCategoryRuleDto(List.of(buildCategoryIdDto("2", 1L)), 1, false)
                ),
                null,
                null,
                List.of(optionGroupDto1, optionGroupDto2));

        when(retryableTarifficatorClient.getShopRegionGroups(774L)).thenReturn(shopRegionGroupsDto);
        when(retryableTarifficatorClient.getRegionGroupTariff(774L, 1L, 11L))
                .thenReturn(deliveryTariffDto);
        List<TariffInfoV1> actual = getTariffs(774L);
        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withChildren(
                        new DeliveryRuleBuilder().withCategories(CollectionFactory.set(new TestCategoryId("1", 1L))).build(),
                        new DeliveryRuleBuilder().withCategories(CollectionFactory.set(new TestCategoryId("2", 1L))).build()
                ).build();
        assertEquals(1, actual.size());
        assertEquals(expected, actual.get(0).getRules().get(0));
    }

    @Test
    void buildAllFeedsOneRule() {
        OptionDto optionDto1 = buildOptionDto(0, 100, 0, 0);
        OptionGroupDto optionGroupDto1 = buildOptionGroupDto(List.of(optionDto1), 0, null, null, true);
        OptionDto optionDto2 = buildOptionDto(0, 200, 0, 0);
        OptionGroupDto optionGroupDto2 = buildOptionGroupDto(List.of(optionDto2), 1, null, null, true);
        OptionDto optionDto3 = buildOptionDto(0, 300, 0, 0);
        OptionGroupDto optionGroupDto3 = buildOptionGroupDto(List.of(optionDto3), 2, null, null, true);

        RegionGroupDto regionGroupDto = buildRegionGroupDto(false, false, List.of(1L), CourierTariffType.CATEGORY,
                RegionGroupStatus.SUCCESS);

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        shopRegionGroupsDto.setRegionsGroups(List.of(regionGroupDto));

        DeliveryTariffDto deliveryTariffDto = buildDeliveryTariffDto(
                CourierTariffType.CATEGORY,
                List.of(
                        buildCategoryRuleDto(List.of(buildCategoryIdDto("0", 1L)), 0, false),
                        buildCategoryRuleDto(List.of(buildCategoryIdDto(null, null)), 1, true),
                        buildCategoryRuleDto(List.of(buildCategoryIdDto(null, 1L)), 2, false)
                ),
                null,
                null,
                List.of(optionGroupDto1, optionGroupDto2, optionGroupDto3));

        when(retryableTarifficatorClient.getShopRegionGroups(774L)).thenReturn(shopRegionGroupsDto);
        when(retryableTarifficatorClient.getRegionGroupTariff(774L, 1L, 11L))
                .thenReturn(deliveryTariffDto);

        List<TariffInfoV1> actual = getTariffs(774);
        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withChildren(
                        new DeliveryRuleBuilder()
                                .withCategories(CollectionFactory.set(new TestCategoryId("0", 1)))
                                .withCost(10000).withOrderBeforeHour(12).withMaxDaysCount(0)
                                .build(),
                        new DeliveryRuleBuilder()
                                .withCategories(null, TreeCriteriaDefaultPolicy.INCLUDE)
                                .withCost(20000).withMaxDaysCount(0)
                                .withOrderBeforeHour(12)
                                .build(),
                        new DeliveryRuleBuilder()
                                .withCategories(CollectionFactory.set(new TestCategoryId(null, 1)))
                                .withCost(30000).withOrderBeforeHour(12).withMaxDaysCount(0)
                                .build()
                ).build();
        assertEquals(expected, actual.get(0).getRules().get(0));
    }

    @Test
    void buildFeedCategoriesOneRule() {
        OptionDto optionDto1 = buildOptionDto(0, 100, 0, 0);
        OptionGroupDto optionGroupDto1 = buildOptionGroupDto(List.of(optionDto1), 0, null, null, true);
        OptionDto optionDto2 = buildOptionDto(0, 200, 0, 0);
        OptionGroupDto optionGroupDto2 = buildOptionGroupDto(List.of(optionDto2), 1, null, null, true);
        OptionDto optionDto3 = buildOptionDto(0, 300, 0, 0);
        OptionGroupDto optionGroupDto3 = buildOptionGroupDto(List.of(optionDto3), 2, null, null, true);

        RegionGroupDto regionGroupDto = buildRegionGroupDto(false, false, List.of(1L), CourierTariffType.CATEGORY,
                RegionGroupStatus.SUCCESS);

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        shopRegionGroupsDto.setRegionsGroups(List.of(regionGroupDto));

        DeliveryTariffDto deliveryTariffDto = buildDeliveryTariffDto(
                CourierTariffType.CATEGORY,
                List.of(
                        buildCategoryRuleDto(List.of(
                                buildCategoryIdDto(null, 1L),
                                buildCategoryIdDto("1", 2L)
                        ), 0, false),
                        buildCategoryRuleDto(List.of(
                                buildCategoryIdDto(null, null)
                        ), 1, true),
                        buildCategoryRuleDto(List.of(
                                buildCategoryIdDto("0", 1L)
                        ), 2, false)
                ),
                null,
                null,
                List.of(optionGroupDto1, optionGroupDto2, optionGroupDto3));

        when(retryableTarifficatorClient.getShopRegionGroups(774L)).thenReturn(shopRegionGroupsDto);
        when(retryableTarifficatorClient.getRegionGroupTariff(774L, 1L, 11L))
                .thenReturn(deliveryTariffDto);

        List<TariffInfoV1> actual = getTariffs(774);
        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withChildren(
                        new DeliveryRuleBuilder()
                                .withCategories(CollectionFactory.set(
                                        new TestCategoryId(null, 1),
                                        new TestCategoryId("1", 2L)
                                ))
                                .withCost(10000).withOrderBeforeHour(12).withMaxDaysCount(0)
                                .build(),
                        new DeliveryRuleBuilder()
                                .withCategories(null, TreeCriteriaDefaultPolicy.INCLUDE)
                                .withCost(20000).withMaxDaysCount(0)
                                .withOrderBeforeHour(12)
                                .build(),
                        new DeliveryRuleBuilder()
                                .withCategories(CollectionFactory.set(new TestCategoryId("0", 1)))
                                .withCost(30000).withOrderBeforeHour(12).withMaxDaysCount(0)
                                .build()
                ).build();
        assertEquals(expected, actual.get(0).getRules().get(0));
    }


    @Test
    void buildFeedCategoriesTwoRule() {
        OptionDto optionDto1 = buildOptionDto(0, 100, 0, 2);
        OptionGroupDto optionGroupDto1 = buildOptionGroupDto(List.of(optionDto1), 0, null, null, true);
        OptionDto optionDto2 = buildOptionDto(0, 200, 0, 2);
        OptionGroupDto optionGroupDto2 = buildOptionGroupDto(List.of(optionDto2), 1, null, null, true);
        OptionDto optionDto3 = buildOptionDto(0, 300, 0, 2);
        OptionGroupDto optionGroupDto3 = buildOptionGroupDto(List.of(optionDto3), 2, null, null, true);

        RegionGroupDto regionGroupDto = buildRegionGroupDto(false, false, List.of(1L), CourierTariffType.CATEGORY,
                RegionGroupStatus.SUCCESS);

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        shopRegionGroupsDto.setRegionsGroups(List.of(regionGroupDto));

        DeliveryTariffDto deliveryTariffDto = buildDeliveryTariffDto(
                CourierTariffType.CATEGORY,
                List.of(
                        buildCategoryRuleDto(List.of(
                                buildCategoryIdDto(null, 1L)
                        ), 0, false),
                        buildCategoryRuleDto(List.of(
                                buildCategoryIdDto("1", 2L)
                        ), 1, false),
                        buildCategoryRuleDto(List.of(
                                buildCategoryIdDto("0", 1L)
                        ), 0, false)
                ),
                null,
                null,
                List.of(optionGroupDto1, optionGroupDto2, optionGroupDto3));

        when(retryableTarifficatorClient.getShopRegionGroups(774L)).thenReturn(shopRegionGroupsDto);
        when(retryableTarifficatorClient.getRegionGroupTariff(774L, 1L, 11L))
                .thenReturn(deliveryTariffDto);

        OptionGroup og = new OptionGroup(null, 0, Short.valueOf("0"), null, null, set(new DeliveryOption(0, 100, 0,
                2, 12)));
        OptionGroup og1 = new OptionGroup(null, 0, Short.valueOf("1"), null, null, set(new DeliveryOption(0, 200, 0,
                2, 12)));
        OptionGroup og2 = new OptionGroup(null, 0, Short.valueOf("2"), null, null, set(new DeliveryOption(0, 300, 0,
                2, 12)));

        List<TariffInfoV1> actual = getTariffs(774);
        DeliveryRule expected = new DeliveryRuleBuilder()
                .withRegions(set(1L))
                .withChildren(
                        new DeliveryRuleBuilder()
                                .withCategories(CollectionFactory.set(new TestCategoryId(null, 1)))
                                .withCost(10000).withOrderBeforeHour(12)
                                .build(),
                        new DeliveryRuleBuilder()
                                .withCategories(CollectionFactory.set(new TestCategoryId("1", 2L)))
                                .withCost(20000)
                                .withOrderBeforeHour(12)
                                .build(),
                        new DeliveryRuleBuilder()
                                .withCategories(CollectionFactory.set(new TestCategoryId("0", 1)))
                                .withCost(30000).withOrderBeforeHour(12)
                                .build()
                ).build();
        assertEquals(expected, actual.get(0).getRules().get(0));
    }

    @Test
    void buildRuleWithEmptyRegions() {
        OptionDto optionDto = buildOptionDto(1, 100, 0, 0);
        OptionGroupDto optionGroupDto = buildOptionGroupDto(List.of(optionDto), null, 1, null, true);

        RegionGroupDto regionGroupDto = buildRegionGroupDto(false, false, null, CourierTariffType.PRICE,
                RegionGroupStatus.NEW);

        ShopRegionGroupsDto shopRegionGroupsDto = new ShopRegionGroupsDto();
        shopRegionGroupsDto.setRegionsGroups(List.of(regionGroupDto));

        DeliveryTariffDto deliveryTariffDto = buildDeliveryTariffDto(
                CourierTariffType.PRICE,
                null,
                List.of(
                        buildPriceRuleDto(1, BigDecimal.valueOf(1000L), null)
                ),
                null,
                List.of(optionGroupDto));

        when(retryableTarifficatorClient.getShopRegionGroups(774L)).thenReturn(shopRegionGroupsDto);
        when(retryableTarifficatorClient.getRegionGroupTariff(774L, 1L, 11L))
                .thenReturn(deliveryTariffDto);

        List<TariffInfoV1> actual = getTariffs(774);
        assertEquals(0, actual.size());
    }

    private CategoryIdDto buildCategoryIdDto(String categoryId, Long feedId) {
        CategoryIdDto categoryIdDto = new CategoryIdDto();
        categoryIdDto.setCategoryId(categoryId);
        categoryIdDto.setFeedId(feedId);

        return categoryIdDto;
    }

    private CategoryRuleDto buildCategoryRuleDto(List<CategoryIdDto> categoryIdDtos, int orderNum, boolean others) {
        CategoryRuleDto categoryRuleDto1 = new CategoryRuleDto();
        categoryRuleDto1.setOrderNum(orderNum);
        categoryRuleDto1.setOthers(others);
        categoryRuleDto1.setIncludes(categoryIdDtos);
        return categoryRuleDto1;
    }

    private PriceRuleDto buildPriceRuleDto(int orderNum, BigDecimal priceFrom, BigDecimal priceTo) {
        PriceRuleDto priceRuleDto = new PriceRuleDto();
        priceRuleDto.setOrderNum(orderNum);
        priceRuleDto.setPriceFrom(priceFrom);
        priceRuleDto.setPriceTo(priceTo);
        return priceRuleDto;
    }

    private OptionDto buildOptionDto(int orderNum, int cost, int daysFrom, int daysTo) {
        OptionDto optionDto = new OptionDto();
        optionDto.setOrderNum(orderNum);
        optionDto.setCost(BigDecimal.valueOf(cost));
        optionDto.setDaysFrom(daysFrom);
        optionDto.setDaysTo(daysTo);
        optionDto.setOrderBeforeHour(12);
        return optionDto;
    }

    private OptionGroupDto buildOptionGroupDto(List<OptionDto> optionDtos,
                                               Integer categoryOrderNum,
                                               Integer priceOrderNum,
                                               Integer weightOrderNum,
                                               boolean hasDelivery) {
        OptionGroupDto optionGroupDto = new OptionGroupDto();
        optionGroupDto.setCategoryOrderNum(categoryOrderNum);
        optionGroupDto.setPriceOrderNum(priceOrderNum);
        optionGroupDto.setWeightOrderNum(weightOrderNum);
        optionGroupDto.setOptions(optionDtos);
        optionGroupDto.setHasDelivery(hasDelivery);
        return optionGroupDto;
    }

    private RegionGroupDto buildRegionGroupDto(
            boolean isSelfRegion,
            boolean isUseYml,
            List<Long> includes,
            CourierTariffType tariffType,
            RegionGroupStatus status) {

        RegionGroupDto regionGroupDto = new RegionGroupDto();
        regionGroupDto.setId(1L);
        regionGroupDto.setDatasourceId(774L);
        regionGroupDto.setName("test");
        regionGroupDto.setSelfRegion(isSelfRegion);
        regionGroupDto.setTariffType(tariffType);
        regionGroupDto.setUseYml(isUseYml);
        regionGroupDto.setIncludes(includes);
        regionGroupDto.setExcludes(null);
        regionGroupDto.setModifiedBy(1L);
        regionGroupDto.setCurrency(Currency.BYR);
        regionGroupDto.setHasDeliveryService(null);
        regionGroupDto.setCheckStatus(status);
        regionGroupDto.setCheckStatusModifiedAt(new Date());
        return regionGroupDto;
    }

    private DeliveryTariffDto buildDeliveryTariffDto(CourierTariffType tariffType,
                                                     List<CategoryRuleDto> categoryRuleDtos,
                                                     List<PriceRuleDto> priceRuleDtos,
                                                     List<WeightRuleDto> weightRuleDtos,
                                                     List<OptionGroupDto> optionGroupDtos) {
        DeliveryTariffDto deliveryTariffDto = new DeliveryTariffDto();
        deliveryTariffDto.setTariffType(tariffType);
        deliveryTariffDto.setNotes(null);
        deliveryTariffDto.setUseYml(false);
        deliveryTariffDto.setCategoryRules(categoryRuleDtos);
        deliveryTariffDto.setPriceRules(priceRuleDtos);
        deliveryTariffDto.setWeightRules(weightRuleDtos);
        deliveryTariffDto.setOptionsGroups(optionGroupDtos);
        return deliveryTariffDto;
    }

    private List<TariffInfoV1> getTariffs(final long shopId) {
        final ShopDeliveryInfo shopDeliveryInfo = exporter.prepareShopInfo(PartnerId.datasourceId(shopId));
        return shopDeliveryInfo.getTariffInfos();
    }

}
