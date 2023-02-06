package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SkuPriceLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.setMockedYtTableExistsCheckServiceAlwaysTrue;
public class SkuPriceLoaderTest extends FunctionalTest {
    private static final LocalDate MOCKED_DATE = LocalDate.of(2021, 6, 15);

    @Autowired
    SkuPriceLoader skuPriceLoader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
                    "//home/market/production/mstat/analyst/market-offers/2021-06-14",
                    "//home/market/production/replenishment/order_planning/2021-06-15/outputs/recommendations_3p",
                    "//home/market/production/replenishment/order_planning/2021-06-15/intermediate" +
                            "/assortment_prices_dicts",
                    "//home/market/production/replenishment/prod/sales_dynamics/calculation_steps/beru_offers" +
                            "/beru_offers_2021-06-15"
            },
            csv = "SkuPriceLoaderTest.yql.csv",
            yqlMock = "SkuPriceLoaderTest.yql.mock"
    )
    @DbUnitDataSet(
            after = "SkuPriceLoaderTest.after.csv"
    )
    public void testLoading() {
        setMockedYtTableExistsCheckServiceAlwaysTrue(skuPriceLoader);
        setTestTime(MOCKED_DATE.atTime(12, 30));
        skuPriceLoader.load();
    }

}
