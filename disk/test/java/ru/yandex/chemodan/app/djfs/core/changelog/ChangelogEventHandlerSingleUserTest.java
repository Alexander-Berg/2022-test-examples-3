package ru.yandex.chemodan.app.djfs.core.changelog;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.filesystem.event.PrivateFolderCreatedEvent;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceType;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class ChangelogEventHandlerSingleUserTest extends DjfsSingleUserTestBase {
    @Autowired
    private ChangelogEventHandler sut;

    @Test
    public void handlePrivateFolderCreatedEvent() {
        Instant now = Instant.now();
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/folder");
        FolderDjfsResource folder = FolderDjfsResource.cons(path, now);

        PrivateFolderCreatedEvent event = new PrivateFolderCreatedEvent();
        event.eventTime = now;
        event.actor = UID;
        event.folder = folder;

        sut.handle(event);

        ListF<Changelog> changelogs = changelogDao.findAll(UID);
        Assert.sizeIs(1, changelogs);

        Changelog changelog = changelogs.first();
        Assert.equals(path, changelog.getPath());
        Assert.equals(now.getMillis() * 1000, changelog.getVersion());
        Assert.equals(folder.getResourceId().get().getFileId().getValue(), changelog.getFileId());
        Assert.equals(DjfsResourceType.DIR, changelog.getResourceType());
        Assert.equals(Changelog.OperationType.NEW, changelog.getOperationType());
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
