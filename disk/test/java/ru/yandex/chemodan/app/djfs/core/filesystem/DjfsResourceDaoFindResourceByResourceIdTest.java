package ru.yandex.chemodan.app.djfs.core.filesystem;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.test.Assert;

public class DjfsResourceDaoFindResourceByResourceIdTest extends DjfsSingleUserTestBase {
    private static final DjfsFileId FA = DjfsFileId.random();
    private static final DjfsFileId FB = DjfsFileId.random();
    private static final DjfsFileId DA = DjfsFileId.random();
    private static final DjfsFileId DB = DjfsFileId.random();
    private static final DjfsResourceId RESOURCE_FA = DjfsResourceId.cons(UID, FA);
    private static final DjfsResourceId RESOURCE_FB = DjfsResourceId.cons(UID, FB);
    private static final DjfsResourceId RESOURCE_DA = DjfsResourceId.cons(UID, DA);
    private static final DjfsResourceId RESOURCE_DB = DjfsResourceId.cons(UID, DB);

    private static final ListF<DjfsResourceArea> AREAS_DISK = Cf.list(DjfsResourceArea.DISK);
    private static final ListF<DjfsResourceArea> AREAS_DISK_TRASH = Cf.list(DjfsResourceArea.DISK,
            DjfsResourceArea.TRASH);

    private static final FolderDjfsResource DISK = FolderDjfsResource.cons(UID, "/disk");
    private static final FolderDjfsResource TRASH = FolderDjfsResource.cons(UID, "/trash");

    private static FolderDjfsResource DISK_D = FolderDjfsResource.cons(DISK, "d");
    private static FolderDjfsResource DISK_D_D = FolderDjfsResource.cons(DISK_D, "d");
    private static FolderDjfsResource TRASH_D = FolderDjfsResource.cons(TRASH, "d");
    private static FolderDjfsResource TRASH_D_D = FolderDjfsResource.cons(TRASH_D, "d");

    private static FolderDjfsResource DISK_DA1 = FolderDjfsResource.cons(DISK, "da1", x -> x.fileId(DA));
    private static FolderDjfsResource DISK_DA2 = FolderDjfsResource.cons(DISK, "da2", x -> x.fileId(DA));
    private static FolderDjfsResource DISK_DB1 = FolderDjfsResource.cons(DISK, "db1", x -> x.fileId(DB));
    private static FolderDjfsResource DISK_DB2 = FolderDjfsResource.cons(DISK, "db2", x -> x.fileId(DB));
    private static FolderDjfsResource DISK_D_DA1 = FolderDjfsResource.cons(DISK_D, "da1", x -> x.fileId(DA));
    private static FolderDjfsResource DISK_D_DA2 = FolderDjfsResource.cons(DISK_D, "da2", x -> x.fileId(DA));
    private static FolderDjfsResource DISK_D_DB1 = FolderDjfsResource.cons(DISK_D, "db1", x -> x.fileId(DB));
    private static FolderDjfsResource DISK_D_DB2 = FolderDjfsResource.cons(DISK_D, "db2", x -> x.fileId(DB));
    private static FolderDjfsResource DISK_D_D_DA1 = FolderDjfsResource.cons(DISK_D_D, "da1", x -> x.fileId(DA));
    private static FolderDjfsResource DISK_D_D_DA2 = FolderDjfsResource.cons(DISK_D_D, "da2", x -> x.fileId(DA));
    private static FolderDjfsResource DISK_D_D_DB1 = FolderDjfsResource.cons(DISK_D_D, "db1", x -> x.fileId(DB));
    private static FolderDjfsResource DISK_D_D_DB2 = FolderDjfsResource.cons(DISK_D_D, "db2", x -> x.fileId(DB));

    private static FileDjfsResource DISK_FA1 = FileDjfsResource.random(DISK, "fa1", x -> x.fileId(FA));
    private static FileDjfsResource DISK_FA2 = FileDjfsResource.random(DISK, "fa2", x -> x.fileId(FA));
    private static FileDjfsResource DISK_FB1 = FileDjfsResource.random(DISK, "fb1", x -> x.fileId(FB));
    private static FileDjfsResource DISK_FB2 = FileDjfsResource.random(DISK, "fb2", x -> x.fileId(FB));
    private static FileDjfsResource DISK_D_FA1 = FileDjfsResource.random(DISK_D, "fa1", x -> x.fileId(FA));
    private static FileDjfsResource DISK_D_FA2 = FileDjfsResource.random(DISK_D, "fa2", x -> x.fileId(FA));
    private static FileDjfsResource DISK_D_FB1 = FileDjfsResource.random(DISK_D, "fb1", x -> x.fileId(FB));
    private static FileDjfsResource DISK_D_FB2 = FileDjfsResource.random(DISK_D, "fb2", x -> x.fileId(FB));
    private static FileDjfsResource DISK_D_D_FA1 = FileDjfsResource.random(DISK_D_D, "fa1", x -> x.fileId(FA));
    private static FileDjfsResource DISK_D_D_FA2 = FileDjfsResource.random(DISK_D_D, "fa2", x -> x.fileId(FA));
    private static FileDjfsResource DISK_D_D_FB1 = FileDjfsResource.random(DISK_D_D, "fb1", x -> x.fileId(FB));
    private static FileDjfsResource DISK_D_D_FB2 = FileDjfsResource.random(DISK_D_D, "fb2", x -> x.fileId(FB));

