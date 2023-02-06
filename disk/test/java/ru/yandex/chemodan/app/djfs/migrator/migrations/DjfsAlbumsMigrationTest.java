package ru.yandex.chemodan.app.djfs.migrator.migrations;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

public class DjfsAlbumsMigrationTest extends AbstractMigrationTypeTest {
    private final DjfsMigrationPlan migrationPlan = DjfsMigrationPlan.builder()
            .migration(new DjfsAlbumsMigration())
            .build();

    @Override
    protected DjfsMigrationPlan migrationPlan() {
        return migrationPlan;
    }

    @Test
    @Override
    public void testCopyWithMinimalBatchSize() {
        int albumsToCopy = countRecordsInTableForUid(srcShard(), "albums");
        int albumItemsToCopy = countRecordsInTableForUid(srcShard(), "album_items");

        runCopying(UID, c -> c.baseBatchSize(1));

        Assert.equals(albumsToCopy, countRecordsInTableForUid(dstShard(), "albums"));
        Assert.equals(albumItemsToCopy, countRecordsInTableForUid(dstShard(), "album_items"));
    }

    @Override
    public void testCheckCopiedWillFailIfSomeRecordIsChanged() {
        checkAllCopiedMustFailIfChangeSomeRecord(
            jdbc -> jdbc.update("update disk.albums set description = 'another' where uid = ? and title = ?", UID, "Пример"),
            jdbc -> jdbc.update("update disk.album_items set description = 'another' where uid = ? and order_index = 1", UID)
        );
    }

    @Test
    public void testRestoreAlbumsCover() {
        runCopying(UID);

        Assert.equalsTu(
                getAlbumAndCoverIds(srcShard()),
                getAlbumAndCoverIds(dstShard())
        );
    }

    @Test
    @Override
    public void testAllTableDataCleaned() {
        testCleanData(shard -> {
            Assert.equals(0, countRecordsInTableForUid(shard, "albums"));
            Assert.equals(0, countRecordsInTableForUid(shard, "album_items"));
        });
    }

    private SetF<Tuple2<String, String>> getAlbumAndCoverIds(JdbcTemplate3 srcShard) {
        return srcShard.query(
                "SELECT id, cover_id FROM disk.albums WHERE uid = ?",
                (rs, rowNum) -> Tuple2.tuple(
                        Arrays.toString(rs.getBytes("id")),
                        Arrays.toString(rs.getBytes("cover_id"))
                ),
                UID.asLong()
        ).unique();
    }
}
