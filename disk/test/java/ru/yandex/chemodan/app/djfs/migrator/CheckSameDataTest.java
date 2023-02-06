package ru.yandex.chemodan.app.djfs.migrator;

import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.Repeat;

import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.UserData;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationUtil;
import ru.yandex.chemodan.app.djfs.migrator.test.BaseDjfsMigratorTest;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

public class CheckSameDataTest extends BaseDjfsMigratorTest {
    private static final DjfsUid UID_FOR_COMPARE = DjfsUid.cons(100);
    private static final UUID ID = UUID.randomUUID();

    @Before
    @Override
    public void setUp() {
        super.setUp();
        sharpeiClient.createUser(UID_FOR_COMPARE, PG_SHARDS[SRC_PG_SHARD_ID]);
        userDao.insert(UserData.cons(UID_FOR_COMPARE));
    }

    @Test
    public void noDifference() {
        insertDiskInfo(srcShard(), ID, "/");

        insertDiskInfo(dstShard(), ID, "/");

        Assert.isEmpty(difference());
    }

    @Repeat(5)
    @Test
    public void onSourceAdditionalFile() {
        insertDiskInfo(srcShard(), ID, "/");
        insertDiskInfo(srcShard(), UUID.randomUUID(), "/another");

        insertDiskInfo(dstShard(), ID, "/");

        Assert.hasSize(1, difference());
    }

    @Test
    public void oneFileOnSource() {
        insertDiskInfo(srcShard(), ID, "/");

        Assert.hasSize(1, difference());
    }

    @Test
    public void oneFileOnDestination() {
        insertDiskInfo(dstShard(), ID, "/");

        Assert.hasSize(1, difference());
    }

    @Repeat(5)
    @Test
    public void onDestinationAdditionalFile() {
        insertDiskInfo(srcShard(), ID, "/");

        insertDiskInfo(dstShard(), ID, "/");
        insertDiskInfo(dstShard(), UUID.randomUUID(), "/another");

        Assert.hasSize(1, difference());
    }

    @Test
    public void changesInRecord() {
        insertDiskInfo(srcShard(), ID, "/");

        insertDiskInfo(dstShard(), ID, "/changed");

        Assert.hasSize(2, difference()); //[(null, some), (another, null)]
    }

    public void insertDiskInfo(JdbcTemplate3 jdbcTemplate3, UUID id, String path) {
        jdbcTemplate3.update("INSERT INTO disk.disk_info (id, uid, parent, version, path, type, data) "
                + "VALUES (?, ?, null, 0, ?, 'dir', '{}')",
                id, UID_FOR_COMPARE, path
        );
    }

    public Tuple2List<Map<String, Object>, Map<String, Object>> difference() {
        return DjfsMigrationUtil
                .calculateDifferenceOnShards(srcShard(), dstShard(), "disk_info", UID_FOR_COMPARE, PgSchema.build(srcShard()));
    }
}
