package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.WarehouseLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class WarehouseLoaderTest extends FunctionalTest {

    @Autowired
    WarehouseLoader warehouseLoader;

    @Test
    @DbUnitDataSet(after = "WarehouseLoaderTest_importWarehouses.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/latest/intermediate/warehouses",
                    "//home/market/production/replenishment/order_planning/latest/intermediate/regions"
            },
            csv = "WarehouseLoaderTest_importWarehouses.yql.csv",
            yqlMock = "WarehouseLoaderTest.yql.mock"
    )

    public void importWarehouses() {
        warehouseLoader.load();
    }

    @Test
    @DbUnitDataSet(
            after = "WarehouseLoaderTest_importWarehousesIfExist.after.csv",
            before = "WarehouseLoaderTest_importWarehousesIfExist.before.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/latest/intermediate/warehouses",
                    "//home/market/production/replenishment/order_planning/latest/intermediate/regions"
            },
            csv = "WarehouseLoaderTest_importWarehouses.yql.csv",
            yqlMock = "WarehouseLoaderTest.yql.mock"
    )

    public void importWarehousesIfAlreadyExist() {
        warehouseLoader.load();
    }
}
