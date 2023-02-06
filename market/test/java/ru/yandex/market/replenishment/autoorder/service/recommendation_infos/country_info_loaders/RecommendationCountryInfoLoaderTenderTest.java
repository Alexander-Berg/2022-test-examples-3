package ru.yandex.market.replenishment.autoorder.service.recommendation_infos.country_info_loaders;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.country_info.RecommendationCountryInfoLoaderTender;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class RecommendationCountryInfoLoaderTenderTest extends FunctionalTest {

    @Autowired
    RecommendationCountryInfoLoaderTender loader;

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
            csv = "RecommendationCountryInfoLoaderTenderTest.yql.csv",
            yqlMock = "RecommendationCountryInfoLoaderTenderTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationCountryInfoLoaderTenderTest.before.csv",
            after = "RecommendationCountryInfoLoaderTenderTest.after.csv")
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
            csv = "RecommendationCountryInfoLoaderTenderTest.yql.csv",
            yqlMock = "RecommendationCountryInfoLoaderTenderTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationCountryInfoLoaderTenderTest_expansion.before.csv",
            after = "RecommendationCountryInfoLoaderTenderTest_expansion.after.csv")
    public void testExpansion() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 8, 0));
        loader.load();
    }
}
