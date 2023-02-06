package ru.yandex.chemodan.app.djfs.core.filesystem.operation;

import java.util.concurrent.atomic.AtomicBoolean;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.copy.CopyOperation;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.copy.CopyOperationHandler;
import ru.yandex.chemodan.app.djfs.core.index.SearchPushGenerator;
import ru.yandex.chemodan.app.djfs.core.operations.MpfsOperationHandler;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.test.RandomFailingInvocationHandler;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.chemodan.queller.celery.job.CeleryJob;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonNumber;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.JsonValue;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class CopyOperationHandlerShareTest extends DjfsDoubleUserTestBase {
    private final ListF<String> RESOURCE_EXCLUDED_FIELDS = Cf.list("id", "parentId", "fileId", "path", "modifyUid",
            "creationTime", "modificationTime", "version", "liveVideoId", "area");

    @Autowired
    private CopyOperationHandler sut;

    @Test
    public void participantCopyFolderWithLivePhotos() {
        DjfsResourcePath owner = DjfsResourcePath.cons(UID_1, "/disk/u1/owner");
        DjfsResourcePath participant = DjfsResourcePath.cons(UID_2, "/disk/shared");
        util.share.create(owner, participant, SharePermissions.READ_WRITE);

        filesystem.initializeArea(UID_1, DjfsResourceArea.ADDITIONAL_DATA);
        DjfsResourcePath livePhoto1 = owner.getChildPath("live_photo_1.heic");
        util.fs.insertLivePhoto(UID_1, livePhoto1.getPath());
        DjfsResourcePath livePhoto2 = owner.getChildPath("live_photo_2.heic");
        util.fs.insertLivePhoto(UID_1, livePhoto2.getPath());

        DjfsResourcePath sourcePath = participant;
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID_2, "/disk/123");
        Operation operation = CopyOperation.create(UID_2, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        DjfsResourcePath participantLivePhotoCopy1 =
                livePhoto1.changeParent(owner, participant).changeParent(participant, destinationPath);
        DjfsResourcePath participantLivePhotoCopy2 =
                livePhoto2.changeParent(owner, participant).changeParent(participant, destinationPath);

        Option<FileDjfsResource> newLivePhoto1 = djfsResourceDao.find2(participantLivePhotoCopy1).cast();
        Assert.some(newLivePhoto1);
        Assert.isTrue(newLivePhoto1.get().isLivePhoto());

        Option<FileDjfsResource> newLivePhoto2 = djfsResourceDao.find2(participantLivePhotoCopy2).cast();
        Assert.some(newLivePhoto2);
        Assert.isTrue(newLivePhoto2.get().isLivePhoto());
    }

    @Test
    public void ownerCopySharedSubfolder() {
        DjfsResourcePath owner = DjfsResourcePath.cons(UID_1, "/disk/u1/owner");
        DjfsResourcePath participant = DjfsResourcePath.cons(UID_2, "/disk/u2/shared");
        util.share.create(owner, participant, SharePermissions.READ_WRITE);
        filesystem.createFile(PRINCIPAL_1, owner.getChildPath("file"));

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID_1, "/disk/u1");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID_1, "/disk/copy");

        Operation operation = CopyOperation.create(UID_1, sourcePath, destinationPath);
        operationDao.insert(operation);

        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath).get(),
                djfsResourceDao.find2(destinationPath).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("owner")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("owner")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("owner").getChildPath("file")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("owner").getChildPath("file")).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void participantCopySharedSubfolder() {
        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath owner1 = DjfsResourcePath.cons(UID_1, "/disk/u1/owner1");
        DjfsResourcePath participant1 = DjfsResourcePath.cons(UID_2, "/disk/u2/shared1");
        util.share.create(owner1, participant1, SharePermissions.READ_WRITE);
        filesystem.createFile(PRINCIPAL_1, owner1.getChildPath("file"));

        DjfsResourcePath owner2 = DjfsResourcePath.cons(UID_1, "/disk/u1/owner2");
        DjfsResourcePath participant2 = DjfsResourcePath.cons(UID_2, "/disk/u2/shared2");
        util.share.create(owner2, participant2, SharePermissions.READ_WRITE);
        filesystem.createFile(PRINCIPAL_1, owner2.getChildPath("file"));

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID_2, "/disk/u2");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID_2, "/disk/copy");

        Operation operation = CopyOperation.create(UID_2, sourcePath, destinationPath);
        operationDao.insert(operation);

        DateTimeUtils.setCurrentMillisSystem();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath).get(),
                djfsResourceDao.find2(destinationPath).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("shared1")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("shared1")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("shared2")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("shared2")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(owner1.getChildPath("file")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("shared1").getChildPath("file")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(owner2.getChildPath("file")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("shared2").getChildPath("file")).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void ownerCopySharedRootFolder() {
        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath owner = DjfsResourcePath.cons(UID_1, "/disk/owner");
        DjfsResourcePath participant = DjfsResourcePath.cons(UID_2, "/disk/shared");
        util.share.create(owner, participant, SharePermissions.READ_WRITE);
        filesystem.createFile(PRINCIPAL_1, owner.getChildPath("file"));

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID_1, "/disk/owner");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID_1, "/disk/copy");

        Operation operation = CopyOperation.create(UID_1, sourcePath, destinationPath);
        operationDao.insert(operation);

        DateTimeUtils.setCurrentMillisSystem();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath).get(),
                djfsResourceDao.find2(destinationPath).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(owner.getChildPath("file")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("file")).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void participantCopySharedRootFolder() {
        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath owner = DjfsResourcePath.cons(UID_1, "/disk/owner");
        DjfsResourcePath participant = DjfsResourcePath.cons(UID_2, "/disk/shared");
        util.share.create(owner, participant, SharePermissions.READ_WRITE);
        filesystem.createFile(PRINCIPAL_1, owner.getChildPath("file"));

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID_2, "/disk/shared");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID_2, "/disk/copy");

        Operation operation = CopyOperation.create(UID_2, sourcePath, destinationPath);
        operationDao.insert(operation);

        DateTimeUtils.setCurrentMillisSystem();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath).get(),
                djfsResourceDao.find2(destinationPath).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(owner.getChildPath("file")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("file")).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void ownerCopyFileFromSharedFolder() {
        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath owner = DjfsResourcePath.cons(UID_1, "/disk/owner");
        DjfsResourcePath participant = DjfsResourcePath.cons(UID_2, "/disk/shared");
        util.share.create(owner, participant, SharePermissions.READ_WRITE);
        filesystem.createFile(PRINCIPAL_1, owner.getChildPath("file"));

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID_1, "/disk/owner/file");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID_1, "/disk/copy");

        Operation operation = CopyOperation.create(UID_1, sourcePath, destinationPath);
        operationDao.insert(operation);

        DateTimeUtils.setCurrentMillisSystem();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.reflectionEquals(djfsResourceDao.find2(owner.getChildPath("file")).get(),
                djfsResourceDao.find2(destinationPath).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void participantCopyFileFromSharedFolder() {
        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath owner = DjfsResourcePath.cons(UID_1, "/disk/owner");
        DjfsResourcePath participant = DjfsResourcePath.cons(UID_2, "/disk/shared");
        util.share.create(owner, participant, SharePermissions.READ_WRITE);
        filesystem.createFile(PRINCIPAL_1, owner.getChildPath("file"));

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID_2, "/disk/shared/file");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID_2, "/disk/copy");

        Operation operation = CopyOperation.create(UID_2, sourcePath, destinationPath);
        operationDao.insert(operation);

        DateTimeUtils.setCurrentMillisSystem();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.reflectionEquals(djfsResourceDao.find2(owner.getChildPath("file")).get(),
                djfsResourceDao.find2(destinationPath).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void participantCopySharedSubfolderWithFailures() {
        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath owner1 = DjfsResourcePath.cons(UID_1, "/disk/u1/owner1");
        DjfsResourcePath participant1 = DjfsResourcePath.cons(UID_2, "/disk/u2/shared1");
        util.share.create(owner1, participant1, SharePermissions.READ_WRITE);
        filesystem.createFile(PRINCIPAL_1, owner1.getChildPath("file1"));

        DjfsResourcePath owner2 = DjfsResourcePath.cons(UID_1, "/disk/u1/owner2");
        DjfsResourcePath participant2 = DjfsResourcePath.cons(UID_2, "/disk/u2/shared2");
        util.share.create(owner2, participant2, SharePermissions.READ_WRITE);
        filesystem.createFile(PRINCIPAL_1, owner2.getChildPath("file2"));

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID_2, "/disk/u2");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID_2, "/disk/copy");

        Operation operation = CopyOperation.create(UID_2, sourcePath, destinationPath);
        operationDao.insert(operation);

        DateTimeUtils.setCurrentMillisSystem();

        randomFailuresProbabilitySource.setFailureProbability(0.4);

        int tryNumber = 0;
        while (tryNumber < 20000) {
            tryNumber += 1;
            try {
                operation = operationDao.find(UID_2, operation.getId()).get();
                MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
                Assert.equals(MpfsOperationHandler.Status.DONE, status);
                break;
            } catch (RandomFailingInvocationHandler.RandomFailureException e) {
                // random failure, retry
            }
        }
        if (tryNumber >= 20000) {
            Assert.fail("did not cope with random failures");
        }

        randomFailuresProbabilitySource.setFailureProbability(0);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath).get(),
                djfsResourceDao.find2(destinationPath).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("shared1")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("shared1")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("shared2")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("shared2")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(owner1.getChildPath("file1")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("shared1").getChildPath("file1")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(owner2.getChildPath("file2")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("shared2").getChildPath("file2")).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void indexerGroupPush() {
        DjfsResourcePath owner = DjfsResourcePath.cons(UID_1, "/disk/owner");
        DjfsResourcePath participant = DjfsResourcePath.cons(UID_2, "/disk/shared");
        util.share.create(owner, participant, SharePermissions.READ_WRITE);

        DjfsResourcePath participantOtherFolder = DjfsResourcePath.cons(UID_2, "/disk/other");
        filesystem.createFolder(PRINCIPAL_2, participantOtherFolder);

        DjfsResourcePath sourcePath = participantOtherFolder;
        DjfsResourcePath destinationPath = participant.getChildPath("copy");

        Operation operation = CopyOperation.create(UID_2, sourcePath, destinationPath);
        operationDao.insert(operation);

        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.SHARED_TASK_ID));
        CeleryJob celeryJob = mockCeleryTaskManager.submitted
                .filter(x -> x.task == SearchPushGenerator.SHARED_TASK_ID).first();

        FolderDjfsResource destination = (FolderDjfsResource) djfsResourceDao.find2(owner.getChildPath("copy")).get();
        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();

        JsonObject data = (JsonObject) ((JsonArray) kwargs.getTs("data")).getArray().get(0);

        Assert.equals(UID_2.asLong(), ((JsonNumber) data.get("uid")).longValue());
        Assert.equals(destination.getPath().getPath(), ((JsonString) data.get("id")).getValue());
        Assert.equals(destination.getFileId().get().getValue(), ((JsonString) data.get("file_id")).getValue());
        Assert.equals(destination.getResourceId().get().toString(), ((JsonString) data.get("resource_id")).getValue());
        Assert.equals(destination.getPath().getName(), ((JsonString) data.get("name")).getValue());
        Assert.equals(now.getMillis() * 1000, ((JsonNumber) data.get("version")).longValue());
        Assert.equals(now.getMillis() * 1000, ((JsonNumber) data.get("real_resource_version")).longValue());
    }
}
