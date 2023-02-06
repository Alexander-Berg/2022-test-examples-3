package ru.yandex.market.replenishment.autoorder.service.recommendation_infos.region_info_loaders;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.region_info.RecommendationRegionSupplierInfoLoader3p;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class RecommendationRegionSupplierInfoLoader3pTest extends FunctionalTest {

    private static final LocalDateTime MOCK_DATE = LocalDateTime.of(2020, 5, 15, 8, 0);

    @Autowired
    RecommendationRegionSupplierInfoLoader3p loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/warehouses",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/transits",
                    "//home/market/production/replenishment/order_planning/2020-05-14/intermediate/suppliers_demand",
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest"
            },
            csv = "RecommendationRegionSupplierInfoLoader3pTest.yql.csv",
            yqlMock = "RecommendationRegionSupplierInfoLoader3pTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationRegionSupplierInfoLoader3pTest.before.csv",
            after = "RecommendationRegionSupplierInfoLoader3pTest.after.csv")
    public void importRecommendationRegionSupplierInfoLoader3pTest() {
        setTestTime(MOCK_DATE);
        loader.load();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/warehouses",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/transits",
                    "//home/market/production/replenishment/order_planning/2020-05-14/intermediate/suppliers_demand",
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest"
            },
            csv = "RecommendationRegionSupplierInfoLoader3pTest.yql.csv",
            yqlMock = "RecommendationRegionSupplierInfoLoader3pTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationRegionSupplierInfoLoader3pTest_expansion.before.csv",
            after = "RecommendationRegionSupplierInfoLoader3pTest_expansion.after.csv")
    public void testExpansion() {
        setTestTime(MOCK_DATE);
        loader.load();
    }
}
