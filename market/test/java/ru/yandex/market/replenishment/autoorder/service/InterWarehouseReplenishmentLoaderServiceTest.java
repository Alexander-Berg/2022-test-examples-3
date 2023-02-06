package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.InterWarehouseReplenishmentQueryLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;
@ActiveProfiles("unittest")
public class InterWarehouseReplenishmentLoaderServiceTest extends FunctionalTest {

    @Autowired
    private InterWarehouseReplenishmentQueryLoader loader;

    @Autowired
    private BeanFactory beanFactory;

    @Test
    @DbUnitDataSet(
            before = "InterWarehouseReplenishmentLoaderServiceTest_load.before.csv",
            after = "InterWarehouseReplenishmentLoaderServiceTest_load.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2021-06-01/intermediate/inter_wh_movements",
                    "//home/market/production/replenishment/order_planning/2021-06-01/intermediate/warehouses",
                    "//home/market/production/replenishment/order_planning/2021-06-01/outputs/transits",
                    "//home/market/production/replenishment/order_planning/2021-06-01/intermediate/forecast_region",
                    "//home/market/production/mstat/dictionaries/stock_sku/1d/latest",
                    "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
                    "//home/market/production/analytics/business/fulfillment/goods_to_sale/prod/latest",
                    "//home/market/production/mstat/analyst/const/hid_to_category_director/latest",
                    "//home/market/production/replenishment/order_planning/2021-06-01/intermediate/ss_region"
            },
            csv = "InterWarehouseReplenishmentLoaderServiceTest_import.yql.csv",
            yqlMock = "InterWarehouseReplenishmentLoaderServiceTest.yql.mock"
    )
    public void testLoading() {
        setTestTime(LocalDateTime.of(2021, 6, 1, 0, 0));
        final InterWarehouseReplenishmentLoaderService loaderService =
                beanFactory.getBean(InterWarehouseReplenishmentLoaderService.class, loader);
        loaderService.load();
    }

}
