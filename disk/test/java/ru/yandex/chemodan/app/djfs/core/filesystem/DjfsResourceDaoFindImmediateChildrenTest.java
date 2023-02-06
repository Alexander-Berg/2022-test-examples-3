package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class DjfsResourceDaoFindImmediateChildrenTest extends DjfsSingleUserTestBase {
    private static final FolderDjfsResource ROOT = FolderDjfsResource.cons(UID, "/disk");

    private static FolderDjfsResource D1 = FolderDjfsResource.cons(ROOT, "d1",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("1", 64))));
    private static FolderDjfsResource D2 = FolderDjfsResource.cons(ROOT, "d2",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("2", 64))));
    private static FolderDjfsResource D3 = FolderDjfsResource.cons(ROOT, "d3",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("3", 64))));
    private static FolderDjfsResource D4 = FolderDjfsResource.cons(ROOT, "d4",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("4", 64))));
    private static FolderDjfsResource D5 = FolderDjfsResource.cons(ROOT, "d5",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("5", 64))));

    private static final FolderDjfsResource D1_D1 = FolderDjfsResource.cons(D1, "d1");
    private static final FolderDjfsResource D1_D2 = FolderDjfsResource.cons(D1, "d2");
    private static final FolderDjfsResource D1_D3 = FolderDjfsResource.cons(D1, "d3");

    private static final FolderDjfsResource D2_D1 = FolderDjfsResource.cons(D2, "d1");
    private static final FolderDjfsResource D2_D2 = FolderDjfsResource.cons(D2, "d2");
    private static final FolderDjfsResource D2_D3 = FolderDjfsResource.cons(D2, "d3");

    private static final FolderDjfsResource D3_D1 = FolderDjfsResource.cons(D3, "d1");
    private static final FolderDjfsResource D3_D2 = FolderDjfsResource.cons(D3, "d2");
    private static final FolderDjfsResource D3_D3 = FolderDjfsResource.cons(D3, "d3");

    private static final FolderDjfsResource D4_D1 = FolderDjfsResource.cons(D4, "d1");
    private static final FolderDjfsResource D4_D2 = FolderDjfsResource.cons(D4, "d2");
    private static final FolderDjfsResource D4_D3 = FolderDjfsResource.cons(D4, "d3");

    private static final FolderDjfsResource D5_D1 = FolderDjfsResource.cons(D5, "d1");
    private static final FolderDjfsResource D5_D2 = FolderDjfsResource.cons(D5, "d2");
    private static final FolderDjfsResource D5_D3 = FolderDjfsResource.cons(D5, "d3");

    private static FileDjfsResource F1 = FileDjfsResource.random(ROOT, "f1",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("11", 32))));
    private static FileDjfsResource F2 = FileDjfsResource.random(ROOT, "f2",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("12", 32))));
    private static FileDjfsResource F3 = FileDjfsResource.random(ROOT, "f3",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("13", 32))));
    private static FileDjfsResource F4 = FileDjfsResource.random(ROOT, "f4",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("14", 32))));
    private static FileDjfsResource F5 = FileDjfsResource.random(ROOT, "f5",
            x -> x.fileId(DjfsFileId.cons(StringUtils.repeat("15", 32))));

    private static final FileDjfsResource D1_F1 = FileDjfsResource.random(D1, "f1");
    private static final FileDjfsResource D1_F2 = FileDjfsResource.random(D1, "f2");
    private static final FileDjfsResource D1_F3 = FileDjfsResource.random(D1, "f3");
    private static final FileDjfsResource D1_F4 = FileDjfsResource.random(D1, "f4");
    private static final FileDjfsResource D1_F5 = FileDjfsResource.random(D1, "f5");

    private static final FileDjfsResource D2_F1 = FileDjfsResource.random(D2, "f1");
    private static final FileDjfsResource D2_F2 = FileDjfsResource.random(D2, "f2");
    private static final FileDjfsResource D2_F3 = FileDjfsResource.random(D2, "f3");
    private static final FileDjfsResource D2_F4 = FileDjfsResource.random(D2, "f4");
    private static final FileDjfsResource D2_F5 = FileDjfsResource.random(D2, "f5");

    private static final FileDjfsResource D3_F1 = FileDjfsResource.random(D3, "f1");
    private static final FileDjfsResource D3_F2 = FileDjfsResource.random(D3, "f2");
    private static final FileDjfsResource D3_F3 = FileDjfsResource.random(D3, "f3");
    private static final FileDjfsResource D3_F4 = FileDjfsResource.random(D3, "f4");
    private static final FileDjfsResource D3_F5 = FileDjfsResource.random(D3, "f5");

    private static final FileDjfsResource D4_F1 = FileDjfsResource.random(D4, "f1");
    private static final FileDjfsResource D4_F2 = FileDjfsResource.random(D4, "f2");
    private static final FileDjfsResource D4_F3 = FileDjfsResource.random(D4, "f3");
    private static final FileDjfsResource D4_F4 = FileDjfsResource.random(D4, "f4");
    private static final FileDjfsResource D4_F5 = FileDjfsResource.random(D4, "f5");

    private static final FileDjfsResource D5_F1 = FileDjfsResource.random(D5, "f1");
    private static final FileDjfsResource D5_F2 = FileDjfsResource.random(D5, "f2");
    private static final FileDjfsResource D5_F3 = FileDjfsResource.random(D5, "f3");
    private static final FileDjfsResource D5_F4 = FileDjfsResource.random(D5, "f4");
    private static final FileDjfsResource D5_F5 = FileDjfsResource.random(D5, "f5");

    @Override
    @Before
    public void setUp() {
        super.setUp();

        djfsResourceDao.insert(UID, D1, D2, D3, D4, D5, D1_D1, D1_D2, D1_D3, D2_D1, D2_D2, D2_D3, D3_D1, D3_D2, D3_D3,
                D4_D1, D4_D2, D4_D3, D5_D1, D5_D2, D5_D3);

        djfsResourceDao.insert(UID, F1, F2, F3, F4, F5, D1_F1, D1_F2, D1_F3, D1_F4, D1_F5, D2_F1, D2_F2, D2_F3, D2_F4,
                D2_F5, D3_F1, D3_F2, D3_F3, D3_F4, D3_F5, D4_F1, D4_F2, D4_F3, D4_F4, D4_F5, D5_F1, D5_F2, D5_F3, D5_F4,
                D5_F5);
    }

    @Test
    public void findImmediateChildFoldersFileIdOrder() {
        ListF<FolderDjfsResource> result = djfsResourceDao.find2ImmediateChildFoldersOrderByFileIdThenId(ROOT,
                Option.empty(), Option.empty(), 10, false);
        Assert.hasSize(5, result);
        Assert.equals(D1.getPath(), result.get(0).getPath());
        Assert.equals(D2.getPath(), result.get(1).getPath());
        Assert.equals(D3.getPath(), result.get(2).getPath());
        Assert.equals(D4.getPath(), result.get(3).getPath());
        Assert.equals(D5.getPath(), result.get(4).getPath());
    }

    @Test
    public void findImmediateChildFoldersStartingFileId() {
        ListF<FolderDjfsResource> result = djfsResourceDao.find2ImmediateChildFoldersOrderByFileIdThenId(ROOT,
                D2.getResourceId().map(DjfsResourceId::getFileId).map(DjfsFileId::getValue), Option.empty(), 10,
                false);
        Assert.hasSize(3, result);
        Assert.equals(D3.getPath(), result.get(0).getPath());
        Assert.equals(D4.getPath(), result.get(1).getPath());
        Assert.equals(D5.getPath(), result.get(2).getPath());
    }

    @Test
    public void findImmediateChildFoldersLimit() {
        ListF<FolderDjfsResource> result = djfsResourceDao.find2ImmediateChildFoldersOrderByFileIdThenId(ROOT,
                Option.empty(), Option.empty(), 2, false);
        Assert.hasSize(2, result);
        Assert.equals(D1.getPath(), result.get(0).getPath());
        Assert.equals(D2.getPath(), result.get(1).getPath());
    }

    @Test
    public void findImmediateChildFoldersStartingFileIdAndLimit() {
        ListF<FolderDjfsResource> result = djfsResourceDao.find2ImmediateChildFoldersOrderByFileIdThenId(ROOT,
                D1.getResourceId().map(DjfsResourceId::getFileId).map(DjfsFileId::getValue), Option.empty(), 3,
                false);
        Assert.hasSize(3, result);
        Assert.equals(D2.getPath(), result.get(0).getPath());
        Assert.equals(D3.getPath(), result.get(1).getPath());
        Assert.equals(D4.getPath(), result.get(2).getPath());
    }

    @Test
    public void findImmediateChildFoldersEmptyResult() {
        ListF<FolderDjfsResource> result = djfsResourceDao.find2ImmediateChildFoldersOrderByFileIdThenId(D1_D1,
                Option.empty(), Option.empty(), 10, false);
        Assert.hasSize(0, result);
    }

    @Test
    public void findImmediateChildFilesFileIdOrder() {
        ListF<FileDjfsResource> result = djfsResourceDao.find2ImmediateChildFilesOrderByFileIdThenId(ROOT,
                Option.empty(), Option.empty(), 10, false);
        Assert.hasSize(5, result);
        Assert.equals(F1.getPath(), result.get(0).getPath());
        Assert.equals(F2.getPath(), result.get(1).getPath());
        Assert.equals(F3.getPath(), result.get(2).getPath());
        Assert.equals(F4.getPath(), result.get(3).getPath());
        Assert.equals(F5.getPath(), result.get(4).getPath());
    }

    @Test
    public void findImmediateChildFilesStartingFileId() {
        ListF<FileDjfsResource> result = djfsResourceDao.find2ImmediateChildFilesOrderByFileIdThenId(ROOT,
                F2.getResourceId().map(DjfsResourceId::getFileId).map(DjfsFileId::getValue), Option.empty(), 10,
                false);
        Assert.hasSize(3, result);
        Assert.equals(F3.getPath(), result.get(0).getPath());
        Assert.equals(F4.getPath(), result.get(1).getPath());
        Assert.equals(F5.getPath(), result.get(2).getPath());
    }

    @Test
    public void findImmediateChildFilesLimit() {
        ListF<FileDjfsResource> result = djfsResourceDao.find2ImmediateChildFilesOrderByFileIdThenId(ROOT,
                Option.empty(), Option.empty(), 2, false);
        Assert.hasSize(2, result);
        Assert.equals(F1.getPath(), result.get(0).getPath());
        Assert.equals(F2.getPath(), result.get(1).getPath());
    }

    @Test
    public void findImmediateChildFilesStartingFileIdAndLimit() {
        ListF<FileDjfsResource> result = djfsResourceDao.find2ImmediateChildFilesOrderByFileIdThenId(ROOT,
                F1.getResourceId().map(DjfsResourceId::getFileId).map(DjfsFileId::getValue), Option.empty(), 3,
                false);
        Assert.hasSize(3, result);
        Assert.equals(F2.getPath(), result.get(0).getPath());
        Assert.equals(F3.getPath(), result.get(1).getPath());
        Assert.equals(F4.getPath(), result.get(2).getPath());
    }

    @Test
    public void findImmediateChildFilesEmptyResult() {
        ListF<FileDjfsResource> result = djfsResourceDao.find2ImmediateChildFilesOrderByFileIdThenId(D1_D1,
                Option.empty(), Option.empty(), 10, false);
        Assert.hasSize(0, result);
    }

    @Test
    public void findImmediateChildrenWithLargeLimit() {
        ListF<DjfsResource> result = djfsResourceDao.find2ImmediateChildren(D1.getPath(), 100);
        Assert.hasSize(8, result);

        SetF<DjfsResourcePath> actualPaths = Cf.toSet(result.map(DjfsResource::getPath));
        SetF<DjfsResourcePath> expectedPaths = Cf.toSet(
                Cf.list(D1_D1, D1_D2, D1_D3, D1_F1, D1_F2, D1_F3, D1_F4, D1_F5).map(DjfsResource::getPath));

        Assert.equals(expectedPaths, actualPaths);
    }

    @Test
    public void findImmediateChildrenWithSmallLimit() {
        ListF<DjfsResource> result = djfsResourceDao.find2ImmediateChildren(D1.getPath(), 6);
        Assert.hasSize(6, result);
    }
}
