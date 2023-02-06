package ru.yandex.market.logistics.tarifficator.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.entity.shop.DeliveryRegionGroupEntity;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.CourierTariffType;
import ru.yandex.market.logistics.tarifficator.repository.shop.DeliveryRegionGroupRepository;
import ru.yandex.market.logistics.tarifficator.util.HierarchyTestUtil;
import ru.yandex.market.logistics.tarifficator.utils.hierarchy.OwnedHierarchicalId;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/repository/region-group/deliveryRegionGroupRepository.before.xml")
public class DeliveryRegionGroupRepositoryTest extends AbstractContextualTest {

    @Autowired
    private DeliveryRegionGroupRepository tested;

    @Test
    void testCount() {
        softly.assertThat(tested.countRegionGroups(774L))
            .isEqualTo(4);
    }

    @Test
    void testGetRegionGroups() {
        List<DeliveryRegionGroupEntity> groupRows = tested.getRegionGroupRows(Set.of(774L));
        softly.assertThat(groupRows).hasSize(4);
    }

    @Test
    void testGetRegionGroup() {
        softly.assertThat(tested.getRegionGroupRow(101))
            .hasValueSatisfying(value ->
                softly.assertThat(value).isEqualTo(
                    DeliveryRegionGroupEntity.builder()
                        .id(101L)
                        .shopId(774L)
                        .name("Тест")
                        .selfRegion(true)
                        .tariffType(CourierTariffType.DEFAULT)
                        .currency(Currency.RUR)
                        .modifiedBy(1L)
                        .notes("Bla-Bla-Bla")
                        .build()
                ));
    }

    @Test
    void testGetRegionGroupWithDeliveryService() {
        softly.assertThat(tested.getRegionGroupRow(103))
            .hasValueSatisfying(value ->
                softly.assertThat(value)
                    .extracting(DeliveryRegionGroupEntity::getName)
                    .isEqualTo("Тест цен")
            );
    }

    @Test
    void testGetRegionUsages() {
        Map<Long, OwnedHierarchicalId<Long, Long>> usages = tested.getRegionUsages(774);
        Map<Long, OwnedHierarchicalId<Long, Long>> expected = ImmutableMap.of(
            1L, new OwnedHierarchicalId<>(1L, null, 101L),
            2L, new OwnedHierarchicalId<>(2L, null, 101L),
            3L, new OwnedHierarchicalId<>(3L, null, 102L),
            4L, new OwnedHierarchicalId<>(4L, null, 103L),
            5L, new OwnedHierarchicalId<>(5L, null, 104L)
        );
        HierarchyTestUtil.assertEquals(expected, usages);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group/saveDeliveryRegionGroup.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateRegionGroup() {
        tested.createRegionGroup(DeliveryRegionGroupEntity.builder()
            .shopId(774L)
            .name("Созданная группа")
            .currency(Currency.RUR)
            .selfRegion(false)
            .tariffType(CourierTariffType.FROM_FEED)
            .notes("Update note")
            .build()
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group/saveDeliveryRegionGroupWithNullableFields.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateRegionGroupWithAllNulableFieldNull() {
        tested.createRegionGroup(DeliveryRegionGroupEntity.builder()
            .shopId(774L)
            .selfRegion(false)
            .currency(Currency.RUR)
            .tariffType(CourierTariffType.FROM_FEED)
            .build()
        );
    }

    @Test
    void testGetRegionGroupsForSeveralShops() {
        softly.assertThat(tested.getRegionGroupRows(Set.of(79620L, 774L)))
            .hasSize(5);
    }

    @Test
    void testGetSelfRegionGroup() {
        softly.assertThat(tested.getSelfRegionGroupRow(2L))
            .hasValueSatisfying(value ->
                softly.assertThat(value)
                    .isEqualTo(DeliveryRegionGroupEntity.builder()
                        .id(203L)
                        .shopId(2L)
                        .selfRegion(true)
                        .name("Группа своего региона")
                        .tariffType(CourierTariffType.WEIGHT)
                        .modifiedBy(1L)
                        .currency(Currency.RUR)
                        .build()
                    )
            );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group/deleteDeliveryRegionGroup.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDelete() {
        tested.deleteRegionGroups(Set.of(105L));
    }

    @Test
    void testGetRegionUsagesZeroShop() {
        softly.assertThatThrownBy(() -> tested.getRegionUsages(0))
            .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group/updateDeliveryRegionGroupFields.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateRegionGroup() {
        tested.updateRegionGroup(DeliveryRegionGroupEntity.builder()
            .currency(Currency.EUR)
            .name("New Name")
            .id(101L)
            .modifiedBy(123L)
            .build()
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group/updateDeliveryRegionGroupType.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateRegionGroupTariffType() {
        tested.updateRegionGroupTariffType(101L, CourierTariffType.CATEGORY_WEIGHT);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group/updateDeliveryRegionGroupName.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateRegionGroupName() {
        tested.updateRegionGroupName(101L, "Обновленное имя");
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group/deleteRegionsFromGroup.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeleteAllRegionsFromGroup() {
        tested.deleteAllRegionGroupRegions(101L);
    }

    @Test
    void testGetRegionsRegionGroup() {
        Map<Long, Long> actual = tested.getRegionsRegionGroup(774L, Set.of(1L, 2L, 3L, 4L, 6L));

        softly.assertThat(actual).isNotNull()
            .hasSize(4)
            .containsEntry(1L, 101L)
            .containsEntry(2L, 101L)
            .containsEntry(3L, 102L)
            .containsEntry(4L, 103L);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/region-group/addDeliveryRegionToGroup.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testAddRegionsToGroup() {
        tested.addRegions(101L, Set.of(1L, 3L, 4L, 6L));
    }

    @Test
    void testGetRegionsFromGroup() {
        softly.assertThat(tested.getRegions(101L))
            .hasSize(2)
            .contains(1L)
            .contains(2L);
    }
}
