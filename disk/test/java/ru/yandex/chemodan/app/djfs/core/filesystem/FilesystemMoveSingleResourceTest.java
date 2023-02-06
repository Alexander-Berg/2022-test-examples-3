package ru.yandex.chemodan.app.djfs.core.filesystem;

import com.mongodb.ReadPreference;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.djfs.core.changelog.Changelog;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.DjfsNotImplementedException;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.ForbiddenResourcePathException;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.ResourceExistsException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.move.MoveCallbacks;
import ru.yandex.chemodan.app.djfs.core.index.SearchPushGenerator;
import ru.yandex.chemodan.app.djfs.core.notification.XivaPushGenerator;
import ru.yandex.chemodan.app.djfs.core.publication.LinkData;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSinglePostgresUserTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.User;
import ru.yandex.chemodan.app.djfs.core.user.UserData;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.chemodan.queller.celery.job.CeleryJob;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.JsonValue;
import ru.yandex.misc.test.Assert;


public class FilesystemMoveSingleResourceTest extends DjfsSinglePostgresUserTestBase {

    @Test(expected = DjfsNotImplementedException.class)
    public void checkSupportedOperationForNotQuickMoveUser() {
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath existingFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/folder");
        DjfsResourcePath destinationFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/new");

        filesystem.createFolder(PRINCIPAL, existingFolderPath);

        filesystem.moveSingleResource(PRINCIPAL, existingFolderPath, destinationFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void checkQuickMoveUserWithDifferentAreas() {
        makeQuickMoveUser(UID);
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath existingFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/folder");
        DjfsResourcePath destinationFolderPath = DjfsResourcePath.cons(user.getUid(), "/hidden/new");

        filesystem.createFolder(PRINCIPAL, existingFolderPath);

        filesystem.moveSingleResource(PRINCIPAL, existingFolderPath, destinationFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void dontAllowMoveInsideTrash() {
        makeQuickMoveUser(UID);
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath existingFolderPath = DjfsResourcePath.cons(user.getUid(), "/trash/folder");
        DjfsResourcePath destinationFolderPath = DjfsResourcePath.cons(user.getUid(), "/trash/new");

        filesystem.createFolder(PRINCIPAL, existingFolderPath);

        filesystem.moveSingleResource(PRINCIPAL, existingFolderPath, destinationFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void dontAllowMoveFromHiddenToTrash() {
        makeQuickMoveUser(UID);
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath existingFolderPath = DjfsResourcePath.cons(user.getUid(), "/hidden/folder");
        DjfsResourcePath destinationFolderPath = DjfsResourcePath.cons(user.getUid(), "/trash/new");

        filesystem.createFolder(PRINCIPAL, existingFolderPath);

        filesystem.moveSingleResource(PRINCIPAL, existingFolderPath, destinationFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void checkSharedFolderOwnerMoveFromShared() {
        makeQuickMoveUser(UID);
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath shareFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/folder");
        filesystem.createFolder(PRINCIPAL, shareFolderPath);
        shareManager.createGroup(shareFolderPath);

        DjfsResourcePath existingFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/folder/inside");
        DjfsResourcePath destinationFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/new");

        filesystem.createFolder(PRINCIPAL, existingFolderPath);

        filesystem.moveSingleResource(PRINCIPAL, existingFolderPath, destinationFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void checkSharedFolderOwnerMoveSharedRoot() {
        makeQuickMoveUser(UID);
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath shareFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/folder");
        filesystem.createFolder(PRINCIPAL, shareFolderPath);
        shareManager.createGroup(shareFolderPath);

        DjfsResourcePath destinationFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/new");

        filesystem.moveSingleResource(PRINCIPAL, shareFolderPath, destinationFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void checkSharedFolderParticipantMoveFromShared() {
        DjfsUid OWNER_UID = DjfsUid.cons(313378);
        DjfsPrincipal OWNER_PRINCIPAL = DjfsPrincipal.cons(OWNER_UID);
        initializePgUser(OWNER_UID, 1);

        makeQuickMoveUser(UID);
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath shareFolderPath = DjfsResourcePath.cons(OWNER_UID, "/disk/folder");
        filesystem.createFolder(OWNER_PRINCIPAL, shareFolderPath);
        String groupId = shareManager.createGroup(shareFolderPath).getId();

        DjfsResourcePath participantFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/participant");
        shareManager.createLink(groupId, participantFolderPath, SharePermissions.READ_WRITE);

        DjfsResourcePath sourceFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/participant/folder");
        DjfsResourcePath destinationFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/new");

        filesystem.createFolder(PRINCIPAL, sourceFolderPath);

        filesystem.moveSingleResource(PRINCIPAL, sourceFolderPath, destinationFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void checkSharedFolderParticipantMoveSharedRoot() {
        DjfsUid OWNER_UID = DjfsUid.cons(313378);
        DjfsPrincipal OWNER_PRINCIPAL = DjfsPrincipal.cons(OWNER_UID);
        initializePgUser(OWNER_UID, 1);

        makeQuickMoveUser(UID);
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath shareFolderPath = DjfsResourcePath.cons(OWNER_UID, "/disk/folder");
        filesystem.createFolder(OWNER_PRINCIPAL, shareFolderPath);
        String groupId = shareManager.createGroup(shareFolderPath).getId();

        DjfsResourcePath participantFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/participant");
        shareManager.createLink(groupId, participantFolderPath, SharePermissions.READ_WRITE);

        DjfsResourcePath destinationFolderPath = DjfsResourcePath.cons(user.getUid(), "/disk/new");

        filesystem.moveSingleResource(PRINCIPAL, participantFolderPath, destinationFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = ResourceExistsException.class)
    public void checkExistingDestinationWithoutForce() {
        makeQuickMoveUser(UID);
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath folder1 = DjfsResourcePath.cons(user.getUid(), "/disk/folder");
        DjfsResourcePath folder2 = DjfsResourcePath.cons(user.getUid(), "/disk/new");
        DjfsResourcePath folder3 = folder1.getChildPath("new");

        filesystem.createFolder(PRINCIPAL, folder1);
        filesystem.createFolder(PRINCIPAL, folder2);
        filesystem.createFolder(PRINCIPAL, folder3);

        filesystem.moveSingleResource(PRINCIPAL, folder3, folder1, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void checkForceWithExistingDestination() {
        makeQuickMoveUser(UID);
        User user = userDao.find(UID).map(UserData::toUser).get();

        DjfsResourcePath folder1 = DjfsResourcePath.cons(user.getUid(), "/disk/folder");
        DjfsResourcePath folder2 = DjfsResourcePath.cons(user.getUid(), "/disk/new");
        DjfsResourcePath folder3 = folder1.getChildPath("new");

        filesystem.createFolder(PRINCIPAL, folder1);
        filesystem.createFolder(PRINCIPAL, folder2);
        filesystem.createFolder(PRINCIPAL, folder3);

        filesystem.moveSingleResource(PRINCIPAL, folder3, folder1, true,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void testFileMoveNotImplemented() {
        makeQuickMoveUser(UID);

        DjfsResourcePath filePath = DjfsResourcePath.cons(UID, "/disk/test.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        DjfsResourcePath fileDestinationPath = DjfsResourcePath.cons(UID, "/disk/new.txt");
        filesystem.moveSingleResource(PRINCIPAL, filePath, fileDestinationPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test
    public void testFolderMoveSameUserSameArea() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath subresoucePath = folderPath.getChildPath("file-1.txt");
        filesystem.createFile(PRINCIPAL, subresoucePath);

        DjfsResourcePath newFolderParentPath = DjfsResourcePath.cons(UID, "/disk/parent");
        filesystem.createFolder(PRINCIPAL, newFolderParentPath);
        DjfsResourcePath newFolderPath = newFolderParentPath.getChildPath("new");

        mockCeleryTaskManager.submitted.clear();
        mockEventHistoryLogger.messageData.clear();

        filesystem.moveSingleResource(PRINCIPAL, folderPath, newFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        Option<DjfsResource> newFolderO = filesystem.find(PRINCIPAL, newFolderPath, Option.of(ReadPreference.primary()));
        Assert.some(newFolderO);

        DjfsResourcePath newSubresourcePath = subresoucePath.changeParent(folderPath, newFolderPath);
        Assert.some(filesystem.find(PRINCIPAL, newSubresourcePath, Option.of(ReadPreference.primary())));
    }

    @Test
    public void testLargeFolderMove() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);

        for (int i = 0; i < 100; ++i) {
            DjfsResourcePath subresoucePath = folderPath.getChildPath("file-" + i + ".txt");
            filesystem.createFile(PRINCIPAL, subresoucePath);
        }
        for (int j = 0; j < 10; ++j) {
            DjfsResourcePath subFolderPath = folderPath.getChildPath("folder-" + j);
            filesystem.createFolder(PRINCIPAL, subFolderPath);
            for (int i = 0; i < 50; ++i) {
                DjfsResourcePath subresoucePath = subFolderPath.getChildPath("file-" + i + ".txt");
                filesystem.createFile(PRINCIPAL, subresoucePath);
            }
        }

        DjfsResourcePath newFolderParentPath = DjfsResourcePath.cons(UID, "/disk/parent");
        filesystem.createFolder(PRINCIPAL, newFolderParentPath);
        DjfsResourcePath newFolderPath = newFolderParentPath.getChildPath("new");

        mockCeleryTaskManager.submitted.clear();
        mockEventHistoryLogger.messageData.clear();
        changelogDao.deleteAll(UID);

        filesystem.moveSingleResource(PRINCIPAL, folderPath, newFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        Option<DjfsResource> newFolderO = filesystem.find(PRINCIPAL, newFolderPath, Option.of(ReadPreference.primary()));
        Assert.some(newFolderO);

        User user = userDao.find(UID).map(UserData::toUser).get();
        Assert.equals(user.version, newFolderO.get().getVersion());
        Assert.equals(user.minimumDeltaVersion, newFolderO.get().getVersion());

        // check changelog
        ListF<Changelog> changelogs = changelogDao.findAll(UID);
        ListF<Changelog> movedDeltas =
                changelogs.filter(c -> c.getOperationType().equals(Changelog.OperationType.MOVED));
        Assert.equals(movedDeltas.length(), 1);
        Changelog movedDelta = movedDeltas.get(0);
        Assert.some(movedDelta.getVersion(), newFolderO.get().getVersion());
        Assert.some(newFolderPath, movedDelta.getNewPath());

        Assert.isEmpty(changelogs.filter(c -> c.getOperationType().equals(Changelog.OperationType.NEW)));
        Assert.isEmpty(changelogs.filter(c -> c.getOperationType().equals(Changelog.OperationType.DELETED)));

        // check search push + event history log
        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.PHOTOSLICE_TASK_ID));
        Assert.sizeIs(1, mockEventHistoryLogger.messageData.filter(
                x -> x.getOrElse("event_type", "").equals("fs-move"))
        );

        // check xiva data
        ListF<CeleryJob> xivaTasks = mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID);
        Assert.sizeIs(1, xivaTasks);
        JsonValue xivaData = xivaTasks.get(0).getKwargs().getO("xiva_data").get();
        Assert.isInstance(xivaData, JsonArray.class);

        ListF<JsonObject> xivaObjects = Cf.toList(((JsonArray)xivaData).getArray()).cast();
        Assert.sizeIs(1, xivaObjects.filter(x -> ((JsonString)x.get("op")).getString().equals("new")));
        Assert.sizeIs(1, xivaObjects.filter(x -> ((JsonString)x.get("op")).getString().equals("deleted")));
    }

    @Test
    public void testSmallFolderMove() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);

        int childrenCount = 0;
        ListF<DjfsResourcePath> childPaths = Cf.arrayList();

        for (int i = 0; i < 10; ++i) {
            DjfsResourcePath subresoucePath = folderPath.getChildPath("file-" + i + ".txt");
            filesystem.createFile(PRINCIPAL, subresoucePath);
            childrenCount++;
            childPaths.add(subresoucePath);
        }
        for (int j = 0; j < 2; ++j) {
            DjfsResourcePath subFolderPath = folderPath.getChildPath("folder-" + j);
            filesystem.createFolder(PRINCIPAL, subFolderPath);
            childrenCount++;
            childPaths.add(subFolderPath);

            for (int i = 0; i < 10; ++i) {
                DjfsResourcePath subresoucePath = subFolderPath.getChildPath("file-" + i + ".txt");
                filesystem.createFile(PRINCIPAL, subresoucePath);
                childrenCount++;
                childPaths.add(subresoucePath);
            }
        }

        DjfsResourcePath newFolderParentPath = DjfsResourcePath.cons(UID, "/disk/parent");
        filesystem.createFolder(PRINCIPAL, newFolderParentPath);
        DjfsResourcePath newFolderPath = newFolderParentPath.getChildPath("new");

        mockCeleryTaskManager.submitted.clear();
        mockEventHistoryLogger.messageData.clear();
        changelogDao.deleteAll(UID);

        filesystem.moveSingleResource(PRINCIPAL, folderPath, newFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        Option<DjfsResource> newFolderO = filesystem.find(PRINCIPAL, newFolderPath, Option.of(ReadPreference.primary()));
        Assert.some(newFolderO);
        SetF<DjfsResourcePath> newChildPaths = Cf.toSet(childPaths.map(x -> x.changeParent(folderPath, newFolderPath)));

        User user = userDao.find(UID).map(UserData::toUser).get();
        Assert.equals(user.version, newFolderO.get().getVersion());
        Assert.assertEmpty(user.minimumDeltaVersion);

        // check changelog
        ListF<Changelog> changelogs = changelogDao.findAll(UID);
        Assert.isEmpty(changelogs.filter(c -> c.getOperationType().equals(Changelog.OperationType.MOVED)));

        ListF<Changelog> changelogNew = changelogs.filter(c -> c.getOperationType().equals(Changelog.OperationType.NEW));
        Assert.sizeIs(childrenCount + 1, changelogNew);
        Assert.equals(newChildPaths.plus(newFolderPath), Cf.toSet(changelogNew.map(Changelog::getPath)));

        Assert.sizeIs(1, changelogs.filter(c -> c.getOperationType().equals(Changelog.OperationType.DELETED)));

        // check search push + event history log
        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.PHOTOSLICE_TASK_ID));
        Assert.sizeIs(1, mockEventHistoryLogger.messageData.filter(
                x -> x.getOrElse("event_type", "").equals("fs-move"))
        );

        // check xiva data
        ListF<CeleryJob> xivaTasks = mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID);
        Assert.sizeIs(1, xivaTasks);
        JsonValue xivaData = xivaTasks.get(0).getKwargs().getO("xiva_data").get();
        Assert.isInstance(xivaData, JsonArray.class);

        ListF<JsonObject> xivaObjects = Cf.toList(((JsonArray)xivaData).getArray()).cast();
        Assert.sizeIs(1, xivaObjects.filter(x -> ((JsonString)x.get("op")).getString().equals("deleted")));

        ListF<JsonObject> xivaObjectsNew = xivaObjects.filter(x -> ((JsonString)x.get("op")).getString().equals("new"));
        Assert.sizeIs(childrenCount + 1, xivaObjectsNew);

        Assert.equals(Cf.toSet(newChildPaths.plus(newFolderPath).map(DjfsResourcePath::getPath)),
                Cf.toSet(xivaObjectsNew.map(x -> ((JsonString)x.get("key")).getString())));
    }

    @Test
    public void checkYarovayaMarkSetForLegacyExternalPublicResources() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL, folderPath, x -> x.isPublic(true));

        LinkData rootLinkData = LinkData.builder()
                .id(UuidUtils.randomToHexString())
                .linkDataPath("/")
                .type("dir")
                .uid(UID)
                .build();
        linkDataDao.insert(rootLinkData);
        LinkData linkData = LinkData.builder()
                .id(UuidUtils.randomToHexString())
                .linkDataPath("/123")
                .type("file")
                .parentId(Option.of(rootLinkData.getId()))
                .uid(UID)
                .targetPath(Option.of(folderPath))
                .resourceId(Option.of(DjfsResourceId.cons(UID, folder.getFileId().get())))
                .build();
        linkDataDao.insert(linkData);

        DjfsResourcePath innerFolder = folderPath.getChildPath("subfolder");
        filesystem.createFolder(PRINCIPAL, innerFolder);
        filesystem.createFile(PRINCIPAL, innerFolder.getChildPath("file.txt"));

        Option<DjfsResource> srcFolder = filesystem.find(PRINCIPAL, innerFolder, Option.of(ReadPreference.primary()));
        Assert.some(srcFolder);
        Assert.isFalse(srcFolder.get().hasYarovayaMark());

        DjfsResourcePath destinationSubfolderPath = DjfsResourcePath.cons(UID, "/disk/new");
        filesystem.moveSingleResource(PRINCIPAL, innerFolder, destinationSubfolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        Option<DjfsResource> destFolder = filesystem.find(PRINCIPAL, destinationSubfolderPath, Option.of(ReadPreference.primary()));
        Assert.some(destFolder);
        Assert.isTrue(destFolder.get().hasYarovayaMark());
    }

    @Test
    public void checkYarovayaMarkSetForFolderWithParentWithYarovayaMark() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath, x -> x.hasYarovayaMark(true));

        DjfsResourcePath innerFolder = folderPath.getChildPath("subfolder");
        filesystem.createFolder(PRINCIPAL, innerFolder);
        filesystem.createFile(PRINCIPAL, innerFolder.getChildPath("file.txt"));

        Option<DjfsResource> srcFolder = filesystem.find(PRINCIPAL, innerFolder, Option.of(ReadPreference.primary()));
        Assert.some(srcFolder);
        Assert.isFalse(srcFolder.get().hasYarovayaMark());

        DjfsResourcePath destinationSubfolderPath = DjfsResourcePath.cons(UID, "/disk/new");
        filesystem.moveSingleResource(PRINCIPAL, innerFolder, destinationSubfolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        Option<DjfsResource> destFolder = filesystem.find(PRINCIPAL, destinationSubfolderPath, Option.of(ReadPreference.primary()));
        Assert.some(destFolder);
        Assert.isTrue(destFolder.get().hasYarovayaMark());
    }

    @Test
    public void checkYarovayaMarkSetForFolderWithPublicFields() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);

        DjfsResourcePath innerFolder = folderPath.getChildPath("subfolder");
        filesystem.createFolder(PRINCIPAL, innerFolder, x -> x.isPublic(true));
        filesystem.createFile(PRINCIPAL, innerFolder.getChildPath("file.txt"));

        Option<DjfsResource> srcFolder = filesystem.find(PRINCIPAL, innerFolder, Option.of(ReadPreference.primary()));
        Assert.some(srcFolder);
        Assert.isFalse(srcFolder.get().hasYarovayaMark());

        DjfsResourcePath destinationSubfolderPath = DjfsResourcePath.cons(UID, "/disk/new");
        filesystem.moveSingleResource(PRINCIPAL, innerFolder, destinationSubfolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        Option<DjfsResource> destFolder = filesystem.find(PRINCIPAL, destinationSubfolderPath, Option.of(ReadPreference.primary()));
        Assert.some(destFolder);
        Assert.isTrue(destFolder.get().hasYarovayaMark());
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void testMoveFolderToPhotounlimIsForbidden() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath subresoucePath = folderPath.getChildPath("file-1.txt");
        filesystem.createFile(PRINCIPAL, subresoucePath);

        DjfsResourcePath newFolderPath = DjfsResourcePath.cons(UID, "/photounlim/folder");

        filesystem.moveSingleResource(PRINCIPAL, folderPath, newFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test
    public void testMoveFromDiskToTrashRoot() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath subresoucePath = folderPath.getChildPath("file-1.txt");
        filesystem.createFile(PRINCIPAL, subresoucePath);

        DjfsResourcePath newFolderPath = DjfsResourcePath.cons(UID, "/trash/folder");

        Instant beforeRemove = Instant.now();
        filesystem.moveSingleResource(PRINCIPAL, folderPath, newFolderPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        Option<DjfsResource> resourceInTrashO = filesystem.find(PRINCIPAL, newFolderPath, Option.of(ReadPreference.primary()));
        Assert.some(resourceInTrashO);

        FolderDjfsResource removedFolder = ((FolderDjfsResource) resourceInTrashO.get());
        Assert.some(removedFolder.getTrashAppendTime());
        Assert.le(beforeRemove, removedFolder.getTrashAppendTime().get());
        Assert.some(folderPath.getPath(), removedFolder.getTrashAppendOriginalPath());

        Option<DjfsResource> removedSubresource =
                filesystem.find(PRINCIPAL, subresoucePath.changeArea(DjfsResourceArea.TRASH), Option.of(ReadPreference.primary()));
        Assert.some(removedSubresource);
        Assert.isFalse(removedSubresource.get().getTrashAppendTime().isPresent());
        Assert.isFalse(removedSubresource.get().getTrashAppendOriginalPath().isPresent());
    }

    @Test(expected = ForbiddenResourcePathException.class)
    public void testMoveFromDiskToTrashNotRoot() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath subresoucePath = folderPath.getChildPath("file-1.txt");
        filesystem.createFile(PRINCIPAL, subresoucePath);

        DjfsResourcePath folderInTrash = DjfsResourcePath.cons(UID, "/trash/folder");
        DjfsResourcePath trashDeepFolder = folderInTrash.getChildPath("subfolder");

        filesystem.moveSingleResource(PRINCIPAL, folderPath, trashDeepFolder, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test(expected = DjfsNotImplementedException.class)
    public void testMoveFileFromDiskToTrashRoot() {
        makeQuickMoveUser(UID);

        DjfsResourcePath filePath = DjfsResourcePath.cons(UID, "/disk/file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        DjfsResourcePath trashPath = filePath.changeArea(DjfsResourceArea.TRASH);

        filesystem.moveSingleResource(PRINCIPAL, filePath, trashPath, false,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
    }

    @Test
    public void trashAppendFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        filesystem.trashAppendResource(PRINCIPAL, folderPath,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        Assert.some(filesystem.find(PRINCIPAL, folderPath.changeArea(DjfsResourceArea.TRASH), Option.of(ReadPreference.primary())));
    }

    @Test
    public void trashAppendFolderWithExistingTarget() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        DjfsResourcePath trashPath = DjfsResourcePath.cons(UID, "/trash/folder");
        filesystem.createFolder(PRINCIPAL, trashPath);
        Assert.some(filesystem.find(PRINCIPAL, folderPath.changeArea(DjfsResourceArea.TRASH), Option.of(ReadPreference.primary())));

        DjfsResource removedFolder = filesystem.trashAppendResource(PRINCIPAL, folderPath,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));
        Assert.equals(DjfsResourceArea.TRASH, removedFolder.getPath().getArea());
        Assert.some(filesystem.find(PRINCIPAL, removedFolder.getPath(), Option.of(ReadPreference.primary())));
    }

    @Test
    public void checkModificationTimeChangeForFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/folder");
        filesystem.createFolder(PRINCIPAL, folderPath, x -> x.modificationTime(Instant.now().minus(100500)));
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        FolderDjfsResource folderBeforeMove = (FolderDjfsResource) filesystem.find(PRINCIPAL, folderPath, Option.of(ReadPreference.primary())).get();
        FileDjfsResource fileBeforeMove = (FileDjfsResource) filesystem.find(PRINCIPAL, filePath, Option.of(ReadPreference.primary())).get();

        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/new");
        filesystem.moveSingleResource(PRINCIPAL, folderPath, destinationPath, false, MoveCallbacks.empty());

        Option<DjfsResource> folderAfterMove = filesystem.find(PRINCIPAL, destinationPath, Option.of(ReadPreference.primary()));
        Option<DjfsResource> fileAfterMove = filesystem.find(PRINCIPAL, filePath.changeParent(folderPath, destinationPath), Option.of(ReadPreference.primary()));

        Assert.some(folderAfterMove);
        Assert.some(fileAfterMove);

        Option<Instant> folderModificationTime = ((FolderDjfsResource) folderAfterMove.get()).getModificationTime();
        Assert.some(folderModificationTime);
        Assert.lt(folderBeforeMove.getModificationTime().get(), folderModificationTime.get());

        Instant fileModificationTime = ((FileDjfsResource) fileAfterMove.get()).getModificationTime();
        Assert.equals(fileBeforeMove.getModificationTime(), fileModificationTime);
    }

    @Test
    public void trashAppendAttachFolder() {
        makeQuickMoveUser(UID);

        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/attach/folder");
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folderPath);
        DjfsResourcePath filePath = folderPath.getChildPath("file.txt");
        filesystem.createFile(PRINCIPAL, filePath);

        filesystem.trashAppendResource(PRINCIPAL, folderPath,
                MoveCallbacks.defaultWithLogging(mockEventHistoryLogger));

        Assert.some(filesystem.find(PRINCIPAL, folderPath.changeArea(DjfsResourceArea.TRASH), Option.of(ReadPreference.primary())));
    }
}
