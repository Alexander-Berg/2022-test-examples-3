package ru.yandex.market.adv.shop.integration.checkouter.datasync.interactor.cleaner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketOrderItemClickHistory;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

@DisplayName("Тесты на сервис MarketOrderItemClickHistoryCleaner")
class MarketOrderItemClickHistoryCleanerTest extends AbstractShopIntegrationTest {

    @Autowired
    @Qualifier("tmsMarketOrderItemClickHistoryCleanerExecutor")
    private Executor executor;

    @DisplayName("Успешно удалили старые данные из market_order_item_click_history")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClickHistory.class,
                    path = "//tmp/orderItemClickHistoryClean_correctData_success_market_order_item_click_history"
            ),
            before = "MarketOrderItemClickHistoryCleanerTest/json/" +
                    "orderItemClickHistoryClean_correctData_success_market_order_item_click_history.before.json",
            after = "MarketOrderItemClickHistoryCleanerTest/json/" +
                    "orderItemClickHistoryClean_correctData_success_market_order_item_click_history.after.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemClickHistoryCleanerTest/csv/" +
                    "orderItemClickHistoryClean_correctData_success.before.csv"
    )
    @Test
    void orderItemClickHistoryClean_correctData_success() {
        run("orderItemClickHistoryClean_correctData_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно обработали ситуацию, когда нет данных о синхронизации в sync_info")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClickHistory.class,
                    path = "//tmp/orderItemClickHistoryClean_emptySyncInfo_success_market_order_item_click_history"
            ),
            before = "MarketOrderItemClickHistoryCleanerTest/json/" +
                    "orderItemClickHistoryClean_emptySyncInfo_success_market_order_item_click_history.before.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemClickHistoryCleanerTest/csv/" +
                    "orderItemClickHistoryClean_emptySyncInfo_success.before.csv"
    )
    @Test
    void orderItemClickHistoryClean_emptySyncInfo_success() {
        run("orderItemClickHistoryClean_emptySyncInfo_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно не удалили данные, когда они не выходят за пределы окна")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemClickHistory.class,
                    path = "//tmp/orderItemClickHistoryClean_inWindow_success_market_order_item_click_history"
            ),
            before = "MarketOrderItemClickHistoryCleanerTest/json/" +
                    "orderItemClickHistoryClean_inWindow_success_market_order_item_click_history.before.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemClickHistoryCleanerTest/csv/" +
                    "orderItemClickHistoryClean_inWindow_success.before.csv"
    )
    @Test
    void orderItemClickHistoryClean_inWindow_success() {
        run("orderItemClickHistoryClean_inWindow_success_",
                () -> executor.doJob(mockContext())
        );
    }
}
