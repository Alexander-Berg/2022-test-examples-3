package ru.yandex.market.adv.shop.integration.checkouter.datasync.interactor.cleaner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketVendorClicksLog;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

@DisplayName("Тесты на сервис MarketVendorClicksLogCleaner")
class MarketVendorClicksLogCleanerTest extends AbstractShopIntegrationTest {

    @Autowired
    @Qualifier("tmsMarketVendorClicksLogCleanerExecutor")
    private Executor executor;

    @DisplayName("Успешно удалили старые данные из market_vendor_clicks_log")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLog.class,
                    path = "//tmp/clicksLogDeleteOldData_correctData_success_market_vendor_clicks_log"
            ),
            before = "MarketVendorClicksLogCleanerTest/json/" +
                    "clicksLogDeleteOldData_correctData_success_market_vendor_clicks_log.before.json",
            after = "MarketVendorClicksLogCleanerTest/json/" +
                    "clicksLogDeleteOldData_correctData_success_market_vendor_clicks_log.after.json"
    )
    @DbUnitDataSet(
            before = "MarketVendorClicksLogCleanerTest/csv/clicksLogDeleteOldData_correctData_success.before.csv"
    )
    @Test
    void clicksLogDeleteOldData_correctData_success() {
        run("clicksLogDeleteOldData_correctData_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно обработали ситуацию, когда нет данных о синхронизации в sync_info")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLog.class,
                    path = "//tmp/clicksLogDeleteOldData_emptySyncInfo_success_market_vendor_clicks_log"
            ),
            before = "MarketVendorClicksLogCleanerTest/json/" +
                    "clicksLogDeleteOldData_emptySyncInfo_success_market_vendor_clicks_log.before.json"
    )
    @DbUnitDataSet(
            before = "MarketVendorClicksLogCleanerTest/csv/clicksLogDeleteOldData_emptySyncInfo_success.before.csv"
    )
    @Test
    void clicksLogDeleteOldData_emptySyncInfo_success() {
        run("clicksLogDeleteOldData_emptySyncInfo_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно не удалили данные, когда они не выходят за пределы окна")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLog.class,
                    path = "//tmp/clicksLogDeleteOldData_inWindow_success_market_vendor_clicks_log"
            ),
            before = "MarketVendorClicksLogCleanerTest/json/" +
                    "clicksLogDeleteOldData_inWindow_success_market_vendor_clicks_log.before.json"
    )
    @DbUnitDataSet(
            before = "MarketVendorClicksLogCleanerTest/csv/clicksLogDeleteOldData_inWindow_success.before.csv"
    )
    @Test
    void clicksLogDeleteOldData_inWindow_success() {
        run("clicksLogDeleteOldData_inWindow_success_",
                () -> executor.doJob(mockContext())
        );
    }
}
