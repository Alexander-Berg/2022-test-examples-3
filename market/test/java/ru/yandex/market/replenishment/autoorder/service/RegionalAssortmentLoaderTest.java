package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.RegionalAssortmentLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class RegionalAssortmentLoaderTest extends FunctionalTest {

    @Autowired
    private RegionalAssortmentLoader regionalAssortmentLoader;

    @Test
    @DbUnitDataSet(
        before = "RegionalAssortmentLoaderTest_import.before.csv",
        after = "RegionalAssortmentLoaderTest_import.after.csv"
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            "//home/market/production/replenishment/order_planning/2021-06-11/intermediate/regional_assortment",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest"
        },
        csv = "RegionalAssortmentLoaderTest_import.yql.csv",
        yqlMock = "RegionalAssortmentLoaderTest.yql.mock"
    )
    public void importTest() {
        setTestTime(LocalDateTime.of(2021, 6, 11, 0, 0));
        regionalAssortmentLoader.load();
    }

}
