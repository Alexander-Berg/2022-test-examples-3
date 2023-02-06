package ru.yandex.chemodan.app.djfs.core.filesystem;

import com.mongodb.ReadPreference;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.test.Assert;

public class FilesystemGetResourceByResourceIdTest extends DjfsSingleUserTestBase {
    private ListF<DjfsResourceArea> defaultAreasToBeSearched = Cf.list(DjfsResourceArea.DISK, DjfsResourceArea.PHOTOUNLIM);

    @Test
    public void getFolder() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/folder");
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, path);
        DjfsResourceId resourceId = folder.getResourceId().get();
        ListF<DjfsResource> folderResources = filesystem.find(PRINCIPAL, Cf.list(resourceId), defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.sizeIs(1, folderResources);
    }

    @Test
    public void getFileFromDisk() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/file.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path);
        DjfsResourceId resourceId = file.getResourceId().get();
        ListF<DjfsResource> fileResources = filesystem.find(PRINCIPAL, Cf.list(resourceId), defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.sizeIs(1, fileResources);
    }

    @Test
    public void getFileFromPhotounlim() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/photounlim/file.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path);
        DjfsResourceId resourceId = file.getResourceId().get();
        ListF<DjfsResource> fileResources = filesystem.find(PRINCIPAL, Cf.list(resourceId), defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.sizeIs(1, fileResources);
    }

    @Test
    public void cannotGetFileWrongArea() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/attach/file.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path);
        DjfsResourceId resourceId = file.getResourceId().get();
        ListF<DjfsResource> fileResources = filesystem.find(PRINCIPAL, Cf.list(resourceId), defaultAreasToBeSearched, Option.of(ReadPreference.primary()));
        Assert.isEmpty(fileResources);
    }

    @Test
    public void returnsEmptyOnEmptyListOfAvailableAreas() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/attach/file.jpg");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL, path);
        DjfsResourceId resourceId = file.getResourceId().get();
        ListF<DjfsResource> fileResources = filesystem.find(PRINCIPAL, Cf.list(resourceId), Cf.list(), Option.of(ReadPreference.primary()));
        Assert.isEmpty(fileResources);
    }
}
