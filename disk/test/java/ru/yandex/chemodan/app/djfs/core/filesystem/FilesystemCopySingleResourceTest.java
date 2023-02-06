package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class FilesystemCopySingleResourceTest extends DjfsSingleUserTestBase {
    @Test
    public void copyFolder() {
        DjfsResourcePath sourceParentPath = DjfsResourcePath.cons(UID, "/disk/source_parent");
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source_parent/source_folder");
        DjfsResourcePath destinationParentPath = DjfsResourcePath.cons(UID, "/disk/destination_parent");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination_parent/destination_folder");

        Instant initial = getNowRoundedToSeconds();
        DateTimeUtils.setCurrentMillisFixed(initial.getMillis());

        filesystem.createFolder(PRINCIPAL, sourceParentPath);
        filesystem.createFolder(PRINCIPAL, destinationParentPath);
        filesystem.createFolder(PRINCIPAL, sourcePath);

        Instant now = initial.plus(Duration.standardSeconds(10));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        filesystem.copySingleResource(PRINCIPAL, sourcePath, destinationPath);

        FolderDjfsResource source = (FolderDjfsResource) djfsResourceDao.find(sourcePath).get();
        FolderDjfsResource destinationParent = (FolderDjfsResource) djfsResourceDao.find(destinationParentPath).get();
        FolderDjfsResource destination = (FolderDjfsResource) djfsResourceDao.find(destinationPath).get();

        Assert.equals(destinationParent.getId(), destination.getParentId().get());
        Assert.notEquals(source.getParentId().get(), destination.getParentId().get());
        Assert.notEquals(source.getFileId().get(), destination.getFileId().get());
        Assert.equals(destinationPath, destination.getPath());
        Assert.some(UID, destination.getModifyUid());

        Assert.notEquals(source.getCreationTime().get(), destination.getCreationTime().get());
        Assert.equals(now, destination.getCreationTime().get());
        Assert.notEquals(source.getModificationTime().get(), destination.getModificationTime().get());
        Assert.equals(now, destination.getModificationTime().get());
        Assert.equals(source.getUploadTime().get(), destination.getUploadTime().get());
        Assert.none(destination.getTrashAppendTime());
        Assert.none(destination.getHiddenAppendTime());

        Assert.notEquals(source.getVersion().get(), destination.getVersion().get());
        Assert.some(now.getMillis() * 1000, destination.getVersion());

        Assert.isTrue(destination.isVisible());
        Assert.isFalse(destination.isPublic());
        Assert.isFalse(destination.isBlocked());
        Assert.isFalse(destination.isPublished());

        Assert.none(destination.getFolderType());
        Assert.none(destination.getTrashAppendOriginalPath());
        Assert.none(destination.getPublicHash());
        Assert.none(destination.getShortUrl());
        Assert.none(destination.getSymlink());
        Assert.none(destination.getFolderUrl());
        Assert.none(destination.getDownloadCounter());
        Assert.none(destination.getCustomProperties());
    }

    @Test
    public void copyFile() {
        DjfsResourcePath sourceParentPath = DjfsResourcePath.cons(UID, "/disk/source_parent");
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source_parent/source_file");
        DjfsResourcePath destinationParentPath = DjfsResourcePath.cons(UID, "/disk/destination_parent");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination_parent/destination_file");

        Instant initial = getNowRoundedToSeconds();
        DateTimeUtils.setCurrentMillisFixed(initial.getMillis());

        filesystem.createFolder(PRINCIPAL, sourceParentPath);
        filesystem.createFolder(PRINCIPAL, destinationParentPath);
        filesystem.createFile(PRINCIPAL, sourcePath);

        Instant now = initial.plus(Duration.standardSeconds(10));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        filesystem.copySingleResource(PRINCIPAL, sourcePath, destinationPath);

        FileDjfsResource source = (FileDjfsResource) djfsResourceDao.find(sourcePath).get();
        FolderDjfsResource destinationParent = (FolderDjfsResource) djfsResourceDao.find(destinationParentPath).get();
        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find(destinationPath).get();

        Assert.equals(destinationParent.getId(), destination.getParentId().get());
        Assert.notEquals(source.getParentId().get(), destination.getParentId().get());
        Assert.notEquals(source.getFileId().get(), destination.getFileId().get());
        Assert.equals(destinationPath, destination.getPath());
        Assert.some(UID, destination.getModifyUid());

        Assert.notEquals(source.getCreationTime(), destination.getCreationTime());
        Assert.equals(now, destination.getCreationTime());
        Assert.notEquals(source.getModificationTime(), destination.getModificationTime());
        Assert.equals(now, destination.getModificationTime());
        Assert.equals(source.getUploadTime().get(), destination.getUploadTime().get());
        Assert.none(destination.getTrashAppendTime());
        Assert.none(destination.getHiddenAppendTime());

        Assert.notEquals(source.getVersion().get(), destination.getVersion().get());
        Assert.some(now.getMillis() * 1000, destination.getVersion());

        Assert.isTrue(destination.isVisible());
        Assert.isFalse(destination.isPublic());
        Assert.isFalse(destination.isBlocked());
        Assert.isFalse(destination.isPublished());

        Assert.equals(source.getSize(), destination.getSize());
        Assert.equals(source.getHid(), destination.getHid());
        Assert.equals(source.getMd5(), destination.getMd5());
        Assert.equals(source.getSha256(), destination.getSha256());
        Assert.equals(source.getFileStid(), destination.getFileStid());
        Assert.equals(source.getDigestStid(), destination.getDigestStid());
        Assert.equals(source.getPreviewStid(), destination.getPreviewStid());
        Assert.equals(source.getExifTime(), destination.getExifTime());
        Assert.equals(source.getAntiVirusScanStatus(), destination.getAntiVirusScanStatus());

        Assert.none(destination.getTrashAppendOriginalPath());
        Assert.none(destination.getPublicHash());
        Assert.none(destination.getShortUrl());
        Assert.none(destination.getSymlink());
        Assert.none(destination.getFolderUrl());
        Assert.none(destination.getDownloadCounter());
        Assert.none(destination.getCustomProperties());
    }

    private Instant getNowRoundedToSeconds() {
        return Instant.now().toDateTime().withMillisOfSecond(0).toInstant();
    }
}
