package ru.yandex.market.adv.shop.integration.checkouter.datasync.interactor.click;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketVendorClicksLog;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketVendorClicksLogSource;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

@DisplayName("Тесты на сервис MarketVendorClicksLogSync")
class MarketVendorClicksLogSyncTest extends AbstractShopIntegrationTest {

    @Autowired
    @Qualifier("tmsMarketVendorClicksLogSyncExecutor")
    private Executor executor;

    @DisplayName("Успешно импортировали данные при пустой таблице sync_info")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLogSource.class,
                    path = "//tmp/syncMarketClicksLog_emptySyncInfo_success_2021-10-21T11:00:00",
                    isDynamic = false
            ),
            before = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_emptySyncInfo_success_2021-10-21T11-00-00.before.json",
            after = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_emptySyncInfo_success_2021-10-21T11-00-00.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLog.class,
                    path = "//tmp/syncMarketClicksLog_emptySyncInfo_success_market_vendor_clicks_log"
            ),
            after = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_emptySyncInfo_success_market_vendor_clicks_log.after.json"
    )
    @DbUnitDataSet(
            after = "MarketVendorClicksLogSyncTest/csv/syncMarketClicksLog_emptySyncInfo_success.after.csv"
    )
    @Test
    void syncMarketClicksLog_emptySyncInfo_success() {
        run("syncMarketClicksLog_emptySyncInfo_success_",
                () -> executor.doJob(mockContext())
        );
    }


    @DisplayName("Успешно импортировали данные при непустой таблице sync_info, где sync_id > 0")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLogSource.class,
                    path = "//tmp/syncMarketClicksLog_syncIdGreaterThan0_success_2021-10-21T11:00:00",
                    isDynamic = false
            ),
            before = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_syncIdGreaterThan0_success_2021-10-21T11-00-00.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLog.class,
                    path = "//tmp/syncMarketClicksLog_syncIdGreaterThan0_success_market_vendor_clicks_log"
            ),
            before = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_syncIdGreaterThan0_success_market_vendor_clicks_log.before.json",
            after = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_syncIdGreaterThan0_success_market_vendor_clicks_log.after.json"
    )
    @DbUnitDataSet(
            before = "MarketVendorClicksLogSyncTest/csv/syncMarketClicksLog_syncIdGreaterThan0_success.before.csv",
            after = "MarketVendorClicksLogSyncTest/csv/syncMarketClicksLog_syncIdGreaterThan0_success.after.csv"
    )
    @Test
    void syncMarketClicksLog_syncIdGreaterThan0_success() {
        run("syncMarketClicksLog_syncIdGreaterThan0_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно импортировали 2 исходные таблицы при непустой таблице sync_info")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLogSource.class,
                    path = "//tmp/syncMarketClicksLog_importTwoTables_success_2021-10-21T11:00:00",
                    isDynamic = false
            ),
            before = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_importTwoTables_success_2021-10-21T11-00-00.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLogSource.class,
                    path = "//tmp/syncMarketClicksLog_importTwoTables_success_2021-10-21T11:30:00",
                    isDynamic = false
            ),
            before = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_importTwoTables_success_2021-10-21T11-30-00.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLog.class,
                    path = "//tmp/syncMarketClicksLog_importTwoTables_success_market_vendor_clicks_log"
            ),
            before = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_importTwoTables_success_market_vendor_clicks_log.before.json",
            after = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_importTwoTables_success_market_vendor_clicks_log.after.json"
    )
    @DbUnitDataSet(
            before = "MarketVendorClicksLogSyncTest/csv/syncMarketClicksLog_importTwoTables_success.before.csv",
            after = "MarketVendorClicksLogSyncTest/csv/syncMarketClicksLog_importTwoTables_success.after.csv"
    )
    @Test
    void syncMarketClicksLog_importTwoTables_success() {
        run("syncMarketClicksLog_importTwoTables_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно не произвели импорт, когда с создания таблицы прошло недостаточно времени")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLogSource.class,
                    path = "//tmp/syncMarketClicksLog_wrongCreationTime_success_2021-10-21T11:00:00",
                    isDynamic = false
            ),
            before = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_wrongCreationTime_success_2021-10-21T11-00-00.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLog.class,
                    path = "//tmp/syncMarketClicksLog_wrongCreationTime_success_market_vendor_clicks_log"
            ),
            before = "MarketVendorClicksLogSyncTest/json/" +
                    "syncMarketClicksLog_wrongCreationTime_success_market_vendor_clicks_log.before.json"
    )
    @DbUnitDataSet(
            before = "MarketVendorClicksLogSyncTest/csv/syncMarketClicksLog_wrongCreationTime_success.before.csv"
    )
    @Test
    void syncMarketClicksLog_wrongCreationTime_success() {
        run("syncMarketClicksLog_wrongCreationTime_success_", 30,
                () -> executor.doJob(mockContext())
        );
    }
}
