package ru.yandex.market.replenishment.autoorder.service.recommendation_infos.wh_info_loaders;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.wh_info.RecommendationWarehouseInfoLoader3p;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class RecommendationWarehouseInfoLoader3pTest extends FunctionalTest {

    @Autowired
    RecommendationWarehouseInfoLoader3p loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/transits",
            },
            csv = "RecommendationWarehouseInfoLoader3pTest.yql.csv",
            yqlMock = "RecommendationWarehouseInfoLoader3pTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationWarehouseInfoLoader3pTest.before.csv",
            after = "RecommendationWarehouseInfoLoader3pTest.after.csv")
    public void importRecommendationWarehouseInfoLoader3pTest() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 8, 0));
        loader.load();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/transits",
            },
            csv = "RecommendationWarehouseInfoLoader3pTest.yql.csv",
            yqlMock = "RecommendationWarehouseInfoLoader3pTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationWarehouseInfoLoader3pTest_expansion.before.csv",
            after = "RecommendationWarehouseInfoLoader3pTest_expansion.after.csv")
    public void testExpansion() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 8, 0));
        loader.load();
    }
}
