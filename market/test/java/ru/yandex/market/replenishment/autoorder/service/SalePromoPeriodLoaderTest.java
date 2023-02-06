package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SalePromoPeriodLoader;
import ru.yandex.market.replenishment.autoorder.utils.TableNamesTestQueryService;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class SalePromoPeriodLoaderTest extends FunctionalTest {

    @Autowired
    private SalePromoPeriodLoader loader;

    @Autowired
    TableNamesTestQueryService tableNamesTestQueryService;

    @Before
    public void setUp() {
        tableNamesTestQueryService.setNotUseLocalTables(true);
    }
    @After
    public void cleanUp() {
        tableNamesTestQueryService.setNotUseLocalTables(false);
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            "//home/market/production/mstat/dictionaries/pricing_mgmt/promohack/hack_ssku_promo_for_analytics_view/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/mstat/dictionaries/mbo/mboc_logistics_params/latest",
            "//home/market/production/replenishment/order_planning/latest/intermediate/warehouses"
        },
        csv = "SalePromoPeriodLoaderTest_import.yql.csv",
        yqlMock = "SalePromoPeriodLoaderTest.yql.mock"
    )
    @DbUnitDataSet(after = "SalePromoPeriodLoaderTest.after.csv")
    public void testLoading() {
        setTestTime(LocalDateTime.of(2022, 6, 9, 0, 0));
        loader.load();
    }
}
