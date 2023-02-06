package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.djfs.core.changelog.Changelog;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.FilesystemException;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.FolderTooDeepException;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.NoPermissionException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceType;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.index.SearchPushGenerator;
import ru.yandex.chemodan.app.djfs.core.notification.XivaPushGenerator;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserWithSharedResourcesTestBase;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.chemodan.queller.celery.job.CeleryJob;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonNull;
import ru.yandex.commune.json.JsonNumber;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.JsonValue;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class FilesystemCreateFolderShareTest extends DjfsDoubleUserWithSharedResourcesTestBase {
    @Test
    public void resource() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));

        FolderDjfsResource folder =
                (FolderDjfsResource) djfsResourceDao.find(OWNER_PATH.getChildPath("subfolder")).get();
        FolderDjfsResource parent = (FolderDjfsResource) djfsResourceDao.find(OWNER_PATH).get();
        Assert.equals(parent.getId(), folder.getParentId().get());
        Assert.notEmpty(folder.getResourceId().get().getFileId().getValue());
        Assert.equals(OWNER_PATH.getChildPath("subfolder"), folder.getPath());
        Assert.equals(UID_2, folder.getModifyUid().get());

        Assert.equals(now, folder.getCreationTime().get());
        Assert.equals(now, folder.getModificationTime().get());
        Assert.equals(now, folder.getUploadTime().get());
        Assert.none(folder.getTrashAppendTime());
        Assert.none(folder.getHiddenAppendTime());

        Assert.equals(now.getMillis() * 1000, folder.getVersion().get());

        Assert.isTrue(folder.isVisible());
        Assert.isFalse(folder.isPublic());
        Assert.isFalse(folder.isBlocked());
        Assert.isFalse(folder.isPublished());

        Assert.none(folder.getFolderType());
        Assert.none(folder.getTrashAppendOriginalPath());
        Assert.none(folder.getPublicHash());
        Assert.none(folder.getShortUrl());
        Assert.none(folder.getSymlink());
        Assert.none(folder.getFolderUrl());
        Assert.none(folder.getDownloadCounter());
        Assert.none(folder.getCustomProperties());
    }

    @Test
    public void changelog() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));

        ListF<Changelog> participantChangelogs = changelogDao.findAll(UID_2);
        Assert.sizeIs(0, participantChangelogs);

        ListF<Changelog> changelogs = changelogDao.findAll(UID_1);
        Assert.sizeIs(1, changelogs);

        Changelog changelog = changelogs.first();
        Assert.equals(OWNER_PATH.getChildPath("subfolder"), changelog.getPath());
        Assert.equals(now.getMillis() * 1000, changelog.getVersion());
        Assert.equals(folder.getResourceId().get().getFileId().getValue(), changelog.getFileId());
        Assert.equals(DjfsResourceType.DIR, changelog.getResourceType());
        Assert.equals(Changelog.OperationType.NEW, changelog.getOperationType());
        Assert.isFalse(changelog.isPublik());
        Assert.isTrue(changelog.isVisible());
        Assert.some(changelog.getDtime());
        Assert.equals(groupId, changelog.getGroupId().get());
        Assert.equals(OWNER_PATH.getPath(), changelog.getGroupPath().get());

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
    public void xivaPushTask() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        long oldVersion1 = now.getMillis() * 1000 - 11;
        long oldVersion2 = now.getMillis() * 1000 - 22;
        userDao.incrementVersionTo(UID_1, oldVersion1);
        userDao.incrementVersionTo(UID_2, oldVersion2);

        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));

        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID));
        CeleryJob celeryJob = mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID).first();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();
        Assert.equals(UID_2.asString(), ((JsonString) kwargs.getTs("committer")).getValue());
        Assert.equals(groupId, ((JsonString) kwargs.getTs("gid")).getValue());
        Assert.equals(String.valueOf(now.getMillis() * 1000), ((JsonString) kwargs.getTs("new_version")).getValue());
        Assert.equals("diff", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("diff_group", ((JsonString) kwargs.getTs("action_name")).getValue());

        JsonObject oldVersion = (JsonObject) kwargs.getTs("old_version");
        Assert.equals(oldVersion1, ((JsonNumber) oldVersion.get(UID_1.asString())).longValue());
        Assert.equals(oldVersion2, ((JsonNumber) oldVersion.get(UID_2.asString())).longValue());

        JsonObject xivaData = (JsonObject) ((JsonArray) kwargs.getTs("xiva_data")).getArray().get(0);
        Assert.equals(OWNER_PATH.getChildPath("subfolder").getPath(), ((JsonString) xivaData.get("key")).getValue());
        Assert.equals("dir", ((JsonString) xivaData.get("resource_type")).getValue());
        Assert.equals(folder.getResourceId().get().getFileId().getValue(),
                ((JsonString) xivaData.get("fid")).getValue());
        Assert.equals("new", ((JsonString) xivaData.get("op")).getValue());
    }

    @Test
    public void indexTask() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));

        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.TASK_ID));
        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.SHARED_TASK_ID));
        CeleryJob celeryJob =
                mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.SHARED_TASK_ID).first();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();
        Assert.equals(groupId, ((JsonString) kwargs.getTs("gid")).getValue());

        JsonObject data = (JsonObject) ((JsonArray) kwargs.getTs("data")).getArray().get(0);
        Assert.equals(UID_2.asLong(), ((JsonNumber) data.get("uid")).longValue());
        Assert.equals(UID_1.asString(), ((JsonString) data.get("shared_folder_owner")).getValue());
        Assert.equals(OWNER_PATH.getChildPath("subfolder").getPath(), ((JsonString) data.get("id")).getValue());
        Assert.equals(folder.getResourceId().get().getFileId().getValue(),
                ((JsonString) data.get("file_id")).getValue());
        Assert.equals("subfolder", ((JsonString) data.get("name")).getValue());
        Assert.equals(now.getMillis() * 1000, ((JsonNumber) data.get("version")).longValue());
        Assert.equals(InstantUtils.toSeconds(now), ((JsonNumber) data.get("ctime")).intValue());
        Assert.equals(InstantUtils.toSeconds(now), ((JsonNumber) data.get("mtime")).intValue());
        Assert.equals(1, ((JsonNumber) data.get("visible")).intValue());
        Assert.equals("mkdir", ((JsonString) data.get("operation")).getValue());
        Assert.equals("modify", ((JsonString) data.get("action")).getValue());
        Assert.equals("dir", ((JsonString) data.get("type")).getValue());
        Assert.isInstance(data.get("folder_type"), JsonNull.class);
        Assert.equals(now.getMillis() * 1000, ((JsonNumber) data.get("real_resource_version")).longValue());
    }

    @Test
    public void eventHistory() {
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));

        Assert.sizeIs(2, mockEventHistoryLogger.messageData);
        MapF<String, String> actual1 = mockEventHistoryLogger.messageData.get(0);
        Assert.sizeIs(8, actual1);
        Assert.equals("fs-mkdir", actual1.getTs("event_type"));
        Assert.equals(folder.getResourceId().get().toString(), actual1.getTs("tgt_folder_id"));
        Assert.equals(OWNER_PATH.getChildPath("subfolder").toString(), actual1.getTs("tgt_rawaddress"));
        Assert.equals(folder.getResourceId().get().getFileId().getValue(), actual1.getTs("resource_file_id"));
        Assert.equals(UID_1.asString(), actual1.getTs("uid"));
        Assert.equals(UID_1.asString(), actual1.getTs("owner_uid"));
        Assert.equals(UID_2.asString(), actual1.getTs("user_uid"));
        Assert.equals("dir", actual1.getTs("resource_type"));

        MapF<String, String> actual2 = mockEventHistoryLogger.messageData.get(1);
        Assert.sizeIs(8, actual2);
        Assert.equals("fs-mkdir", actual2.getTs("event_type"));
        Assert.equals(folder.getResourceId().get().toString(), actual2.getTs("tgt_folder_id"));
        Assert.equals(PARTICIPANT_PATH.getChildPath("subfolder").toString(), actual2.getTs("tgt_rawaddress"));
        Assert.equals(folder.getResourceId().get().getFileId().getValue(), actual2.getTs("resource_file_id"));
        Assert.equals(UID_2.asString(), actual2.getTs("uid"));
        Assert.equals(UID_1.asString(), actual2.getTs("owner_uid"));
        Assert.equals(UID_2.asString(), actual2.getTs("user_uid"));
        Assert.equals("dir", actual2.getTs("resource_type"));
    }

    @Test(expected = FilesystemException.class)
    public void noParentFolderThrowsException() {
        filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("one").getChildPath("two"));
    }

    @Test(expected = NoPermissionException.class)
    public void participantWithReadPermissionThrowsException() {
        shareManager.changeLinkPermissions(groupLinkId, SharePermissions.READ);
        filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));
    }

    @Test(expected = FilesystemException.class)
    public void createOverAlreadyExistingFolderThrowsException() {
        filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));
        filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));
    }

    @Test(expected = FilesystemException.class)
    public void createOverAlreadyExistingFileThrowsException() {
        filesystem.createFile(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));
        filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));
    }

    @Test
    public void createFolderExceedingDepthLimit() {
        DjfsResourcePath path = PARTICIPANT_PATH;
        final int startDepth = StringUtils.countMatches(PARTICIPANT_PATH.getPath(), "/");
        Assert.ge(filesystem.getFolderDepthMax(), startDepth);
        for (int i = startDepth; i < filesystem.getFolderDepthMax(); ++i) {
            FolderDjfsResource res = filesystem.createFolder(PRINCIPAL_2, path.getChildPath("subfolder"));
            path = res.getPath();
        }
        final DjfsResourcePath lastPath = path;
        Assert.assertThrows(
            () -> filesystem.createFolder(PRINCIPAL_2, lastPath.getChildPath("subfolder")),
            FolderTooDeepException.class
        );
    }
}
