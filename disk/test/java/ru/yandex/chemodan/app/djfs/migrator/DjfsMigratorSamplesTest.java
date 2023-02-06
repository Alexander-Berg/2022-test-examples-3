package ru.yandex.chemodan.app.djfs.migrator;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationPlan;
import ru.yandex.chemodan.app.djfs.migrator.test.BaseDjfsMigratorTest;
import ru.yandex.misc.test.Assert;

public class DjfsMigratorSamplesTest extends BaseDjfsMigratorTest {
    @Override
    @Before
    public void setUp() {
        super.setUp();
        loadUserForMigrationData();
        loadSampleUsers();
    }

    @Test
    public void testCopyUserIndexTable() {
        int sizeToCopy = countRecordsInTableForUid(srcShard(), "user_index");

        migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        Assert.equals(sizeToCopy, countRecordsInTableForUid(dstShard(), "user_index"));
    }

    @Test
    public void testCopyFoldersTable() {
        int sizeToCopy = countRecordsInTableForUid(srcShard(), "folders");

        migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        Assert.equals(sizeToCopy, countRecordsInTableForUid(dstShard(), "folders"));
    }

    @Test
    public void testCopyFilesData() {
        int filesToCopy = countRecordsInTableForUid(srcShard(), "files");
        ListF<UUID> storageFilesToCopy = idsOfStorageFilesForUid(srcShard());
        int versionDataToCopy = countRecordsInTableForUid(srcShard(), "version_data");

        migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        Assert.equals(filesToCopy, countRecordsInTableForUid(dstShard(), "files"));
        Assert.equals(storageFilesToCopy.size(), countStorageFilesByIds(dstShard(), storageFilesToCopy));
        Assert.equals(versionDataToCopy, countRecordsInTableForUid(dstShard(), "version_data"));
    }

    @Test
    public void testCopyingTableWithDependencies() {
        int userActivityToCopySize = countRecordsInTableForUid(srcShard(), "user_activity_info");
        int userIndexToCopySize = countRecordsInTableForUid(srcShard(), "user_index");

        migrator.copyDataAndSwitchShard(UID, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        Assert.equals(userActivityToCopySize, countRecordsInTableForUid(dstShard(), "user_activity_info"));
        Assert.equals(userIndexToCopySize, countRecordsInTableForUid(dstShard(), "user_index"));
    }
}
