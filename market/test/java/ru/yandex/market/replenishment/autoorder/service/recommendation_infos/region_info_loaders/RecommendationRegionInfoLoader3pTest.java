package ru.yandex.market.replenishment.autoorder.service.recommendation_infos.region_info_loaders;

import java.time.LocalDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.YtTableService;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.region_info.RecommendationRegionInfoLoader3p;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.setMockedYtTableExistsCheckServiceAlwaysTrue;

@ActiveProfiles("unittest")
public class RecommendationRegionInfoLoader3pTest extends FunctionalTest {

    private static final LocalDateTime MOCK_DATE = LocalDateTime.of(2020, 5, 15, 8, 0);

    @Autowired
    RecommendationRegionInfoLoader3p loader;

    @Autowired
    YtTableService ytTableService;

    @Before
    public void setUp() {
        setMockedYtTableExistsCheckServiceAlwaysTrue(loader);
    }

    @After
    public void cleanUp() {
        ReflectionTestUtils.setField(loader, "ytTableService", ytTableService);
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/testing/replenishment/autoorder/import_preparation/actual_fit/2020-05-15",
                    "//home/market/testing/replenishment/autoorder/import_preparation/forecast_oos/2020-05-15",
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
                    "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_stock_flattened/2020-05",
                    "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
                    "//home/market/production/replenishment/order_planning/2020-04-30/outputs/simulation",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/forecast_region",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/warehouses",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/transits",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers_demand"
            },
            csv = "RecommendationRegionInfoLoader3pTest.yql.csv",
            yqlMock = "RecommendationRegionInfoLoader3pTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationRegionInfoLoader3pTest_before_commit.csv",
            after = "RecommendationRegionInfoLoader3pTest_after_commit.csv")
    public void importRecommendationRegionInfoLoader3pTest_WillCommitTran() {
        setTestTime(MOCK_DATE);
        loader.load();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/testing/replenishment/autoorder/import_preparation/actual_fit/2020-05-15",
                    "//home/market/testing/replenishment/autoorder/import_preparation/forecast_oos/2020-05-15",
                    "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
                    "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_stock_flattened/2020-05",
                    "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
                    "//home/market/production/replenishment/order_planning/2020-04-30/outputs/simulation",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/forecast_region",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/warehouses",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
                    "//home/market/production/replenishment/order_planning/2020-05-15/outputs/transits",
                    "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers_demand"
            },
            csv = "RecommendationRegionInfoLoader3pTest.yql.csv",
            yqlMock = "RecommendationRegionInfoLoader3pTest.yql.mock"
    )
    @DbUnitDataSet(before = "RecommendationRegionInfoLoader3pTest_expansion_before_commit.csv",
            after = "RecommendationRegionInfoLoader3pTest_expansion_after_commit.csv")
    public void testExpansion_WillCommitTran() {
        setTestTime(MOCK_DATE);
        loader.load();
    }
}