    private static FolderDjfsResource TRASH_DA1 = FolderDjfsResource.cons(TRASH, "da1", x -> x.fileId(DA));
    private static FolderDjfsResource TRASH_DA2 = FolderDjfsResource.cons(TRASH, "da2", x -> x.fileId(DA));
    private static FolderDjfsResource TRASH_DB1 = FolderDjfsResource.cons(TRASH, "db1", x -> x.fileId(DB));
    private static FolderDjfsResource TRASH_DB2 = FolderDjfsResource.cons(TRASH, "db2", x -> x.fileId(DB));
    private static FolderDjfsResource TRASH_D_DA1 = FolderDjfsResource.cons(TRASH_D, "da1", x -> x.fileId(DA));
    private static FolderDjfsResource TRASH_D_DA2 = FolderDjfsResource.cons(TRASH_D, "da2", x -> x.fileId(DA));
    private static FolderDjfsResource TRASH_D_DB1 = FolderDjfsResource.cons(TRASH_D, "db1", x -> x.fileId(DB));
    private static FolderDjfsResource TRASH_D_DB2 = FolderDjfsResource.cons(TRASH_D, "db2", x -> x.fileId(DB));
    private static FolderDjfsResource TRASH_D_D_DA1 = FolderDjfsResource.cons(TRASH_D_D, "da1", x -> x.fileId(DA));
    private static FolderDjfsResource TRASH_D_D_DA2 = FolderDjfsResource.cons(TRASH_D_D, "da2", x -> x.fileId(DA));
    private static FolderDjfsResource TRASH_D_D_DB1 = FolderDjfsResource.cons(TRASH_D_D, "db1", x -> x.fileId(DB));
    private static FolderDjfsResource TRASH_D_D_DB2 = FolderDjfsResource.cons(TRASH_D_D, "db2", x -> x.fileId(DB));

    private static FileDjfsResource TRASH_FA1 = FileDjfsResource.random(TRASH, "fa1", x -> x.fileId(FA));
    private static FileDjfsResource TRASH_FA2 = FileDjfsResource.random(TRASH, "fa2", x -> x.fileId(FA));
    private static FileDjfsResource TRASH_FB1 = FileDjfsResource.random(TRASH, "fb1", x -> x.fileId(FB));
    private static FileDjfsResource TRASH_FB2 = FileDjfsResource.random(TRASH, "fb2", x -> x.fileId(FB));
    private static FileDjfsResource TRASH_D_FA1 = FileDjfsResource.random(TRASH_D, "fa1", x -> x.fileId(FA));
    private static FileDjfsResource TRASH_D_FA2 = FileDjfsResource.random(TRASH_D, "fa2", x -> x.fileId(FA));
    private static FileDjfsResource TRASH_D_FB1 = FileDjfsResource.random(TRASH_D, "fb1", x -> x.fileId(FB));
    private static FileDjfsResource TRASH_D_FB2 = FileDjfsResource.random(TRASH_D, "fb2", x -> x.fileId(FB));
    private static FileDjfsResource TRASH_D_D_FA1 = FileDjfsResource.random(TRASH_D_D, "fa1", x -> x.fileId(FA));
    private static FileDjfsResource TRASH_D_D_FA2 = FileDjfsResource.random(TRASH_D_D, "fa2", x -> x.fileId(FA));
    private static FileDjfsResource TRASH_D_D_FB1 = FileDjfsResource.random(TRASH_D_D, "fb1", x -> x.fileId(FB));
    private static FileDjfsResource TRASH_D_D_FB2 = FileDjfsResource.random(TRASH_D_D, "fb2", x -> x.fileId(FB));

    @Override
    @Before
    public void setUp() {
        super.setUp();
        djfsResourceDao.insert(UID, DISK_D, DISK_D_D, TRASH_D, TRASH_D_D);
    }

