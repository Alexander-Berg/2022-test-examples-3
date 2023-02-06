package ru.yandex.chemodan.app.djfs.core.filesystem;

import java.util.UUID;

import com.mongodb.ReadPreference;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author yappo
 */
public class DjfsResourceDaoFindResourcesByFileIdsAndUidTest extends DjfsResourceDaoFindResourceTestBase {
    private static FileDjfsResource F2 = FileDjfsResource.random(ROOT, "f2");
    private static FolderDjfsResource D2 = FolderDjfsResource.cons(ROOT, "d2");
    private static FileDjfsResource TF2 = FileDjfsResource.random(TRASH_ROOT, "tf2");
    private static FolderDjfsResource TD2 = FolderDjfsResource.cons(TRASH_ROOT, "d2_trash");

    private static final FolderDjfsResource PHOTOUNLIM_ROOT = FolderDjfsResource.cons(UID, "/photounlim");
    private static FileDjfsResource F1_PHOTOUNLIM_DUP = FileDjfsResource.random(PHOTOUNLIM_ROOT,
            "f1_photounlim_dup", x -> x.fileId(F1.getFileId()));
    private static FileDjfsResource F2_PHOTOUNLIM_DUP = FileDjfsResource.random(PHOTOUNLIM_ROOT,
            "f2_photounlim_dup", x -> x.fileId(F2.getFileId()));
    private static FolderDjfsResource D1_PHOTOUNLIM_DUP = FolderDjfsResource.cons(PHOTOUNLIM_ROOT, "d1_photounlim_dup",
            x -> x.fileId(D1.getFileId()));
    private static FolderDjfsResource D2_PHOTOUNLIM_DUP = FolderDjfsResource.cons(PHOTOUNLIM_ROOT, "d2_photounlim_dup",
            x -> x.fileId(D2.getFileId()));

    private static FolderDjfsResource D1_PHOTOUNLIM_HIGHEST_VERSION =
            FolderDjfsResource.cons(PHOTOUNLIM_ROOT, "d1_photounlim_dup_highest_version",
                    x -> x.fileId(D1.getFileId()).version(Option.of(InstantUtils.toVersion(
                            Instant.now()) + 1000)));

    @Test
    public void findSeveralFilesInEnabledAreas() {
        djfsResourceDao.insert(UID, F1, F2);
        ListF<DjfsResourceId> resourceIds = Cf.list(F1, F2).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.assertContainsAll(files.map(x -> x.getResourceId().get().getFileId()),
                Cf.list(F1, F2).map(x -> x.getResourceId().get().getFileId()));
    }

    @Test
    public void doNotFindFilesThatAreNotFromEnabledAreas() {
        djfsResourceDao.insert(UID, F1, TF2);
        ListF<DjfsResourceId> resourceIds = Cf.list(F1, TF2).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.assertContainsAll(files.map(x -> x.getResourceId().get().getFileId()),
                Cf.list(F1).map(x -> x.getResourceId().get().getFileId()));
    }

    @Test
    public void findSeveralFilesButSomeHaveDuplicateFileIds() {
        djfsResourceDao.insert(UID, F1, F1_DUP, F2, F2_PHOTOUNLIM_DUP);
        ListF<DjfsResourceId> resourceIds = Cf.list(F1, F2).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.sizeIs(2, files);
        Assert.assertContainsAll(files.map(x -> x.getResourceId().get().getFileId()),
                Cf.list(F1, F2).map(x -> x.getResourceId().get().getFileId()));
    }

    @Test
    public void findOneFileThatHasSeveralDuplicatesWithMinId() {
        djfsResourceDao.insert(UID, F1, F1_DUP, F1_PHOTOUNLIM_DUP);
        ListF<DjfsResourceId> resourceIds = Cf.list(F1).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.sizeIs(1, files);
        DjfsResource file = files.first();
        Assert.equals(Cf.list(F1, F1_DUP, F1_PHOTOUNLIM_DUP).map(DjfsResource::getId).minBy(UUID::toString),
                file.getId());
    }

    @Test
    public void cannotFindNonexistentFiles() {
        ListF<DjfsResourceId> resourceIds = Cf.list(F1, F2).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.isEmpty(files);
    }

    @Test
    public void findSeveralFoldersInEnabledAreas() {
        djfsResourceDao.insert(UID, D1, D2);
        ListF<DjfsResourceId> resourceIds = Cf.list(D1, D2).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.assertContainsAll(files.map(x -> x.getResourceId().get().getFileId()),
                Cf.list(D1, D2).map(x -> x.getResourceId().get().getFileId()));
    }

    @Test
    public void findOneFolderInEnabledAreaAndOtherInDisabled() {
        djfsResourceDao.insert(UID, D1, TD2);
        ListF<DjfsResourceId> resourceIds = Cf.list(D1, TD2).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.assertContainsAll(files.map(x -> x.getResourceId().get().getFileId()),
                Cf.list(D1).map(x -> x.getResourceId().get().getFileId()));
    }

    @Test
    public void findSeveralFoldersButSomeHaveDuplicateFileIds() {
        djfsResourceDao.insert(UID, D1, D1_DUP, D2, D2_PHOTOUNLIM_DUP);
        ListF<DjfsResourceId> resourceIds = Cf.list(D1, D2).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.sizeIs(2, files);
        Assert.assertContainsAll(files.map(x -> x.getResourceId().get().getFileId()),
                Cf.list(D1, D2).map(x -> x.getResourceId().get().getFileId()));
    }

    @Test
    public void ifThereAreServeralFoldersWithTheSameFileIdHaveToFetchOneWithHighestVersion() {
        djfsResourceDao.insert(UID, D1, D1_DUP, D1_PHOTOUNLIM_DUP, D1_PHOTOUNLIM_HIGHEST_VERSION);
        ListF<DjfsResourceId> resourceIds = Cf.list(D1).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.sizeIs(1, files);
        DjfsResource file = files.first();
        Assert.equals(Cf.list(D1, D1_DUP, D1_PHOTOUNLIM_DUP, D1_PHOTOUNLIM_HIGHEST_VERSION).maxBy(x -> x.getVersion().get()).getPath(), file.getPath());
    }

    @Test
    public void findFoldersButTheyAreMissing() {
        ListF<DjfsResourceId> resourceIds = Cf.list(D1, D2).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.isEmpty(files);
    }

    @Test
    public void findFileAndFolderInEnabledAreas() {
        djfsResourceDao.insert(UID, F1);
        djfsResourceDao.insert(UID, D1);
        ListF<DjfsResourceId> resourceIds = Cf.list(F1, D1).map(x -> x.getResourceId().get());
        ListF<DjfsResource> files = filesystem.find(PRINCIPAL, resourceIds, defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.assertContainsAll(files.map(x -> x.getResourceId().get().getFileId()),
                Cf.list(F1, D1).map(x -> x.getResourceId().get().getFileId()));
    }
}
