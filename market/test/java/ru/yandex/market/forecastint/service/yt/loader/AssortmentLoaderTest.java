package ru.yandex.market.forecastint.service.yt.loader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class AssortmentLoaderTest extends AbstractFunctionalTest {

    @Autowired
    private AssortmentLoader assortmentLoader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
                    "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
                    "//home/market/production/mbo/export/recent/models/sku"
            },
            csv = "AssortmentLoaderTest_testLoading.yql.csv",
            yqlMock = "AssortmentLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(
            after = "AssortmentLoaderTest_testLoading.after.csv",
            before = "AssortmentLoaderTest_testLoading.before.csv"
    )
    public void testLoading() {
        assortmentLoader.load();
    }
}
