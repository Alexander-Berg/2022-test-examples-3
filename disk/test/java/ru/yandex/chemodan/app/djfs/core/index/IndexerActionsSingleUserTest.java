package ru.yandex.chemodan.app.djfs.core.index;

import java.util.UUID;

import com.mongodb.ReadPreference;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.client.DiskSearchResponseItem;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.UserData;
import ru.yandex.chemodan.app.djfs.core.user.UserNotInitializedException;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.commune.a3.action.WebRequestMock;
import ru.yandex.misc.bender.BenderMapper;
import ru.yandex.misc.bender.parse.BenderParser;
import ru.yandex.misc.test.Assert;

import static ru.yandex.chemodan.app.djfs.core.user.UserData.USER_OBJ;


public class IndexerActionsSingleUserTest extends DjfsSingleUserTestBase {
    @Autowired
    private IndexerActions indexerActions;

    @Autowired
    private IndexerManager indexerManager;

    @Test
    public void checkFidAndParentFidsForFolder() {
        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk/not_shared_folder");

        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL, DjfsResourcePath.cons(UID, folderPath.getPath()));
        DjfsResourceId resourceId = folder.getResourceId().get();
        WebRequestMock webRequestMock = new WebRequestMock();
        webRequestMock.getHttpServletRequest().setAttribute(USER_OBJ, USER);

        IndexerResourcesListPojo response =
                indexerActions.getResourcesByResourceIds(Cf.list(resourceId.toString()),
                        UID.toString(), createRequestWithUserObj(USER));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.equals(folderPath.getPath(), item.key);
        Assert.equals("dir", item.type);

        Assert.equals(item.fid, UuidUtils.toHexString(folder.getId()));

