package ru.yandex.chemodan.app.djfs.core.legacy;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.legacy.formatting.FilePojo;
import ru.yandex.chemodan.app.djfs.core.legacy.formatting.FolderPojo;
import ru.yandex.chemodan.app.djfs.core.legacy.formatting.ResourcePojo;
import ru.yandex.chemodan.app.djfs.core.user.UserNotInitializedException;
import ru.yandex.chemodan.app.djfs.core.web.JsonStringResult;
import ru.yandex.misc.test.Assert;


public class BulkInfoTest extends LegacyActionsTestBase {

    @Test
    public void successfullFolderFetchingNotShared() {
        String folderPathString = "/disk/not_shared_folder";
        filesystem.createFolder(PRINCIPAL_1, DjfsResourcePath.cons(UID_1, folderPathString));
        JsonStringResult response = legacyFilesystemActions.bulk_info(
                Cf.list(folderPathString),
                UID_1.toString(),
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
        String rawPathString = "/disk/not_shared_file.txt";
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID_1, rawPathString);
        filesystem.createFile(PRINCIPAL_1, filePath);
        JsonStringResult response = legacyFilesystemActions.bulk_info(
                Cf.list(rawPathString),
                UID_1.toString(),
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
    public void successfulMultipleFileFetchingFromOneNotSharedFolder() {
        successfulMultipleFileFetchingNotShared(false);
    }

    @Test
    public void successfulMultipleFileFetchingFromDifferentNotSharedFolders() {
        successfulMultipleFileFetchingNotShared(true);
    }

    private void successfulMultipleFileFetchingNotShared(boolean differentFolders) {
        if (differentFolders) {
            String rawFolderPath = "/disk/folder";
            filesystem.createFolder(PRINCIPAL_1,  DjfsResourcePath.cons(UID_1, rawFolderPath));
        }

        String rawPathString = "/disk/not_shared_file.txt";
        DjfsResourcePath filePath = DjfsResourcePath.cons(UID_1, rawPathString);
        String rawPathString2 = "/disk/not_shared_file2.txt";
        DjfsResourcePath filePath2 = DjfsResourcePath.cons(UID_1, rawPathString2);
        String rawPathString3 = "/disk/" + (differentFolders ? "folder/" : "") + "not_shared_file3.txt";
        DjfsResourcePath filePath3 = DjfsResourcePath.cons(UID_1, rawPathString3);
        filesystem.createFile(PRINCIPAL_1, filePath);
        filesystem.createFile(PRINCIPAL_1, filePath2);
        filesystem.createFile(PRINCIPAL_1, filePath3);
        JsonStringResult response = legacyFilesystemActions.bulk_info(
                Cf.list(rawPathString, rawPathString3, rawPathString2),
                UID_1.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        );
        ListF<ResourcePojo> resources = parseJsonListStringResult(response);
        Assert.equals(resources.size(), 3);
        // check resources order
        Assert.equals(rawPathString, resources.get(0).path.get());
        Assert.equals(rawPathString3, resources.get(1).path.get());
        Assert.equals(rawPathString2, resources.get(2).path.get());
    }

    @Test
    public void successfullFolderFetchingShared() {
        DjfsResourcePath ownerPath = OWNER_PATH.getChildPath("subfolder");
        DjfsResourcePath participantPath = PARTICIPANT_PATH.getChildPath("subfolder");
        filesystem.createFolder(PRINCIPAL_1, ownerPath);
        JsonStringResult responseOwner = legacyFilesystemActions.bulk_info(
                Cf.list(ownerPath.toString()),
                UID_1.toString(),
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

        JsonStringResult responseParticipant = legacyFilesystemActions.bulk_info(
                Cf.list(participantPath.toString()),
                UID_2.toString(),
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
        filesystem.createFile(PRINCIPAL_1, ownerPath);
        JsonStringResult responseOwner = legacyFilesystemActions.bulk_info(
                Cf.list(ownerPath.toString()),
                UID_1.toString(),
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

        JsonStringResult responseParticipant = legacyFilesystemActions.bulk_info(
                Cf.list(participantPath.toString()),
                UID_2.toString(),
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
        JsonStringResult response = legacyFilesystemActions.bulk_info(
                Cf.list(folder.getPath().toString()),
                UID_2.toString(),
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
    public void errorOnUninitializedUser() {
        Assert.assertThrows(() -> legacyFilesystemActions.bulk_info(
                Cf.list("/disk/123.txt"),
                "123",
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        ), UserNotInitializedException.class);
    }

    @Test
    public void rootTest() {
        JsonStringResult response = legacyFilesystemActions.bulk_info(
                Cf.list("/disk"),
                UID_1.toString(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
        );
        ListF<ResourcePojo> resources = parseJsonListStringResult(response);
        Assert.hasSize(1, resources);
    }
}
