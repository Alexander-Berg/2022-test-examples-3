package ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_loaders;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.not_grouped_recommendations.NotGrouped3PLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class NotGrouped3PLoaderTest extends FunctionalTest {

    @Autowired
    NotGrouped3PLoader loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations_3p",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/warehouses",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/ss_region_reduced",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/manual_stock_model"
            },
            csv = "NotGrouped3PLoaderTest.yql.csv",
            yqlMock = "NotGrouped3PLoaderTest.yql.mock"
    )
    @DbUnitDataSet(before = "NotGrouped3PLoaderTest_import.before.csv",
            after = "NotGrouped3PLoaderTest_import.after.csv")
    public void importNotGrouped() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 0, 0));
        loader.load();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations_3p",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/warehouses",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/ss_region_reduced",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/manual_stock_model"
            },
            csv = "NotGrouped3PLoaderTest.yql.csv",
            yqlMock = "NotGrouped3PLoaderTest.yql.mock"
    )
    @DbUnitDataSet(before = "NotGrouped3PLoaderTest_expansion.before.csv",
            after = "NotGrouped3PLoaderTest_expansion.after.csv")
    public void testExpansion() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 0, 0));
        loader.load();
    }
}
