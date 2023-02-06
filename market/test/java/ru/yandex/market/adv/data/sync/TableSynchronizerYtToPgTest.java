package ru.yandex.market.adv.data.sync;

import java.time.Instant;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeKeyField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для проверки импорта из Yt таблиц в Postgres таблицы
 */
public class TableSynchronizerYtToPgTest extends AbstractTableSynchronizerTest {

    private static final String SOURCE_FOLDER = "//tmp/adv_data_sync/sources/";
    private static final SyncSettings SYNC_SETTINGS = new SyncSettings(true, false);
    private static final SyncSettings SYNC_CLEAR_SETTINGS = new SyncSettings(true, true);

    private final PgTable destination = PgTable.builder()
            .schema("test")
            .name("destination_table")
            .model(DestinationTable.class)
            .key(Set.of("id"))
            .build();

    @Autowired
    private TableSynchronizer ytTableSynchronizer;

    @DisplayName("Тестирует, синхронизацию YT-> PG без очистки")
    @DbUnitDataSet(
            before = "/pg/sync_YtToPgNoClear_Ok.before.csv",
            after = "/pg/sync_YtToPgNoClear_Ok.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = SourceTable.class,
                    path = SOURCE_FOLDER + "sync_YtToPgNoClear_Ok"
            ),
            before = "/yt/source_not_empty.json"
    )
    @Test
    public void sync_YtToPgNoClear_Ok() {
        sync(SYNC_SETTINGS, source("sync_YtToPgNoClear_Ok"), destination);
    }

    @DisplayName("Тестирует, синхронизацию YT-> PG с очисткой старых записей")
    @DbUnitDataSet(
            before = "/pg/sync_YtToPgWithClear_Ok.before.csv",
            after = "/pg/sync_YtToPgWithClear_Ok.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = SourceTable.class,
                    path = SOURCE_FOLDER + "sync_YtToPgWithClear_Ok"
            ),
            before = "/yt/source_not_empty.json"
    )
    @Test
    public void sync_YtToPgWithClear_Ok() {
        sync(SYNC_CLEAR_SETTINGS, source("sync_YtToPgWithClear_Ok"), destination);
    }

    @DisplayName("Тестирует, синхронизацию YT-> PG без очистки записей, но с обновлением")
    @DbUnitDataSet(
            before = "/pg/sync_YtToPgWithNoClearAndUpdateExisting_Ok.before.csv",
            after = "/pg/sync_YtToPgWithNoClearAndUpdateExisting_Ok.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = SourceTable.class,
                    path = SOURCE_FOLDER + "sync_YtToPgWithNoClearAndUpdateExisting_Ok"
            ),
            before = "/yt/sync_YtToPgWithNoClearAndUpdateExisting_Ok.before.json"
    )
    @Test
    public void sync_YtToPgWithNoClearAndUpdateExisting_Ok() {
        sync(SYNC_SETTINGS, source("sync_YtToPgWithNoClearAndUpdateExisting_Ok"), destination);
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

    @Table("destination_table")
    @Data
    public static class DestinationTable implements Syncable {

        private int id;

        @Column("first_name")
        private String firstName;

        private Instant syncedAt;
    }

    private void sync(SyncSettings settings, YtTable source, PgTable destination) {

        DataSyncTask<YtTable, SourceTable, PgTable, DestinationTable> task = new DataSyncTask<>() {

            @Override
            public YtTable getSource() {
                return source;
            }

            @Override
            public PgTable getDestination() {
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
        return YtTable.builder()
                .proxy(testCluster)
                .path(SOURCE_FOLDER + path)
                .model(SourceTable.class)
                .dynamic(true)
                .build();
    }
}
