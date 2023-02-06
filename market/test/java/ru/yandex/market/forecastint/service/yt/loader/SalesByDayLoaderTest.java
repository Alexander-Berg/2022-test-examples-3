package ru.yandex.market.forecastint.service.yt.loader;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.forecastint.utils.TestUtils;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class SalesByDayLoaderTest extends AbstractFunctionalTest {

    @Autowired
    private SalesByDayLoader salesByDayLoader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2021-12",
                    "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
                    "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_delivery",
                    "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
                    "//home/market/production/mstat/dictionaries/shop_real_supplier/latest"
            },
            csv = "SalesByDayLoaderTest.yql.csv",
            yqlMock = "SalesByDayLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(after = "SalesByDayLoaderTest_testLoading.after.csv")
    void test() {
        TestUtils.setMockedTimeServiceWithNowDateTime(salesByDayLoader,
                LocalDateTime.of(2021, 12, 29, 0, 0, 0));
        salesByDayLoader.load();
    }

}
