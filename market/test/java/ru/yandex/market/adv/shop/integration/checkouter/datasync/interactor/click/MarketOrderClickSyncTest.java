package ru.yandex.market.adv.shop.integration.checkouter.datasync.interactor.click;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.properties.MarketOrderClickSyncProperties;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketOrderItemClick;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketVendorClicksLog;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.order.MarketOrderItemEntity;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

/**
 * Date: 30.05.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
@DisplayName("Тесты на сервис MarketOrderClickSync")
class MarketOrderClickSyncTest extends AbstractShopIntegrationTest {

    @Autowired
    @Qualifier("tmsMarketOrderClickSyncExecutor")
    private Executor executor;
    @Autowired
    private MarketOrderClickSyncProperties marketOrderClickSyncProperties;

    @DisplayName("Синхронизация кликов и товаров не началась, так как нет стартовой синхронизации кликов.")
    @DbUnitDataSet(
            before = "MarketOrderClickSync/csv/execute_nullClickLog_nothing.before.csv",
            after = "MarketOrderClickSync/csv/execute_nullClickLog_nothing.after.csv"
    )
    @Test
    void execute_nullClickLog_nothing() {
        run("execute_nullClickLog_nothing_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Синхронизация кликов и товаров без начального времени")
    @DbUnitDataSet(
            before = "MarketOrderClickSync/csv/execute_startWithDefault_syncAllData.before.csv",
            after = "MarketOrderClickSync/csv/execute_startWithDefault_syncAllData.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLog.class,
                    path = "//tmp/execute_startWithDefault_syncAllData_market_vendor_clicks_log"
            ),
            before = "MarketOrderClickSync/json/yt/click/execute_startWithDefault_syncAllData.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemEntity.class,
                    path = "//tmp/execute_startWithDefault_syncAllData_market_order_item"
            ),
            before = "MarketOrderClickSync/json/yt/order/execute_startWithDefault_syncAllData.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClick.class,
                    path = "//tmp/execute_startWithDefault_syncAllData_market_order_item_click"
            ),
            before = "MarketOrderClickSync/json/yt/sync/execute_startWithDefault_syncAllData.before.json",
            after = "MarketOrderClickSync/json/yt/sync/execute_startWithDefault_syncAllData.after.json"
    )
    @Test
    void execute_startWithDefault_syncAllData() {
        run("execute_startWithDefault_syncAllData_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Синхронизация кликов и товаров с начально заданным временем")
    @DbUnitDataSet(
            before = "MarketOrderClickSync/csv/execute_startWithCurrentState_syncPartialData.before.csv",
            after = "MarketOrderClickSync/csv/execute_startWithCurrentState_syncPartialData.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketVendorClicksLog.class,
                    path = "//tmp/execute_startWithCurrentState_syncPartialData_market_vendor_clicks_log"
            ),
            before = "MarketOrderClickSync/json/yt/click/execute_startWithCurrentState_syncPartialData.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemEntity.class,
                    path = "//tmp/execute_startWithCurrentState_syncPartialData_market_order_item"
            ),
            before = "MarketOrderClickSync/json/yt/order/execute_startWithCurrentState_syncPartialData.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClick.class,
                    path = "//tmp/execute_startWithCurrentState_syncPartialData_market_order_item_click"
            ),
            before = "MarketOrderClickSync/json/yt/sync/execute_startWithCurrentState_syncPartialData.before.json",
            after = "MarketOrderClickSync/json/yt/sync/execute_startWithCurrentState_syncPartialData.after.json"
    )
    @Test
    void execute_startWithCurrentState_syncPartialData() {
        try {
            marketOrderClickSyncProperties.setSaveWhenEmpty(false);
            run("execute_startWithCurrentState_syncPartialData_",
                    () -> executor.doJob(mockContext())
            );
        } finally {
            marketOrderClickSyncProperties.setSaveWhenEmpty(true);
        }
    }
}
