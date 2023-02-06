package ru.yandex.chemodan.app.djfs.core.legacy;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.legacy.exception.LegacyServiceNotFoundException;
import ru.yandex.chemodan.app.djfs.core.legacy.formatting.FilePojo;
import ru.yandex.chemodan.app.djfs.core.legacy.formatting.FolderPojo;
import ru.yandex.chemodan.app.djfs.core.legacy.formatting.ResourcePojo;
import ru.yandex.chemodan.app.djfs.core.user.UserNotInitializedException;
import ru.yandex.chemodan.app.djfs.core.web.JsonStringResult;
import ru.yandex.misc.test.Assert;


public class BulkInfoByResourceIdsTest extends LegacyActionsTestBase {

    @Test
    public void successfullFolderFetchingNotShared() {
        String folderPathString = "/disk/not_shared_folder";
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, folderPathString));
        DjfsResourceId folderResourceId = folder.getResourceId().get();
        JsonStringResult response = legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(folderResourceId.toString()),
                UID_1.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        );
        ListF<ResourcePojo> resources = parseJsonListStringResult(response);
        Assert.equals(resources.size(), 1);
        ResourcePojo resource = resources.first();
        Assert.isTrue(resource instanceof FolderPojo);
        Assert.equals("/disk/not_shared_folder", resource.path.get());
        Assert.equals("/disk/not_shared_folder/", resource.id.get());
        Assert.equals("dir", resource.type);
        Assert.equals("not_shared_folder", resource.name);
        Assert.isTrue(resource.ctime.isPresent());
        Assert.isTrue(resource.mtime.isPresent());
        Assert.isTrue(resource.utime.isPresent());
    }

    @Test
    public void successfullFileFetchingNotShared() {
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID_1, "/disk/not_shared_file.txt");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, filePath);
        DjfsResourceId fileResourceId = file.getResourceId().get();
        JsonStringResult response = legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(fileResourceId.toString()),
                UID_1.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        );
        ListF<ResourcePojo> resources = parseJsonListStringResult(response);
        Assert.equals(resources.size(), 1);
        ResourcePojo resource = resources.first();
        Assert.isTrue(resource instanceof FilePojo);
        Assert.equals("/disk/not_shared_file.txt", resource.path.get());
        Assert.equals("/disk/not_shared_file.txt", resource.id.get());
        Assert.equals("file", resource.type);
        Assert.equals("not_shared_file.txt", resource.name);
        Assert.isTrue(resource.ctime.isPresent());
        Assert.isTrue(resource.mtime.isPresent());
        Assert.isTrue(resource.utime.isPresent());
    }

    @Test
    public void successfullFolderFetchingShared() {
        DjfsResourcePath ownerPath = OWNER_PATH.getChildPath("subfolder");
        DjfsResourcePath participantPath = PARTICIPANT_PATH.getChildPath("subfolder");
        FolderDjfsResource folder = filesystem.createFolder(PRINCIPAL_1, ownerPath);
        DjfsResourceId folderResourceId = folder.getResourceId().get();
        JsonStringResult responseOwner = legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(folderResourceId.toString()),
                UID_1.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        );
        ListF<ResourcePojo> ownerResources = parseJsonListStringResult(responseOwner);
        Assert.equals(ownerResources.size(), 1);
        ResourcePojo resource = ownerResources.first();
        Assert.isTrue(resource instanceof FolderPojo);
        Assert.equals(ownerPath.getPath(), resource.path.get());
        Assert.equals(ownerPath.getPath() + "/", resource.id.get());
        Assert.equals("dir", resource.type);
        Assert.equals("subfolder", resource.name);
        Assert.isTrue(resource.ctime.isPresent());
        Assert.isTrue(resource.mtime.isPresent());
        Assert.isTrue(resource.utime.isPresent());

        JsonStringResult responseParticipant = legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(folderResourceId.toString()),
                UID_2.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        );
        ListF<ResourcePojo> resources = parseJsonListStringResult(responseParticipant);
        Assert.equals(resources.size(), 1);
        ResourcePojo participantResource = resources.first();
        Assert.isTrue(participantResource instanceof FolderPojo);
        Assert.equals(participantPath.getPath(), participantResource.path.get());
        Assert.equals(participantPath.getPath() + "/", participantResource.id.get());
        Assert.equals("dir", participantResource.type);
        Assert.equals("subfolder", participantResource.name);
        Assert.isTrue(participantResource.ctime.isPresent());
        Assert.isTrue(participantResource.mtime.isPresent());
        Assert.isTrue(participantResource.utime.isPresent());
    }

    @Test
    public void successfullFileFetchingShared() {
        DjfsResourcePath ownerPath = OWNER_PATH.getChildPath("file.txt");
        DjfsResourcePath participantPath = PARTICIPANT_PATH.getChildPath("file.txt");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, ownerPath);
        DjfsResourceId fileResourceId = file.getResourceId().get();
        JsonStringResult responseOwner = legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(fileResourceId.toString()),
                UID_1.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        );
        ListF<ResourcePojo> ownerResources = parseJsonListStringResult(responseOwner);
        Assert.equals(ownerResources.size(), 1);
        ResourcePojo resource = ownerResources.first();
        Assert.isTrue(resource instanceof FilePojo);
        Assert.equals(ownerPath.getPath(), resource.path.get());
        Assert.equals(ownerPath.getPath(), resource.id.get());
        Assert.equals("file", resource.type);
        Assert.equals("file.txt", resource.name);
        Assert.isTrue(resource.ctime.isPresent());
        Assert.isTrue(resource.mtime.isPresent());
        Assert.isTrue(resource.utime.isPresent());

        JsonStringResult responseParticipant = legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(fileResourceId.toString()),
                UID_2.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        );
        ListF<ResourcePojo> resources = parseJsonListStringResult(responseParticipant);
        Assert.equals(resources.size(), 1);
        ResourcePojo participantResource = resources.first();
        Assert.isTrue(participantResource instanceof FilePojo);
        Assert.equals(participantPath.getPath(), participantResource.path.get());
        Assert.equals(participantPath.getPath(), participantResource.id.get());
        Assert.equals("file", participantResource.type);
        Assert.equals("file.txt", participantResource.name);
        Assert.isTrue(participantResource.ctime.isPresent());
        Assert.isTrue(participantResource.mtime.isPresent());
        Assert.isTrue(participantResource.utime.isPresent());
    }

    @Test
    public void failedResourceFetchingByNotParticipant() {
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, "/disk/not_shared_folder"));
        DjfsResourceId folderResourceId = folder.getResourceId().get();
        JsonStringResult response = legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(folderResourceId.toString()),
                UID_2.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        );
        ListF<ResourcePojo> resources = parseJsonListStringResult(response);
        Assert.isEmpty(resources);
    }

    @Test
    public void fetchFileOnlyFromAllowedAreas() {
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID_1, "/disk/not_shared_file.txt");
        FileDjfsResource file = filesystem.createFile(PRINCIPAL_1, filePath);
        DjfsResourceId fileResourceId = file.getResourceId().get();
        JsonStringResult response = legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(fileResourceId.toString()),
                UID_1.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.of(Cf.hashSet("/trash"))
        );
        ListF<ResourcePojo> resources = parseJsonListStringResult(response);
        Assert.isEmpty(resources);
    }

    @Test
    public void fetchFolderOnlyFromAllowedAreas() {
        String folderPathString = "/disk/not_shared_folder";
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, folderPathString));
        DjfsResourceId folderResourceId = folder.getResourceId().get();
        JsonStringResult response = legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(folderResourceId.toString()),
                UID_1.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.of(Cf.hashSet("/trash"))
        );
        ListF<ResourcePojo> resources = parseJsonListStringResult(response);
        Assert.isEmpty(resources);
    }

    @Test
    public void errorOnUnknownService() {
        String folderPathString = "/disk/not_shared_folder";
        FolderDjfsResource folder =
                filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, folderPathString));
        DjfsResourceId folderResourceId = folder.getResourceId().get();
        Assert.assertThrows(() -> legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list(folderResourceId.toString()),
                UID_1.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.of(Cf.hashSet("/test"))
        ), LegacyServiceNotFoundException.class);
    }

    @Test
    public void errorOnUninitializedUser() {
        Assert.assertThrows(() -> legacyFilesystemActions.bulk_info_by_resource_ids(
                Cf.list("123:3110a374221ad68d686f4d8a47c8c1a409292345dca8f0350ae6e6d300e059e1"),
                "123",
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        ), UserNotInitializedException.class);
    }
}
