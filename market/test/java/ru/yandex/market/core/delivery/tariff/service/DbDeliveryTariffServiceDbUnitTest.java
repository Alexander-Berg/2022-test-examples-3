package ru.yandex.market.core.delivery.tariff.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Bank;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.api.delivery.failure.RegionGroupFailureReason;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.category.FeedCategoryService;
import ru.yandex.market.core.category.model.ShopCategory;
import ru.yandex.market.core.delivery.RegionGroupPaymentType;
import ru.yandex.market.core.delivery.tariff.DeliveryTariffTestHelper;
import ru.yandex.market.core.delivery.tariff.model.CategoryId;
import ru.yandex.market.core.delivery.tariff.model.CategoryRule;
import ru.yandex.market.core.delivery.tariff.model.DeliveryOption;
import ru.yandex.market.core.delivery.tariff.model.DeliveryRuleId;
import ru.yandex.market.core.delivery.tariff.model.DeliveryTariff;
import ru.yandex.market.core.delivery.tariff.model.OptionGroup;
import ru.yandex.market.core.delivery.tariff.model.PriceRule;
import ru.yandex.market.core.delivery.tariff.model.RegionGroup;
import ru.yandex.market.core.delivery.tariff.model.RegionGroupStatus;
import ru.yandex.market.core.delivery.tariff.model.RegionInfo;
import ru.yandex.market.core.delivery.tariff.model.TariffType;
import ru.yandex.market.core.delivery.tariff.model.WeightRule;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.protocol.model.ActionType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.common.util.collections.CollectionFactory.set;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.before.csv")
class DbDeliveryTariffServiceDbUnitTest extends FunctionalTest {

    @Autowired
    private DeliveryTariffService deliveryTariffService;

    @Autowired
    private RegionService regionService;

    @Autowired
    private FeedCategoryService feedCategoryService;

    @BeforeEach
    void setUp() {
        doAnswer((Answer<Collection<ShopCategory>>) invocation -> CollectionFactory.list(
                shopCategory(1, "1", null, "test1"), shopCategory(1, "2", "1", "test2"),
                shopCategory(1, "3", "1", "test3"), shopCategory(1, "4", null, "test4")
        )).when(feedCategoryService).getDatasourceCategoriesSubtree(anyLong(), any());
    }

