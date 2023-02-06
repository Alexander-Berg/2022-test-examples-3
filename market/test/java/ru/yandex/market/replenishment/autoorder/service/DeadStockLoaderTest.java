package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.DeadStockLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
public class DeadStockLoaderTest extends FunctionalTest {

    @Autowired
    DeadStockLoader loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/monetize/dynamic_pricing/deadstock_sales/deadstock_status/latest",
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest"
            },
            csv = "DeadStockLoaderTest_doImport.yql.csv",
            yqlMock = "DeadStockLoaderTest.yql.mock"
    )
    @DbUnitDataSet(before = "DeadStockLoaderTest_doImport.before.csv",
            after = "DeadStockLoaderTest_doImport.after.csv")
    public void doImport() {
        loader.load();
    }
}
