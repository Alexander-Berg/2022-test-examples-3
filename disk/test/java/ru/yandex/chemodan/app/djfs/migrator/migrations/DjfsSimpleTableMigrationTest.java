package ru.yandex.chemodan.app.djfs.migrator.migrations;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

public class DjfsSimpleTableMigrationTest extends AbstractMigrationTypeTest {
    private final DjfsMigrationPlan migrationPlan = DjfsMigrationPlan.builder()
            .simple("user_index")
            .build();

    @Override
    protected DjfsMigrationPlan migrationPlan() {
        return migrationPlan;
    }

    @Override
    public void testCheckCopiedWillFailIfSomeRecordIsChanged() {
        checkAllCopiedMustFailIfChangeSomeRecord(
            jdbc -> jdbc.update("update disk.user_index set version = version + 1 where uid = ?", UID)
        );
    }

    @Test
    @Override
    public void testCopyWithMinimalBatchSize() {
        int userIndexToCopy = countRecordsInTableForUid(srcShard(), "user_index");

        runCopying(UID, c -> c.baseBatchSize(1));

        Assert.equals(userIndexToCopy, countRecordsInTableForUid(dstShard(), "user_index"));
    }

    @Test
    @Override
    public void testAllTableDataCleaned() {
        testCleanData(shard -> Assert.equals(0, countRecordsInTableForUid(shard, "user_index")));
    }
}
