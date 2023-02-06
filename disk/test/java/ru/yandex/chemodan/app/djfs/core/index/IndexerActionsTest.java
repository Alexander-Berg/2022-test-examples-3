package ru.yandex.chemodan.app.djfs.core.index;

import com.mongodb.ReadPreference;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.MediaType;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.PhotosliceAlbumType;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserWithSharedResourcesTestBase;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

public class IndexerActionsTest extends DjfsDoubleUserWithSharedResourcesTestBase {
    @Autowired
    private IndexerActions indexerActions;

    @Test
    public void successfullFolderFetchingNotShared() {
        String folderPathString = "/disk/not_shared_folder";
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, folderPathString));
        DjfsResourceId resourceId = folder.getResourceId().get();
        IndexerResourcesListPojo response =
                indexerActions.getResourcesByResourceIds(Cf.list(resourceId.toString()),
                        UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.equals(folderPathString, item.key);
        Assert.equals("dir", item.type);
        Assert.isEmpty(item.sharedFolderOwner);
    }

    @Test
    public void successfullFileFetchingNotShared() {
        String filePathString = "/disk/not_shared_file.txt";
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, filePathString));
        DjfsResourceId resourceId = file.getResourceId().get();
        IndexerResourcesListPojo response =
                indexerActions.getResourcesByResourceIds(Cf.list(resourceId.toString()),
                        UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.equals(filePathString, item.key);
        Assert.equals("file", item.type);
    }

    @Test
    public void getSharedResourceByParticipantChangesReturnedPath() {
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(folder.getResourceId().get().toString()), UID_2.toString(), createRequestWithUserObj(USER_2));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.equals(PARTICIPANT_PATH.getChildPath("subfolder").getPath(), item.key);
        Assert.some(UID_1.toString(), item.sharedFolderOwner);
    }

    @Test
    public void getSharedResourceByOwnerChangesReturnedPath() {
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(folder.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.equals(OWNER_PATH.getChildPath("subfolder").getPath(), item.key);
        Assert.some(UID_1.toString(), item.sharedFolderOwner);
    }

    @Test
    public void failedResourceFetchingMissingResource() {
        DjfsResourceId resourceId = DjfsResourceId.cons(UID_1, StringUtils.repeat("2", 64));
        IndexerResourcesListPojo result = indexerActions.getResourcesByResourceIds(
                Cf.list(resourceId.toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.isEmpty(result.items);
    }

    @Test
    public void failedResourceFetchingByNotParticipant() {
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/not_shared_folder"));
        IndexerResourcesListPojo result = indexerActions.getResourcesByResourceIds(
                Cf.list(folder.getResourceId().get().toString()), UID_2.toString(), createRequestWithUserObj(USER_2));

        Assert.isEmpty(result.items);
    }

    @Test
    public void missingMediatypeGetItFromMimetypeOrFilename() {
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1.txt"),
                x -> x.mediaType(Option.empty()));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.equals(MediaType.DOCUMENT.getStringRepresentation(), item.mediaType);
    }

    @Test
    public void missingMediatypeCannotGetItFromFilenameOrMimetypeReturnUnknown() {
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1"),
                x -> x.mediaType(Option.empty()).mimetype(Option.of("any")));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.equals(MediaType.UNKNOWN.getStringRepresentation(), item.mediaType);
    }

    @Test
    public void photosliceTimeIsTooLarge() {
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1.txt"),
                x -> x.exifTime(Option.of(new Instant("2079-09-01T17:06:43+03"))));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.equals(3460802803L, item.photosliceTime.get());
        Assert.equals(3460802803L, item.exifTime.get());
    }

    @Test
    public void failToReturnResourceOfNotExistingUser() {
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1.txt"));
        DjfsResourceId resourceId = DjfsResourceId.cons("1234", file.getResourceId().get().getFileId().getValue());
        IndexerResourcesListPojo result = indexerActions.getResourcesByResourceIds(Cf.list(resourceId.toString()),
                UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.isEmpty(result.items);
    }

    @Test
    public void returnEmptyForMimetypeIfItIsMissing() {
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1"),
                x -> x.mediaType(Option.empty()).mimetype(Option.empty()));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.isEmpty(item.mimetype);
        Assert.equals(MediaType.UNKNOWN.getStringRepresentation(), item.mediaType);
    }

    @Test
    public void doNotReturnEtimeIfFotkiTagsPresented() {
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1.jpg"),
                x -> x.exifTime(Option.of(Instant.now())).fotkiTags(Option.of("some,tags")));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.equals("some\ntags", item.fotkiTags.get());
        Assert.isEmpty(item.exifTime);
    }

    @Test
    public void doNotReturnEtimeAndFotkiTagsIfIfFotkiTagsIsEmptyString() {
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1.jpg"),
                x -> x.exifTime(Option.of(Instant.now())).fotkiTags(Option.of("")));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.isEmpty(item.exifTime);
        Assert.isEmpty(item.fotkiTags);
    }

    @Test
    public void successfullFilesFetchingNotShared() {
        String filePathString = "/disk/not_shared_file_1.txt";
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, filePathString));
        DjfsResourceId resourceId1 = file.getResourceId().get();
        filePathString = "/disk/not_shared_file_2.txt";
        file = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, filePathString));
        DjfsResourceId resourceId2 = file.getResourceId().get();

        IndexerResourcesListPojo response = indexerActions
                .getResourcesByResourceIds(Cf.list(resourceId1, resourceId2).map(DjfsResourceId::toString),
                        UID_1.toString(), createRequestWithUserObj(USER_1));
        Assert.sizeIs(2, response.items);
        Assert.assertContainsAll(Cf.list(resourceId1, resourceId2).map(DjfsResourceId::toString),
                response.items.map(x -> x.resourceId));
    }

    @Test
    public void fetchSharedResourcesAmongWithNotSharedByOwner() {
        String filePathString = "/disk/not_shared_file_1.txt";
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, filePathString));
        DjfsResourceId resourceId1 = file.getResourceId().get();
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));
        DjfsResourceId resourceId2 = folder.getResourceId().get();
        IndexerResourcesListPojo response = indexerActions
                .getResourcesByResourceIds(Cf.list(resourceId1, resourceId2).map(DjfsResourceId::toString),
                        UID_1.toString(), createRequestWithUserObj(USER_1));
        Assert.sizeIs(2, response.items);
        Assert.assertContainsAll(Cf.list(resourceId1, resourceId2).map(DjfsResourceId::toString),
                response.items.map(x -> x.resourceId));
    }

    @Test
    public void fetchSharedResourcesAmongWithNotSharedByParticipant() {
        String filePathString = "/disk/not_shared_file_1.txt";
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, filePathString));
        DjfsResourceId resourceId1 = file.getResourceId().get();
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getChildPath("subfolder"));
        DjfsResourceId resourceId2 = folder.getResourceId().get();
        IndexerResourcesListPojo response = indexerActions
                .getResourcesByResourceIds(Cf.list(resourceId1, resourceId2).map(DjfsResourceId::toString),
                        UID_2.toString(), createRequestWithUserObj(USER_2));
        Assert.sizeIs(1, response.items);
        Assert.equals(folder.getResourceId().get().toString(), response.items.first().resourceId);
        Assert.isTrue(response.items.first().key.startsWith("/disk/participant"));
    }

    @Test
    public void fetchResourcesMissingResourcesNotIncluded() {
        String filePathString = "/disk/not_shared_file_1.txt";
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, filePathString));
        DjfsResourceId resourceId1 = file.getResourceId().get();
        DjfsResourceId resourceId2 =
                DjfsResourceId.cons(UID_1, "b17c59b70739710335122151e4fadbb58fc2f42c8a164e8336d9987a23c3860d");

        IndexerResourcesListPojo response = indexerActions
                .getResourcesByResourceIds(Cf.list(resourceId1, resourceId2).map(DjfsResourceId::toString),
                        UID_1.toString(), createRequestWithUserObj(USER_1));
        Assert.sizeIs(1, response.items);
        Assert.assertContainsAll(Cf.list(resourceId1).map(DjfsResourceId::toString),
                response.items.map(x -> x.resourceId));
    }

    @Test(expected = IndexerGetResourcesTooManyResourcesException.class)
    public void tooManyResourceInSingleRequest() {
        indexerActions.getResourcesByResourceIds(Cf.repeat("test", 101),
                UID_1.toString(), createRequestWithUserObj(USER_1));
    }

    @Test
    public void failToReturnResourcesOfNotExistingUser() {
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1.txt"));
        DjfsResourceId resourceId = DjfsResourceId.cons("1234", file.getResourceId().get().getFileId().getValue());
        IndexerResourcesListPojo response =
                indexerActions.getResourcesByResourceIds(Cf.list(resourceId.toString()), UID_1.toString(), createRequestWithUserObj(USER_1));
        Assert.isEmpty(response.items);
    }

    @Test
    public void dontReturnMtimeAndCtimeForRootFolders() {
        DjfsResource photounlimRootFolder =
                djfsResourceDao.find(DjfsResourcePath.cons(UID_1, "/photounlim")).get();
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(photounlimRootFolder.getResourceId().get().toString()),
                UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.isEmpty(item.modificationTime);
        Assert.isEmpty(item.creationTime);
    }

    @Test
    public void returnPreviewStidIfExists() {
        String previewStid = UuidUtils.randomToHexString();
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1.jpg"),
                x -> x.previewStid(Option.of(previewStid)));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.notEmpty(item.previewStid);
        Assert.equals(previewStid, item.previewStid.get());
    }

    @Test
    public void doNotReturnPreviewStidIfDoesntExists() {
        FileDjfsResource fileWithoutPreviewStid =
                filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/f1.jpg"));
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(fileWithoutPreviewStid.getResourceId().get().toString()),
                UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.isEmpty(item.previewStid);
    }

    @Test
    public void returnsRelevantResourceInSameCollection() {
        DjfsFileId fileId = DjfsFileId.random();
        String path1String = "/disk/f1";
        FileDjfsResource file1 = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, path1String),
                x -> x.fileId(fileId).version(Option.of(2000L)));
        String path2String = "/disk/f2";
        FileDjfsResource file2 = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, path2String),
                x -> x.fileId(fileId).version(Option.of(1000L)));

        // it is important that file2.id > file1.id
        // otherwise file1 will always be returned
        Assert.gt(file2.getId(), file1.getId());

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(DjfsResourceId.cons(UID_1, fileId).toString()),
                UID_1.asString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.equals(path1String, item.key);
        Assert.equals("file", item.type);
    }

    @Test
    public void returnsRelevantResourceInDifferentCollections() {
        DjfsFileId fileId = DjfsFileId.random();
        String path1String = "/disk/f1";
        FileDjfsResource file1 = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, path1String),
                x -> x.fileId(fileId).version(Option.of(1000L)));
        String path2String = "/trash/f2";
        FileDjfsResource file2 = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, path2String),
                x -> x.fileId(fileId).version(Option.of(2000L)));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(DjfsResourceId.cons(UID_1, fileId).toString()),
                UID_1.asString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.equals(path2String, item.key);
        Assert.equals("file", item.type);
    }

    @Test
    public void returnsNotFoundWhenVersionIsInHidden() {
        DjfsFileId fileId = DjfsFileId.random();
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/hidden/f1"),
                x -> x.fileId(fileId).version(Option.of(2000L)));

        IndexerResourcesListPojo result = indexerActions.getResourcesByResourceIds(
                Cf.list(DjfsResourceId.cons(UID_1, fileId).toString()),
                UID_1.asString(), createRequestWithUserObj(USER_1));

        Assert.isEmpty(result.items);
    }

    @Test
    public void returnRelevantResourcesByBatchFromDifferentCollections() {
        filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/extra_folder"));

        DjfsFileId requestedFileId1 = DjfsFileId.random();
        String correctFilePath1 = "/disk/extra_folder/result_file_1";
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/stale_file_1a"),
                x -> x.fileId(requestedFileId1).version(Option.of(1000L)));
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/stale_file_1b"),
                x -> x.fileId(requestedFileId1).version(Option.of(1001L)));
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/trash/stale_file_1c"),
                x -> x.fileId(requestedFileId1).version(Option.of(1002L)));
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, correctFilePath1),
                x -> x.fileId(requestedFileId1).version(Option.of(2000L)));

        DjfsFileId needlessFileId1 = DjfsFileId.random();
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/unmatched_file_2a"),
                x -> x.fileId(needlessFileId1).version(Option.of(3000L)));
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/unmatched_file_2b"),
                x -> x.fileId(needlessFileId1).version(Option.of(2000L)));

        DjfsFileId requestedFileId2 = DjfsFileId.random();
        String correctFilePath3 = "/trash/result_file_3";
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, correctFilePath3),
                x -> x.fileId(requestedFileId2).version(Option.of(2000L)));
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/stale_file_3"),
                x -> x.fileId(requestedFileId2).version(Option.of(1000L)));

        DjfsFileId needlessFileId2 = DjfsFileId.random();
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/trash/unmatched_file_4a"),
                x -> x.fileId(needlessFileId2).version(Option.of(6000L)));
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/trash/unmatched_file_4b"),
                x -> x.fileId(needlessFileId2).version(Option.of(2000L)));

        DjfsFileId requestedFileId3 = DjfsFileId.random();
        String correctFolderPath1 = "/disk/result_folder_1";
        filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, correctFolderPath1),
                x -> x.fileId(requestedFileId3).version(Option.of(2000L)));
        filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/stale_folder_1"),
                x -> x.fileId(requestedFileId3).version(Option.of(1000L)));

        DjfsFileId needlessFileId3 = DjfsFileId.random();
        filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/unmatched_folder_2a"),
                x -> x.fileId(needlessFileId3).version(Option.of(3000L)));
        filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/unmatched_folder_2b"),
                x -> x.fileId(needlessFileId3).version(Option.of(1000L)));

        DjfsFileId requestedFileId4 = DjfsFileId.random();
        String correctFolderPath3 = "/trash/result_folder_3";
        filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, correctFolderPath3),
                x -> x.fileId(requestedFileId4).version(Option.of(3000L)));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(requestedFileId1, requestedFileId2, requestedFileId3, requestedFileId4).map(x -> DjfsResourceId.cons(UID_1, x).toString()),
                UID_1.asString(), createRequestWithUserObj(USER_1));

        Assert.equals(Cf.set(correctFilePath1, correctFilePath3, correctFolderPath1, correctFolderPath3), Cf.toSet(response.items.map(x -> x.key)));
    }

    @Test
    public void bulkReturnsEmptyListWhenVersionIsInHiddenOrResourceNotFound() {
        DjfsFileId fileId1 = DjfsFileId.random();
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/hidden/f1"),
                x -> x.fileId(fileId1).version(Option.of(2000L)));

        DjfsFileId fileId2 = DjfsFileId.random();

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(fileId1, fileId2).map(x -> DjfsResourceId.cons(UID_1, x).toString()),
                UID_1.asString(), createRequestWithUserObj(USER_1));

        Assert.isEmpty(response.items);
    }

    @Test
    public void bulkReturnsResourceFromDiskEvenIfResourceInHiddenWithHigherVersionExists() {
        DjfsFileId fileId1 = DjfsFileId.random();
        String correctFilePath = "/disk/file_1";
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/file_1"),
                x -> x.fileId(fileId1).version(Option.of(1000L)));
        filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/hidden/file_1"),
                x -> x.fileId(fileId1).version(Option.of(2000L)));

        DjfsFileId fileId2 = DjfsFileId.random();
        String correctFolderPath = "/disk/folder_1";
        filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/folder_1"),
                x -> x.fileId(fileId2).version(Option.of(1000L)));
        filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/hidden/folder_1"),
                x -> x.fileId(fileId2).version(Option.of(2000L)));

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(fileId1, fileId2).map(x -> DjfsResourceId.cons(UID_1, x).toString()),
                UID_1.asString(), createRequestWithUserObj(USER_1));

        Assert.equals(Cf.set(correctFilePath, correctFolderPath), Cf.toSet(response.items.map(x -> x.key)));
    }

    @Test
    public void checkFidAndParentFidsForFolderInSharedFolder() {
        DjfsResourcePath subfolderPath = PARTICIPANT_PATH.getChildPath("subfolder");

        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_2, subfolderPath);
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(folder.getResourceId().get().toString()), UID_2.toString(), createRequestWithUserObj(USER_2));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.equals(PARTICIPANT_PATH.getChildPath("subfolder").getPath(), item.key);
        Assert.some(UID_1.toString(), item.sharedFolderOwner);

        Assert.equals(subfolderPath.getPath(), item.key);
        Assert.equals("dir", item.type);

        Assert.equals(item.fid, UuidUtils.toHexString(folder.getId()));

        util.fs.assertParentIds(item.parentFids, subfolderPath, true);
    }

    @Test
    public void checkFidAndParentFidsForFolderInSharedFolderWithDifferentNesting() {
        DjfsResourcePath ownerPath = DjfsResourcePath.cons(UID_1, "/disk/owner-2/share-owner");
        filesystem.createFolder(PRINCIPAL_1, ownerPath.getParent());
        filesystem.createFolder(PRINCIPAL_1, ownerPath);

        DjfsResourcePath participantPath = DjfsResourcePath.cons(UID_2, "/disk/participant-2/1/2/3/4/5");
        ListF<DjfsResourcePath> allParents = participantPath.getAllParents().filter(x -> !x.isRoot() && !x.isAreaRoot());
        for (DjfsResourcePath parent : allParents) {
            filesystem.createFolder(PRINCIPAL_2, parent);
        }
        filesystem.createFolder(PRINCIPAL_2, participantPath);

        groupId = shareManager.createGroup(ownerPath).getId();
        groupLinkId = shareManager.createLink(groupId, participantPath, SharePermissions.READ_WRITE).getId();

        DjfsResourcePath subfolderPath = participantPath.getChildPath("subfolder");
        filesystem.createFolder(PRINCIPAL_2, subfolderPath);

        DjfsResourcePath subSubfolderPath = subfolderPath.getChildPath("subsubfolder");
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL_2, subSubfolderPath);

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(folder.getResourceId().get().toString()), UID_2.toString(), createRequestWithUserObj(USER_2));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.equals(subSubfolderPath.getPath(), item.key);
        Assert.some(UID_1.toString(), item.sharedFolderOwner);

        Assert.equals(subSubfolderPath.getPath(), item.key);
        Assert.equals("dir", item.type);

        Assert.equals(item.fid, UuidUtils.toHexString(folder.getId()));

        util.fs.assertParentIds(item.parentFids, subSubfolderPath, true);
    }

    @Test
    public void checkFidAndParentFidsForSharedFolder() {
        Option<DjfsResource> folder =
                filesystem.find(PRINCIPAL_2, PARTICIPANT_PATH, Option.of(ReadPreference.primary()));
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(folder.get().getResourceId().get().toString()),
                UID_2.toString(), createRequestWithUserObj(USER_2));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.equals(PARTICIPANT_PATH.getPath(), item.key);
        Assert.equals("dir", item.type);

        Assert.equals(item.fid, UuidUtils.toHexString(folder.get().getId()));

        util.fs.assertParentIds(item.parentFids, PARTICIPANT_PATH, true);
    }

    @Test
    public void checkFidAndParentFidsForSharedFolderByOwner() {
        DjfsResourcePath ownerSubfolder = OWNER_PATH.getChildPath("subfolder");
        FolderDjfsResource subfolder = filesystem.createFolder(PRINCIPAL_1, ownerSubfolder);

        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(subfolder.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerFolderPojo item = (IndexerFolderPojo)response.items.get(0);

        Assert.equals(ownerSubfolder.getPath(), item.key);
        Assert.equals("dir", item.type);

        Assert.equals(item.fid, UuidUtils.toHexString(subfolder.getId()));

        util.fs.assertParentIds(item.parentFids, ownerSubfolder, true);
    }

    @Test
    public void checkFidAndParentFidsForFileInSharedFolder() {
        DjfsResourcePath filePath = PARTICIPANT_PATH.getChildPath("file.txt");

        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_2, filePath);
        IndexerResourcesListPojo response = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_2.toString(), createRequestWithUserObj(USER_2));

        Assert.sizeIs(1, response.items);
        IndexerFilePojo item = (IndexerFilePojo)response.items.get(0);

        Assert.equals(filePath.getPath(), item.key);
        Assert.equals("file", item.type);

        Assert.some(file.getParentId(), item.parentFid);
    }

    @Test
    public void checkPhotosliceAlbumTypeForFile() {
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID_1, "/disk/test.jpg");
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_1, filePath, x -> x.photosliceAlbumType(PhotosliceAlbumType.CAMERA));

        IndexerResourcesListPojo indexerResources = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, indexerResources.items);
        IndexerFilePojo indexerResource = (IndexerFilePojo)indexerResources.items.get(0);

        Assert.some(PhotosliceAlbumType.CAMERA.value(), indexerResource.photosliceAlbumType);
    }

    @Test
    public void checkPhotosliceAlbumExclusionsForFile() {
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID_1, "/disk/test.jpg");
        FileDjfsResource file =
                filesystem.createFile(PRINCIPAL_1, filePath, x -> x.albumsExclusion("camera"));

        IndexerResourcesListPojo indexerResources = indexerActions.getResourcesByResourceIds(
                Cf.list(file.getResourceId().get().toString()), UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, indexerResources.items);
        IndexerFilePojo indexerResource = (IndexerFilePojo)indexerResources.items.get(0);

        Assert.hasSize(1, indexerResource.albumsExclusions);
        Assert.in("camera", indexerResource.albumsExclusions.get());
    }

    private void checkResourceArea(String resourcePathString, boolean isFolder, String expectedArea) {
        DjfsResource resource;
        if (isFolder) {
            resource = filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, resourcePathString));
        } else {
            resource = filesystem.createFile(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, resourcePathString));
        }

        DjfsResourceId resourceId = resource.getResourceId().get();
        IndexerResourcesListPojo response =
                indexerActions.getResourcesByResourceIds(Cf.list(resourceId.toString()),
                        UID_1.toString(), createRequestWithUserObj(USER_1));

        Assert.sizeIs(1, response.items);
        IndexerResourcePojo item = response.items.get(0);

        Assert.equals(expectedArea, item.area);
    }

    @Test
    public void areaForFolderInDisk() {
        checkResourceArea("/disk/folder", true, "disk");
    }

    @Test
    public void areaForFolderInTrash() {
        checkResourceArea("/trash/folder", true, "trash");
    }

    @Test
    public void areaForFileInDisk() {
        checkResourceArea("/disk/file.txt", false, "disk");
    }

    @Test
    public void areaForFileInTrash() {
        checkResourceArea("/trash/file.txt", false, "trash");
    }
}