    @Test
    @DbUnitDataSet(after = "DbDeliveryTariffServiceDbUnitTest.testCreateRegionGroup.after.csv")
    void testCreateRegionGroup() {
        RegionGroup group = DeliveryTariffTestHelper.createTestRegionGroup(null, 774L, "test", false, TariffType.DEFAULT, false,
                set(1L), null, 1L, null);

        doReturn(Collections.singletonList(
                new Region(1, "1", null))).when(regionService).getRootBranchRegions(any());

        deliveryTariffService.createRegionGroup(group, null, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testUpdateRegionGroupMultiGroup.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testUpdateRegionGroupMultiGroup.after.csv")
    void testUpdateRegionGroupMultiGroup() {
        doReturn(Collections.emptyList()).when(regionService).getRootBranchRegions(anySet());

        RegionGroup group = deliveryTariffService.getRegionGroup(1L);
        group.setIncludes(set(1L, 2L));
        group.setModifiedBy(1L);

        doReturn(Arrays.asList(
                new Region(1, "1", null),
                new Region(2, "2", null)
        )).when(regionService).getRootBranchRegions(anySet());

        deliveryTariffService.updateRegionGroup(group, null, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testUpdateRGAddChildRegionFromAnotherRegionGroup.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testUpdateRGAddChildRegionFromAnotherRegionGroup.after.csv"
    )
    void testUpdateRGAddChildRegionFromAnotherRegionGroup() {
        RegionGroup group = prepareRegionGroup();
        deliveryTariffService.updateRegionGroup(group, null, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testUpdateRGAddChildRegionSameRegionGroup.before.csv"
    )
    void testUpdateRGAddChildRegionSameRegionGroup() {
        RegionGroup group = prepareRegionGroup();
        deliveryTariffService.updateRegionGroup(group, null, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    private RegionGroup prepareRegionGroup() {
        RegionGroup group = deliveryTariffService.getRegionGroup(1L);
        group.setModifiedBy(1L);
        group.getIncludes().add(4L);
        prepareRegionsSubtree();
        return group;
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testCreateRGAddChildRegionSameRegionGroup.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testCreateRGAddChildRegionSameRegionGroup.after.csv"
    )
    void testCreateRGAddChildRegionSameRegionGroup() {
        RegionGroup group = DeliveryTariffTestHelper.createTestRegionGroup(null, 774L, "test1", false, TariffType.UNIFORM, false,
                set(1L, 4L), null, 1L, null);
        prepareRegionsSubtree();
        deliveryTariffService.createRegionGroup(group, null, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testCreateRGAddChildRegionFromAnotherRegionGroup.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testCreateRGAddChildRegionFromAnotherRegionGroup.after.csv"
    )
    void testCreateRGAddChildRegionFromAnotherRegionGroup() {
        prepareRegionsSubtree();
        RegionGroup group1 = DeliveryTariffTestHelper.createTestRegionGroup(null, 774L, "test1", false, TariffType.UNIFORM, false,
                set(1L, 4L), null, 1L, null);

        deliveryTariffService.createRegionGroup(group1, null, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testCreateRGWithForceRemoveRegions.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testCreateRGWithForceRemoveRegions.after.csv"
    )
    void testCreateRGWithForceRemoveRegions() {
        RegionGroup regionGroup = DeliveryTariffTestHelper.createTestRegionGroup(null, 774L, "test3", false, TariffType.DEFAULT,
                false, set(1L), null, 1L, null);

        Map<Long, Set<Long>> forceRemoveRegions = Collections.singletonMap(100L, CollectionFactory.set(350L));

        doReturn(Collections.singletonList(
                new Region(1, "1", null)
        )).when(regionService).getRootBranchRegions(anySet());

        deliveryTariffService.createRegionGroup(regionGroup, forceRemoveRegions, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testUpdateRegionGroupForceExclude.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testUpdateRegionGroupForceExclude.after.csv"
    )
    void testUpdateRegionGroupForceExclude() {
        doReturn(Collections.emptyList()).when(regionService).getRootBranchRegions(anySet());

        RegionGroup group = deliveryTariffService.getRegionGroup(1L);
        group.setModifiedBy(1L);
        Map<Long, Set<Long>> excludes = new HashMap<>();
        excludes.put(2L, set(2L));

        doReturn(Collections.singletonList(
                new Region(1, "1", null)
        )).when(regionService).getRootBranchRegions(any());

        deliveryTariffService.updateRegionGroup(group, excludes, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testDeleteRegionGroup.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testDeleteRegionGroup.after.csv"
    )
    void testDeleteRegionGroup() {
        doReturn(Collections.emptyList()).when(regionService).getRootBranchRegions(any());
        deliveryTariffService.deleteRegionGroup(1L, ActionType.REMOVE_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroups.before.csv"
    )
    void testGetRegionGroups() {
        List<RegionGroup> regionGroupExpected = list(
                DeliveryTariffTestHelper.createTestRegionGroup(1L, 774L, "test1", false, TariffType.UNIFORM,
                        false, set(1L), null, 1L, null),
                DeliveryTariffTestHelper.createTestRegionGroup(2L, 774L, "test2", false, TariffType.UNIFORM,
                        false, set(2L), null, 1L, null));

        List<RegionGroup> regionGroupsActual = deliveryTariffService.getRegionGroups(774L);
        for (RegionGroup group : regionGroupsActual) {
            group.setModifiedAt(null);
        }
        Assertions.assertEquals(regionGroupExpected, regionGroupsActual);
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionsChildren.before.csv"
    )
    void testGetRegionsChildren() {
        Collection<RegionInfo> expected = set(
                new RegionInfo(1, "Москва", null, 3L, 2L, true),
                new RegionInfo(10645, "Владимирская область", null, 3L, 0L, true)
        );

        Collection<RegionInfo> result = deliveryTariffService.getRegionsChildren(3L);
        assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testCreateSelfRegion.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testCreateSelfRegion.after.csv"
    )
    void testCreateSelfRegion() {
        deliveryTariffService.createSelfRegionGroup(774L, ActionType.EDIT_DELIVERY_REGION_GROUP.getId(), null);
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testCreateSelfRegionWrong.before.csv"
    )
    void testCreateSelfRegionWrong() {
        assertThrows(
                IllegalStateException.class,
                () -> deliveryTariffService.createSelfRegionGroup(774L, ActionType.EDIT_DELIVERY_REGION_GROUP.getId(), null)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "one_region_group.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffPrice.after.csv"
    )
    void testSaveTariffPrice() {
        DeliveryTariff tariff = new DeliveryTariff(
                1,
                TariffType.PRICE,
                null, false,
                null,
                new PriceRule[]{
                        new PriceRule(new DeliveryRuleId(1, 0), null, BigDecimal.valueOf(1000L)),
                        new PriceRule(new DeliveryRuleId(1, 1), BigDecimal.valueOf(1000L), null)
                },
                null,
                Arrays.asList(
                        new OptionGroup(null, 1, null, (short) 0, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        ),
                        new OptionGroup(null, 1, null, (short) 1, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.ZERO, (short) 0, (short) 1, (byte) 12)
                                )
                        )
                )
        );
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "one_region_group.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffWeight.after.csv"
    )
    void testSaveTariffWeight() {
        DeliveryTariff tariff = new DeliveryTariff(
                1,
                TariffType.WEIGHT,
                null, false,
                null,
                null,
                new WeightRule[]{
                        new WeightRule(new DeliveryRuleId(1, 0), null, 1),
                        new WeightRule(new DeliveryRuleId(1, 1), 1, null)
                },
                Arrays.asList(
                        new OptionGroup(null, 1, null, null, (short) 0,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        ),
                        new OptionGroup(null, 1, null, null, (short) 1,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.ZERO, (short) 0, (short) 1, (byte) 12)
                                )
                        )
                )
        );
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "one_region_group.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategory.after.csv"
    )
    void testSaveTariffCategory() {
        DeliveryTariff tariff = new DeliveryTariff(
                1,
                TariffType.CATEGORY,
                null, false,
                new CategoryRule[]{
                        new CategoryRule(new DeliveryRuleId(1, 0), categoryIds("1", 1)),
                        new CategoryRule(new DeliveryRuleId(1, 1), categoryIds("2", 1)),
                        new CategoryRule(new DeliveryRuleId(1, 2), true)
                },
                null,
                null,
                Arrays.asList(
                        new OptionGroup(null, 1, (short) 0, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        ),
                        new OptionGroup(null, 1, (short) 1, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.ZERO, (short) 0, (short) 1, (byte) 12)
                                )
                        ),
                        new OptionGroup(null, 1, (short) 2, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.ZERO, (short) 0, (short) 1, (byte) 12)
                                )
                        )
                )
        );
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    //1 - 2
    @Test
    @DbUnitDataSet(
            before = "one_region_group.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffSpecCategory.after.csv"
    )
    void testSaveTariffSpecCategory() {
        DeliveryTariff tariff = new DeliveryTariff(
                1,
                TariffType.CATEGORY,
                null, false,
                new CategoryRule[]{
                        new CategoryRule(new DeliveryRuleId(1, 0), categoryIds("1", 1, "2", 1)),
                        new CategoryRule(new DeliveryRuleId(1, 1), true),
                },
                null,
                null,
                Arrays.asList(
                        new OptionGroup(null, 1, (short) 0, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        ),
                        new OptionGroup(null, 1, (short) 1, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        )
                )
        );
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "one_region_group.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategoryDouble.after.csv"
    )
    void testSaveTariffCategoryDouble() {
        DeliveryTariff tariff = new DeliveryTariff(
                1,
                TariffType.CATEGORY,
                null, false,
                new CategoryRule[]{
                        new CategoryRule(new DeliveryRuleId(1, 0), categoryIds("1", 1)),
                        new CategoryRule(new DeliveryRuleId(1, 1), categoryIds("2", 1)),
                        new CategoryRule(new DeliveryRuleId(1, 2), true)

                },
                null,
                null,
                Arrays.asList(
                        new OptionGroup(null, 1, (short) 0, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        ),
                        new OptionGroup(null, 1, (short) 1, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.ZERO, (short) 0, (short) 1, (byte) 12)
                                )
                        ),
                        new OptionGroup(null, 1, (short) 2, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, (short) 0, BigDecimal.ZERO, (short) 0, (short) 1, (byte) 12)
                                )
                        )

                )
        );
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategorySame.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategorySame.after.csv"
    )
    void testSaveTariffCategorySame() {
        DeliveryTariff tariff = new DeliveryTariff(
                1,
                TariffType.CATEGORY,
                null, false,
                new CategoryRule[]{
                        new CategoryRule(new DeliveryRuleId(1, 0), categoryIds("1", 1)),
                        new CategoryRule(new DeliveryRuleId(1, 1), categoryIds("2", 1)),
                        new CategoryRule(new DeliveryRuleId(1, 2), true)
                },
                null,
                null,
                Arrays.asList(
                        new OptionGroup(null, 1, (short) 0, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, 100, 0, 1, 12)
                                )
                        ),
                        new OptionGroup(null, 1, (short) 1, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, 0, 0, 1, 12)
                                )
                        ),
                        new OptionGroup(null, 1, (short) 2, null, null,
                                Collections.singletonList(
                                        new DeliveryOption(0, 0, 0, 1, 12)
                                )
                        )
                )
        );
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "one_region_group.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategoryOthers.after.csv"
    )
    void testSaveTariffCategoryOthers() {
        checkSaveTariffCategory();
    }

    @Test
    @DbUnitDataSet(
            before = {"one_region_group.csv", "DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategoryOthersSame.before.csv"},
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategoryOthersSame.after.csv"
    )
    void testSaveTariffCategoryOthersSame() {
        checkSaveTariffCategory();
    }

    @Test
    @DbUnitDataSet(
            before = {"DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategoryCleanup.before.csv"},
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategoryCleanup.after.csv"
    )
    void testSaveTariffCategoryCleanup() {
        DeliveryTariff tariff = new DeliveryTariff(
                1,
                TariffType.CATEGORY,
                null, false,
                new CategoryRule[]{
                        new CategoryRule(new DeliveryRuleId(1, 0), categoryIds("1", 1)),
                        new CategoryRule(new DeliveryRuleId(1, 1), true)
                },
                null,
                null,
                list(
                        new OptionGroup(null, 1, (short) 0, null, null,
                                list(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        ),
                        new OptionGroup(null, 1, (short) 1, null, null,
                                list(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        )
                )
        );
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(
            before = "one_region_group.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testSaveTariffCategoryFeedRule.after.csv"
    )
    void testSaveTariffCategoryFeedRule() {
        DeliveryTariff tariff = new DeliveryTariff(
                1,
                TariffType.CATEGORY,
                null, false,
                new CategoryRule[]{
                        new CategoryRule(new DeliveryRuleId(1, 0), categoryIds(null, 1)),
                        new CategoryRule(new DeliveryRuleId(1, 1), true)
                },
                null,
                null,
                list(
                        new OptionGroup(null, 1, (short) 0, null, null,
                                list(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        ),
                        new OptionGroup(null, 1, (short) 1, null, null,
                                list(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        )
                )
        );
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    /**
     * Тест проверят, что при измененеия региона в локально группе:
     * - старый регион удаляется (https://st.yandex-team.ru/MBI-20320)
     * - группа регионов приобретает корректное имя (совпадающее с именем региона)
     */
    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testUpdateSelfRegionGroupRemovesOldRegionOnRegionChange.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testUpdateSelfRegionGroupRemovesOldRegionOnRegionChange.after.csv"
    )
    void testUpdateSelfRegionGroupRemovesOldRegionOnRegionChange() {
        deliveryTariffService.updateSelfRegionGroup(774L, 1234L, BigDecimal.valueOf(5L));
    }

    /**
     * Тест на синхронизацию с {@code ParamValue.LOCAL_DELIVERY_REGION} - в случае если не передан явный аргумент
     * {@code localRegionId}, то нужно взять его из параметра
     */
    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testUpdateSelfRegionGroupRemovesOldRegionOnRegionChange.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testUpdateSelfRegionGroupRemovesOldRegionOnRegionChange.after.csv"
    )
    void testUpdateSelfRegionFromParamType21() {
        deliveryTariffService.updateSelfRegionGroup(774L, 1234L, null);
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testUpdateSelfRegionNoChange.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testUpdateSelfRegionNoChange.after.csv"
    )
    void testUpdateSelfRegionNoChange() {
        deliveryTariffService.updateSelfRegionGroup(774L, ActionType.EDIT_DELIVERY_REGION_GROUP.getId(), BigDecimal.valueOf(3L));
    }


    /**
     * Тест проверят, что при изменении региона в локальной группе:
     * - старый регион удаляется (https://st.yandex-team.ru/MBI-20320)
     * - группа регионов приобретает корректное имя (совпадающее с именем региона)
     */
    @Test
    @Disabled
    @DbUnitDataSet
    void testUpdateSelfRegionGroupChangeCurrencyOnRegionChange() {
        Map<Long, Pair<Currency, Bank>> currency = CollectionFactory.newHashMap();
        currency.put(3L, new Pair<>(Currency.RUR, Bank.BUSD));
        currency.put(5L, new Pair<>(Currency.EUR, Bank.BUSD));
        doReturn(currency).when(regionService).getRegionCurrencies(any());

        deliveryTariffService.updateSelfRegionGroup(774L, 1234L, BigDecimal.valueOf(5L));
    }

    /**
     * Тест проверят, что при изменении регионов в группе страые регионы удаляются
     */
    @Test
    @DbUnitDataSet(
            before = "DbDeliveryTariffServiceDbUnitTest.testUpdateRegionGroupRemovesOldRegionOnRegionChange.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.testUpdateRegionGroupRemovesOldRegionOnRegionChange.after.csv"
    )
    void testUpdateRegionGroupRemovesOldRegionOnRegionChange() {
        doReturn(Collections.emptyList()).when(regionService).getRootBranchRegions(anySet());

        RegionGroup regionGroupBefore = deliveryTariffService.getRegionGroup(1L);
        regionGroupBefore.setIncludes(CollectionFactory.set(5L));
        regionGroupBefore.setModifiedBy(1L);
        regionGroupBefore.setName("Иваново");

        Collection<Region> c = new ArrayList<>();
        c.add(new Region(5, "Иваново", null));
        c.add(new Region(3, "ЦФО", null));

        doReturn(c).when(regionService).getRootBranchRegions(any());

        deliveryTariffService.updateRegionGroup(regionGroupBefore, null, 1234L);
    }

    protected CategoryId categoryId(String categoryId, Number feedId) {
        return new CategoryId(categoryId, feedId.longValue());
    }

    private SortedSet<CategoryId> categoryIds(Object... ids) {
        SortedSet<CategoryId> result = new TreeSet<>();
        for (int i = 0; i < ids.length; i += 2) {
            result.add(categoryId((String) ids[i], (Number) ids[1]));
        }
        return result;
    }

    private ShopCategory shopCategory(long feedId, String categoryId, String parentId, String name) {
        ShopCategory category = new ShopCategory();
        category.setFeedId(feedId);
        category.setCategoryId(categoryId);
        category.setParentCategoryId(parentId);
        category.setName(name);
        return category;
    }

    /**
     * Тесты на формирование excludoв
     */
    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludes.before.csv")
    void testGetRegionGroupExcludes() {
        prepareRegionsSubtree();
        RegionGroup group1 = deliveryTariffService.getRegionGroup(1L);
        assertEquals(group1.getIncludes(), CollectionFactory.set(1L));
        assertEquals(group1.getExcludes(), CollectionFactory.set(3L));
        assertEquals(11212L, group1.getTarifficatorId());

        RegionGroup group2 = deliveryTariffService.getRegionGroup(2L);
        assertEquals(group2.getIncludes(), CollectionFactory.set(3L));
        assertEquals(0, group2.getExcludes().size());
        assertNull(group2.getTarifficatorId());
    }

    /**
     * Дерево регионов 1-2-3-4
     * ГР1: регион 1 (exclude 2)
     * ГР2: регион 2 (exclude 3)
     * ГР3: регион 3
     */
    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludes3.before.csv")
    void testGetRegionGroupExcludes3() {
        prepareRegionsSubtree();
        RegionGroup group1 = deliveryTariffService.getRegionGroup(1L);
        assertEquals(CollectionFactory.set(1L), group1.getIncludes());
        assertEquals(CollectionFactory.set(2L), group1.getExcludes());

        RegionGroup group2 = deliveryTariffService.getRegionGroup(2L);
        assertEquals(CollectionFactory.set(2L), group2.getIncludes());
        assertEquals(CollectionFactory.set(3L), group2.getExcludes());

        RegionGroup group3 = deliveryTariffService.getRegionGroup(3L);
        assertEquals(CollectionFactory.set(3L), group3.getIncludes());
        assertEquals(0, group3.getExcludes().size());
    }

    /**
     * Дерево регионов 1-2-3-4
     * ГР1: регион 1,4 (exclude 2,3)
     * ГР2: регион 2 (exclude 3)
     * ГР3: регион 3 (exclude 4)
     */
    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludes4.before.csv")
    void testGetRegionGroupExcludes4() {
        prepareRegionsSubtree();
        RegionGroup group1 = deliveryTariffService.getRegionGroup(1L);
        assertEquals(CollectionFactory.set(1L, 4L), group1.getIncludes());
        assertEquals(CollectionFactory.set(2L), group1.getExcludes());

        RegionGroup group2 = deliveryTariffService.getRegionGroup(2L);
        assertEquals(CollectionFactory.set(2L), group2.getIncludes());
        assertEquals(CollectionFactory.set(3L), group2.getExcludes());

        RegionGroup group3 = deliveryTariffService.getRegionGroup(3L);
        assertEquals(CollectionFactory.set(3L), group3.getIncludes());
        assertEquals(CollectionFactory.set(4L), group3.getExcludes());
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludes1.before.csv")
    void testGetRegionGroupExcludes1() {
        prepareRegionsSubtree();
        RegionGroup group1 = deliveryTariffService.getRegionGroup(1L);
        assertEquals(CollectionFactory.set(1L), group1.getIncludes());
        assertEquals(0, group1.getExcludes().size());
    }

    private void prepareRegionsSubtree() {
        Collection<Region> c = CollectionFactory.list(new Region(1, "test1", null), new Region(2, "test2", 1L),
                new Region(3, "test3", 2L), new Region(4, "test4", 3L),
                new Region(5, "test5", 4L), new Region(6, "test6", 5L),
                new Region(7, "test7", 6L), new Region(8, "test8", 7L),
                new Region(9, "test9", 8L)
        );
        doReturn(c).when(regionService).getRootBranchRegions(any());
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludes9.before.csv")
    void testGetRegionGroupExcludes9() {
        prepareRegionsSubtree();
        RegionGroup group1 = deliveryTariffService.getRegionGroup(1L);
        assertEquals(CollectionFactory.set(1L, 5L, 9L), group1.getIncludes());
        assertEquals(CollectionFactory.set(3L, 6L), group1.getExcludes());

        RegionGroup group2 = deliveryTariffService.getRegionGroup(2L);
        assertEquals(CollectionFactory.set(3L, 6L), group2.getIncludes());
        assertEquals(CollectionFactory.set(4L, 8L), group2.getExcludes());

        RegionGroup group3 = deliveryTariffService.getRegionGroup(3L);
        assertEquals(CollectionFactory.set(4L, 8L), group3.getIncludes());
        assertEquals(CollectionFactory.set(5L, 9L), group3.getExcludes());
    }

    @Test
    @DisplayName("При удалении фида удалить категорийные правила этого фида.")
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.deleteFeedUnreferencedCategoryRules1.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.deleteFeedUnreferencedCategoryRules1.after.csv")
    void deleteFeedUnreferencedCategoryRules1() {
        deleteFeedUnreferencedCategoryRules();
    }

    @Test
    @DisplayName("При удалении фида не удалять категорийные правила другого фида.")
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.deleteFeedUnreferencedCategoryRules2.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.deleteFeedUnreferencedCategoryRules2.after.csv")
    void deleteFeedUnreferencedCategoryRules2() {
        deleteFeedUnreferencedCategoryRules();
    }

    @Test
    @DisplayName("При удалении фида всегда оставлять категорийное правило others=1.")
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.deleteFeedUnreferencedCategoryRules3.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.deleteFeedUnreferencedCategoryRules3.after.csv")
    void deleteFeedUnreferencedCategoryRules3() {
        deleteFeedUnreferencedCategoryRules();
    }

    @Test
    @DisplayName("При удалении фида, удалять категорийные правила, но оставлять ценовые правила")
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.deleteFeedUnreferencedCategoryRules4.before.csv",
            after = "DbDeliveryTariffServiceDbUnitTest.deleteFeedUnreferencedCategoryRules4.after.csv")
    void deleteFeedUnreferencedCategoryRules4() {
        deleteFeedUnreferencedCategoryRules();
    }

    private void deleteFeedUnreferencedCategoryRules() {
        deliveryTariffService.deleteFeedUnreferencedCategoryRules(1L, 1L);
    }

    private void checkSaveTariffCategory() {
        DeliveryTariff tariff = new DeliveryTariff(
                1,
                TariffType.CATEGORY,
                null, false,
                new CategoryRule[]{
                        new CategoryRule(new DeliveryRuleId(1, 0), true)
                },
                null,
                null,
                list(
                        new OptionGroup(null, 1, (short) 0, null, null,
                                list(
                                        new DeliveryOption(0, (short) 0, BigDecimal.valueOf(100), (short) 0, (short) 1, (byte) 12)
                                )
                        )
                )
        );
        deliveryTariffService.saveDeliveryTariff(tariff, ActionType.EDIT_DELIVERY_REGION_GROUP.getId());
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludes1.before.csv")
    void testGetCurrentRegions1() {
        prepareAllRegions();
        Set<Long> regions = deliveryTariffService.getUnfoldedRegions(1L).keySet();
        assertEquals(CollectionFactory.set(1L), regions);

        Set<Long> regions2 = deliveryTariffService.getUnfoldedRegions(2L).keySet();
        assertTrue(CollectionUtils.isEmpty(regions2));
    }

    /**
     * 1-2-3
     */
    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludes3.before.csv")
    void testGetCurrentRegions3() {
        prepareAllRegions();
        Set<Long> regions = deliveryTariffService.getUnfoldedRegions(1L).keySet();
        assertTrue(CollectionUtils.isEmpty(regions));

        Set<Long> regions2 = deliveryTariffService.getUnfoldedRegions(2L).keySet();
        assertTrue(CollectionUtils.isEmpty(regions2));

        Set<Long> regions3 = deliveryTariffService.getUnfoldedRegions(3L).keySet();
        assertEquals(CollectionFactory.set(3L), regions3);
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludes4.before.csv")
    void testGetCurrentRegions4() {
        prepareAllRegions();
        Set<Long> regions = deliveryTariffService.getUnfoldedRegions(1L).keySet();
        assertEquals(CollectionFactory.set(4L), regions);

        Set<Long> regions2 = deliveryTariffService.getUnfoldedRegions(2L).keySet();
        assertTrue(CollectionUtils.isEmpty(regions2));

        Set<Long> regions3 = deliveryTariffService.getUnfoldedRegions(3L).keySet();
        assertTrue(CollectionUtils.isEmpty(regions3));
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludes9.before.csv")
    void testGetCurrentRegions9() {
        prepareAllRegions();
        Set<Long> regions = deliveryTariffService.getUnfoldedRegions(1L).keySet();
        assertEquals(CollectionFactory.set(9L), regions);


        Set<Long> regions2 = deliveryTariffService.getUnfoldedRegions(2L).keySet();
        assertTrue(CollectionUtils.isEmpty(regions2));

        Set<Long> regions3 = deliveryTariffService.getUnfoldedRegions(3L).keySet();
        assertTrue(CollectionUtils.isEmpty(regions3));
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludesTree.before.csv")
    void testGetCurrentRegionsTree() {
        Collection<Region> c = CollectionFactory.list(new Region(1, "test1", null), new Region(2, "test2", 1L),
                new Region(3, "test3", 1L), new Region(4, "test4", 1L),
                new Region(5, "test5", 2L), new Region(6, "test6", 4L),
                new Region(7, "test7", 4L), new Region(8, "test8", 1L),
                new Region(9, "test9", 8L)
        );
        doReturn(c.stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()))).when(regionService).getAllRegions(any());
        Set<Long> regions = deliveryTariffService.getUnfoldedRegions(1L).keySet();
        assertEquals(CollectionFactory.set(2L, 3L, 7L), regions);


        Set<Long> regions2 = deliveryTariffService.getUnfoldedRegions(2L).keySet();
        assertEquals(CollectionFactory.set(6L, 8L), regions2);

        Set<Long> regions3 = deliveryTariffService.getUnfoldedRegions(3L).keySet();
        assertEquals(CollectionFactory.set(5L), regions3);
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludesTree3.before.csv")
    void testGetCurrentRegionsTree3() {
        Collection<Region> c = CollectionFactory.list(new Region(1, "test1", null), new Region(2, "test2", 1L),
                new Region(3, "test3", 1L), new Region(4, "test4", 1L),
                new Region(5, "test5", 1L), new Region(6, "test6", 3L),
                new Region(7, "test7", 3L), new Region(8, "test8", 3L),
                new Region(9, "test9", 4L), new Region(10, "test10", 4L),
                new Region(11, "test11", 5L), new Region(12, "test12", 5L),
                new Region(13, "test13", 8L), new Region(14, "test14", 8L),
                new Region(15, "test15", 12L)
        );
        doReturn(c.stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()))).when(regionService).getAllRegions(any());
        Set<Long> regions = deliveryTariffService.getUnfoldedRegions(1L).keySet();
        assertEquals(CollectionFactory.set(9L, 11L, 14L), regions);


        Set<Long> regions2 = deliveryTariffService.getUnfoldedRegions(2L).keySet();
        assertEquals(CollectionFactory.set(2L, 10L), regions2);

        Set<Long> regions3 = deliveryTariffService.getUnfoldedRegions(3L).keySet();
        assertEquals(CollectionFactory.set(6L, 7L, 12L, 13L), regions3);
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetRegionGroupExcludesTree4.before.csv")
    void testGetCurrentRegionsTree4() {
        Collection<Region> c = CollectionFactory.list(new Region(1, "test1", null), new Region(2, "test2", 1L),
                new Region(3, "test3", 1L), new Region(4, "test4", 1L),
                new Region(5, "test5", 1L), new Region(6, "test6", 2L),
                new Region(7, "test7", 2L), new Region(8, "test8", 2L),
                new Region(9, "test9", 3L), new Region(10, "test10", 3L),
                new Region(11, "test11", 3L), new Region(12, "test12", 4L),
                new Region(13, "test13", 4L), new Region(14, "test14", 5L),
                new Region(15, "test15", 5L), new Region(16, "test16", 5L),
                new Region(17, "test17", 12L), new Region(18, "test18", 12L),
                new Region(19, "test19", 15L), new Region(20, "test20", 15L),
                new Region(21, "test21", 18L), new Region(22, "test22", 18L)

        );
        doReturn(c.stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()))).when(regionService).getAllRegions(any());
        Set<Long> regions = deliveryTariffService.getUnfoldedRegions(1L).keySet();
        assertEquals(CollectionFactory.set(17L, 16L), regions);


        Set<Long> regions2 = deliveryTariffService.getUnfoldedRegions(2L).keySet();
        assertEquals(CollectionFactory.set(3L, 8L, 13L), regions2);

        Set<Long> regions3 = deliveryTariffService.getUnfoldedRegions(3L).keySet();
        assertEquals(CollectionFactory.set(6L, 18L), regions3);

        Set<Long> regions4 = deliveryTariffService.getUnfoldedRegions(4L).keySet();
        assertEquals(CollectionFactory.set(7L, 20L), regions4);
    }

    @Test
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.testGetStatus.before.csv")
    void testGetStatus() {
        final Map<Long, RegionGroupStatus> actual = deliveryTariffService.getRegionGroupStatuses(Arrays.asList(1L, 2L));
        assertEquals(2, actual.size());
        final Date date1 = Date.from(LocalDate.of(2018, 3, 4).atStartOfDay(ZoneId.systemDefault()).toInstant());
        final RegionGroupStatus status1 = new RegionGroupStatus(1L, ParamCheckStatus.NEW, date1,
                Collections.singleton(RegionGroupFailureReason.NO_DELIVERY),
                Collections.singleton(RegionGroupPaymentType.COURIER_CARD),
                "comment");
        assertEquals(status1, actual.get(1L));

        final Date date2 = Date.from(LocalDate.of(2017, 3, 4).atStartOfDay(ZoneId.systemDefault()).toInstant());
        final RegionGroupStatus status2 = new RegionGroupStatus(2L, ParamCheckStatus.FAIL, date2, Collections.emptySet(), Collections.emptySet(), null);
        assertEquals(status2, actual.get(2L));
    }

    @Test
    @DisplayName("Тесты на апдейт тариффикаторного идентификатора региональой группы")
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.updateTarifficatorId.before.csv")
    @DbUnitDataSet(after = "DbDeliveryTariffServiceDbUnitTest.updateTarifficatorId.after.csv")
    void testUpdateTarifficatorId() {
        deliveryTariffService.updateRegionGroupTarifficatorId(2L, 123L);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getRegionGroupIdsCorrespondence")
    @DisplayName("Тесты на получение маппинга идентификаторов (тариффикаторного и мбайного)")
    @DbUnitDataSet(before = "DbDeliveryTariffServiceDbUnitTest.idCorrespondance.before.csv")
    void getCorrespondingRegionGroupIds(String displayName, long shopId, Map<Long, Long> expected) {
        assertEquals(expected, deliveryTariffService.getRegionGroupIdsMappings(shopId));
    }

    public static Stream<Arguments> getRegionGroupIdsCorrespondence() {
        return Stream.of(
                Arguments.of("Получение маппинга с существующими группами", 774L, Map.of(11L, 1L, 22L, 2L)),
                Arguments.of("Получение маппинга с несуществующими группами", 775L, new HashMap<>())
                );
    }

    private void prepareAllRegions() {
        Collection<Region> c = CollectionFactory.list(new Region(1, "test1", null), new Region(2, "test2", 1L),
                new Region(3, "test3", 2L), new Region(4, "test4", 3L),
                new Region(5, "test5", 4L), new Region(6, "test6", 5L),
                new Region(7, "test7", 6L), new Region(8, "test8", 7L),
                new Region(9, "test9", 8L)
        );
        doReturn(c.stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()))).when(regionService).getAllRegions(any());
    }
}
