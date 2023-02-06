package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.CompetitorPriceLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.setMockedYtTableExistsCheckService;
@ActiveProfiles("unittest")
public class CompetitorPriceLoaderTest extends FunctionalTest {
    @Autowired
    CompetitorPriceLoader loader;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test/dco_upload_table" +
                            "/2021-06-18T08--colon--00--colon--00"
            },
            csv = "CompetitorPriceLoaderTest_import.yql.csv",
            yqlMock = "CompetitorPriceLoaderTest_import.yql.mock.json"
    )
    @DbUnitDataSet(after = "CompetitorPriceLoaderTest.after.csv")
    public void importTest() {
        setMockedObjects(true);
        loader.load();
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test/dco_upload_table" +
                            "/2021-06-18T04--colon--00--colon--00",
                    "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test/dco_upload_table" +
                            "/2021-06-18T08--colon--00--colon--00"
            },
            csv = "CompetitorPriceLoaderTest_tableNotExists.yql.csv",
            yqlMock = "CompetitorPriceLoaderTest_tableNotExists.yql.mock.json"
    )
    @DbUnitDataSet(after = "CompetitorPriceLoaderTest.after.csv")
    public void whenTableNotExistUsePrevious() {
        setMockedObjects(false);
        loader.load();
    }

    private void setMockedObjects(boolean currentPathExists) {
        setExistYtTablesChecker(currentPathExists);
        setTestTime(LocalDateTime.of(2021, 6, 18, 10, 0));
    }

    private void setExistYtTablesChecker(boolean currentPathExists) {
        var path = "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test/dco_upload_table" +
                "/2021-06-18T08:00:00";
        var pathBefore = "//home/market/production/mstat/analyst/regular/mbi/blue_prices/test" +
                "/dco_upload_table/2021-06-18T04:00:00";
        setMockedYtTableExistsCheckService(loader,
                Map.of(path, currentPathExists,
                        pathBefore, true));


//        var yt = mock(Yt.class);
//        var cypress = mock(Cypress.class);
//        var ytFactory = Mockito.mock(YtFactory.class);
//        when(ytFactory.getYt(YtCluster.HAHN)).thenReturn(yt);
//        when(yt.cypress()).thenReturn(cypress);
//        when(cypress.exists(YPath.simple(path))).thenReturn(currentPathExists);
//        when(cypress.exists(YPath.simple(pathBefore))).thenReturn(true);
//        ReflectionTestUtils.setField(loader, "ytFactory", ytFactory);

    }

}
