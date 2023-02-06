package ru.yandex.chemodan.app.djfs.core.index;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.share.GroupLink;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.test.Assert;

public class IndexerActionsDoubleUserTest extends DjfsDoubleUserTestBase {
    @Autowired
    private IndexerActions indexerActions;

    @Test
    public void sharedFolderVersionFieldForSharedResources() {
        DjfsResourcePath ownerFolderPath = DjfsResourcePath.cons(UID_1, "/disk/folder");
        filesystem.createFolder(PRINCIPAL_1, ownerFolderPath);

        DjfsResourcePath ownerFilePath = ownerFolderPath.getChildPath("file.txt");

        Instant fileCreationTime = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(fileCreationTime.getMillis());
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, ownerFilePath);
        DateTimeUtils.setCurrentMillisSystem();

        String groupId = shareManager.createGroup(ownerFolderPath).getId();
        DjfsResourcePath participantFolderPath = DjfsResourcePath.cons(UID_2, "/disk/folder");
        filesystem.createFolder(PRINCIPAL_2, participantFolderPath);

        Instant createLinkTime = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(createLinkTime.getMillis());
        shareManager.createLink(groupId, participantFolderPath, SharePermissions.READ_WRITE);
        DateTimeUtils.setCurrentMillisSystem();

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_2.toString(), createRequestWithUserObj(USER_2));

        Assert.sizeIs(1, response.items);
        IndexerResourcePojo item = response.items.get(0);

        Assert.isTrue(item.sharedFolderVersion.isPresent());
        Assert.equals(item.sharedFolderVersion.get(), fileCreationTime.getMillis() * 1000 + (long)InstantUtils.toSeconds(createLinkTime) * 1000000);
    }

    @Test
    public void sharedFolderVersionForGroupLinkWithoutCtime() {
        DjfsResourcePath ownerFolderPath = DjfsResourcePath.cons(UID_1, "/disk/folder");
        filesystem.createFolder(PRINCIPAL_1, ownerFolderPath);

        DjfsResourcePath ownerFilePath = ownerFolderPath.getChildPath("file.txt");

        Instant fileCreationTime = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(fileCreationTime.getMillis());
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, ownerFilePath);
        DateTimeUtils.setCurrentMillisSystem();

        String groupId = shareManager.createGroup(ownerFolderPath).getId();
        DjfsResourcePath participantFolderPath = DjfsResourcePath.cons(UID_2, "/disk/folder");
        filesystem.createFolder(PRINCIPAL_2, participantFolderPath);

        GroupLink groupLink = GroupLink.builder()
                .id(UuidUtils.randomToHexString())
                .groupId(groupId)
                .uid(UID_2)
                .path(participantFolderPath)
                .version(1)
                .permissions(SharePermissions.READ_WRITE)
                .build();
        Assert.assertThrows(() -> mongoGroupLinkDao.insert(groupLink), NullPointerException.class);
    }

    @Test
    public void sharedFolderVersionFieldForSharedRoot() {
        DjfsResourcePath ownerFolderPath = DjfsResourcePath.cons(UID_1, "/disk/folder");
        filesystem.createFolder(PRINCIPAL_1, ownerFolderPath);

        String groupId = shareManager.createGroup(ownerFolderPath).getId();
        Instant folderCreationTime = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(folderCreationTime.getMillis());
        DjfsResourcePath participantFolderPath = DjfsResourcePath.cons(UID_2, "/disk/folder");
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL_2, participantFolderPath);
        DateTimeUtils.setCurrentMillisSystem();

        Instant createLinkTime = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(createLinkTime.getMillis());
        shareManager.createLink(groupId, participantFolderPath, SharePermissions.READ_WRITE);
        DateTimeUtils.setCurrentMillisSystem();

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(folder.getResourceId().get().toString()), UID_2.toString(), createRequestWithUserObj(USER_2));

        Assert.sizeIs(1, response.items);
        IndexerResourcePojo item = response.items.get(0);

        Assert.isFalse(item.sharedFolderVersion.isPresent());
        Assert.equals(item.version, folderCreationTime.getMillis() * 1000);
    }
}
