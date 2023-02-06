package ru.yandex.market.adv.shop.integration.checkouter.datasync.interactor.cleaner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.order.MarketOrderItemEntity;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

@DisplayName("Тесты на сервис MarketOrderItemCleaner")
class MarketOrderItemCleanerTest extends AbstractShopIntegrationTest {

    @Autowired
    @Qualifier("tmsMarketOrderItemCleanerExecutor")
    private Executor executor;

    @DisplayName("Успешно удалили старые данные из market_order_item")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemEntity.class,
                    path = "//tmp/orderItemDeleteOldData_correctData_success_market_order_item"
            ),
            before = "MarketOrderItemCleanerTest/json/" +
                    "orderItemDeleteOldData_correctData_success_market_order_item.before.json",
            after = "MarketOrderItemCleanerTest/json/" +
                    "orderItemDeleteOldData_correctData_success_market_order_item.after.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemCleanerTest/csv/orderItemDeleteOldData_correctData_success.before.csv"
    )
    @Test
    void orderItemDeleteOldData_correctData_success() {
        run("orderItemDeleteOldData_correctData_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно обработали ситуацию, когда нет данных о синхронизации в sync_info")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemEntity.class,
                    path = "//tmp/orderItemDeleteOldData_emptySyncInfo_success_market_order_item"
            ),
            before = "MarketOrderItemCleanerTest/json/" +
                    "orderItemDeleteOldData_emptySyncInfo_success_market_order_item.before.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemCleanerTest/csv/orderItemDeleteOldData_emptySyncInfo_success.before.csv"
    )
    @Test
    void orderItemDeleteOldData_emptySyncInfo_success() {
        run("orderItemDeleteOldData_emptySyncInfo_success_",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно не удалили данные, когда они не выходят за пределы окна")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemEntity.class,
                    path = "//tmp/orderItemDeleteOldData_inWindow_success_market_order_item"
            ),
            before = "MarketOrderItemCleanerTest/json/" +
                    "orderItemDeleteOldData_inWindow_success_market_order_item.before.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemCleanerTest/csv/orderItemDeleteOldData_inWindow_success.before.csv"
    )
    @Test
    void orderItemDeleteOldData_inWindow_success() {
        run("orderItemDeleteOldData_inWindow_success_",
                () -> executor.doJob(mockContext())
        );
    }
}
