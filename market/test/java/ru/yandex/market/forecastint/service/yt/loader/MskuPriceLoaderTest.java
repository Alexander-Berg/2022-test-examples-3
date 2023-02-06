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

public class MskuPriceLoaderTest extends AbstractFunctionalTest {

    @Autowired
    private MskuPriceLoader loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test/dco_upload_table/2022-01" +
                            "-09T04--colon--00--colon--00",
            },
            csv = "MskuPriceLoaderTest_testLoading.yql.csv",
            yqlMock = "MskuPriceLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(
            after = "MskuPriceLoaderTest_testLoading.after.csv",
            before = "MskuPriceLoaderTest_testLoading.before.csv"
    )
    public void testLoadingExists() {
        setExistYtTablesChecker(true);
        setMockedTimeServiceWithNowDateTime(loader,
                LocalDateTime.of(2022, 1, 9, 5, 22));
        loader.load();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test/dco_upload_table/2022-01" +
                            "-09T00--colon--00--colon--00",
            },
            csv = "MskuPriceLoaderTest_testLoading.yql.csv",
            yqlMock = "MskuPriceLoaderTest_testLoadingNotExists.yql.mock.json"
    )
    @DbUnitDataSet(
            after = "MskuPriceLoaderTest_testLoadingNotExists.after.csv",
            before = "MskuPriceLoaderTest_testLoading.before.csv"
    )
    public void testLoadingNotExists() {
        setExistYtTablesChecker(false);
        setMockedTimeServiceWithNowDateTime(loader,
                LocalDateTime.of(2022, 1, 9, 5, 22));
        loader.load();
    }

    private void setExistYtTablesChecker(boolean currentPathExists) {
        var path = "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test/dco_upload_table" +
                "/2022-01-09T04:00:00";
        setMockedYtTableExistsCheckService(loader,
                Map.of(path, currentPathExists));
    }

}