    private void assertEquivalentIds(ListF<DjfsResource> expected, ListF<DjfsResource> actual) {
        Assert.equals(
                expected.toMap(DjfsResource::getId, x -> x).keySet(),
                actual.toMap(DjfsResource::getId, x -> x).keySet()
        );
    }

    @Test
    public void singleFile() {
        djfsResourceDao.insert(UID, DISK_D_FA1, DISK_D_FB1);
        ListF<DjfsResource> expected = Cf.list(DISK_D_FA1);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_FA.getFileId().getValue()), AREAS_DISK);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void singleFolder() {
        djfsResourceDao.insert(UID, DISK_D_DA1, DISK_D_DB1);
        ListF<DjfsResource> expected = Cf.list(DISK_D_DA1);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_DA.getFileId().getValue()), AREAS_DISK);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void manyFiles() {
        djfsResourceDao.insert(UID, Cf.list(DISK_DA1, DISK_DA2, DISK_DB1, DISK_DB2, DISK_D_DA1, DISK_D_DA2, DISK_D_DB1,
                DISK_D_DB2, DISK_D_D_DA1, DISK_D_D_DA2, DISK_D_D_DB1, DISK_D_D_DB2, DISK_FA1, DISK_FA2, DISK_FB1,
                DISK_FB2, DISK_D_FA1, DISK_D_FA2, DISK_D_FB1, DISK_D_FB2, DISK_D_D_FA1, DISK_D_D_FA2, DISK_D_D_FB1,
                DISK_D_D_FB2, TRASH_DA1, TRASH_DA2, TRASH_DB1, TRASH_DB2, TRASH_D_DA1, TRASH_D_DA2, TRASH_D_DB1,
                TRASH_D_DB2, TRASH_D_D_DA1, TRASH_D_D_DA2, TRASH_D_D_DB1, TRASH_D_D_DB2, TRASH_FA1, TRASH_FA2,
                TRASH_FB1, TRASH_FB2, TRASH_D_FA1, TRASH_D_FA2, TRASH_D_FB1, TRASH_D_FB2, TRASH_D_D_FA1, TRASH_D_D_FA2,
                TRASH_D_D_FB1, TRASH_D_D_FB2)
        );
        ListF<DjfsResource> expected = Cf.list(DISK_FA1, DISK_FA2, DISK_D_FA1, DISK_D_FA2, DISK_D_D_FA1, DISK_D_D_FA2,
                TRASH_FA1, TRASH_FA2, TRASH_D_FA1, TRASH_D_FA2, TRASH_D_D_FA1, TRASH_D_D_FA2);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_FA.getFileId().getValue()), AREAS_DISK_TRASH);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void manyFolders() {
        djfsResourceDao.insert(UID, Cf.list(DISK_DA1, DISK_DA2, DISK_DB1, DISK_DB2, DISK_D_DA1, DISK_D_DA2, DISK_D_DB1,
                DISK_D_DB2, DISK_D_D_DA1, DISK_D_D_DA2, DISK_D_D_DB1, DISK_D_D_DB2, DISK_FA1, DISK_FA2, DISK_FB1,
                DISK_FB2, DISK_D_FA1, DISK_D_FA2, DISK_D_FB1, DISK_D_FB2, DISK_D_D_FA1, DISK_D_D_FA2, DISK_D_D_FB1,
                DISK_D_D_FB2, TRASH_DA1, TRASH_DA2, TRASH_DB1, TRASH_DB2, TRASH_D_DA1, TRASH_D_DA2, TRASH_D_DB1,
                TRASH_D_DB2, TRASH_D_D_DA1, TRASH_D_D_DA2, TRASH_D_D_DB1, TRASH_D_D_DB2, TRASH_FA1, TRASH_FA2,
                TRASH_FB1, TRASH_FB2, TRASH_D_FA1, TRASH_D_FA2, TRASH_D_FB1, TRASH_D_FB2, TRASH_D_D_FA1, TRASH_D_D_FA2,
                TRASH_D_D_FB1, TRASH_D_D_FB2)
        );
        ListF<DjfsResource> expected = Cf.list(DISK_DA1, DISK_DA2, DISK_D_DA1, DISK_D_DA2, DISK_D_D_DA1, DISK_D_D_DA2,
                TRASH_DA1, TRASH_DA2, TRASH_D_DA1, TRASH_D_DA2, TRASH_D_D_DA1, TRASH_D_D_DA2);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_DA.getFileId().getValue()), AREAS_DISK_TRASH);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void doesNotReturnFilesFromNotSearchedAreas() {
        djfsResourceDao.insert(UID, DISK_D_FA1, TRASH_D_FA1);
        ListF<DjfsResource> expected = Cf.list(DISK_D_FA1);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_FA.getFileId().getValue()), AREAS_DISK);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void doesNotReturnFoldersFromNotSearchedAreas() {
        djfsResourceDao.insert(UID, DISK_D_DA1, TRASH_D_DA1);
        ListF<DjfsResource> expected = Cf.list(DISK_D_DA1);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_DA.getFileId().getValue()), AREAS_DISK);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void returnsFilesFromDifferentSearchedAreas() {
        djfsResourceDao.insert(UID, DISK_D_FA1, TRASH_D_FA1);
        ListF<DjfsResource> expected = Cf.list(DISK_D_FA1, TRASH_D_FA1);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_FA.getFileId().getValue()), AREAS_DISK_TRASH);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void returnsFoldersFromDifferentSearchedAreas() {
        djfsResourceDao.insert(UID, DISK_D_DA1, TRASH_D_DA1);
        ListF<DjfsResource> expected = Cf.list(DISK_D_DA1, TRASH_D_DA1);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_DA.getFileId().getValue()), AREAS_DISK_TRASH);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void returnsFilesFromDifferentParents() {
        djfsResourceDao.insert(UID, DISK_D_FA1, DISK_D_D_FA1);
        ListF<DjfsResource> expected = Cf.list(DISK_D_FA1, DISK_D_D_FA1);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_FA.getFileId().getValue()), AREAS_DISK);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void returnsFoldersFromDifferentParents() {
        djfsResourceDao.insert(UID, DISK_D_DA1, DISK_D_D_DA1);
        ListF<DjfsResource> expected = Cf.list(DISK_D_DA1, DISK_D_D_DA1);
        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_DA.getFileId().getValue()), AREAS_DISK);
        ListF<DjfsResource> actual = result.map(Tuple2::get1);

        assertEquivalentIds(expected, actual);
    }

    @Test
    public void checkFolderParentIds() {
        djfsResourceDao.insert(UID, DISK_D_D_DA1);

        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_DA.getFileId().getValue()), AREAS_DISK);
        Assert.sizeIs(1, result);

        Tuple2<DjfsResource, ListF<UUID>> folderWithParentIds = result.get(0);
        util.fs.assertParentIdsUuid(folderWithParentIds.get2(), folderWithParentIds.get1().getPath());
    }

    @Test
    public void checkFileParentIds() {
        djfsResourceDao.insert(UID, DISK_D_D_FA1);

        ListF<Tuple2<DjfsResource, ListF<UUID>>> result =
                djfsResourceDao.findWithParentIds(UID, Cf.list(RESOURCE_FA.getFileId().getValue()), AREAS_DISK);
        Assert.sizeIs(1, result);

        Tuple2<DjfsResource, ListF<UUID>> fileWithParentIds = result.get(0);
        util.fs.assertParentIdsUuid(fileWithParentIds.get2(), fileWithParentIds.get1().getPath());
    }

    @Test
    public void checkSeveralFolderParentIds() {
        djfsResourceDao.insert(UID, DISK_D_D_DA1);
        djfsResourceDao.insert(UID, TRASH_D_D_DB1);

        ListF<Tuple2<DjfsResource, ListF<UUID>>> result = djfsResourceDao.findWithParentIds(
                UID, Cf.list(RESOURCE_DA, RESOURCE_DB).map(x -> x.getFileId().getValue()), AREAS_DISK_TRASH
        );
        Assert.sizeIs(2, result);

        for (Tuple2<DjfsResource, ListF<UUID>> folderWithParentIds : result) {
            util.fs.assertParentIdsUuid(folderWithParentIds.get2(), folderWithParentIds.get1().getPath());
        }
    }

    @Test
    public void checkSeveralFileParentIds() {
        djfsResourceDao.insert(UID, DISK_D_D_FA1);
        djfsResourceDao.insert(UID, TRASH_D_D_FB1);

        ListF<Tuple2<DjfsResource, ListF<UUID>>> result = djfsResourceDao.findWithParentIds(
                UID, Cf.list(RESOURCE_FA, RESOURCE_FB).map(x -> x.getFileId().getValue()), AREAS_DISK_TRASH
        );
        Assert.sizeIs(2, result);

        for (Tuple2<DjfsResource, ListF<UUID>> folderWithParentIds : result) {
            util.fs.assertParentIdsUuid(folderWithParentIds.get2(), folderWithParentIds.get1().getPath());
        }
    }
}
