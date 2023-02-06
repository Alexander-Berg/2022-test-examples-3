package ru.yandex.chemodan.app.djfs.migrator.migrations;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.mongodb.ReadPreference;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.test.Assert;

public class DjfsFilesDataMigrationTest extends AbstractMigrationTypeTest {
    private final DjfsMigrationPlan migrationPlan = DjfsMigrationPlan.builder()
            .withSelfReference("folders", "fid", "parent_fid")
            .simple("version_links")
            .migration(new DjfsFilesDataMigration())
            .migration(new DjfsAdditionalFileLinksMigration())
            .build();

    @Override
    protected DjfsMigrationPlan migrationPlan() {
        return migrationPlan;
    }

    @Override
    public void testCheckCopiedWillFailIfSomeRecordIsChanged() {
        checkAllCopiedMustFailIfChangeSomeRecord(
            jdbc -> jdbc.update("update disk.files set name = 'renamed.jpg' where uid = ? and name = ?", UID, "1.jpg"),
            jdbc -> jdbc.update("update disk.version_data set platform_created = 'another' where uid = ? and platform_created = ?", UID, "web")
        );
    }

    @Test
    @Override
    public void testCopyWithMinimalBatchSize() {
        int filesToCopy = countRecordsInTableForUid(srcShard(), "files");
        ListF<UUID> storageFilesToCopy = idsOfStorageFilesForUid(srcShard());
        int versionDataToCopy = countRecordsInTableForUid(srcShard(), "version_data");

        runCopying(UID, c -> c.baseBatchSize(1));

        Assert.equals(filesToCopy, countRecordsInTableForUid(dstShard(), "files"));
        Assert.equals(storageFilesToCopy.size(), countStorageFilesByIds(dstShard(), storageFilesToCopy));
        Assert.equals(versionDataToCopy, countRecordsInTableForUid(dstShard(), "version_data"));
    }

    @Test
    @Override
    public void testAllTableDataCleaned() {
        ListF<UUID> storageFilesToCopy = idsOfStorageFilesForUid(srcShard());
        testCleanData(shard -> {
            Assert.equals(0, countRecordsInTableForUid(shard, "files"));
            Assert.equals(0, countStorageFilesByIds(shard, storageFilesToCopy));
            Assert.equals(0, countRecordsInTableForUid(shard, "version_data"));
        });
    }

    @Test
    public void testCleanDataIfStorageFileUsedByAnotherUser() {
        ListF<UUID> storageFilesToCopy = idsOfStorageFilesForUid(srcShard());

        DjfsUid anotherUid = DjfsUid.cons(113131);
        initializePgUser(anotherUid, PG_SHARDS[SRC_PG_SHARD_ID]);

        DjfsResourcePath filePath = DjfsResourcePath.cons(anotherUid, "/disk/test.jpg");
        filesystem.createFile(DjfsPrincipal.cons(anotherUid), filePath, x ->
                x.hid(UuidUtils.toHexString(storageFilesToCopy.get(0)))
        );


        for (DjfsTableMigration migration : migrationPlan.getMigrations().reverse()) {
            migration.cleanData(srcShard(), UID, 1);
        }

        Assert.equals(0, countRecordsInTableForUid(srcShard(), "files"));
        Assert.equals(1, countStorageFilesByIds(srcShard(), storageFilesToCopy));
        Assert.equals(0, countRecordsInTableForUid(srcShard(), "version_data"));
    }

