package ru.yandex.market.replenishment.autoorder.service.recommendation_infos.country_info_loaders;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.country_info.RecommendationCountryInfoLoader1p;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class RecommendationCountryInfoLoader1pTest extends FunctionalTest {

    @Autowired
    RecommendationCountryInfoLoader1p loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2020-04-30/outputs/simulation",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/forecast_region",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
            },
            csv = "RecommendationCountryInfoLoader1pTest.yql.csv",
            yqlMock = "RecommendationCountryInfoLoader1pTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationCountryInfoLoader1pTest.before.csv",
            after = "RecommendationCountryInfoLoader1pTest.after.csv")
    public void importRecommendationCountryInfoLoaderTest() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 8, 0));
        loader.load();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2020-04-30/outputs/simulation",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/forecast_region",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
            },
            csv = "RecommendationCountryInfoLoader1pTest.yql.csv",
            yqlMock = "RecommendationCountryInfoLoader1pTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationCountryInfoLoader1pTest_expansion.before.csv",
            after = "RecommendationCountryInfoLoader1pTest_expansion.after.csv")
    public void testExpansion() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 8, 0));
        loader.load();
    }
}