        util.fs.assertParentIds(item.parentFids, folderPath, true);
    }

    @Test
    public void checkFidAndParentFidsForDiskFolder() {
        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID, "/disk");

        FolderDjfsResource folder = (FolderDjfsResource) filesystem.find(PRINCIPAL, folderPath, Option.of(ReadPreference.primary())).get();
        DjfsResourceId resourceId = folder.getResourceId().get();
        IndexerResourcesListPojo response =
                indexerActions.getResourcesByResourceIds(Cf.list(resourceId.toString()),
                        UID.toString(), createRequestWithUserObj(USER));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.equals(folderPath.getPath(), item.key);
        Assert.equals("dir", item.type);

        Assert.equals(item.fid, UuidUtils.toHexString(folder.getId()));
        Assert.isEmpty(item.parentFids);
    }

    @Test
    public void checkFidAndParentFidsForFile() {
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID, "/disk/not_shared_file.txt");
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL, filePath);
        DjfsResourceId resourceId = file.getResourceId().get();
        IndexerResourcesListPojo response =
                indexerActions.getResourcesByResourceIds(Cf.list(resourceId.toString()),
                        UID.toString(), createRequestWithUserObj(USER));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.equals(filePath.getPath(), item.key);
        Assert.equals("file", item.type);

        Option<DjfsResource> parentFolder = djfsResourceDao.find(filePath.getParent());
        Assert.equals(UuidUtils.toHexString(parentFolder.get().getId()), item.parentFid);
    }

    @Test
    public void checkFidAndParentFidForFileInSharedRootParticipant() {
        DjfsUid participantUid = DjfsUid.cons(3331337);
        Assert.assertThrows(() -> userDao.find(participantUid), UserNotInitializedException.class);
        initializeUser(participantUid, 1);
        DjfsPrincipal participantPrincipal = DjfsPrincipal.cons(participantUid);

        DjfsResourcePath OWNER_PATH = DjfsResourcePath.cons(UID, "/disk/owner/share-owner");
        DjfsResourcePath OWNER_FILE_PATH = DjfsResourcePath.cons(UID, "/disk/owner/share-owner/file.txt");
        DjfsResourcePath PARTICIPANT_PATH =
                DjfsResourcePath.cons(participantUid, "/disk/participant/share-participant");

        filesystem.createFolder(PRINCIPAL, OWNER_PATH.getParent());
        filesystem.createFolder(PRINCIPAL, OWNER_PATH);
        FileDjfsResource sharedFile = filesystem.createFile(PRINCIPAL, OWNER_FILE_PATH);
        filesystem.createFolder(participantPrincipal, PARTICIPANT_PATH.getParent());
        UUID participantShareRootId = filesystem.createFolder(participantPrincipal, PARTICIPANT_PATH).getId();

        String groupId = shareManager.createGroup(OWNER_PATH).getId();
        shareManager.createLink(groupId, PARTICIPANT_PATH, SharePermissions.READ_WRITE);

        DjfsResourceId resourceId = sharedFile.getResourceId().get();
        UserData participantUser = userDao.find(participantUid)
                .getOrThrow(() -> new UserNotInitializedException(participantUid));
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(resourceId.toString()),
                participantUid.toString(),
                createRequestWithUserObj(participantUser)
        );

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);
        Assert.equals("/disk/participant/share-participant/file.txt", item.key);
        Assert.equals(UuidUtils.toHexString(participantShareRootId), item.parentFid);
    }

    @Test
    public void checkFidAndParentFidForFileInSharedRootOwner() {
        DjfsUid participantUid = DjfsUid.cons(331337);
        Assert.assertThrows(() -> userDao.find(participantUid), UserNotInitializedException.class);
        initializeUser(participantUid, 1);
        DjfsPrincipal participantPrincipal = DjfsPrincipal.cons(participantUid);

        DjfsResourcePath OWNER_PATH = DjfsResourcePath.cons(UID, "/disk/owner/share-owner");
        DjfsResourcePath OWNER_FILE_PATH = DjfsResourcePath.cons(UID, "/disk/owner/share-owner/file.txt");
        DjfsResourcePath PARTICIPANT_PATH =
                DjfsResourcePath.cons(participantUid, "/disk/participant/share-participant");

        filesystem.createFolder(PRINCIPAL, OWNER_PATH.getParent());
        UUID ownerSharedRootId = filesystem.createFolder(PRINCIPAL, OWNER_PATH).getId();
        FileDjfsResource sharedFile = filesystem.createFile(PRINCIPAL, OWNER_FILE_PATH);
        filesystem.createFolder(participantPrincipal, PARTICIPANT_PATH.getParent());
        filesystem.createFolder(participantPrincipal, PARTICIPANT_PATH);

        String groupId = shareManager.createGroup(OWNER_PATH).getId();
        shareManager.createLink(groupId, PARTICIPANT_PATH, SharePermissions.READ_WRITE);

        DjfsResourceId resourceId = sharedFile.getResourceId().get();
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(resourceId.toString()),
                UID.toString(),
                createRequestWithUserObj(USER)
        );

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);
        Assert.equals(OWNER_FILE_PATH.getPath(), item.key);
        Assert.equals(UuidUtils.toHexString(ownerSharedRootId), item.parentFid);
    }

    @Test
    public void fileFetchingForResourceIdOfUninitializedUser() {
        DjfsUid uninitializedUid = DjfsUid.cons(42424242);
        DjfsResourceId notExistingResourceId = DjfsResourceId.cons(uninitializedUid, DjfsFileId.random());

        Assert.assertThrows(() -> userDao.find(uninitializedUid), UserNotInitializedException.class);

        IndexerResourcesListPojo response =
                indexerActions.getResourcesByResourceIds(Cf.list(notExistingResourceId.toString()),
                        UID.toString(), createRequestWithUserObj(USER));

        Assert.isEmpty(response.items);
    }

    @Test
    public void sharedFolderWithGroupLinkButWithoutParticipantRoot() {
        DjfsUid participantUid = DjfsUid.cons(3313377);
        Assert.assertThrows(() -> userDao.find(participantUid), UserNotInitializedException.class);
        initializeUser(participantUid, 1);
        DjfsPrincipal participantPrincipal = DjfsPrincipal.cons(participantUid);

        DjfsResourcePath OWNER_PATH = DjfsResourcePath.cons(UID, "/disk/owner/share-owner");
        DjfsResourcePath PARTICIPANT_PATH =
                DjfsResourcePath.cons(participantUid, "/disk/participant/share-participant");

        filesystem.createFolder(PRINCIPAL, OWNER_PATH.getParent());
        filesystem.createFolder(PRINCIPAL, OWNER_PATH);
        FolderDjfsResource parentFolder = filesystem.createFolder(participantPrincipal, PARTICIPANT_PATH.getParent());
        // We do not create participant folder here:
        // filesystem.createFolder(participantPrincipal, PARTICIPANT_PATH);

        String groupId = shareManager.createGroup(OWNER_PATH).getId();
        shareManager.createLink(groupId, PARTICIPANT_PATH, SharePermissions.READ_WRITE);

        DjfsResourcePath folderPath = PARTICIPANT_PATH.getChildPath("subfolder");
        FolderDjfsResource folder = filesystem.createFolder(participantPrincipal, folderPath);

        DjfsResourceId resourceId = folder.getResourceId().get();
        UserData participantUser = userDao.find(participantUid)
                .getOrThrow(() -> new UserNotInitializedException(participantUid));
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(resourceId.toString(), parentFolder.getResourceId().get().toString()),
                participantUid.toString(), createRequestWithUserObj(participantUser)
        );

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);
        Assert.equals(parentFolder.getPath().getPath(), item.key);
    }

    @Test
    public void fetchExtractedDataFromSearch() {
        DjfsResourceId resourceId1 = DjfsResourceId.cons(UID, DjfsFileId.random());
        DjfsResourceId resourceId2 = DjfsResourceId.cons(UID, DjfsFileId.random());
        DjfsResourceId resourceId3 = DjfsResourceId.cons(UID, DjfsFileId.random());

        DjfsResourcePath filePath1 = DjfsResourcePath.cons(UID, "/disk/file-1.txt");
        DjfsResourcePath filePath2 = DjfsResourcePath.cons(UID, "/disk/file-2.txt");
        DjfsResourcePath filePath3 = DjfsResourcePath.cons(UID, "/disk/file-3.txt");

        filesystem.createFile(PRINCIPAL, filePath1, x -> x.fileId(resourceId1.getFileId()));
        filesystem.createFile(PRINCIPAL, filePath2, x -> x.fileId(resourceId2.getFileId()));
        filesystem.createFile(PRINCIPAL, filePath3, x -> x.fileId(resourceId3.getFileId()));

        ListF<DiskSearchResponseItem> diskSearchItems = Cf.list(
                new DiskSearchResponseItem(
                        Option.of(resourceId1.getFileId().getValue()),
                        Option.of("0.5"),
                        Option.of("800"),
                        Option.of("600"),
                        Option.of("landscape"),
                        Option.empty(),
                        Option.empty()
                ),
                new DiskSearchResponseItem(
                        Option.of(resourceId2.getFileId().getValue()),
                        Option.of("0.3"),
                        Option.of("768"),
                        Option.of("1024"),
                        Option.of("portrait"),
                        Option.empty(),
                        Option.empty()
                ),
                new DiskSearchResponseItem(
                        Option.of(resourceId3.getFileId().getValue()),
                        Option.of("0.42"),
                        Option.of("100"),
                        Option.of("200"),
                        Option.of("landscape"),
                        Option.empty(),
                        Option.empty()
                )
        );

        for (DiskSearchResponseItem item : diskSearchItems) {
            diskSearchHttpClient.addItem(item);
        }

        indexerManager.fetchExtractedDataFromDiskSearch(UID, Cf.list(
            resourceId1,
            resourceId2,
            resourceId3
        ));

        FileDjfsResource file1 = (FileDjfsResource) filesystem.find(PRINCIPAL, filePath1, Option.of(ReadPreference.primary())).get();
        FileDjfsResource file2 = (FileDjfsResource) filesystem.find(PRINCIPAL, filePath2, Option.of(ReadPreference.primary())).get();
        FileDjfsResource file3 = (FileDjfsResource) filesystem.find(PRINCIPAL, filePath3, Option.of(ReadPreference.primary())).get();

        Assert.some(file1.getAesthetics());
        Assert.isTrue(Math.abs(file1.getAesthetics().get() - 0.5) < 0.0001);
        Assert.some(800, file1.getWidth());
        Assert.some(600, file1.getHeight());
        Assert.some(0, file1.getAngle());

        Assert.some(file2.getAesthetics());
        Assert.isTrue(Math.abs(file2.getAesthetics().get() - 0.3) < 0.0001);
        Assert.some(1024, file2.getWidth());
        Assert.some(768, file2.getHeight());
        Assert.some(90, file2.getAngle());

        Assert.some(file3.getAesthetics());
        Assert.isTrue(Math.abs(file3.getAesthetics().get() - 0.42) < 0.0001);
        Assert.some(100, file3.getWidth());
        Assert.some(200, file3.getHeight());
        Assert.some(0, file3.getAngle());
    }

    @Test
    public void parseExtraDataInDimensionCallbackBody() {
        String postData = "[{\"id\":\"4019602220:15692d49f20961fcc38c9774cdce8079f8a6b35b414368c74b07c34526953fda\",\"width\":800,\"height\":600,\"orientation\":\"portrait\",\"some_new_field\":\"xxx\"},\n"
                + "{\"id\":\"4019602220:15692d49f20961fcc38c9774cdce8079f8a6b35b414368c74b07c34526953fdb\",\"width\":800,\"height\":600,\"orientation\":\"portrait\",\"some_new_field\":\"yyy\"},\n"
                + "{\"id\":\"4019602220:15692d49f20961fcc38c9774cdce8079f8a6b35b414368c74b07c34526953fdc\",\"width\":800,\"height\":600,\"orientation\":\"landscape\",\"some_new_field\":\"zzz\"}]";

        BenderParser<IndexerBinaryData> dimensionsDataParser =
                new BenderMapper().createParser(IndexerBinaryData.class);
        ListF<IndexerBinaryData> indexerBinaryData = dimensionsDataParser.parseListJson(postData);
        Assert.sizeIs(3, indexerBinaryData);
    }
}
