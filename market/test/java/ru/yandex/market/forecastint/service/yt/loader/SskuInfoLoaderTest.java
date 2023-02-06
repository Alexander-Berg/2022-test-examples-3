package ru.yandex.market.forecastint.service.yt.loader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class SskuInfoLoaderTest extends AbstractFunctionalTest {

    @Autowired
    private SskuInfoLoader sskuInfoLoader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
                    "//home/market/production/mbo/stat/mboc_offers/latest",
                    "//home/market/production/mbo/mboc/offer-mapping",
                    "//home/market/production/deepmind/dictionaries/ssku_status/latest"
            },
            csv = "SskuInfoLoaderTest_testLoading.yql.csv",
            yqlMock = "SskuInfoLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(
            after = "SskuInfoLoaderTest_testLoading.after.csv",
            before = "SskuInfoLoaderTest_testLoading.before.csv"
    )
    public void testLoadingExists() {
        sskuInfoLoader.load();
    }
}
