package ru.yandex.market.deliverycalculator.storage.repository;

import java.util.Optional;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.WarehouseEntity;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Тест для {@link WarehouseRepository}.
 */
public class WarehouseRepositoryTest extends FunctionalTest {

    @Autowired
    private WarehouseRepository tested;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(after = "database/storeWarehouses.after.csv")
    public void testSave() {
        WarehouseEntity entity1 = WarehouseEntity.builder()
                .withId(1L)
                .withLmsId(11L)
                .withRegionId(213)
                .withTariffRegionId(212)
                .withActive(true)
                .withName("Warehouse 1")
                .build();
        WarehouseEntity entity2 = WarehouseEntity.builder()
                .withId(2L)
                .withLmsId(22L)
                .withRegionId(214)
                .withTariffRegionId(214)
                .withActive(false)
                .withName("Warehouse 2")
                .build();
        WarehouseEntity entity3 = WarehouseEntity.builder()
                .withId(3L)
                .withLmsId(33L)
                .withRegionId(214)
                .withTariffRegionId(214)
                .withActive(true)
                .build();

        tested.saveAllBatch(asList(entity1, entity2, entity3));
    }

    @Test
    @DbUnitDataSet(before = "database/storeWarehouses.after.csv")
    public void testGetById() {
        Optional<WarehouseEntity> entity = tested.findById(1L);

        assertTrue(entity.isPresent());
        assertEquals(WarehouseEntity.builder()
                .withId(1L)
                .withLmsId(11L)
                .withRegionId(213)
                .withTariffRegionId(212)
                .withActive(true)
                .withName("Warehouse 1")
                .build(), entity.get());
    }

    @Test
    @DbUnitDataSet(before = "database/storeWarehouses.after.csv")
    public void testFindExisting() {
        assertEquals(Sets.newHashSet(1L, 2L, 3L), tested.findExisting());
    }

    @Test
    @DbUnitDataSet(before = "database/deactivateWarehouses.before.csv",
            after = "database/deactivateWarehouses.after.csv")
    public void testDeactivate() {
        transactionTemplate.execute(status -> {
            tested.deactivateWhereIdsIn(asList(1L, 2L, 3L));
            return null;
        });
    }
}
