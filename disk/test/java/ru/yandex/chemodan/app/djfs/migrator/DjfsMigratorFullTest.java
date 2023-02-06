package ru.yandex.chemodan.app.djfs.migrator;

import java.sql.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.CollectionF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationPlan;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationPlanTest;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsTableMigration;
import ru.yandex.chemodan.app.djfs.migrator.test.BaseDjfsMigratorTest;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;


public class DjfsMigratorFullTest extends BaseDjfsMigratorTest {
    @Override
    @Before
    public void setUp() {
        super.setUp();
        loadUserForMigrationData();
    }

    @Test
    public void testAllDataIsCopied() {
        SetF<String> tablesInDb = srcShard().query(
                DjfsMigrationPlanTest.getAllTablesQuery, (rs, rowNum) -> rs.getString("table_name")
        ).unique();

        ListF<String> exclusions = Cf.list("async_tasks_data", "last_files_cache");

        MapF<String, SetF<MapF<String, Object>>> dataBefore = fetchData(srcShard(), tablesInDb);

        migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false,
                DjfsMigrationPlan.allTablesMigrations);

        MapF<String, SetF<MapF<String, Object>>> dataAfter = fetchData(dstShard(), tablesInDb);

        for (String table : tablesInDb.minus(exclusions)) {
            Assert.equals(
                    dataBefore.getTs(table).size(),
                    dataAfter.getTs(table).size(),
                    "not all rows copied on " + table
            );

            Assert.equalsTu(
                    dataBefore.getTs(table),
                    dataAfter.getTs(table),
                    "not all values is same on " + table + ", may be problem on " +
                            dataBefore.getTs(table).flatMap(MapF::entrySet).unique()
                                    .minus(dataAfter.getTs(table).flatMap(MapF::entrySet)) +
                            " full diff is"
            );
        }
    }

    @Test
    public void testMigrationOnShardWithSameData() {
        loadSampleDataOnShard(DST_PG_SHARD_ID);

        DjfsMigrator.CopyResult result = migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false,
                DjfsMigrationPlan.allTablesMigrations);

        Assert.equals(DjfsMigrationState.COPY_SUCCESS, result.getState(), result.getMessage());
    }

    @Test
    public void testAllDataIsCleaned() {
        ListF<String> tablesInDb = srcShard().query(
                DjfsMigrationPlanTest.getAllTablesQuery, (rs, rowNum) -> rs.getString("table_name")
        );

        for (DjfsTableMigration migration : DjfsMigrationPlan.allTablesMigrations.getMigrations().reverse()) {
            migration.cleanData(srcShard(), UID, 1);
        }

        for (String table : tablesInDb) {
            Assert.equals(0, srcShard().queryForInt("SELECT count(*) FROM disk." + table), table);
        }
    }

    @Test
    public void testReportStateCalledForAllTables() {
        SetF<String> migratedTables = DjfsMigrationPlan.allTablesMigrations.getTablesOrder().unique();

        SetF<String> tablesWithoutSampleData = migratedTables
                .filter(table -> srcShard()
                        .queryForObject("SELECT NOT exists(SELECT * FROM disk." + table + ")", Boolean.class));

        AtomicInteger reportsCall = new AtomicInteger(0);

        DjfsCopyConfiguration migrationConf = migratorConfigFactory
                .copyConfigBuilder(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID)
                .build();

        PgSchema pgSchema = PgSchema.build(migrationConf.srcShardJdbcTemplate());
        for (DjfsTableMigration migration : DjfsMigrationPlan.allTablesMigrations.getMigrations()) {
            migration.runCopying(migrationConf, pgSchema, reportsCall::incrementAndGet);
        }

        Assert.equals(
                migratedTables
                        .minus1("storage_files")
                        .minus1("duplicated_storage_files")
                        .minus1("last_files_cache")
                        .minus1("async_tasks_data")
                        .minus(tablesWithoutSampleData)
                        .size(),
                reportsCall.get(),
                "callback wasn't called for all tables"
        );
    }

    @NotNull
    private static MapF<String, SetF<MapF<String, Object>>> fetchData(JdbcTemplate3 shard, CollectionF<String> tables) {
        return Cf.toMap(tables.map(table -> Tuple2.tuple(
                table,
                normalizeValues(shard.query("SELECT * FROM disk." + table, new ColumnMapRowMapper()))
        )));
    }

    private static SetF<MapF<String, Object>> normalizeValues(ListF<Map<String, Object>> rows) {
        return rows.map(row -> Cf.toHashMap(row).mapValues(DjfsMigratorFullTest::normalizeValue)).unique();
    }

    @SneakyThrows
    private static Object normalizeValue(Object value) {
        if (value instanceof byte[]) {
            return Arrays.toString((byte[]) value);
        } else if (value instanceof Array) {
            return Arrays.toString((Object[]) ((Array) value).getArray());
        } else {
            return value;
        }
    }
}
