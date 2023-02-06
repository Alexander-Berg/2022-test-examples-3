package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class FilesystemEnsureAllParentFoldersExistsTest extends DjfsSingleUserTestBase {
    @Test
    public void ensureAllParentFoldersExist() {
        filesystem.ensureAllParentFoldersExist(PRINCIPAL, DjfsResourcePath.cons(UID, "/notes/a/b/c/f"));

        Assert.isTrue(
                djfsResourceDao.find(DjfsResourcePath.cons(UID, "/notes")).get() instanceof FolderDjfsResource);
        Assert.isTrue(
                djfsResourceDao.find(DjfsResourcePath.cons(UID, "/notes/a")).get() instanceof FolderDjfsResource);
        Assert.isTrue(
                djfsResourceDao.find(DjfsResourcePath.cons(UID, "/notes/a/b")).get() instanceof FolderDjfsResource);
        Assert.isTrue(
                djfsResourceDao.find(DjfsResourcePath.cons(UID, "/notes/a/b/c")).get() instanceof FolderDjfsResource);

        Assert.none(djfsResourceDao.find(DjfsResourcePath.cons(UID, "/notes/a/b/c/f")));
    }
}
