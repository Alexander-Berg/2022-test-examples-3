package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.FulfillmentShopRequestIncrementalLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
public class FulfillmentShopRequestIncrementalLoaderTest extends FunctionalTest {

    @Autowired
    private FulfillmentShopRequestIncrementalLoader fulfillmentShopRequestIncrementalLoader;

    @Test
    @DbUnitDataSet(
            before = "FulfilmentShopRequestIncrementalLoaderTest_testLoadIsOk.before.csv",
            after = "FulfilmentShopRequestIncrementalLoaderTest_testLoadIsOk.after.csv"
    )
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1h/latest",
                    "//home/market/production/mstat/dictionaries/fulfillment_request_item/1h/latest"
            },
            csv = "FulfilmentShopRequestIncrementalLoaderTest_testLoadIsOk.yql.csv",
            yqlMock = "FulfilmentShopRequestIncrementalLoaderTest.yql.mock"
    )
    public void testLoadIsOk() {
        fulfillmentShopRequestIncrementalLoader.load();
    }
}
