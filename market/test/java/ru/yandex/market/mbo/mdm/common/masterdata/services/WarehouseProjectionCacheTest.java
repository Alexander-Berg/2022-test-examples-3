package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.BmdmWarehouseWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.WarehouseProjectionRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

public class WarehouseProjectionCacheTest extends MdmBaseDbTestClass {

    @Autowired
    WarehouseProjectionRepository warehouseProjectionRepository;

    private WarehouseProjectionCache cache;

    @Before
    public void setUp() {
        cache = new WarehouseProjectionCacheImpl(warehouseProjectionRepository);
        warehouseProjectionRepository.deleteAll();
    }

    @Test
    public void testCacheUpdatedCorrectly() {
        var result = cache.getAll();
        Assertions.assertThat(result).hasSize(0);

        var warehouse1 = new BmdmWarehouseWrapper(1L, 2L, "a");
        var warehouse2 = new BmdmWarehouseWrapper(2L, 3L, "b");
        warehouseProjectionRepository.insertOrUpdateAll(List.of(warehouse1, warehouse2));
        cache.refresh();
        result = cache.getAll();
        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).containsExactlyInAnyOrder(warehouse1, warehouse2);
    }

}
