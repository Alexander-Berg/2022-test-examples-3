package ru.yandex.market.logistics.tarifficator.service.shop;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PartnerPlacementProgramType;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryRegionGroup;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/service/shop/deliveryTariffService.before.xml")
public class DeliveryTariffServiceTest extends AbstractContextualTest {

    @Autowired
    private DeliveryTariffService tested;

    @Test
    @ExpectedDatabase(
        value = "/service/shop/createGroup1.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreationWhenRegionExistInSameGroup() {
        softly.assertThat(callCreateGroup(Set.of(213L, 1L)))
            .isNotNull();
    }

    @Test
    @DatabaseSetup("/service/shop/createGroup2.before.xml")
    @ExpectedDatabase(
        value = "/service/shop/createGroup2.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreationWhenRegionsParentExistInOtherGroup() {
        softly.assertThat(callCreateGroup(Set.of(225L, 213L)))
            .isNotNull();
    }

    @Test
    @DatabaseSetup("/service/shop/createGroup3.before.xml")
    @ExpectedDatabase(
        value = "/service/shop/createGroup3.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreationWhenRegionExistInOtherGroup() {
        softly.assertThat(callCreateGroup(Set.of(3L)))
            .isNotNull();
    }

    @Test
    @DatabaseSetup("/service/shop/deleteGroup.before.xml")
    @ExpectedDatabase(
        value = "/service/shop/deleteGroup.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeleteRegionGroup() {
        tested.deleteRegionGroups(774L, Set.of(300L, 400L), 1L);
    }

    @Test
    @DisplayName("Создание новой локальной региональной группы")
    @ExpectedDatabase(
        value = "/service/shop/updateTariffsNoSelfRegionGroup.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateTariffsNoSelfRegionGroup() {
        tested.updateTariffsOnCurrencyOrLocalRegionUpdate(
            774L,
            100L,
            Currency.EUR,
            Set.of(PartnerPlacementProgramType.DROPSHIP),
            213L
        );
    }

    @Test
    @DisplayName("Обновление существующей локальной региональной группы")
    @DatabaseSetup("/service/shop/updateTariffsSelfRegionGroupUpdate.before.xml")
    @ExpectedDatabase(
        value = "/service/shop/updateTariffsSelfRegionGroupUpdate.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateTariffsSelfRegionGroupUpdate() {
        tested.updateTariffsOnCurrencyOrLocalRegionUpdate(
            774L,
            1L,
            Currency.USD,
            Set.of(PartnerPlacementProgramType.DROPSHIP),
            197L
        );
    }

    @Test
    @DisplayName("Проверка, что группы и их тарифы обновляются")
    @DatabaseSetup("/service/shop/testUpdateSelfRegionGroupAndOthers.before.xml")
    @ExpectedDatabase(
        value = "/service/shop/testUpdateSelfRegionGroupAndOthers.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/shop/deliveryOptions.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateSelfRegionGroupAndOthers() {
        tested.updateTariffsOnCurrencyOrLocalRegionUpdate(
            774L,
            1L,
            Currency.EUR,
            Set.of(PartnerPlacementProgramType.DROPSHIP),
            42L
        );
    }

    @Test
    @DisplayName("Валюта осталась той же самой, но обновился локальный регион собственной группы")
    @DatabaseSetup("/service/shop/testUpdateTariffsSameCurrencyDifferentRegion.before.xml")
    @ExpectedDatabase(
        value = "/service/shop/testUpdateTariffsSameCurrencyDifferentRegion.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateTariffsSameCurrencyDifferentRegion() {
        tested.updateTariffsOnCurrencyOrLocalRegionUpdate(
            774L,
            1L,
            Currency.RUR,
            Set.of(PartnerPlacementProgramType.DROPSHIP),
            42L
        );
    }

    @Test
    @DisplayName("Расчет выколотых регионов")
    @DatabaseSetup("/controller/shop/region-group/db/getAllRegionGroups.before.xml")
    void testCorrectExcludedCalculation() {
        List<DeliveryRegionGroup> regionGroups = tested.getRegionGroups(774L);

        softly.assertThat(regionGroups)
            .isNotEmpty()
            .hasSize(3);

        softly.assertThat(regionGroups)
            .element(0)
            .extracting(DeliveryRegionGroup::getExcludes)
            .isEqualTo(Set.of(2L));
        softly.assertThat(regionGroups)
            .element(1)
            .extracting(DeliveryRegionGroup::getExcludes)
            .isEqualTo(new HashSet<>());
        softly.assertThat(regionGroups)
            .element(2)
            .extracting(DeliveryRegionGroup::getExcludes)
            .isEqualTo(Set.of(20298L, 20295L));
    }

    private long callCreateGroup(Set<Long> regions) {
        return tested.createRegionGroup(
            774L,
            "Группа 1",
            regions,
            false,
            Currency.EUR,
            Set.of(PartnerPlacementProgramType.DROPSHIP),
            100L
        );
    }
}
