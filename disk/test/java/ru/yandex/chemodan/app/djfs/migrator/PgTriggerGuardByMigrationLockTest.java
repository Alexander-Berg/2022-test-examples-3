package ru.yandex.chemodan.app.djfs.migrator;

import java.util.Map;
import java.util.concurrent.Semaphore;

import com.mongodb.ReadPreference;
import lombok.SneakyThrows;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.djfs.core.db.DjfsShardInfo;
import ru.yandex.chemodan.app.djfs.core.db.pg.SharpeiShardResolver;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.lock.LockManager;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationPlan;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationPlanTest;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationUtil;
import ru.yandex.chemodan.app.djfs.migrator.test.BaseDjfsMigratorTest;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

public class PgTriggerGuardByMigrationLockTest extends BaseDjfsMigratorTest {
    private static final DjfsUid UID = DjfsUid.cons(123135456);

    @Autowired
    private LockManager lockManager;
    @Autowired
    private SharpeiShardResolver sharpeiShardResolver;

    @Before
    public void setUp() {
        super.setUp();
        initializePgUser(UID, PG_SHARDS[SRC_PG_SHARD_ID]);
    }

    @Test
    public void checkAllTablesHasTriggers() {
        SetF<String> tablesInDb = srcShard().query(
                DjfsMigrationPlanTest.getAllTablesQuery,
                (rs, rowNum) -> rs.getString("table_name")
        ).unique();

        ListF<String> exclusions = DjfsMigrationPlan.allTablesMigrations.getIgnore()
                .plus("storage_files")
                .plus("duplicated_storage_files")
                .plus("last_files_cache")
                .plus("async_tasks_data");

        String columnName = "uid";

        for (String table : tablesInDb.minus(exclusions)) {
            ListF<Map<String, Object>> rows = srcShard().query(
                    "select event_manipulation, event_object_table, action_statement " +
                            "from information_schema.triggers where trigger_name = ?",
                    new ColumnMapRowMapper(),
                    table + "_guard_by_migration_lock"
            );

            String message = "can't find a trigger for the table " + table + ". The trigger must look like this:\n"
                    + "CREATE TRIGGER " + table + "_guard_by_migration_lock\n"
                    + "    AFTER INSERT OR UPDATE OR DELETE\n"
                    + "    ON disk." + table + "\n"
                    + "    FOR EACH ROW\n"
                    + "EXECUTE PROCEDURE disk.guard_by_migration_lock('" + columnName + "');\n"
                    + "That trigger is needed to lock table while migration";

            Assert.equals(
                    Cf.set("INSERT", "UPDATE", "DELETE"),
                    rows.map(row -> row.get("event_manipulation")).unique(),
                    message
            );

            Assert.equals(
                    Cf.set(table),
                    rows.map(row -> row.get("event_object_table")).unique(),
                    message
            );

            Assert.equals(
                    Cf.set("EXECUTE PROCEDURE disk.guard_by_migration_lock('" + columnName + "')"),
                    rows.map(row -> row.get("action_statement")).unique(),
                    message
            );

            Assert.assertContains(
                    srcShard().queryForList(
                            "select column_name from information_schema.columns where table_name = ?",
                            String.class,
                            table
                    ),
                    columnName
            );
        }
    }

    @SneakyThrows
    @Test
    public void lockShouldWaitForOperationEnd() {
        JdbcTemplate3 shard = pgShardResolver.resolve(UID);
        DjfsShardInfo.Pg userShard = new DjfsShardInfo.Pg(sharpeiShardResolver.shardByUid(UID).get().getShardId());

        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/test.jpg");

        Semaphore semaphore = new Semaphore(0);

        Thread insertFile = new Thread(() -> DjfsMigrationUtil.doInTransaction(shard.getDataSource(), () -> {
            filesystem.createFile(DjfsPrincipal.cons(UID), path);
            semaphore.release();
            sleep();
        }));
        insertFile.start();

        try {
            semaphore.acquire();
            lockManager.lockForMigration(UID, userShard, Duration.standardSeconds(1), "");
            Assert.some(filesystem.find(DjfsPrincipal.cons(UID), path, Option.of(ReadPreference.primary())));
        } finally {
            insertFile.join();
        }
    }

    @Test
    public void shouldThrowCustomException() {
        JdbcTemplate3 shard = pgShardResolver.resolve(UID);
        DjfsShardInfo.Pg userShard = new DjfsShardInfo.Pg(sharpeiShardResolver.shardByUid(UID).get().getShardId());
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/test.jpg");
        filesystem.createFile(DjfsPrincipal.cons(UID), path);


        lockManager.lockForMigration(UID, userShard, Duration.standardMinutes(1), "");

        Assert.assertThrows(
                () -> shard.update("UPDATE disk.files SET version = version + 1 WHERE uid = ?", UID),
                BadSqlGrammarException.class,
                e -> e.getMessage().contains("user locked for migration")
        );
    }

    @Test
    public void shouldCacheMigrationCheckResultInTransaction() {
        JdbcTemplate3 shard = pgShardResolver.resolve(UID);
        DjfsShardInfo.Pg userShard = new DjfsShardInfo.Pg(sharpeiShardResolver.shardByUid(UID).get().getShardId());
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/test.jpg");
        filesystem.createFile(DjfsPrincipal.cons(UID), path);

        DjfsMigrationUtil.doInTransaction(shard.getDataSource(), () -> {
            //попали в кеш в рамках транзакции
            shard.update("UPDATE disk.files SET version = version + 1 WHERE uid = ?", UID);

            //в другой транзакции изменить запись не получится из-за лока, но зато можно проверить кеш
            lockManager.lockForMigration(UID, userShard, Duration.standardMinutes(1), "");

            shard.update("UPDATE disk.files SET version = version + 1 WHERE uid = ?", UID);
            Assert.isFalse(lockManager.isLockedForMigration(UID, userShard));
        });
    }

    @Test
    public void shouldNotCacheMigrationCheckResultWithoutTransaction() {
        JdbcTemplate3 shard = pgShardResolver.resolve(UID);
        DjfsShardInfo.Pg userShard = new DjfsShardInfo.Pg(sharpeiShardResolver.shardByUid(UID).get().getShardId());
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/test.jpg");
        filesystem.createFile(DjfsPrincipal.cons(UID), path);

        shard.update("UPDATE disk.files SET version = version + 1 WHERE uid = ?", UID);

        lockManager.lockForMigration(UID, userShard, Duration.standardMinutes(1), "");

        Assert.assertThrows(
                () -> shard.update("UPDATE disk.files SET version = version + 1 WHERE uid = ?", UID),
                BadSqlGrammarException.class
        );
    }

    @SneakyThrows
    private void sleep() {
        Thread.sleep(100);
    }
}
