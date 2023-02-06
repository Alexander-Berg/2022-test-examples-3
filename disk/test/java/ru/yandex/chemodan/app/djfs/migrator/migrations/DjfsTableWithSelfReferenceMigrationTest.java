package ru.yandex.chemodan.app.djfs.migrator.migrations;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

public class DjfsTableWithSelfReferenceMigrationTest extends AbstractMigrationTypeTest {
    private final DjfsMigrationPlan migrationPlan = DjfsMigrationPlan.builder()
            .withSelfReference("disk_info", "id", "parent")
            .build();

    @Override
    public void testCheckCopiedWillFailIfSomeRecordIsChanged() {
        checkAllCopiedMustFailIfChangeSomeRecord(
            jdbc -> jdbc.update("update disk.disk_info set version = version + 1 where uid = ? and path = ?", UID, "/limit"),
            jdbc -> jdbc.update("update disk.disk_info set version = version + 1 where uid = ? and path = ?", UID, "/total_size")
        );
    }

    @Override
    protected DjfsMigrationPlan migrationPlan() {
        return migrationPlan;
    }

    @Test
    @Override
    public void testCopyWithMinimalBatchSize() {
        int diskInfoToCopy = countRecordsInTableForUid(srcShard(), "disk_info");

        runCopying(UID, c -> c.baseBatchSize(1));

        Assert.equals(diskInfoToCopy, countRecordsInTableForUid(dstShard(), "disk_info"));
    }

    @Test
    @Override
    public void testAllTableDataCleaned() {
        testCleanData(shard -> Assert.equals(0, countRecordsInTableForUid(shard, "disk_info")));
    }

}
