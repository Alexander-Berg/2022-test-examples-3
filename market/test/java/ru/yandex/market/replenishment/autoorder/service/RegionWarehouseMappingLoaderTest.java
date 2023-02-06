package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.RegionWarehouseMappingLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class RegionWarehouseMappingLoaderTest extends FunctionalTest {

    @Autowired
    RegionWarehouseMappingLoader mappingLoader;

    @Test
    @DbUnitDataSet(
            before = "RegionWarehouseMappingLoaderTest_importRegionToWarehouse.before.csv",
            after = ""
    )
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/analytics/business/operations/replenishment/warehouse_to_regions/latest"
            },
            csv = "RegionWarehouseMappingLoaderTest_importRegionToWarehouse.yql.csv",
            yqlMock = "RegionWarehouseMappingLoaderTest.yql.mock"
    )
    public void importRegionToWarehouse() {
        mappingLoader.load();
    }
}
