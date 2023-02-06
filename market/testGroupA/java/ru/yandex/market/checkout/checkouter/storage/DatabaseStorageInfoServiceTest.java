package ru.yandex.market.checkout.checkouter.storage;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.storage.monitoring.DatabaseStorageInfoService;
import ru.yandex.market.checkout.checkouter.storage.monitoring.DatabaseStorageSizeLogger;
import ru.yandex.market.checkout.checkouter.storage.monitoring.TableStorageInfo;

/**
 * @author jkt on 27/11/2021.
 */
public class DatabaseStorageInfoServiceTest extends AbstractServicesTestBase {

    @Autowired
    private DatabaseStorageInfoService databaseStorageInfoService;

    @Autowired
    private DatabaseStorageSizeLogger databaseStorageSizeLogger;

    @Test
    public void shouldReturnTableSizesCorrectly() {
        List<TableStorageInfo> tablesStorageInfo = databaseStorageInfoService.getTablesStorageInfo();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(tablesStorageInfo).isNotEmpty();
            softly.assertThat(tablesStorageInfo)
                    .extracting(TableStorageInfo::getTableName)
                    .contains("orders", "order_event", "order_item");
            softly.assertThat(tablesStorageInfo)
                    .extracting(TableStorageInfo::getTableSizeBytes)
                    .anyMatch(value -> value > 0);
            softly.assertThat(tablesStorageInfo)
                    .extracting(TableStorageInfo::getTotalStorageBytes)
                    .anyMatch(value -> value > 0);
            softly.assertThat(tablesStorageInfo)
                    .extracting(TableStorageInfo::getIndexSizeBytes)
                    .anyMatch(value -> value > 0);
        });
    }

    @Test
    public void shouldLogSizesWithoutErrors() {
        Assertions.assertThatCode(() -> databaseStorageSizeLogger.logDbEntitiesSize())
                .doesNotThrowAnyException();
    }
}
