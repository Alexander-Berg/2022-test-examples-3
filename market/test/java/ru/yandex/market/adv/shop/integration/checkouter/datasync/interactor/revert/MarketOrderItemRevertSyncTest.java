package ru.yandex.market.adv.shop.integration.checkouter.datasync.interactor.revert;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.properties.MarketOrderItemRevertTableProperties;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.revert.MarketOrderItemRevert;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.revert.MarketOrderItemRevertSource;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Date: 19.07.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
@DisplayName("Тесты на сервис MarketOrderItemRevertSync")
@ParametersAreNonnullByDefault
class MarketOrderItemRevertSyncTest extends AbstractShopIntegrationTest {

    @Autowired
    @Qualifier("tmsMarketOrderItemRevertSyncExecutor")
    private Executor executor;
    @Autowired
    private MarketOrderItemRevertTableProperties properties;
    @Autowired
    @Qualifier("ytOrderItemRevertStaticClient")
    private YtClientProxy ytClient;

    @DisplayName("Успешно импортировали данные при пустой таблице sync_info")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertSource.class,
                    path = "//tmp/adv_shop_integration_revert_source/" +
                            "syncMarketOrderItemRevert_emptySyncInfo_success/" +
                            "syncMarketOrderItemRevert_emptySyncInfo_success_1",
                    isDynamic = false
            ),
            before = "MarketOrderItemRevertSync/json/source/" +
                    "syncMarketOrderItemRevert_emptySyncInfo_success_1.before.json",
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertSource.class,
                    path = "//tmp/adv_shop_integration_revert_source/" +
                            "syncMarketOrderItemRevert_emptySyncInfo_success/" +
                            "syncMarketOrderItemRevert_emptySyncInfo_success_2",
                    isDynamic = false
            ),
            before = "MarketOrderItemRevertSync/json/source/" +
                    "syncMarketOrderItemRevert_emptySyncInfo_success_2.before.json",
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevert.class,
                    path = "//tmp/syncMarketOrderItemRevert_emptySyncInfo_success_market_order_item_revert"
            ),
            before = "MarketOrderItemRevertSync/json/target/" +
                    "syncMarketOrderItemRevert_emptySyncInfo_success.before.json",
            after = "MarketOrderItemRevertSync/json/target/" +
                    "syncMarketOrderItemRevert_emptySyncInfo_success.after.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemRevertSync/csv/syncMarketOrderItemRevert_emptySyncInfo_success.before.csv",
            after = "MarketOrderItemRevertSync/csv/syncMarketOrderItemRevert_emptySyncInfo_success.after.csv"
    )
    @Test
    void syncMarketOrderItemRevert_emptySyncInfo_success() {
        run("syncMarketOrderItemRevert_emptySyncInfo_success",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Ничего не сделали, так как нет таблицы источника")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevert.class,
                    path = "//tmp/syncMarketOrderItemRevert_emptySourceTables_success_market_order_item_revert"
            ),
            before = "MarketOrderItemRevertSync/json/target/" +
                    "syncMarketOrderItemRevert_emptySourceTables_success.before.json",
            after = "MarketOrderItemRevertSync/json/target/" +
                    "syncMarketOrderItemRevert_emptySourceTables_success.after.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemRevertSync/csv/syncMarketOrderItemRevert_emptySourceTables_success.before.csv",
            after = "MarketOrderItemRevertSync/csv/syncMarketOrderItemRevert_emptySourceTables_success.after.csv"
    )
    @Test
    void syncMarketOrderItemRevert_emptySourceTables_success() {
        try {
            ytClient.createPath("//tmp/adv_shop_integration_revert_source/" +
                    "syncMarketOrderItemRevert_emptySourceTables_success", Map.of());
            run("syncMarketOrderItemRevert_emptySourceTables_success",
                    () -> executor.doJob(mockContext())
            );
        } finally {
            ytClient.deletePath("//tmp/adv_shop_integration_revert_source/" +
                    "syncMarketOrderItemRevert_emptySourceTables_success");
        }
    }

    @DisplayName("Успешно импортировали данные при заполненной таблице sync_info, но отсутствии источника")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertSource.class,
                    path = "//tmp/adv_shop_integration_revert_source/" +
                            "syncMarketOrderItemRevert_syncInfoWithoutTable_success/" +
                            "syncMarketOrderItemRevert_syncInfoWithoutTable_success",
                    isDynamic = false
            ),
            before = "MarketOrderItemRevertSync/json/source/" +
                    "syncMarketOrderItemRevert_syncInfoWithoutTable_success.before.json",
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevert.class,
                    path = "//tmp/syncMarketOrderItemRevert_syncInfoWithoutTable_success_market_order_item_revert"
            ),
            before = "MarketOrderItemRevertSync/json/target/" +
                    "syncMarketOrderItemRevert_syncInfoWithoutTable_success.before.json",
            after = "MarketOrderItemRevertSync/json/target/" +
                    "syncMarketOrderItemRevert_syncInfoWithoutTable_success.after.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemRevertSync/csv/syncMarketOrderItemRevert_syncInfoWithoutTable_success.before.csv",
            after = "MarketOrderItemRevertSync/csv/syncMarketOrderItemRevert_syncInfoWithoutTable_success.after.csv"
    )
    @Test
    void syncMarketOrderItemRevert_syncInfoWithoutTable_success() {
        run("syncMarketOrderItemRevert_syncInfoWithoutTable_success",
                () -> executor.doJob(mockContext())
        );
    }

    @DisplayName("Успешно импортировали данные при заполненной таблице sync_info")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertSource.class,
                    path = "//tmp/adv_shop_integration_revert_source/" +
                            "syncMarketOrderItemRevert_syncInfoExisted_success/" +
                            "syncMarketOrderItemRevert_syncInfoExisted_success_1",
                    isDynamic = false
            ),
            before = "MarketOrderItemRevertSync/json/source/" +
                    "syncMarketOrderItemRevert_syncInfoExisted_success_1.before.json",
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevertSource.class,
                    path = "//tmp/adv_shop_integration_revert_source/" +
                            "syncMarketOrderItemRevert_syncInfoExisted_success/" +
                            "syncMarketOrderItemRevert_syncInfoExisted_success_2",
                    isDynamic = false
            ),
            before = "MarketOrderItemRevertSync/json/source/" +
                    "syncMarketOrderItemRevert_syncInfoExisted_success_2.before.json",
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemRevert.class,
                    path = "//tmp/syncMarketOrderItemRevert_syncInfoExisted_success_market_order_item_revert"
            ),
            before = "MarketOrderItemRevertSync/json/target/" +
                    "syncMarketOrderItemRevert_syncInfoExisted_success.before.json",
            after = "MarketOrderItemRevertSync/json/target/" +
                    "syncMarketOrderItemRevert_syncInfoExisted_success.after.json"
    )
    @DbUnitDataSet(
            before = "MarketOrderItemRevertSync/csv/syncMarketOrderItemRevert_syncInfoExisted_success.before.csv",
            after = "MarketOrderItemRevertSync/csv/syncMarketOrderItemRevert_syncInfoExisted_success.after.csv"
    )
    @Test
    void syncMarketOrderItemRevert_syncInfoExisted_success() {
        run("syncMarketOrderItemRevert_syncInfoExisted_success",
                () -> executor.doJob(mockContext())
        );
    }

    @Override
    protected void run(String newPrefix, Runnable runnable) {
        String oldPrefix = properties.getPrefix();
        properties.setPrefix("//tmp/adv_shop_integration_revert_source/" + newPrefix);
        try {
            super.run(newPrefix + "_", runnable);
        } finally {
            properties.setPrefix(oldPrefix);
        }
    }
}
