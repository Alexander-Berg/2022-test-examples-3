package ru.yandex.market.adv.shop.integration.checkouter.datasync.interactor.cleaner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.revert.MarketOrderItemRevertHistory;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

@DisplayName("Тесты на сервис MarketOrderItemRevertHistoryCleaner")
class MarketOrderItemRevertHistoryCleanerTest extends AbstractShopIntegrationTest {

    @Autowired
    @Qualifier("tmsMarketOrderItemRevertHistoryCleanerExecutor")
    private Executor executor;

    @DisplayName("Успешно удалили старые данные из market_order_item_revert_history")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertHistory.class,
                    path = "//tmp/orderItemRevertHistoryClean_correctData_success_market_order_item_revert_history"
            ),
            before = "MarketOrderItemRevertHistoryCleanerTest/json/" +
                    "orderItemRevertHistoryClean_correctData_success.before.json",
            after = "MarketOrderItemRevertHistoryCleanerTest/json/" +
                    "orderItemRevertHistoryClean_correctData_success.after.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemRevertHistoryCleanerTest/csv/" +
                    "orderItemRevertHistoryClean_correctData_success.before.csv"
    )
    @Test
    void orderItemRevertHistoryClean_correctData_success() {
        run("orderItemRevertHistoryClean_correctData_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно обработали ситуацию, когда нет данных о синхронизации в sync_info")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertHistory.class,
                    path = "//tmp/orderItemRevertHistoryClean_emptySyncInfo_success_market_order_item_revert_history"
            ),
            before = "MarketOrderItemRevertHistoryCleanerTest/json/" +
                    "orderItemRevertHistoryClean_emptySyncInfo_success.before.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemRevertHistoryCleanerTest/csv/" +
                    "orderItemRevertHistoryClean_emptySyncInfo_success.before.csv"
    )
    @Test
    void orderItemRevertHistoryClean_emptySyncInfo_success() {
        run("orderItemRevertHistoryClean_emptySyncInfo_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно не удалили данные из market_order_item_revert_history, когда они не выходят за пределы окна")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertHistory.class,
                    path = "//tmp/orderItemRevertHistoryClean_inWindow_success_market_order_item_revert_history"
            ),
            before = "MarketOrderItemRevertHistoryCleanerTest/json/" +
                    "orderItemRevertHistoryClean_inWindow_success.before.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemRevertHistoryCleanerTest/csv/" +
                    "orderItemRevertHistoryClean_inWindow_success.before.csv"
    )
    @Test
    void orderItemRevertHistoryClean_inWindow_success() {
        run("orderItemRevertHistoryClean_inWindow_success_",
                () -> executor.doJob(mockContext())
        );
    }
}
