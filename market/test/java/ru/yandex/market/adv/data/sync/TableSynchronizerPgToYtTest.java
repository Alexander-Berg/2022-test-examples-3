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
 * Тесты для проверки импорта из Postgres таблиц в YT таблицы
 */
public class TableSynchronizerPgToYtTest extends AbstractTableSynchronizerTest {

    private static final String DESTINATION_FOLDER = "//tmp/adv_data_sync/destinations/";

    private static final SyncSettings SYNC_SETTINGS = new SyncSettings(true, false);
    private static final SyncSettings SYNC_CLEAR_SETTINGS = new SyncSettings(true, true);

    private final PgTable source = PgTable.builder()
            .schema("test")
            .name("source_table")
            .model(SourceTable.class)
            .key(Set.of("id"))
            .build();

    @Autowired
    private TableSynchronizer tableSynchronizer;


    @DisplayName("Тестирует, синхронизацию PG-> YT без очистки записей, но с обновлением")
    @DbUnitDataSet(
            before = "/TableSynchronizerPgToYtTest/pg/sync_PgToYtWithNoClearAndUpdateExisting_Ok.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TableSynchronizerPgToYtTest.DestinationTable.class,
                    path = DESTINATION_FOLDER + "sync_PgToYtWithNoClearAndUpdateExisting_Ok"
            ),
            before = "/TableSynchronizerPgToYtTest/yt/sync_PgToYtWithNoClearAndUpdateExisting_Ok.before.json",
            after = "/TableSynchronizerPgToYtTest/yt/sync_PgToYtWithNoClearAndUpdateExisting_Ok.after.json"
    )
    @Test
    public void sync_PgToYtWithNoClearAndUpdateExisting_Ok() {
        sync(SYNC_SETTINGS, source, destination("sync_PgToYtWithNoClearAndUpdateExisting_Ok"));
    }

    @DisplayName("Тестирует, синхронизацию PG-> YT с очисткой старых данных")
    @DbUnitDataSet(
            before = "/TableSynchronizerPgToYtTest/pg/sync_PgToYtWithClear_Ok.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TableSynchronizerPgToYtTest.DestinationTable.class,
                    path = DESTINATION_FOLDER + "sync_PgToYtWithClear_Ok"
            ),
            before = "/TableSynchronizerPgToYtTest/yt/sync_PgToYtWithClear_Ok.before.json",
            after = "/TableSynchronizerPgToYtTest/yt/sync_PgToYtWithClear_Ok.after.json"
    )
    @Test
    public void sync_PgToYtWithClear_Ok() {
        sync(SYNC_CLEAR_SETTINGS, source, destination("sync_PgToYtWithClear_Ok"));
    }

    @DisplayName("Тестирует, синхронизацию PG-> YT с переносом данных без наличия таблиц")
    @DbUnitDataSet(
            before = "/TableSynchronizerPgToYtTest/pg/sync_PgToYtWithOutCreatedTables_Ok.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TableSynchronizerPgToYtTest.DestinationTable.class,
                    path = DESTINATION_FOLDER + "sync_PgToYtWithOutCreatedTables_Ok"
            ),
            after = "/TableSynchronizerPgToYtTest/yt/sync_PgToYtWithOutCreatedTables_Ok.after.json"
    )
    @Test
    public void sync_PgToYtWithOutCreatedTables_Ok() {
        sync(SYNC_SETTINGS, source, destination("sync_PgToYtWithOutCreatedTables_Ok"));
    }

    @DisplayName("Тестирует, синхронизацию PG-> YT с null в разных полях")
    @DbUnitDataSet(
            before = "/TableSynchronizerPgToYtTest/pg/sync_PgToYtWithNulls_zeroOnPrimitive.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TableSynchronizerPgToYtTest.DestinationTable.class,
                    path = DESTINATION_FOLDER + "sync_PgToYtWithNulls_zeroOnPrimitive"
            ),
            after = "/TableSynchronizerPgToYtTest/yt/sync_PgToYtWithNulls_zeroOnPrimitive.after.json"
    )
    @Test
    public void sync_PgToYtWithNulls_zeroOnPrimitive() {
        sync(SYNC_SETTINGS, source, destination("sync_PgToYtWithNulls_zeroOnPrimitive"));
    }


    @Table("source_table")
    @Data
    public static class SourceTable {

        private long id;

        @Column("first_name")
        private String firstName;

        private int age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
    public static class DestinationTable implements Syncable {

        @YTreeKeyField
        private long id;

        @YTreeField(key = "first_name")
        private String firstName;

        private int age;

        @YTreeField(key = "synced_at")
        private Instant syncedAt;
    }

    private void sync(SyncSettings settings, PgTable source, YtTable destination) {

        DataSyncTask<PgTable, SourceTable, YtTable, DestinationTable> task = new DataSyncTask<>() {

            @Override
            public PgTable getSource() {
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
                        destinationRow.setAge(sourceRow.getAge());
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

        tableSynchronizer.sync(task);
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
