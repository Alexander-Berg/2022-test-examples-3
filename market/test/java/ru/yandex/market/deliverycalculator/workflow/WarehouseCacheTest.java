package ru.yandex.market.deliverycalculator.workflow;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WarehouseCacheTest extends FunctionalTest {

    @Autowired
    private WarehouseCache tested;

    @Test
    @DbUnitDataSet(before = "warehouses.csv")
    void testCorrectCacheLoad() {
        assertEquals(of(10776), tested.getWarehouseRegion(1L));
        assertEquals(of(52), tested.getWarehouseRegion(2L));
        assertEquals(of(1), tested.getWarehouseRegion(3L));
        assertEquals(Optional.empty(), tested.getWarehouseRegion(5L));
        assertEquals(Optional.empty(), tested.getWarehouseRegion(6L));
    }

}
