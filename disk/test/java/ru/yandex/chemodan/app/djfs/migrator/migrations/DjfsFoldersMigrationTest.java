package ru.yandex.chemodan.app.djfs.migrator.migrations;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

public class DjfsFoldersMigrationTest extends AbstractMigrationTypeTest {
    private final DjfsMigrationPlan migrationPlan = DjfsMigrationPlan.builder()
            .migration(new DjfsFoldersMigration())
            .build();

    @Override
    public void testCheckCopiedWillFailIfSomeRecordIsChanged() {
        checkAllCopiedMustFailIfChangeSomeRecord(
            jdbc -> jdbc.update("update disk.folders set name = 'renamed' where uid = ? and name = ?", UID, "inner folder"),
            jdbc -> jdbc.update("update disk.folders set name = 'renamed' where uid = ? and name = ?", UID, "subinner folder")
        );
    }

    @Override
    protected DjfsMigrationPlan migrationPlan() {
        return migrationPlan;
    }

    @Test
    @Override
    public void testCopyWithMinimalBatchSize() {
        int foldersToCopy = countRecordsInTableForUid(srcShard(), "folders");

        runCopying(UID, c -> c.baseBatchSize(1));

        Assert.equals(foldersToCopy, countRecordsInTableForUid(dstShard(), "folders"));
    }

    @Test
    @Override
    public void testAllTableDataCleaned() {
        testCleanData(shard -> Assert.equals(0, countRecordsInTableForUid(shard, "folders")));
    }

}
