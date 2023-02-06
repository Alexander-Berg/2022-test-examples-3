package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.misc.test.Assert;

public class DjfsResourceDaoBulkFindResourcesByPaths extends DjfsResourceDaoFindResourceTestBase {
    static private FileDjfsResource F2 = FileDjfsResource.random(ROOT, "f2");
    static private FolderDjfsResource D2 = FolderDjfsResource.cons(ROOT, "d2");

    @Test
    public void notFindNotExistentResource() {
        ListF<DjfsResource> resources = filesystem
                .findByPaths(PRINCIPAL, Cf.list(DjfsResourcePath.cons(UID, "/disk/qr2rasd")));
        Assert.isEmpty(resources);
    }

    @Test
    public void findDuplicateFiles() {
        djfsResourceDao.insert(UID, F1);
        ListF<DjfsResourcePath> paths = Cf.list(F1, F1).map(DjfsResource::getPath);
        ListF<DjfsResource> files = filesystem.findByPaths(PRINCIPAL, paths);
        Assert.isTrue(files.length() == 2);
        Assert.isTrue(files.get(0) instanceof FileDjfsResource);
        Assert.isTrue(files.get(1) instanceof FileDjfsResource);
        Assert.isTrue(files.get(0).getPath().equals(files.get(1).getPath()));
    }

    @Test
    public void findDuplicateFolders() {
        djfsResourceDao.insert(UID, D1);
        ListF<DjfsResourcePath> paths = Cf.list(D1, D1).map(DjfsResource::getPath);
        ListF<DjfsResource> folders = filesystem.findByPaths(PRINCIPAL, paths);
        Assert.isTrue(folders.length() == 2);
        Assert.isTrue(folders.get(0) instanceof FolderDjfsResource);
        Assert.isTrue(folders.get(1) instanceof FolderDjfsResource);
        Assert.isTrue(folders.get(0).getPath().equals(folders.get(1).getPath()));
    }

    @Test
    public void findSeveralFiles() {
        djfsResourceDao.insert(UID, F1, F2);
        ListF<DjfsResourcePath> paths = Cf.list(F1, F2).map(DjfsResource::getPath);
        ListF<DjfsResource> files = filesystem.findByPaths(PRINCIPAL, paths);
        Assert.isTrue(files.length() == 2);
        Assert.isTrue(files.get(0) instanceof FileDjfsResource);
        Assert.isTrue(files.get(1) instanceof FileDjfsResource);
        Assert.assertContainsAll(files.map(DjfsResource::getPath), Cf.list(F1, F2).map(DjfsResource::getPath));
    }

    @Test
    public void findSeveralFolders() {
        djfsResourceDao.insert(UID, D1, D2);
        ListF<DjfsResourcePath> paths = Cf.list(D1, D2).map(DjfsResource::getPath);
        ListF<DjfsResource> folders = filesystem.findByPaths(PRINCIPAL, paths);
        Assert.isTrue(folders.length() == 2);
        Assert.isTrue(folders.get(0) instanceof FolderDjfsResource);
        Assert.isTrue(folders.get(1) instanceof FolderDjfsResource);
        Assert.assertContainsAll(folders.map(DjfsResource::getPath), Cf.list(D1, D2).map(DjfsResource::getPath));
    }

    @Test
    public void findSeveralFilesAndFolders() {
        djfsResourceDao.insert(UID, F1, F2);
        djfsResourceDao.insert(UID, D1, D2);
        ListF<DjfsResourcePath> paths = Cf.list(F1, F2, D1, D2).map(DjfsResource::getPath);
        ListF<DjfsResource> resources = filesystem.findByPaths(PRINCIPAL, paths);
        Assert.isTrue(resources.length() == 4);
        Assert.assertContainsAll(
                resources.map(DjfsResource::getPath), Cf.list(F1, F2, D1, D2).map(DjfsResource::getPath));
    }
}
