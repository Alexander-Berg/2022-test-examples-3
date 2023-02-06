package ru.yandex.market.adv.data.sync;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeKeyField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

/**
 * Тесты для проверки импорта из YT таблиц в YT таблицы
 */
public class TableSynchronizerYtToYtTest extends AbstractTableSynchronizerTest {

    private static final String SOURCE_FOLDER = "//tmp/adv_data_sync/sources/";
    private static final String DESTINATION_FOLDER = "//tmp/adv_data_sync/destinations/";

    private static final SyncSettings SYNC_SETTINGS = new SyncSettings(true, false);
    private static final SyncSettings SYNC_CLEAR_SETTINGS = new SyncSettings(true, true);

    @Autowired
    private TableSynchronizer ytTableSynchronizer;

    @DisplayName("Тестирует, что синхронизация упадет с ошибкой, если источник не найден")
    @Test
    public void sync_SourceTableNotExist_Fail() {
        Assertions.assertThatThrownBy(() -> sync(SYNC_SETTINGS, source("any"), destination("any")))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("Тестирует, что синхронизация не произойдет, т.к. источник существует и пуст")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = SourceTable.class,
                    path = SOURCE_FOLDER + "sync_SourceTableEmpty_DoNothing"
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = DestinationTable.class,
                    path = DESTINATION_FOLDER + "sync_SourceTableEmpty_DoNothing"
            ),
            before = "/yt/destination_original.json",
            after = "/yt/destination_original.json"
    )
    @Test
    public void sync_SourceTableEmpty_DoNothing() {
        sync(SYNC_SETTINGS, source("sync_SourceTableEmpty_DoNothing"), destination("sync_SourceTableEmpty_DoNothing"));
    }

    @DisplayName("Тестирует, что будет создана таблица назначение, т.к. ее не было и будет равна источнику")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = SourceTable.class,
                    path = SOURCE_FOLDER + "sync_DestinationNotExist_DestinationCreated"
            ),
            before = "/yt/source_not_empty.json",
            after = "/yt/source_not_empty.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = DestinationTable.class,
                    path = DESTINATION_FOLDER + "sync_DestinationNotExist_DestinationCreated"
            ),
            after = "/yt/destination_original.json",
            create = false
    )
    @Test
    public void sync_DestinationNotExist_DestinationCreated() {
        sync(SYNC_SETTINGS, source("sync_DestinationNotExist_DestinationCreated"), destination(
                "sync_DestinationNotExist_DestinationCreated"));
    }

    @DisplayName("Тестирует, что данные синхронизировались из динамической в динамическую таблицу точно")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = SourceTable.class,
                    path = SOURCE_FOLDER + "sync_FullDynToDynSync_Ok"
            ),
            before = "/yt/source_not_empty.json",
            after = "/yt/source_not_empty.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = DestinationTable.class,
                    path = DESTINATION_FOLDER + "sync_FullDynToDynSync_Ok"
            ),
            before = "/yt/destination_empty.json",
            after = "/yt/destination_original.json"
    )
    @Test
    public void sync_FullDynToDynSync_Ok() {
        sync(SYNC_SETTINGS, source("sync_FullDynToDynSync_Ok"), destination("sync_FullDynToDynSync_Ok"));
    }

    @DisplayName("Тестирует, что данные синхронизировались из статической в динамическую таблицу точно")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = SourceTable.class,
                    path = SOURCE_FOLDER + "sync_FullStaticToDynSync_Ok",
                    isDynamic = false
            ),
            before = "/yt/source_not_empty.json",
            after = "/yt/source_not_empty.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = DestinationTable.class,
                    path = DESTINATION_FOLDER + "sync_FullStaticToDynSync_Ok"
            ),
            before = "/yt/destination_empty.json",
            after = "/yt/destination_original.json"
    )
    @Test
    public void sync_FullStaticToDynSync_Ok() {
        sync(SYNC_SETTINGS, source("sync_FullStaticToDynSync_Ok", false), destination("sync_FullStaticToDynSync_Ok"));
    }

    @DisplayName("Тестирует, что данные из предыдущей синхронизации очистились")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = SourceTable.class,
                    path = SOURCE_FOLDER + "sync_OldDataDeleted_Ok"
            ),
            before = "/yt/source_not_empty.json",
            after = "/yt/source_not_empty.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = DestinationTable.class,
                    path = DESTINATION_FOLDER + "sync_OldDataDeleted_Ok"
            ),
            before = "/yt/destination_has_old.json",
            after = "/yt/destination_original.json"
    )
    @Test
    public void sync_OldDataDeleted_Ok() {
        sync(SYNC_CLEAR_SETTINGS, source("sync_OldDataDeleted_Ok"), destination("sync_OldDataDeleted_Ok"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
    public static class SourceTable {

        @YTreeKeyField
        private int id;

        @YTreeField(key = "first_name")
        private String firstName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
    public static class DestinationTable implements Syncable {

        @YTreeKeyField
        private int id;

        @YTreeField(key = "first_name")
        private String firstName;

        @YTreeField(key = "synced_at")
        private Instant syncedAt;

    }

    private void sync(SyncSettings settings, YtTable source, YtTable destination) {

        DataSyncTask<YtTable, SourceTable, YtTable, DestinationTable> task = new DataSyncTask<>() {
            @Override
            public YtTable getSource() {
                return source;
            }

            @Override
            public YtTable getDestination() {
                return destination;
            }

            @Override
            public Transformer<SourceTable, DestinationTable> getTransformer() {
                return new Transformer<>() {
                    @Override
                    public DestinationTable transform(SourceTable sourceRow, Instant timestamp) {
                        DestinationTable destinationRow = new DestinationTable();
                        destinationRow.setFirstName(sourceRow.getFirstName());
                        destinationRow.setId(sourceRow.getId());
                        destinationRow.setSyncedAt(timestamp);
                        return destinationRow;
                    }
                };
            }

            @Override
            public SyncSettings getSyncSettings() {
                return settings;
            }
        };

        ytTableSynchronizer.sync(task);
    }

    private YtTable source(String path) {
        return source(path, true);
    }

    private YtTable source(String path, boolean dynamic) {
        return YtTable.builder()
                .proxy(testCluster)
                .path(SOURCE_FOLDER + path)
                .model(SourceTable.class)
                .dynamic(dynamic)
                .build();
    }

    private YtTable destination(String path) {
        return YtTable.builder()
                .proxy(testCluster)
                .path(DESTINATION_FOLDER + path)
                .model(DestinationTable.class)
                .dynamic(true)
                .build();
    }
}
