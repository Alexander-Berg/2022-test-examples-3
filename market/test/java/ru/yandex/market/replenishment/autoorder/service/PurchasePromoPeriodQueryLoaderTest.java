package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.PurchasePromoPeriodQueryLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
public class PurchasePromoPeriodQueryLoaderTest extends FunctionalTest {

    @Autowired
    PurchasePromoPeriodQueryLoader purchasePromoPeriodQueryLoader;

    @Test
    @DbUnitDataSet(before = "PurchasePromoPeriodQueryLoaderTest.before.csv",
            after = "PurchasePromoPeriodQueryLoaderTest.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/axapta_supplier_prices/latest"
            },
            csv = "PurchasePromoPeriodQueryLoaderTest.yql.csv",
            yqlMock = "PurchasePromoPeriodQueryLoaderTest.yql.mock"
    )
    public void testLoading() {
        purchasePromoPeriodQueryLoader.load();
    }
}
