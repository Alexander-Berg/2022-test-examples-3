package ru.yandex.chemodan.app.djfs.core.filesystem;

import java.util.UUID;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.djfs.core.SafeCloseable;
import ru.yandex.chemodan.app.djfs.core.changelog.Changelog;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.FilesystemException;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.ResourceExistsException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceType;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.index.SearchPushGenerator;
import ru.yandex.chemodan.app.djfs.core.notification.XivaPushGenerator;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.User;
import ru.yandex.chemodan.app.djfs.core.user.UserData;
import ru.yandex.chemodan.app.djfs.core.user.UserNotInitializedException;
import ru.yandex.chemodan.app.djfs.core.user.UserType;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.chemodan.app.djfs.core.web.ConnectionIdHolder;
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
public class FilesystemCreateFolderSingleUserTest extends DjfsSingleUserTestBase {
    private static DjfsResourcePath PATH = DjfsResourcePath.cons(UID, "/disk/folder");

    @Test
    public void resource() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        filesystem.createFolder(PRINCIPAL, PATH);

        FolderDjfsResource folder = (FolderDjfsResource) djfsResourceDao.find(PATH).get();
        FolderDjfsResource parent = (FolderDjfsResource) djfsResourceDao.find(PATH.getParent()).get();

        Assert.notEquals(PATH.getPgId(), folder.getId());
        Assert.equals(parent.getId(), folder.getParentId().get());
        Assert.notEmpty(folder.getResourceId().get().getFileId().getValue());
        Assert.equals(PATH, folder.getPath());
        Assert.equals(UID, folder.getModifyUid().get());

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

        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, PATH);

        ListF<Changelog> changelogs = changelogDao.findAll(UID);
        Assert.sizeIs(1, changelogs);

        Changelog changelog = changelogs.first();
        Assert.equals(PATH, changelog.getPath());
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

    @Test
    public void userVersionIncreases() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        long oldVersion = now.getMillis() * 1000 - 111111;
        userDao.setVersion(UID, oldVersion);

        filesystem.createFolder(PRINCIPAL, PATH);

        User user = userDao.find(UID).map(UserData::toUser).get();
        Assert.equals(now.getMillis() * 1000, user.version.get());
    }

    @Test
    public void userVersionDoesNotDecrease() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        long oldVersion = now.getMillis() * 1000 + 222222;
        userDao.setVersion(UID, oldVersion);

        filesystem.createFolder(PRINCIPAL, PATH);

        User user = userDao.find(UID).map(UserData::toUser).get();
        Assert.equals(oldVersion, user.version.get());
    }

    @Test
    public void xivaPushTask() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        long oldVersion = now.getMillis() * 1000 - 333333;
        userDao.setVersion(UID, oldVersion);

        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, PATH);

        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID));
        CeleryJob celeryJob = mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID).first();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();
        Assert.equals(UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals(String.valueOf(now.getMillis() * 1000), ((JsonString) kwargs.getTs("new_version")).getValue());
        Assert.equals(oldVersion, ((JsonNumber) kwargs.getTs("old_version")).longValue());
        Assert.equals("diff", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("diff", ((JsonString) kwargs.getTs("action_name")).getValue());
        Assert.none(kwargs.getO("connection_id"));

        JsonObject xivaData = (JsonObject) ((JsonArray) kwargs.getTs("xiva_data")).getArray().get(0);
        Assert.equals(PATH.getPath(), ((JsonString) xivaData.get("key")).getValue());
        Assert.equals("dir", ((JsonString) xivaData.get("resource_type")).getValue());
        Assert.equals(folder.getFileId().get().getValue(), ((JsonString) xivaData.get("fid")).getValue());
        Assert.equals("new", ((JsonString) xivaData.get("op")).getValue());
    }

    @Test
    public void xivaPushTaskUsesConnectionIdIfSet() {
        try (SafeCloseable ignored = ConnectionIdHolder.set("random_connection")) {
            filesystem.createFolder(PRINCIPAL, PATH);
        }

        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID));
        CeleryJob celeryJob = mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID).first();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();
        Assert.equals("random_connection", ((JsonString) kwargs.getTs("connection_id")).getValue());
    }

    @Test
    public void indexTask() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, PATH);

        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.TASK_ID));
        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.SHARED_TASK_ID));
        CeleryJob celeryJob =
                mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.TASK_ID).first();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();

        JsonObject data = (JsonObject) ((JsonArray) kwargs.getTs("data")).getArray().get(0);
        Assert.equals(UID.asLong(), ((JsonNumber) data.get("uid")).longValue());
        Assert.equals(PATH.getPath(), ((JsonString) data.get("id")).getValue());
        Assert.equals(folder.getResourceId().get().getFileId().getValue(),
                ((JsonString) data.get("file_id")).getValue());
        Assert.equals(folder.getResourceId().get().toString(), ((JsonString) data.get("resource_id")).getValue());
        Assert.equals(PATH.getName(), ((JsonString) data.get("name")).getValue());
        Assert.equals(now.getMillis() * 1000, ((JsonNumber) data.get("version")).longValue());
        Assert.equals(InstantUtils.toSeconds(now), ((JsonNumber) data.get("ctime")).intValue());
        Assert.equals(InstantUtils.toSeconds(now), ((JsonNumber) data.get("mtime")).intValue());
        Assert.equals(1, ((JsonNumber) data.get("visible")).intValue());
        Assert.equals("mkdir", ((JsonString) data.get("operation")).getValue());
        Assert.equals("modify", ((JsonString) data.get("action")).getValue());
        Assert.equals("dir", ((JsonString) data.get("type")).getValue());
        Assert.isInstance(data.get("shared_folder_owner"), JsonNull.class);
        Assert.isInstance(data.get("folder_type"), JsonNull.class);
    }

    @Test
    public void eventHistory() {
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, PATH);

        Assert.sizeIs(1, mockEventHistoryLogger.messageData);
        MapF<String, String> actual = mockEventHistoryLogger.messageData.get(0);
        Assert.sizeIs(7, actual);
        Assert.equals("fs-mkdir", actual.getTs("event_type"));
        Assert.equals(folder.getResourceId().get().toString(), actual.getTs("tgt_folder_id"));
        Assert.equals(PATH.toString(), actual.getTs("tgt_rawaddress"));
        Assert.equals(UID.asString(), actual.getTs("uid"));
        Assert.equals(folder.getResourceId().get().getFileId().getValue(), actual.getTs("resource_file_id"));
        Assert.equals(UID.asString(), actual.getTs("owner_uid"));
        Assert.equals("dir", actual.getTs("resource_type"));
    }

    @Test
    public void noIndexForNonStandardUser() {
        userDao.changeType(UID, UserType.ATTACH);
        filesystem.createFolder(PRINCIPAL, PATH);
        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.TASK_ID));
    }

    @Test(expected = FilesystemException.class)
    public void noParentFolderThrowsException() {
        filesystem.createFolder(PRINCIPAL, PATH.getChildPath("folder"));
    }

    @Test(expected = UserNotInitializedException.class)
    public void nonexistentUidThrowsException() {
        filesystem.createFolder(DjfsPrincipal.cons(DjfsUid.cons(111)), PATH);
    }

    @Test(expected = ResourceExistsException.class)
    public void createOverAlreadyExistingFolderThrowsException() {
        filesystem.createFolder(PRINCIPAL, PATH);
        filesystem.createFolder(PRINCIPAL, PATH);
    }

    @Test
    public void createOverAlreadyExistingFolderWithDifferentIdInPgThrowsException() {
        djfsResourceDao.insert(FolderDjfsResource.cons(PATH).toBuilder().id(UUID.randomUUID()).build());
        Assert.assertThrows(() -> filesystem.createFolder(PRINCIPAL, PATH), ResourceExistsException.class);
    }

    @Test(expected = ResourceExistsException.class)
    public void createOverAlreadyExistingFileThrowsException() {
        filesystem.createFile(PRINCIPAL, PATH);
        filesystem.createFolder(PRINCIPAL, PATH);
    }

    @Test
    public void createOverAlreadyExistingFileWithDifferentIdInPgThrowsException() {
        djfsResourceDao.insert(FileDjfsResource.random(PATH, x -> x.id(UUID.randomUUID())));
        Assert.assertThrows(() -> filesystem.createFolder(PRINCIPAL, PATH), ResourceExistsException.class);
    }

    @Test(expected = FilesystemException.class)
    public void createInAttachThrowsException() {
        filesystem.createFolder(PRINCIPAL, DjfsResourcePath.cons(UID, "/attach/folder"));
    }

    @Test(expected = InvalidNewFolderNameException.class)
    public void longNewFolderNameThrowsException() {
        filesystem.createFolder(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/" + StringUtils.repeat("a", 1000)));
    }

    @Test(expected = InvalidNewFolderNameException.class)
    public void longNewFolderPathThrowsException() {
        String part = "/" + StringUtils.repeat("a", 200);
        filesystem.createFolder(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk" + StringUtils.repeat(part, 200)));
    }

    @Test(expected = InvalidNewFolderNameException.class)
    public void dotInNewFolderNameThrowsException() {
        filesystem.createFolder(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/."));
    }

    @Test(expected = InvalidNewFolderNameException.class)
    public void dotInNewFolderPathThrowsException() {
        filesystem.createFolder(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/./folder"));
    }

    @Test(expected = InvalidNewFolderNameException.class)
    public void twoDotsInNewFolderNameThrowsException() {
        filesystem.createFolder(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/.."));
    }

    @Test(expected = InvalidNewFolderNameException.class)
    public void twoDotsInNewFolderPathThrowsException() {
        filesystem.createFolder(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/../folder"));
    }

    @Test
    public void createYaFotkiInAttachAsSystem() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        long oldVersion = now.getMillis() * 1000 - 111111;
        userDao.setVersion(UID, oldVersion);

        filesystem.createFolder(DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(UID, "/attach/YaFotki"));

        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID));
        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.TASK_ID));
        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.SHARED_TASK_ID));
        Assert.sizeIs(0, mockEventHistoryLogger.messageData);

        User user = userDao.find(UID).map(UserData::toUser).get();
        Assert.equals(oldVersion, user.version.get());
        Assert.assertContains(user.collections, DjfsResourceArea.ATTACH.mongoCollectionName);

        ListF<Changelog> changelogs = changelogDao.findAll(UID);
        Assert.sizeIs(0, changelogs);
    }

    @Test
    public void createSubfolderInYaFotkiAttachAsSystem() {
        filesystem.createFolder(DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(UID, "/attach/YaFotki"));
        filesystem.createFolder(DjfsPrincipal.SYSTEM, DjfsResourcePath.cons(UID, "/attach/YaFotki/subfolder"));
    }

    @Test
    public void createShareProductionFolderInShare() {
        DjfsPrincipal principal = DjfsPrincipal.cons(DjfsUid.SHARE_PRODUCTION);
        initializeUser(DjfsUid.SHARE_PRODUCTION, 1);
        filesystem.createFolder(principal, DjfsResourcePath.cons(DjfsUid.SHARE_PRODUCTION, "/share"));
        filesystem.createFolder(principal, DjfsResourcePath.cons(DjfsUid.SHARE_PRODUCTION, "/share/dist"));
    }

    @Test
    public void initializeArea() {
        DjfsResourcePath rootPath = DjfsResourcePath.root(UID, DjfsResourceArea.NOTES);
        DjfsResourcePath areaPath = DjfsResourcePath.areaRoot(UID, DjfsResourceArea.NOTES);
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/notes/random");
        Assert.none(djfsResourceDao.find(areaPath));

        filesystem.createFolder(PRINCIPAL, path);

        FolderDjfsResource folder = (FolderDjfsResource) djfsResourceDao.find(path).get();
        FolderDjfsResource areaRoot = (FolderDjfsResource) djfsResourceDao.find(areaPath).get();
        FolderDjfsResource root = (FolderDjfsResource) djfsResourceDao.find(rootPath).get();

        Assert.equals(areaRoot.getId(), folder.getParentId().get());
        Assert.equals(root.getId(), areaRoot.getParentId().get());
        Assert.none(root.getParentId());
    }
}
