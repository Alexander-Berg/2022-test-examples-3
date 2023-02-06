package ru.yandex.market.billing.imports.warehouse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;

public class WarehouseInfoDaoTest extends FunctionalTest {

    @Autowired
    private WarehouseInfoDao warehouseInfoDao;

    @DbUnitDataSet(before = "csv/WarehouseInfoDaoTest.GeoLocations.before.csv")
    @Test
    void getWarehousesGeoLocations() {
        Map<Long, Long> warehouseIdToRegionIdMap = warehouseInfoDao.getWarehousesGeoLocations(List.of(172L, 145L, 1L));
        assertThat(warehouseIdToRegionIdMap).hasSize(2);
        assertThat(warehouseIdToRegionIdMap.get(172L)).isEqualTo(213L);
        assertThat(warehouseIdToRegionIdMap.get(145L)).isEqualTo(21651L);
    }
}
