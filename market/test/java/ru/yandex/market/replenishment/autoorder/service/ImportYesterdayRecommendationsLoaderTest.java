package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.ImportYesterdayRecommendationsLoader;

import static org.mockito.Mockito.verify;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.setMockedYtTableExistsCheckService;

@MockBean(AlertsService.class)
public class ImportYesterdayRecommendationsLoaderTest extends FunctionalTest {

    private static final String TABLE_PREFIX = "//home/market/production/replenishment/order_planning/";
    private static final String STOCK_TABLE = "/intermediate/stock_alpaca";
    private static final String STOCKS_WITH_LIFETIMES_TABLE = "/intermediate/stock_with_lifetime";
    private static final String SUPPLIERS_TABLE = "/intermediate/suppliers";
    private static final String RAW_TRANSITS_TABLE = "/intermediate/transits_raw";
    private static final String TRANSIT_TABLE = "/outputs/transits";

    @Autowired
    private ImportYesterdayRecommendationsLoader loader;
    @Autowired
    private AlertsService alertsService;

    @Test
    @DbUnitDataSet(
        before = "ImportYesterdayRecommendationsLoaderTest_reimportSuccess.before.csv",
        after = "ImportYesterdayRecommendationsLoaderTest_reimportSuccess.after.csv")
    public void testReimportSuccess() {
        setTestTime(LocalDateTime.of(2021, 12, 1, 7, 45));
        final LocalDate now = LocalDate.of(2021, 12, 1);
        setMockedYtTableExistsCheckService(loader,
            Map.of(
                makeTablePath(now, STOCK_TABLE), true,
                makeTablePath(now, STOCKS_WITH_LIFETIMES_TABLE), true,
                makeTablePath(now, SUPPLIERS_TABLE), true,
                makeTablePath(now, TRANSIT_TABLE), true,
                makeTablePath(now, RAW_TRANSITS_TABLE), true
            ));
        loader.load();
    }

    @Test
    @DbUnitDataSet(
        before = "ImportYesterdayRecommendationsLoaderTest_notInInterval.before.csv",
        after = "ImportYesterdayRecommendationsLoaderTest_notInInterval.after.csv")
    public void testNotInInterval() {
        setTestTime(LocalDateTime.of(2021, 12, 1, 10, 0));
        final LocalDate now = LocalDate.of(2021, 12, 1);
        setMockedYtTableExistsCheckService(loader,
            Map.of(
                makeTablePath(now, STOCK_TABLE), true,
                makeTablePath(now, STOCKS_WITH_LIFETIMES_TABLE), true,
                makeTablePath(now, SUPPLIERS_TABLE), true,
                makeTablePath(now, TRANSIT_TABLE), true,
                makeTablePath(now, RAW_TRANSITS_TABLE), true
            ));
        loader.load();
    }

    @Test
    @DbUnitDataSet(
        before = "ImportYesterdayRecommendationsLoaderTest_importInProgress.before.csv",
        after = "ImportYesterdayRecommendationsLoaderTest_importInProgress.after.csv")
    public void testImportInProgress() {
        setTestTime(LocalDateTime.of(2021, 12, 1, 7, 45));
        final LocalDate now = LocalDate.of(2021, 12, 1);
        setMockedYtTableExistsCheckService(loader,
            Map.of(
                makeTablePath(now, STOCK_TABLE), true,
                makeTablePath(now, STOCKS_WITH_LIFETIMES_TABLE), true,
                makeTablePath(now, SUPPLIERS_TABLE), true,
                makeTablePath(now, TRANSIT_TABLE), true,
                makeTablePath(now, RAW_TRANSITS_TABLE), true
            ));
        loader.load();
    }

    @Test
    @DbUnitDataSet(
        before = "ImportYesterdayRecommendationsLoaderTest_tablesNotExist.before.csv",
        after = "ImportYesterdayRecommendationsLoaderTest_tablesNotExist.after.csv")
    public void testTablesNotExist() {
        setTestTime(LocalDateTime.of(2021, 12, 1, 7, 45));
        final LocalDate now = LocalDate.of(2021, 12, 1);
        setMockedYtTableExistsCheckService(loader,
            Map.of(
                makeTablePath(now, STOCK_TABLE), false,
                makeTablePath(now, STOCKS_WITH_LIFETIMES_TABLE), true,
                makeTablePath(now, SUPPLIERS_TABLE), true,
                makeTablePath(now, RAW_TRANSITS_TABLE), true,
                makeTablePath(now, TRANSIT_TABLE), false
            ));
        loader.load();

        verify(alertsService).pushAlertToTelegram("Импорт вчерашних рекомендаций с актульными стоками и транзитами не был начат из-за того, что таблицы не существуют: //home/market/production/replenishment/order_planning/2021-12-01/intermediate/stock_alpaca\\n" +
                "//home/market/production/replenishment/order_planning/2021-12-01/outputs/transits");
    }

    private String makeTablePath(LocalDate now, String tableName) {
        return TABLE_PREFIX + now.toString() + tableName;
    }

}
