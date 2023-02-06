package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.DailyWarehouseLimitsLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
public class DailyWarehouseLimitsLoaderTest extends FunctionalTest {
    @Autowired
    DailyWarehouseLimitsLoader loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2021-08-17/intermediate/delivery_limit_groups"
            },
            csv = "DailyWarehouseLimitsLoaderTest_testLoading.yql.csv",
            yqlMock = "DailyWarehouseLimitsLoaderTest_testLoading.yql.mock"
    )
    @DbUnitDataSet(before = "DailyWarehouseLimitsLoaderTest_testLoading.before.csv",
            after = "DailyWarehouseLimitsLoaderTest_testLoading.after.csv")
    public void testLoading() {
        setTestTime(LocalDateTime.of(2021, 8, 17, 0, 0));
        loader.load();
    }
}
