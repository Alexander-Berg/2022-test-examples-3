package ru.yandex.market.forecastint.service.yt.loader;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static ru.yandex.market.forecastint.utils.TestUtils.setMockedTimeServiceWithNowDateTime;
import static ru.yandex.market.forecastint.utils.TestUtils.setMockedYtTableExistsCheckService;

public class SskuPriceLoaderTest extends AbstractFunctionalTest {

    @Autowired
    private SskuPriceLoader loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/replenishment/order_planning/2022-01-29/intermediate" +
                            "/assortment_prices_dicts",
                    "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
                    "//home/market/production/replenishment/prod/sales_dynamics/calculation_steps/beru_offers" +
                            "/beru_offers_2022-01-29"
            },
            csv = "SskuPriceLoaderTest_testLoading.yql.csv",
            yqlMock = "SskuPriceLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(
            after = "SskuPriceLoaderTest_testLoading.after.csv",
            before = "SskuPriceLoaderTest_testLoading.before.csv"
    )
    public void test() {
        setMockedTimeServiceWithNowDateTime(loader,
                LocalDateTime.of(2022, 1, 29, 5, 22));
        setMockedYtTableExistsCheckService(loader,
                Map.of("//home/market/production/replenishment/order_planning/2022-01-29/intermediate" +
                                "/assortment_prices_dicts", true,
                        "//home/market/production/replenishment/prod/sales_dynamics/calculation_steps/beru_offers" +
                                "/beru_offers_2022-01-29", true));
        loader.load();
    }
}
