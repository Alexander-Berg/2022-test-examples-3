package ru.yandex.chemodan.app.djfs.core.changelog;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.event.SharedFolderCreatedEvent;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceType;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfo;
import ru.yandex.chemodan.app.djfs.core.share.event.UserKickedFromGroupEvent;
import ru.yandex.chemodan.app.djfs.core.share.event.UserLeftGroupEvent;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class ChangelogEventHandlerDoubleUserTest extends DjfsDoubleUserTestBase {
    @Autowired
    private ChangelogEventHandler sut;

    @Test
    public void handleSharedFolderCreatedEvent() {
        Instant now = Instant.now();
        DjfsResourcePath owner_path = DjfsResourcePath.cons(UID_1, "/disk/owner");
        DjfsResourcePath owner_folder_path = DjfsResourcePath.cons(UID_1, "/disk/owner/folder");
        DjfsResourcePath participant_path = DjfsResourcePath.cons(UID_2, "/disk/participant");
        FolderDjfsResource folder = FolderDjfsResource.cons(owner_folder_path, now);
        ShareInfo shareInfo = util.share.shareInfo(owner_path, participant_path);

        SharedFolderCreatedEvent event = new SharedFolderCreatedEvent();
        event.eventTime = now;
        event.actor = UID_1;
        event.folder = folder;
        event.shareInfo = shareInfo;

        sut.handle(event);

        ListF<Changelog> participantChangelogs = changelogDao.findAll(UID_2);
        Assert.sizeIs(0, participantChangelogs);

        ListF<Changelog> ownerChangelogs = changelogDao.findAll(UID_1);
        Assert.sizeIs(1, ownerChangelogs);

        Changelog changelog = ownerChangelogs.first();
        Assert.equals(owner_folder_path, changelog.getPath());
        Assert.equals(now.getMillis() * 1000, changelog.getVersion());
        Assert.equals(folder.getResourceId().get().getFileId().getValue(), changelog.getFileId());
        Assert.equals(DjfsResourceType.DIR, changelog.getResourceType());
        Assert.equals(Changelog.OperationType.NEW, changelog.getOperationType());
        Assert.isFalse(changelog.isPublik());
        Assert.isTrue(changelog.isVisible());
        Assert.some(changelog.getDtime());
        Assert.equals(shareInfo.getGroupId(), changelog.getGroupId().get());
        Assert.equals(owner_path.getPath(), changelog.getGroupPath().get());

        Assert.none(changelog.getMd5());
        Assert.none(changelog.getSha256());
        Assert.none(changelog.getSize());
        Assert.none(changelog.getMimetype());
        Assert.none(changelog.getMediaType());
        Assert.none(changelog.getAntiVirusScanStatus());
        Assert.none(changelog.getHasPreview());
        Assert.none(changelog.getHasExternalSetprop());
        Assert.none(changelog.getExifTime());
        Assert.none(changelog.getModificationTime());
    }

    @Test
    public void handleUserKickedFromGroupEvent() {
        Instant now = Instant.now();
        DjfsResourcePath owner_path = DjfsResourcePath.cons(UID_1, "/disk/owner");
        DjfsResourcePath participant_path = DjfsResourcePath.cons(UID_2, "/disk/participant");
        FolderDjfsResource folder = FolderDjfsResource.cons(participant_path, now);
        ShareInfo shareInfo = util.share.shareInfo(owner_path, participant_path);

        UserKickedFromGroupEvent event = UserKickedFromGroupEvent.builder()
                .instant(now)
                .uid(UID_2)
                .userShareRoot(Option.of(folder))
                .shareInfo(shareInfo)
                .build();

        sut.handle(event);

        ListF<Changelog> ownerChangelogs = changelogDao.findAll(UID_1);
        Assert.sizeIs(0, ownerChangelogs);

        ListF<Changelog> participantChangelogs = changelogDao.findAll(UID_2);
        Assert.sizeIs(1, participantChangelogs);

        Changelog changelog = participantChangelogs.first();
        Assert.equals(participant_path, changelog.getPath());
        Assert.equals(now.getMillis() * 1000, changelog.getVersion());
        Assert.equals(folder.getResourceId().get().getFileId().getValue(), changelog.getFileId());
        Assert.equals(DjfsResourceType.DIR, changelog.getResourceType());
        Assert.equals(Changelog.OperationType.DELETED, changelog.getOperationType());
        Assert.isFalse(changelog.isPublik());
        Assert.isTrue(changelog.isVisible());
        Assert.some(changelog.getDtime());
        Assert.none(changelog.getGroupId());
        Assert.none(changelog.getGroupPath());

        Assert.none(changelog.getMd5());
        Assert.none(changelog.getSha256());
        Assert.none(changelog.getSize());
        Assert.none(changelog.getMimetype());
        Assert.none(changelog.getMediaType());
        Assert.none(changelog.getAntiVirusScanStatus());
        Assert.none(changelog.getHasPreview());
        Assert.none(changelog.getHasExternalSetprop());
        Assert.none(changelog.getExifTime());
        Assert.none(changelog.getModificationTime());
    }

    @Test
    public void handleUserLeftGroupEventEvent() {
        Instant now = Instant.now();
        DjfsResourcePath owner_path = DjfsResourcePath.cons(UID_1, "/disk/owner");
        DjfsResourcePath participant_path = DjfsResourcePath.cons(UID_2, "/disk/participant");
        FolderDjfsResource folder = FolderDjfsResource.cons(participant_path, now);
        ShareInfo shareInfo = util.share.shareInfo(owner_path, participant_path);

        UserLeftGroupEvent event = UserLeftGroupEvent.builder()
                .instant(now)
                .uid(UID_2)
                .userShareRoot(Option.of(folder))
                .shareInfo(shareInfo)
                .build();

        sut.handle(event);

        ListF<Changelog> ownerChangelogs = changelogDao.findAll(UID_1);
        Assert.sizeIs(0, ownerChangelogs);

        ListF<Changelog> participantChangelogs = changelogDao.findAll(UID_2);
        Assert.sizeIs(1, participantChangelogs);

        Changelog changelog = participantChangelogs.first();
        Assert.equals(participant_path, changelog.getPath());
        Assert.equals(now.getMillis() * 1000, changelog.getVersion());
        Assert.equals(folder.getResourceId().get().getFileId().getValue(), changelog.getFileId());
        Assert.equals(DjfsResourceType.DIR, changelog.getResourceType());
        Assert.equals(Changelog.OperationType.DELETED, changelog.getOperationType());
        Assert.isFalse(changelog.isPublik());
        Assert.isTrue(changelog.isVisible());
        Assert.some(changelog.getDtime());
        Assert.none(changelog.getGroupId());
        Assert.none(changelog.getGroupPath());

        Assert.none(changelog.getMd5());
        Assert.none(changelog.getSha256());
        Assert.none(changelog.getSize());
        Assert.none(changelog.getMimetype());
        Assert.none(changelog.getMediaType());
        Assert.none(changelog.getAntiVirusScanStatus());
        Assert.none(changelog.getHasPreview());
        Assert.none(changelog.getHasExternalSetprop());
        Assert.none(changelog.getExifTime());
        Assert.none(changelog.getModificationTime());
    }
}
