package ru.yandex.market.core.delivery.tariff.db;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.tariff.DeliveryTariffTestHelper;
import ru.yandex.market.core.delivery.tariff.db.dao.DeliveryTariffDao;
import ru.yandex.market.core.delivery.tariff.hierarchy.HierarchyTestUtil;
import ru.yandex.market.core.delivery.tariff.hierarchy.OwnedHierarchicalId;
import ru.yandex.market.core.delivery.tariff.model.CategoryId;
import ru.yandex.market.core.delivery.tariff.model.CategoryRule;
import ru.yandex.market.core.delivery.tariff.model.DeliveryRule;
import ru.yandex.market.core.delivery.tariff.model.DeliveryRuleId;
import ru.yandex.market.core.delivery.tariff.model.PriceRule;
import ru.yandex.market.core.delivery.tariff.model.RegionGroup;
import ru.yandex.market.core.delivery.tariff.model.RegionGroupRow;
import ru.yandex.market.core.delivery.tariff.model.TariffType;
import ru.yandex.market.core.delivery.tariff.model.WeightRule;
import ru.yandex.market.core.delivery.tariff.service.DeliveryRuleService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.common.util.collections.CollectionFactory.set;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class DeliveryTariffDaoTest extends FunctionalTest {

    private static final long REGION_GROUP_ID1 = 101;
    private static final long REGION_GROUP_ID2 = 102;
    private static final long REGION_GROUP_ID3 = 103;
    private static final long REGION_GROUP_ID4 = 104;

    @Autowired
    private DeliveryTariffDao deliveryTariffDao;

    @Autowired
    private DeliveryRuleService deliveryRuleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DbUnitDataSet(before = "DeliveryTariffDaoTest.before.csv")
    void testGetRegionGroups() {
        List<RegionGroupRow> groupRows = deliveryTariffDao.getRegionGroupRows(Collections.singletonList(774L));
        assertEquals(4, groupRows.size());
    }

    @Test
    @DbUnitDataSet(before = "DeliveryTariffDaoTest.before.csv")
    void testGetRegionGroup() {
        RegionGroupRow groupRow = deliveryTariffDao.getRegionGroupRow(REGION_GROUP_ID1);

        assertEquals("Тест", groupRow.getName());
        assertEquals(false, groupRow.getHasDeliveryService());
    }

    @Test
    @DbUnitDataSet(before = "DeliveryTariffDaoTest.before.csv")
    void testGetRegionGroupWithDeliveryService() {
        RegionGroupRow groupRow = deliveryTariffDao.getRegionGroupRow(REGION_GROUP_ID3);

        assertEquals("Тест цен", groupRow.getName());
        assertEquals(true, groupRow.getHasDeliveryService());
    }

    @Test
    @DbUnitDataSet(before = "DeliveryTariffDaoTest.before.csv")
    void testGetRegionUsages() {
        Map<Long, OwnedHierarchicalId<Long, Long>> usages = deliveryTariffDao.getRegionUsages(774);
        Map<Long, OwnedHierarchicalId<Long, Long>> expected = ImmutableMap.of(
                1L, new OwnedHierarchicalId<>(1L, null, REGION_GROUP_ID1),
                2L, new OwnedHierarchicalId<>(2L, null, REGION_GROUP_ID1),
                3L, new OwnedHierarchicalId<>(3L, null, REGION_GROUP_ID2),
                4L, new OwnedHierarchicalId<>(4L, null, REGION_GROUP_ID3),
                5L, new OwnedHierarchicalId<>(5L, null, REGION_GROUP_ID4)
        );
        HierarchyTestUtil.assertEquals(expected, usages);
    }

    @Test
    @DbUnitDataSet(before = "DeliveryTariffDaoTest.before.csv")
    void testGetPriceRules() {
        final List<DeliveryRule> rules = deliveryRuleService.getRules(REGION_GROUP_ID3);
        final List<DeliveryRule> expected = ImmutableList.of(
                new PriceRule(new DeliveryRuleId(REGION_GROUP_ID3, (short) 1), null, BigDecimal.valueOf(1000L)),
                new PriceRule(new DeliveryRuleId(REGION_GROUP_ID3, (short) 2), BigDecimal.valueOf(1000L), null)
        );
        assertEquals(expected, rules);
    }

    @Test
    @DbUnitDataSet(before = "DeliveryTariffDaoTest.before.csv")
    void testGetWeightRules() {
        final List<DeliveryRule> rules = deliveryRuleService.getRules(REGION_GROUP_ID4);
        final List<DeliveryRule> expected = ImmutableList.of(
                new WeightRule(new DeliveryRuleId(REGION_GROUP_ID4, (short) 1), null, 1),
                new WeightRule(new DeliveryRuleId(REGION_GROUP_ID4, (short) 2), 1, null)
        );
        assertEquals(expected, rules);
    }

    @Test
    @DbUnitDataSet(before = "DeliveryTariffDaoTest.before.csv")
    void testGetCategoryRules() {
        final List<DeliveryRule> rules = deliveryRuleService.getRules(REGION_GROUP_ID2);
        final List<DeliveryRule> expected = ImmutableList.of(
                new CategoryRule(new DeliveryRuleId(REGION_GROUP_ID2, (short) 1),
                        new TreeSet<>(set(new CategoryId("1", 1000L), new CategoryId("2", 1000L))))
        );
        assertEquals(expected, rules);
    }

    @Test
    @DbUnitDataSet(before = "DeliveryTariffDaoTest.before.csv")
    void testCreateRegionGroup() {
        int oldCountGroups = JdbcTestUtils.countRowsInTable(jdbcTemplate, "SHOPS_WEB.DELIVERY_REGION_GROUPS");
        RegionGroup group = DeliveryTariffTestHelper.createTestRegionGroup(null, 774, "Новая тестовая группа", false, TariffType.DEFAULT, true, null, null, 1L, false);
        deliveryTariffDao.createRegionGroup(group);
        assertNotNull(group.getId());
        int newCountGroups = JdbcTestUtils.countRowsInTable(jdbcTemplate, "SHOPS_WEB.DELIVERY_REGION_GROUPS");
        assertEquals(oldCountGroups + 1, newCountGroups);
        assertNotNull(group.getHasDeliveryService());
    }

    @Test
    @DbUnitDataSet(
            before = "DeliveryTariffDaoTest.testDelete.before.csv",
            after = "DeliveryTariffDaoTest.testDelete.after.csv"
    )
    void testDelete() {
        deliveryTariffDao.deleteRegionGroup(100);
    }

    @Test
    @DbUnitDataSet(
            before = "DeliveryTariffDaoTest.createRegionGroupNew.before.csv",
            after = "DeliveryTariffDaoTest.createRegionGroupNew.after.csv"
    )
    void createRegionGroupNew() {
        RegionGroup group1 = DeliveryTariffTestHelper.createTestRegionGroup(null, 774, "Новая тестовая группа1", false, TariffType.DEFAULT, false, null, null, 1L, false);
        RegionGroup group2 = DeliveryTariffTestHelper.createTestRegionGroup(null, 774, "Новая тестовая группа2", false, TariffType.DEFAULT, false, null, null, 1L, false);
        deliveryTariffDao.createRegionGroup(group1);
        deliveryTariffDao.createRegionGroup(group2);
    }

    @Test
    void testGetRegionUsagesZeroShop() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> deliveryTariffDao.getRegionUsages(0)
        );
    }

}
