package ru.yandex.market.deliverycalculator.storage.repository;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.PreferableTariffRegionEntity;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@DbUnitDataSet
public class PreferableTariffRegionRepositoryTest extends FunctionalTest {

    @Autowired
    private PreferableTariffRegionRepository tested;

    @Test
    @DbUnitDataSet(after = "database/storePreferableRegions.after.csv")
    void testSaveAll() {
        tested.saveAllBatch(createTestData());
    }

    @Test
    @DbUnitDataSet(before = "database/storePreferableRegions.after.csv")
    void testFindAll() {
        assertEquals(createTestData(), tested.findAll());
    }

    private List<PreferableTariffRegionEntity> createTestData() {
        PreferableTariffRegionEntity entity1 = PreferableTariffRegionEntity.builder()
                .withRegionId(1)
                .withTariffRegionId(2)
                .build();
        PreferableTariffRegionEntity entity2 = PreferableTariffRegionEntity.builder()
                .withRegionId(3)
                .withTariffRegionId(4)
                .build();

        return asList(entity1, entity2);
    }
}
