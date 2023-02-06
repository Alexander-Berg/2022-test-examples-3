package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.filesystem.exception.NoPermissionException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserWithSharedResourcesTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class FilesystemCopySingleResourceShareTest extends DjfsDoubleUserWithSharedResourcesTestBase {
    @Test
    public void copyFolderToSharedAsParticipant() {
        DjfsResourcePath sourceParentPath = DjfsResourcePath.cons(PARTICIPANT_UID, "/disk/source_parent");
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(PARTICIPANT_UID, "/disk/source_parent/source_folder");
        DjfsResourcePath participantDestinationPath = PARTICIPANT_PATH.getChildPath("destination_folder");
        DjfsResourcePath ownerDestinationPath = OWNER_PATH.getChildPath("destination_folder");

        Instant initial = getNowRoundedToSeconds();
        DateTimeUtils.setCurrentMillisFixed(initial.getMillis());

        filesystem.createFolder(PARTICIPANT_PRINCIPAL, sourceParentPath);
        filesystem.createFolder(PARTICIPANT_PRINCIPAL, sourcePath);

        Instant now = initial.plus(Duration.standardSeconds(10));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        filesystem.copySingleResource(PARTICIPANT_PRINCIPAL, sourcePath, participantDestinationPath);

        FolderDjfsResource source = (FolderDjfsResource) djfsResourceDao.find(sourcePath).get();
        FolderDjfsResource destinationParent = (FolderDjfsResource) djfsResourceDao.find(OWNER_PATH).get();
        FolderDjfsResource destination = (FolderDjfsResource) djfsResourceDao.find(ownerDestinationPath).get();

        Assert.notEquals(ownerDestinationPath.getPgId(), destination.getId());
        Assert.equals(destinationParent.getId(), destination.getParentId().get());
        Assert.notEquals(source.getParentId().get(), destination.getParentId().get());
        Assert.notEquals(source.getFileId().get(), destination.getFileId().get());
        Assert.equals(ownerDestinationPath, destination.getPath());
        Assert.some(PARTICIPANT_UID, destination.getModifyUid());

        Assert.notEquals(source.getCreationTime().get(), destination.getCreationTime().get());
        Assert.equals(now, destination.getCreationTime().get().toDateTime().withMillisOfSecond(0).toInstant());
        Assert.notEquals(source.getModificationTime().get(), destination.getModificationTime().get());
        Assert.equals(now, destination.getModificationTime().get().toDateTime().withMillisOfSecond(0).toInstant());
        Assert.equals(source.getUploadTime().get().toDateTime().withMillisOfSecond(0).toInstant(),
                destination.getUploadTime().get().toDateTime().withMillisOfSecond(0).toInstant());
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
    public void copyFileToSharedAsParticipant() {
        DjfsResourcePath sourceParentPath = DjfsResourcePath.cons(PARTICIPANT_UID, "/disk/source_parent");
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(PARTICIPANT_UID, "/disk/source_parent/source_file");
        DjfsResourcePath participantDestinationPath = PARTICIPANT_PATH.getChildPath("destination_file");
        DjfsResourcePath ownerDestinationPath = OWNER_PATH.getChildPath("destination_file");

        Instant initial = getNowRoundedToSeconds();
        DateTimeUtils.setCurrentMillisFixed(initial.getMillis());

        filesystem.createFolder(PARTICIPANT_PRINCIPAL, sourceParentPath);
        filesystem.createFile(PARTICIPANT_PRINCIPAL, sourcePath);

        Instant now = initial.plus(Duration.standardSeconds(10));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        filesystem.copySingleResource(PARTICIPANT_PRINCIPAL, sourcePath, participantDestinationPath);

        FileDjfsResource source = (FileDjfsResource) djfsResourceDao.find(sourcePath).get();
        FolderDjfsResource destinationParent = (FolderDjfsResource) djfsResourceDao.find(OWNER_PATH).get();
        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find(ownerDestinationPath).get();

        Assert.notEquals(ownerDestinationPath.getPgId(), destination.getId());
        Assert.equals(destinationParent.getId(), destination.getParentId().get());
        Assert.notEquals(source.getParentId().get(), destination.getParentId().get());
        Assert.notEquals(source.getFileId().get(), destination.getFileId().get());
        Assert.equals(ownerDestinationPath, destination.getPath());
        Assert.some(PARTICIPANT_UID, destination.getModifyUid());

        Assert.notEquals(source.getCreationTime(), destination.getCreationTime());
        Assert.equals(now, destination.getCreationTime().toDateTime().withMillisOfSecond(0).toInstant());
        Assert.notEquals(source.getModificationTime(), destination.getModificationTime());
        Assert.equals(now, destination.getModificationTime().toDateTime().withMillisOfSecond(0).toInstant());
        Assert.equals(source.getUploadTime().get().toDateTime().withMillisOfSecond(0).toInstant(),
                destination.getUploadTime().get().toDateTime().withMillisOfSecond(0).toInstant());
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

    @Test
    public void participantWithReadPermissionThrowsException() {
        shareManager.changeLinkPermissions(groupLinkId, SharePermissions.READ);
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(PARTICIPANT_UID, "/disk/source");
        filesystem.createFolder(PARTICIPANT_PRINCIPAL, sourcePath);
        Assert.assertThrows(() -> filesystem.copySingleResource(PARTICIPANT_PRINCIPAL, sourcePath,
                PARTICIPANT_PATH.getChildPath("subfolder")), NoPermissionException.class);
    }

    private Instant getNowRoundedToSeconds() {
        return Instant.now().toDateTime().withMillisOfSecond(0).toInstant();
    }
}
