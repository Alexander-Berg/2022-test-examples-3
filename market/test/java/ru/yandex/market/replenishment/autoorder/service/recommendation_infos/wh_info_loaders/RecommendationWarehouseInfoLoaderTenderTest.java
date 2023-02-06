package ru.yandex.market.replenishment.autoorder.service.recommendation_infos.wh_info_loaders;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.wh_info.RecommendationWarehouseInfoLoaderTender;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class RecommendationWarehouseInfoLoaderTenderTest extends FunctionalTest {

    @Autowired
    RecommendationWarehouseInfoLoaderTender loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/transits",
            },
            csv = "RecommendationWarehouseInfoLoaderTenderTest.yql.csv",
            yqlMock = "RecommendationWarehouseInfoLoaderTenderTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationWarehouseInfoLoaderTenderTest.before.csv",
            after = "RecommendationWarehouseInfoLoaderTenderTest.after.csv")
    public void importRecommendationWarehouseInfoLoaderTenderTest() {
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
            csv = "RecommendationWarehouseInfoLoaderTenderTest.yql.csv",
            yqlMock = "RecommendationWarehouseInfoLoaderTenderTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationWarehouseInfoLoaderTenderTest_expansion.before.csv",
            after = "RecommendationWarehouseInfoLoaderTenderTest_expansion.after.csv")
    public void testExpansion() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 8, 0));
        loader.load();
    }
}
