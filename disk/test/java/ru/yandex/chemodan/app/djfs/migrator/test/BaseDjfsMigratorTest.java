package ru.yandex.chemodan.app.djfs.migrator.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.UUID;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.db.DjfsShardInfo;
import ru.yandex.chemodan.app.djfs.core.db.pg.PgArray;
import ru.yandex.chemodan.app.djfs.core.db.pg.ResultSetUtils;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.migrator.DjfsMigrator;
import ru.yandex.chemodan.app.djfs.migrator.DjfsMigratorConfigFactory;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;

@ContextConfiguration(classes = {
        TestContextConfiguration2.class,
})
public abstract class BaseDjfsMigratorTest extends DjfsTestBase {

    protected static final DjfsUid UID = DjfsUid.cons(222222);

    @Autowired
    protected DjfsMigratorConfigFactory migratorConfigFactory;
    @Autowired
    protected DjfsMigrator migrator;

    private static final String sampleDatabaseQueryPath =
            "ut/src/test/java/ru/yandex/chemodan/app/djfs/migrator/fixtures/sample_database.sql";

    protected static final int SRC_PG_SHARD_ID = 0;
    protected static final int DST_PG_SHARD_ID = 1;

    protected void loadUserForMigrationData() {
        sharpeiClient.createUser(UID, PG_SHARD_1);
        loadSampleDataOnShard(SRC_PG_SHARD_ID);
    }

    protected void loadSampleUsers() {
        initializePgUser(DjfsUid.cons(100), PG_SHARDS[SRC_PG_SHARD_ID]);
        initializePgUser(DjfsUid.cons(101), PG_SHARDS[SRC_PG_SHARD_ID]);
        initializePgUser(DjfsUid.cons(200), PG_SHARDS[DST_PG_SHARD_ID]);
        initializePgUser(DjfsUid.cons(201), PG_SHARDS[DST_PG_SHARD_ID]);
    }


    @SneakyThrows
    protected void loadSampleDataOnShard(int shardId) {
        DjfsShardInfo.Pg shardInfo = new DjfsShardInfo.Pg(shardId);
        JdbcTemplate3 shard = pgShardResolver.resolve(shardInfo);

        BufferedReader fileBuffer = new BufferedReader(new FileReader(sampleDatabaseQueryPath));
        String currentLine;
        StringBuilder queryBuffer = new StringBuilder();
        while ((currentLine = fileBuffer.readLine()) != null) {
            queryBuffer.append(currentLine).append("\n ");
        }
        fileBuffer.close();

        shard.execute(queryBuffer.toString());
    }

    protected ListF<UUID> idsOfStorageFilesForUid(JdbcTemplate3 shard) {
        return shard.query(""
                        + "SELECT storage_id FROM disk.files WHERE uid = :uid"
                        + "    UNION "
                        + "SELECT storage_id FROM disk.version_data WHERE uid = :uid AND storage_id NOTNULL ",
                (rs, i) -> ResultSetUtils.getUuid(rs, "storage_id"),
                Cf.map("uid", UID.asLong())
        );
    }

    protected int countStorageFilesByIds(JdbcTemplate3 shard, ListF<UUID> ids) {
        return shard.queryForInt(
                "SELECT count(*) FROM disk.storage_files WHERE storage_id = ANY (?)",
                PgArray.uuidArray(ids.toArray(new UUID[0]))
        );
    }

    protected int countRecordsInTableForUid(JdbcTemplate3 srcShard, String table) {
        return srcShard.queryForInt("SELECT count(*) FROM disk." + table + " where uid = ?", UID);
    }

    protected JdbcTemplate3 dstShard() {
        return pgShardResolver.resolve(new DjfsShardInfo.Pg(DST_PG_SHARD_ID));
    }

    protected JdbcTemplate3 srcShard() {
        return pgShardResolver.resolve(new DjfsShardInfo.Pg(SRC_PG_SHARD_ID));
    }
}