    @Test
    public void shouldNotOverrideStidOnCopyIfConflicts() {
        UUID storageId = UUID.randomUUID();
        String storageIdHex = UuidUtils.toHexString(storageId);
        String stidOnSourceShard = "1231231231";
        String stidOnDestinationShard = "1232131231231123213";

        String filePath = "/disk/test.jpg";
        DjfsUid userToMigrate = createUserWithFile(
                SRC_PG_SHARD_ID, filePath, x -> x.hid(storageIdHex).fileStid(stidOnSourceShard)
        );

        createUserWithFile(DST_PG_SHARD_ID, "/disk/some.jpg", x -> x.hid(storageIdHex).fileStid(stidOnDestinationShard));

        migrator.copyDataAndSwitchShard(userToMigrate, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        String hid = filesystem.find(DjfsPrincipal.cons(userToMigrate), DjfsResourcePath.cons(userToMigrate, filePath), Option.of(ReadPreference.primary()))
                .map(FileDjfsResource.class::cast)
                .getOrThrow(AssertionError::new)
                .getFileStid();

        Assert.equals(stidOnDestinationShard, hid);
    }

    @Test
    public void shouldSaveDuplicatedStidOnCopy() {
        UUID storageId = UUID.randomUUID();
        String storageIdHex = UuidUtils.toHexString(storageId);
        String stidOnSourceShard = "1231231231";
        String stidOnDestinationShard = "1232131231231123213";

        DjfsUid userToMigrate = createUserWithFile(
                SRC_PG_SHARD_ID, "/disk/test.jpg", x -> x.hid(storageIdHex).fileStid(stidOnSourceShard)
        );

        createUserWithFile(DST_PG_SHARD_ID, "/disk/some.jpg", x -> x.hid(storageIdHex).fileStid(stidOnDestinationShard));

        migrator.copyDataAndSwitchShard(userToMigrate, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        SetF<Tuple2<UUID, String>> savedDuplicates = dstShard().query(
                "SELECT storage_id, stid FROM disk.duplicated_storage_files",
                (rs, i) -> Tuple2.tuple(UUID.fromString(rs.getString("storage_id")), rs.getString("stid"))
        ).unique();

        Assert.equals(
                Cf.set(Tuple2.tuple(storageId, stidOnSourceShard)),
                savedDuplicates
        );
    }

    @Test
    public void shouldCopyPreviousDuplicatedStid() {
        UUID storageId = UUID.randomUUID();
        String storageIdHex = UuidUtils.toHexString(storageId);
        String stidInStorageTable = "1231231231";
        String previousStidDuplicate = "123123123112313131321";

        DjfsUid userToMigrate = createUserWithFile(
                SRC_PG_SHARD_ID, "/disk/test.jpg", x -> x.hid(storageIdHex).fileStid(stidInStorageTable)
        );

        srcShard().update(
                "INSERT INTO disk.duplicated_storage_files(storage_id, stid) VALUES (?, ?)",
                storageId, previousStidDuplicate
        );

        migrator.copyDataAndSwitchShard(userToMigrate, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        SetF<Tuple2<UUID, String>> savedDuplicates = dstShard().query(
                "SELECT storage_id, stid FROM disk.duplicated_storage_files",
                (rs, i) -> Tuple2.tuple(UUID.fromString(rs.getString("storage_id")), rs.getString("stid"))
        ).unique();

        Assert.equals(
                Cf.set(Tuple2.tuple(storageId, previousStidDuplicate)),
                savedDuplicates
        );
    }

    @Test
    public void copyPreviousDuplicatedStidWhenItAlreadyDuplicatedOnDestinationShard() {
        UUID storageId = UUID.randomUUID();
        String storageIdHex = UuidUtils.toHexString(storageId);
        String stidInStorageTable = "1231231231";
        String previousStidDuplicate = "123123123112313131321";

        DjfsUid userToMigrate = createUserWithFile(
                SRC_PG_SHARD_ID, "/disk/test.jpg", x -> x.hid(storageIdHex).fileStid(stidInStorageTable)
        );

        srcShard().update(
                "INSERT INTO disk.duplicated_storage_files(storage_id, stid) VALUES (?, ?)",
                storageId, previousStidDuplicate
        );

        createUserWithFile(
                DST_PG_SHARD_ID, "/disk/test.jpg", x -> x.hid(storageIdHex).fileStid(stidInStorageTable)
        );
        dstShard().update(
                "INSERT INTO disk.duplicated_storage_files(storage_id, stid) VALUES (?, ?)",
                storageId, previousStidDuplicate
        );

        migrator.copyDataAndSwitchShard(userToMigrate, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        SetF<Tuple2<UUID, String>> savedDuplicates = dstShard().query(
                "SELECT storage_id, stid FROM disk.duplicated_storage_files",
                (rs, i) -> Tuple2.tuple(UUID.fromString(rs.getString("storage_id")), rs.getString("stid"))
        ).unique();

        Assert.equals(
                Cf.set(Tuple2.tuple(storageId, previousStidDuplicate)),
                savedDuplicates
        );
    }

    @Test
    public void shouldCopyPreviousDuplicatedStidWithNewConflict() {
        UUID storageId = UUID.randomUUID();
        String storageIdHex = UuidUtils.toHexString(storageId);
        String stidInSourceShard = "1231231231";
        String stidOnDestinationShard = "12312312317897965465478";
        String previousStidDuplicate = "123123123112313131321";

        String filePath = "/disk/test.jpg";
        DjfsUid userToMigrate = createUserWithFile(
                SRC_PG_SHARD_ID, filePath, x -> x.hid(storageIdHex).fileStid(stidInSourceShard)
        );

        createUserWithFile(DST_PG_SHARD_ID, "/disk/some.jpg", x -> x.hid(storageIdHex).fileStid(stidOnDestinationShard));

        srcShard().update(
                "INSERT INTO disk.duplicated_storage_files(storage_id, stid) VALUES (?, ?)",
                storageId, previousStidDuplicate
        );

        migrator.copyDataAndSwitchShard(userToMigrate, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        SetF<Tuple2<UUID, String>> savedDuplicates = dstShard().query(
                "SELECT storage_id, stid FROM disk.duplicated_storage_files",
                (rs, i) -> Tuple2.tuple(UUID.fromString(rs.getString("storage_id")), rs.getString("stid"))
        ).unique();

        Assert.equals(
                Cf.set(
                        Tuple2.tuple(storageId, previousStidDuplicate),
                        Tuple2.tuple(storageId, stidInSourceShard)
                ),
                savedDuplicates
        );
    }

    @Test
    public void shouldNotSaveDuplicatedStidsOnCopyIfDifferentStorageIds() {
        String stidOnSourceShard = "1231231231";
        String stidOnDestinationShard = "1232131231231123213";

        String filePath = "/disk/test.jpg";
        DjfsUid userToMigrate = createUserWithFile(
                SRC_PG_SHARD_ID, filePath, x -> x.fileStid(stidOnSourceShard)
        );

        createUserWithFile(DST_PG_SHARD_ID, "/disk/some.jpg", x -> x.fileStid(stidOnDestinationShard));

        migrator.copyDataAndSwitchShard(userToMigrate, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        Integer duplicatesCount = dstShard().queryForObject(
                "SELECT count(*) FROM disk.duplicated_storage_files",
                Integer.class
        );

        Assert.equals(0, duplicatesCount);
    }

    @Test
    public void shouldNotSaveDuplicatedStidsOnCopyIfDifferentNoConflict() {
        UUID storageId = UUID.randomUUID();
        String storageIdHex = UuidUtils.toHexString(storageId);

        String stid = "1231231231";

        String filePath = "/disk/test.jpg";
        DjfsUid userToMigrate = createUserWithFile(
                SRC_PG_SHARD_ID, filePath, x -> x.hid(storageIdHex).fileStid(stid)
        );

        createUserWithFile(DST_PG_SHARD_ID, "/disk/some.jpg", x -> x.hid(storageIdHex).fileStid(stid));

        migrator.copyDataAndSwitchShard(userToMigrate, SRC_PG_SHARD_ID, DST_PG_SHARD_ID, false, DjfsMigrationPlan.allTablesMigrations);

        Integer duplicatesCount = dstShard().queryForObject(
                "SELECT count(*) FROM disk.duplicated_storage_files",
                Integer.class
        );

        Assert.equals(0, duplicatesCount);
    }

    @NotNull
    private DjfsUid createUserWithFile(int shardId, String path,
            Function<FileDjfsResource.Builder, FileDjfsResource.Builder> file) {
        DjfsUid uid = DjfsUid.cons(Math.abs(ThreadLocalRandom.current().nextLong() + 1));
        initializePgUser(uid, PG_SHARDS[shardId]);

        filesystem.createFile(
                DjfsPrincipal.cons(uid), DjfsResourcePath.cons(uid, path),
                file
        );
        return uid;
    }
}
