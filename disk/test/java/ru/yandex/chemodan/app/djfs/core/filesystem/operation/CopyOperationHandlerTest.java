package ru.yandex.chemodan.app.djfs.core.filesystem.operation;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mongodb.ReadPreference;
import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.SafeCloseable;
import ru.yandex.chemodan.app.djfs.core.changelog.Changelog;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceType;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.MediaType;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.copy.CopyOperation;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.copy.CopyOperationData;
import ru.yandex.chemodan.app.djfs.core.filesystem.operation.copy.CopyOperationHandler;
import ru.yandex.chemodan.app.djfs.core.index.SearchPushGenerator;
import ru.yandex.chemodan.app.djfs.core.notification.XivaPushGenerator;
import ru.yandex.chemodan.app.djfs.core.operations.MpfsOperationHandler;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.test.RandomFailingInvocationHandler;
import ru.yandex.chemodan.app.djfs.core.util.ByteConstants;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.chemodan.app.djfs.core.util.YcridUtils;
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
public class CopyOperationHandlerTest extends DjfsSingleUserTestBase {
    private final ListF<String> RESOURCE_EXCLUDED_FIELDS = Cf.list("id", "parentId", "fileId", "path", "modifyUid",
            "creationTime", "modificationTime", "version", "liveVideoId", "area");

    @Autowired
    private CopyOperationHandler sut;

    @Test
    public void copyFile() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        DjfsResource source = djfsResourceDao.find(sourcePath).get();
        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();
        Assert.reflectionEquals(source, destination, RESOURCE_EXCLUDED_FIELDS);
        Assert.notEquals(source.getFileId().get(), destination.getFileId().get());
        Assert.equals(now, destination.getCreationTime());
        Assert.equals(now, destination.getModificationTime());
        Assert.some(InstantUtils.toVersion(now), destination.getVersion());
    }

    @Test
    public void copyPublicFile() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x -> x
                .isPublic(true)
                .publicHash("hash")
                .shortUrl("url")
                .downloadCounter(12));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();
        Assert.isFalse(destination.isPublic());
        Assert.none(destination.getPublicHash());
        Assert.none(destination.getDownloadCounter());
        Assert.none(destination.getShortUrl());
    }

    @Test
    public void copyYarovayaMarkedFile() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x -> x.hasYarovayaMark(true));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();
        Assert.isFalse(destination.hasYarovayaMark());
    }

    @Test
    public void copyLivePhotoFile() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.initializeArea(UID, DjfsResourceArea.ADDITIONAL_DATA);

        FolderDjfsResource livePhotoParent =
                (FolderDjfsResource) djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK)).get();
        FolderDjfsResource liveVideoParent = (FolderDjfsResource) djfsResourceDao
                .find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.ADDITIONAL_DATA)).get();

        FileDjfsResource livePhotoFile = FileDjfsResource.random(livePhotoParent, "source", x -> x.isLivePhoto(true));
        FileDjfsResource liveVideoFile = FileDjfsResource.random(liveVideoParent, "not_important");

        transactionUtils.executeInNewOrCurrentTransaction(UID, () -> {
            djfsResourceDao.insert2(liveVideoFile);
            djfsResourceDao.insert2AndLinkTo(livePhotoFile, liveVideoFile);
        });

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        FileDjfsResource sourceLivePhotoFile = (FileDjfsResource) djfsResourceDao.find2(sourcePath).get();
        FileDjfsResource sourceLiveVideoFile = djfsResourceDao.find2AdditionalFileFor(sourceLivePhotoFile).get();


        FileDjfsResource destinationLivePhotoFile = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();
        FileDjfsResource destinationLiveVideoFile =
                djfsResourceDao.find2AdditionalFileFor(destinationLivePhotoFile).get();

        Assert.reflectionEquals(sourceLivePhotoFile, destinationLivePhotoFile, RESOURCE_EXCLUDED_FIELDS);
        Assert.reflectionEquals(sourceLiveVideoFile, destinationLiveVideoFile, RESOURCE_EXCLUDED_FIELDS);
        Assert.notEquals(sourceLiveVideoFile.getId(), destinationLiveVideoFile.getId());
    }

    @Test
    public void copySetsSourcePlatform() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x-> x.externalProperty("some", "value"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        try(SafeCloseable ignored = YcridUtils.setThreadLocal("test-ycrid")) {
            MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
            Assert.equals(MpfsOperationHandler.Status.DONE, status);
        }

        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();
        Assert.equals("value", destination.getExternalProperties().getTs("some"));
        Assert.equals("test", destination.getExternalProperties().getTs("source_platform"));
    }

    @Test
    public void copyOverwritesSourcePlatform() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x-> x.externalProperty("source_platform", "mpfs"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        try(SafeCloseable ignored = YcridUtils.setThreadLocal("test-ycrid")) {
            MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
            Assert.equals(MpfsOperationHandler.Status.DONE, status);
        }

        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();
        Assert.equals("test", destination.getExternalProperties().getTs("source_platform"));
    }

    @Test
    public void indexerPushForFile() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x -> x.mimetype(Option.empty()).mediaType(Option.empty()));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        mockCeleryTaskManager.submitted.clear();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.TASK_ID));
        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.PHOTOSLICE_TASK_ID));
        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.SHARED_TASK_ID));
        Assert.sizeIs(0, mockCeleryTaskManager.submitted
                .filter(x -> x.task == SearchPushGenerator.SHARED_PHOTOSLICE_TASK_ID));

        CeleryJob celeryJob = mockCeleryTaskManager.submitted
                .filter(x -> x.task == SearchPushGenerator.TASK_ID).first();

        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();

        JsonObject data = (JsonObject) ((JsonArray) kwargs.getTs("data")).getArray().get(0);
        Assert.equals(UID.asLong(), ((JsonNumber) data.get("uid")).longValue());
        Assert.equals(destination.getPath().getPath(), ((JsonString) data.get("id")).getValue());
        Assert.equals(destination.getFileId().get().getValue(), ((JsonString) data.get("file_id")).getValue());
        Assert.equals(destination.getResourceId().get().toString(), ((JsonString) data.get("resource_id")).getValue());
        Assert.equals(destination.getPath().getName(), ((JsonString) data.get("name")).getValue());
        Assert.equals(now.getMillis() * 1000, ((JsonNumber) data.get("version")).longValue());
        Assert.equals(InstantUtils.toSeconds(destination.getCreationTime()),
                ((JsonNumber) data.get("ctime")).intValue());
        Assert.equals(InstantUtils.toSeconds(now), ((JsonNumber) data.get("mtime")).intValue());
        Assert.isInstance(data.get("etime"), JsonNull.class);
        Assert.equals(1, ((JsonNumber) data.get("visible")).intValue());
        Assert.equals("copy_resource", ((JsonString) data.get("operation")).getValue());
        Assert.equals("modify", ((JsonString) data.get("action")).getValue());
        Assert.equals("file", ((JsonString) data.get("type")).getValue());
        Assert.isInstance(data.get("mimetype"), JsonNull.class);
        Assert.isInstance(data.get("mediatype"), JsonNull.class);
        Assert.equals(destination.getFileStid(), ((JsonString) data.get("stid")).getValue());
        Assert.equals(destination.getSize(), ((JsonNumber) data.get("size")).longValue());
        Assert.equals(destination.getMd5(), ((JsonString) data.get("md5")).getValue());
        Assert.isInstance(data.get("external_url"), JsonNull.class);
    }

    @Test
    public void indexerPushForImageFile() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x -> x
                .mediaType(MediaType.IMAGE)
                .exifTime(Instant.parse("2018-02-03T12:04:05Z"))
                .mimetype("image/jpeg")
                .previewStid("previews-tid"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        mockCeleryTaskManager.submitted.clear();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.TASK_ID));
        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.PHOTOSLICE_TASK_ID));
        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.SHARED_TASK_ID));
        Assert.sizeIs(0, mockCeleryTaskManager.submitted
                .filter(x -> x.task == SearchPushGenerator.SHARED_PHOTOSLICE_TASK_ID));

        CeleryJob celeryJob = mockCeleryTaskManager.submitted
                .filter(x -> x.task == SearchPushGenerator.PHOTOSLICE_TASK_ID).first();

        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();

        JsonObject data = (JsonObject) ((JsonArray) kwargs.getTs("data")).getArray().get(0);
        Assert.equals(UID.asLong(), ((JsonNumber) data.get("uid")).longValue());
        Assert.equals(destination.getPath().getPath(), ((JsonString) data.get("id")).getValue());
        Assert.equals(destination.getFileId().get().getValue(), ((JsonString) data.get("file_id")).getValue());
        Assert.equals(destination.getResourceId().get().toString(), ((JsonString) data.get("resource_id")).getValue());
        Assert.equals(destination.getPath().getName(), ((JsonString) data.get("name")).getValue());
        Assert.equals(now.getMillis() * 1000, ((JsonNumber) data.get("version")).longValue());
        Assert.equals(InstantUtils.toSeconds(destination.getCreationTime()),
                ((JsonNumber) data.get("ctime")).intValue());
        Assert.equals(InstantUtils.toSeconds(now), ((JsonNumber) data.get("mtime")).intValue());
        Assert.equals(InstantUtils.toSeconds(destination.getExifTime().get()),
                ((JsonNumber) data.get("etime")).intValue());
        Assert.equals(1, ((JsonNumber) data.get("visible")).intValue());
        Assert.equals("copy_resource", ((JsonString) data.get("operation")).getValue());
        Assert.equals("modify", ((JsonString) data.get("action")).getValue());
        Assert.equals("file", ((JsonString) data.get("type")).getValue());
        Assert.equals("image/jpeg", ((JsonString) data.get("mimetype")).getValue());
        Assert.equals("image", ((JsonString) data.get("mediatype")).getValue());
        Assert.equals(destination.getFileStid(), ((JsonString) data.get("stid")).getValue());
        Assert.equals(destination.getSize(), ((JsonNumber) data.get("size")).longValue());
        Assert.equals(destination.getMd5(), ((JsonString) data.get("md5")).getValue());
        Assert.isInstance(data.get("external_url"), JsonNull.class);
    }

    @Test
    public void indexerPushForFolder() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        mockCeleryTaskManager.submitted.clear();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.TASK_ID));
        Assert.sizeIs(0, mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.SHARED_TASK_ID));

        CeleryJob celeryJob = mockCeleryTaskManager.submitted.filter(x -> x.task == SearchPushGenerator.TASK_ID)
                .first();

        FolderDjfsResource destination = (FolderDjfsResource) djfsResourceDao.find2(destinationPath).get();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();

        JsonObject data = (JsonObject) ((JsonArray) kwargs.getTs("data")).getArray().get(0);
        Assert.equals(UID.asLong(), ((JsonNumber) data.get("uid")).longValue());
        Assert.equals(destination.getPath().getPath(), ((JsonString) data.get("id")).getValue());
        Assert.equals(destination.getFileId().get().getValue(), ((JsonString) data.get("file_id")).getValue());
        Assert.equals(destination.getResourceId().get().toString(), ((JsonString) data.get("resource_id")).getValue());
        Assert.equals(destination.getPath().getName(), ((JsonString) data.get("name")).getValue());
        Assert.equals(now.getMillis() * 1000, ((JsonNumber) data.get("version")).longValue());
        Assert.equals(InstantUtils.toSeconds(destination.getCreationTime().get()),
                ((JsonNumber) data.get("ctime")).intValue());
        Assert.equals(InstantUtils.toSeconds(now), ((JsonNumber) data.get("mtime")).intValue());
        Assert.equals(1, ((JsonNumber) data.get("visible")).intValue());
        Assert.equals("copy_resource", ((JsonString) data.get("operation")).getValue());
        Assert.equals("modify", ((JsonString) data.get("action")).getValue());
        Assert.equals("dir", ((JsonString) data.get("type")).getValue());
        Assert.isInstance(data.get("shared_folder_owner"), JsonNull.class);
        Assert.isInstance(data.get("folder_type"), JsonNull.class);
    }

    @Test
    public void xivaPushForFile() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        long oldVersion = now.getMillis() * 1000 - 333333;
        userDao.setVersion(UID, oldVersion);

        // no space push
        diskInfoDao.setLimit(UID, 10 * ByteConstants.GiB);

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        mockCeleryTaskManager.submitted.clear();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID));

        CeleryJob celeryJob = mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID).first();

        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();

        Assert.equals(UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals(String.valueOf(now.getMillis() * 1000), ((JsonString) kwargs.getTs("new_version")).getValue());
        Assert.equals(oldVersion, ((JsonNumber) kwargs.getTs("old_version")).longValue());
        Assert.equals("diff", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("diff", ((JsonString) kwargs.getTs("action_name")).getValue());
        Assert.none(kwargs.getO("connection_id"));

        JsonObject xivaData = (JsonObject) ((JsonArray) kwargs.getTs("xiva_data")).getArray().get(0);
        Assert.equals(destinationPath.getPath(), ((JsonString) xivaData.get("key")).getValue());
        Assert.equals("file", ((JsonString) xivaData.get("resource_type")).getValue());
        Assert.equals(destination.getFileId().get().getValue(), ((JsonString) xivaData.get("fid")).getValue());
        Assert.equals(destination.getMd5(), ((JsonString) xivaData.get("md5")).getValue());
        Assert.equals(destination.getSha256(), ((JsonString) xivaData.get("sha256")).getValue());
        Assert.equals(destination.getSize(), ((JsonNumber) xivaData.get("size")).longValue());
        Assert.equals("new", ((JsonString) xivaData.get("op")).getValue());
    }

    @Test
    public void xivaPushForFolder() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        // no space push
        diskInfoDao.setLimit(UID, 10 * ByteConstants.GiB);

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        mockCeleryTaskManager.submitted.clear();

        long oldVersion = now.getMillis() * 1000 - 333333;
        userDao.setVersion(UID, oldVersion);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.sizeIs(1, mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID));

        CeleryJob celeryJob = mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID).first();

        FolderDjfsResource destination = (FolderDjfsResource) djfsResourceDao.find2(destinationPath).get();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();

        Assert.equals(UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals(String.valueOf(now.getMillis() * 1000), ((JsonString) kwargs.getTs("new_version")).getValue());
        Assert.equals(oldVersion, ((JsonNumber) kwargs.getTs("old_version")).longValue());
        Assert.equals("diff", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("diff", ((JsonString) kwargs.getTs("action_name")).getValue());
        Assert.none(kwargs.getO("connection_id"));

        JsonObject xivaData = (JsonObject) ((JsonArray) kwargs.getTs("xiva_data")).getArray().get(0);
        Assert.equals(destinationPath.getPath(), ((JsonString) xivaData.get("key")).getValue());
        Assert.equals("dir", ((JsonString) xivaData.get("resource_type")).getValue());
        Assert.equals(destination.getFileId().get().getValue(), ((JsonString) xivaData.get("fid")).getValue());
        Assert.equals("new", ((JsonString) xivaData.get("op")).getValue());
    }

    @Test
    public void xivaLowSpacePush() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        long oldVersion = now.getMillis() * 1000 - 333333;
        userDao.setVersion(UID, oldVersion);

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x-> x.size(512 * ByteConstants.MiB));

        diskInfoDao.setLimit(UID, 10 * ByteConstants.GiB);
        diskInfoDao.setTotalUsed(UID, 9 * ByteConstants.GiB);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        mockCeleryTaskManager.submitted.clear();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        ListF<CeleryJob> jobs = mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID)
                .filter(x -> Objects.equals(((JsonString)x.getKwargs().getTs("action_name")).getString(), "space"));
        Assert.sizeIs(1, jobs);
        CeleryJob celeryJob = jobs.first();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();

        Assert.equals(UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("1", ((JsonString) kwargs.getTs("new_version")).getValue());
        Assert.equals("space_is_low", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("space", ((JsonString) kwargs.getTs("action_name")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.none(kwargs.getO("connection_id"));

        JsonObject root = (JsonObject) ((JsonObject) kwargs.getTs("xiva_data")).get("root");
        Assert.equals("space", ((JsonString) root.get("tag")).getValue());

        JsonObject parameters = (JsonObject) root.get("parameters");
        Assert.equals("is_low", ((JsonString) parameters.get("type")).getValue());
        Assert.equals(10 * ByteConstants.GiB, ((JsonNumber) parameters.get("limit")).longValue());
        Assert.equals(512 * ByteConstants.MiB, ((JsonNumber) parameters.get("free")).longValue());
        Assert.equals(9 * ByteConstants.GiB + 512 * ByteConstants.MiB, ((JsonNumber) parameters.get("used")).longValue());

        JsonArray values = (JsonArray) ((JsonObject) kwargs.getTs("xiva_data")).get("values");
        Assert.isTrue(values.isEmpty());
    }

    @Test
    public void xivaNoFreeSpacePush() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        long oldVersion = now.getMillis() * 1000 - 333333;
        userDao.setVersion(UID, oldVersion);

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x-> x.size(ByteConstants.GiB));

        diskInfoDao.setLimit(UID, 10 * ByteConstants.GiB);
        diskInfoDao.setTotalUsed(UID, 9 * ByteConstants.GiB);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        mockCeleryTaskManager.submitted.clear();

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        ListF<CeleryJob> jobs = mockCeleryTaskManager.submitted.filter(x -> x.task == XivaPushGenerator.TASK_ID)
                .filter(x -> Objects.equals(((JsonString)x.getKwargs().getTs("action_name")).getString(), "space"));
        Assert.sizeIs(1, jobs);
        CeleryJob celeryJob = jobs.first();

        MapF<String, JsonValue> kwargs = celeryJob.getKwargs();

        Assert.equals(UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("1", ((JsonString) kwargs.getTs("new_version")).getValue());
        Assert.equals("space_is_full", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("space", ((JsonString) kwargs.getTs("action_name")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.none(kwargs.getO("connection_id"));

        JsonObject root = (JsonObject) ((JsonObject) kwargs.getTs("xiva_data")).get("root");
        Assert.equals("space", ((JsonString) root.get("tag")).getValue());

        JsonObject parameters = (JsonObject) root.get("parameters");
        Assert.equals("is_full", ((JsonString) parameters.get("type")).getValue());
        Assert.equals(10 * ByteConstants.GiB, ((JsonNumber) parameters.get("limit")).longValue());
        Assert.equals(0L, ((JsonNumber) parameters.get("free")).longValue());
        Assert.equals(10 * ByteConstants.GiB, ((JsonNumber) parameters.get("used")).longValue());

        JsonArray values = (JsonArray) ((JsonObject) kwargs.getTs("xiva_data")).get("values");
        Assert.isTrue(values.isEmpty());
    }

    @Test
    public void copyFileChangesQuota() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");
        long size = 333;

        quotaManager.setLimit(UID, 1000);
        filesystem.createFile(PRINCIPAL, sourcePath, x -> x.size(size));
        diskInfoDao.setTotalUsed(UID, 222);
        diskInfoDao.setTrashUsed(UID, 111);

        Assert.equals(222L, quotaManager.getTotalUsed(UID));
        Assert.equals(111L, quotaManager.getTrashUsed(UID));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.equals(555L, quotaManager.getTotalUsed(UID));
        Assert.equals(111L, quotaManager.getTrashUsed(UID));
    }

    @Test
    public void copyFileToTrashChangesQuota() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/trash/destination");
        long size = 333;

        quotaManager.setLimit(UID, 1000);
        filesystem.createFile(PRINCIPAL, sourcePath, x -> x.size(size));
        diskInfoDao.setTotalUsed(UID, 222);
        diskInfoDao.setTrashUsed(UID, 111);

        Assert.equals(222L, quotaManager.getTotalUsed(UID));
        Assert.equals(111L, quotaManager.getTrashUsed(UID));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.equals(555L, quotaManager.getTotalUsed(UID));
        Assert.equals(444L, quotaManager.getTrashUsed(UID));
    }

    @Test
    public void copyFileExceedingQuotaFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");
        long size = 222;

        quotaManager.setLimit(UID, 100);
        filesystem.createFile(PRINCIPAL, sourcePath, x -> x.size(size));
        long before = quotaManager.getTotalUsed(UID);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);
        Assert.none(djfsResourceDao.find2(destinationPath));

        long after = quotaManager.getTotalUsed(UID);
        Assert.equals(after, before);
    }

    @Test
    public void lockReleasedAfterCopy() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.isFalse(lockManager.isLocked(sourcePath));
        Assert.isFalse(lockManager.isLocked(destinationPath));
    }

    @Test
    public void copyEmptyFolder() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        DjfsResource source = djfsResourceDao.find2(sourcePath).get();
        DjfsResource destination = djfsResourceDao.find2(destinationPath).get();
        Assert.reflectionEquals(source, destination, RESOURCE_EXCLUDED_FIELDS);
        Assert.notEquals(source.getFileId().get(), destination.getFileId().get());
    }

    @Test
    public void copyFolderWithResources() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file1"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file2"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file3"));
        filesystem.createFolder(PRINCIPAL, sourcePath.getChildPath("subfolder"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("subfolder").getChildPath("file1"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("subfolder").getChildPath("file2"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("subfolder").getChildPath("file3"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath).get(),
                djfsResourceDao.find2(destinationPath).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("file1")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("file1")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("file2")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("file2")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("file3")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("file3")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("subfolder").getChildPath("file1")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("subfolder").getChildPath("file1")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("subfolder").getChildPath("file2")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("subfolder").getChildPath("file2")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("subfolder").getChildPath("file3")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("subfolder").getChildPath("file3")).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void copyFolderWithRestrictedResources() {
        FolderDjfsResource source = FolderDjfsResource.cons(UID, "/disk/source");
        FolderDjfsResource destination = FolderDjfsResource.cons(UID, "/disk/destination");
        FolderDjfsResource restrictedFolder_1 = FolderDjfsResource.cons(source, "..");
        FolderDjfsResource restrictedFolder_2 = FolderDjfsResource.cons(source, ".");

        FileDjfsResource file1 = FileDjfsResource.random(restrictedFolder_1, "file1");
        FileDjfsResource file2 = FileDjfsResource.random(restrictedFolder_2, "file2");
        FileDjfsResource file3 = FileDjfsResource.random(source, "file3");
        djfsResourceDao.insert(UID, source, restrictedFolder_1,restrictedFolder_2);
        djfsResourceDao.insert(UID, file1, file2, file3);

        DjfsResourcePath sourcePath = source.getPath();
        DjfsResourcePath destinationPath = destination.getPath();

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath).get(),
                djfsResourceDao.find2(destinationPath).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("..")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("..")).get(),
                RESOURCE_EXCLUDED_FIELDS);
        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath(".")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath(".")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("..").getChildPath("file1")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("..").getChildPath("file1")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath(".").getChildPath("file2")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath(".").getChildPath("file2")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("file3")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("file3")).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void copyFolderWithResourcesChangesQuota() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");
        long size = 63;

        quotaManager.setLimit(UID, 10000);
        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file1"), x -> x.size(1));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file2"), x -> x.size(2));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file3"), x -> x.size(4));
        filesystem.createFolder(PRINCIPAL, sourcePath.getChildPath("subfolder"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("subfolder").getChildPath("file1"), x -> x.size(8));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("subfolder").getChildPath("file2"), x -> x.size(16));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("subfolder").getChildPath("file3"), x -> x.size(32));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);
        long before = quotaManager.getTotalUsed(UID);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        long after = quotaManager.getTotalUsed(UID);
        Assert.equals(size, after - before);
    }

    @Test
    public void copyFolderWithResourcesExceedingQuotaFails() {
        DjfsFileId f1 = DjfsFileId.cons(StringUtils.repeat("1", 64));
        DjfsFileId f2 = DjfsFileId.cons(StringUtils.repeat("2", 64));
        DjfsFileId f3 = DjfsFileId.cons(StringUtils.repeat("3", 64));
        DjfsFileId f4 = DjfsFileId.cons(StringUtils.repeat("4", 64));
        DjfsFileId f5 = DjfsFileId.cons(StringUtils.repeat("5", 64));

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        quotaManager.setLimit(UID, 10);
        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file1"), x -> x.size(1).fileId(f1));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file2"), x -> x.size(2).fileId(f2));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file3"), x -> x.size(4).fileId(f3));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file4"), x -> x.size(8).fileId(f4));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file5"), x -> x.size(16).fileId(f5));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);
        long before = quotaManager.getTotalUsed(UID);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);

        long after = quotaManager.getTotalUsed(UID);
        Assert.equals(7L, after - before);
        Assert.some(djfsResourceDao.find2(destinationPath));
        Assert.some(djfsResourceDao.find2(destinationPath.getChildPath("file1")));
        Assert.some(djfsResourceDao.find2(destinationPath.getChildPath("file2")));
        Assert.some(djfsResourceDao.find2(destinationPath.getChildPath("file3")));
        Assert.none(djfsResourceDao.find2(destinationPath.getChildPath("file4")));
        Assert.none(djfsResourceDao.find2(destinationPath.getChildPath("file5")));
    }

    @Test
    public void copyFolderOverExistingFolderSucceeds() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFolder(PRINCIPAL, destinationPath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);
    }

    @Test
    public void copyFileOverExistingFileWithSameHashesSucceeds() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");
        String md5 = StringUtils.repeat("a", 32);
        String sha256 = StringUtils.repeat("a", 64);
        String hid = StringUtils.repeat("a", 32);

        filesystem.createFile(PRINCIPAL, sourcePath, x -> x.md5(md5).sha256(sha256).size(0).hid(hid));
        filesystem.createFile(PRINCIPAL, destinationPath, x -> x.md5(md5).sha256(sha256).size(0).hid(hid));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);
    }

    @Test
    public void folderChangelog() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        changelogDao.deleteAll(UID);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        DjfsResource destination = djfsResourceDao.find2(destinationPath).get();

        ListF<Changelog> changelogs = changelogDao.findAll(UID);
        Assert.sizeIs(1, changelogs);

        Changelog changelog = changelogs.first();
        Assert.equals(destinationPath, changelog.getPath());
        Assert.equals(now.getMillis() * 1000, changelog.getVersion());
        Assert.equals(destination.getResourceId().get().getFileId().getValue(), changelog.getFileId());
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
    public void fileChangelog() {
        Instant now = InstantUtils.fromSeconds(InstantUtils.toSeconds(Instant.now()));
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath, x -> x
                .mediaType(MediaType.IMAGE)
                .exifTime(Instant.parse("2018-02-03T12:04:05Z"))
                .mimetype("image/jpeg")
                .previewStid("previews-tid")
                .externalProperty("external", "property"));
        changelogDao.deleteAll(UID);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        FileDjfsResource destination = (FileDjfsResource) djfsResourceDao.find2(destinationPath).get();

        ListF<Changelog> changelogs = changelogDao.findAll(UID);
        Assert.sizeIs(1, changelogs);

        Changelog changelog = changelogs.first();
        Assert.equals(destinationPath, changelog.getPath());
        Assert.equals(now.getMillis() * 1000, changelog.getVersion());
        Assert.equals(destination.getResourceId().get().getFileId().getValue(), changelog.getFileId());
        Assert.equals(DjfsResourceType.FILE, changelog.getResourceType());
        Assert.equals(Changelog.OperationType.NEW, changelog.getOperationType());
        Assert.isFalse(changelog.isPublik());
        Assert.isTrue(changelog.isVisible());
        Assert.some(changelog.getDtime());
        Assert.none(changelog.getGroupId());
        Assert.none(changelog.getGroupPath());
        Assert.equals(destination.getMd5(), changelog.getMd5().get());
        Assert.equals(destination.getSha256(), changelog.getSha256().get());
        Assert.equals(destination.getSize(), changelog.getSize().get());
        Assert.equals(destination.getMimetype(), changelog.getMimetype());
        Assert.equals(destination.getMediaType(), changelog.getMediaType());
        Assert.equals(destination.getAntiVirusScanStatus(), changelog.getAntiVirusScanStatus());
        Assert.some(true, changelog.getHasPreview());
        Assert.some(true, changelog.getHasExternalSetprop());
        Assert.equals(destination.getExifTime(), changelog.getExifTime());
        Assert.equals(destination.getModificationTime(), changelog.getModificationTime().get());
    }

    @Test
    public void folderEventHistoryLog() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        FolderDjfsResource source = filesystem.createFolder(PRINCIPAL, sourcePath);
        mockEventHistoryLogger.messageData.clear();

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        DjfsResource destination = djfsResourceDao.find2(destinationPath).get();

        Assert.sizeIs(1, mockEventHistoryLogger.messageData);
        MapF<String, String> actual = mockEventHistoryLogger.messageData.get(0);
        Assert.sizeIs(11, actual);
        Assert.equals("fs-copy", actual.getTs("event_type"));
        Assert.equals("dir", actual.getTs("resource_type"));
        Assert.equals(UID.asString(), actual.getTs("uid"));
        Assert.equals(UID.asString(), actual.getTs("owner_uid"));
        Assert.equals(destination.getFileId().get().getValue(), actual.getTs("resource_file_id"));
        Assert.equals(sourcePath.getFullPath(), actual.getTs("src_rawaddress"));
        Assert.equals(destinationPath.getFullPath(), actual.getTs("tgt_rawaddress"));
        Assert.equals(source.getResourceId().get().getValue(), actual.getTs("src_folder_id"));
        Assert.equals(destination.getResourceId().get().getValue(), actual.getTs("tgt_folder_id"));
        Assert.equals("false", actual.getTs("overwritten"));
        Assert.equals("false", actual.getTs("force"));
    }

    @Test
    public void fileEventHistoryLog() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/folder1/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/folder2/destination");

        FolderDjfsResource sourceParent = filesystem.createFolder(PRINCIPAL, sourcePath.getParent());
        FolderDjfsResource destinationParent = filesystem.createFolder(PRINCIPAL, destinationPath.getParent());

        filesystem.createFile(PRINCIPAL, sourcePath, x -> x.mediaType(MediaType.IMAGE));
        mockEventHistoryLogger.messageData.clear();

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        DjfsResource destination = djfsResourceDao.find2(destinationPath).get();

        Assert.sizeIs(1, mockEventHistoryLogger.messageData);
        MapF<String, String> actual = mockEventHistoryLogger.messageData.get(0);
        Assert.sizeIs(13, actual);
        Assert.equals("fs-copy", actual.getTs("event_type"));
        Assert.equals("file", actual.getTs("resource_type"));
        Assert.equals(UID.asString(), actual.getTs("uid"));
        Assert.equals(UID.asString(), actual.getTs("owner_uid"));
        Assert.equals("image", actual.getTs("resource_media_type"));
        Assert.equals("image", actual.getTs("lenta_media_type"));
        Assert.equals(destination.getFileId().get().getValue(), actual.getTs("resource_file_id"));
        Assert.equals(sourcePath.getFullPath(), actual.getTs("src_rawaddress"));
        Assert.equals(destinationPath.getFullPath(), actual.getTs("tgt_rawaddress"));
        Assert.equals(sourceParent.getResourceId().get().getValue(), actual.getTs("src_folder_id"));
        Assert.equals(destinationParent.getResourceId().get().getValue(), actual.getTs("tgt_folder_id"));
        Assert.equals("false", actual.getTs("force"));
        Assert.equals("false", actual.getTs("overwritten"));
    }

    @Test
    public void copyIntoDestinationWithoutParentFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/target/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);
    }

    @Test
    public void copyIntoDestinationWithParentFileFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/file/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, destinationPath.getParent());

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);
    }

    @Test
    public void copySameSourceAndDestinationFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/source");

        filesystem.createFile(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);
    }

    @Test
    public void copyIntoSelfFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/source/into/self");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFolder(PRINCIPAL, sourcePath.getChildPath("into"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);

        Assert.none(djfsResourceDao.find2(destinationPath));
    }

    @Test
    public void copyFileOverExistingFileWithDifferentHashesFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, destinationPath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);
    }

    @Test
    public void copyFolderOverExistingFileFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, destinationPath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);
    }

    @Test
    public void copyFileOverExistingFolderFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);
        filesystem.createFolder(PRINCIPAL, destinationPath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);
    }

    @Test
    public void copyToPhotounlimFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/photounlim/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);
    }

    @Test
    public void copyFolderWithFileOverExistingFolderWithFolderFails() {
        DjfsFileId f1 = DjfsFileId.cons(StringUtils.repeat("1", 64));
        DjfsFileId f2 = DjfsFileId.cons(StringUtils.repeat("2", 64));

        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("child"), x -> x.fileId(f1));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file"), x -> x.fileId(f2));
        filesystem.createFolder(PRINCIPAL, destinationPath);
        filesystem.createFolder(PRINCIPAL, destinationPath.getChildPath("child"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);

        Assert.some(djfsResourceDao.find2(destinationPath));
        Assert.some(djfsResourceDao.find2(destinationPath.getChildPath("child")));
        Assert.none(djfsResourceDao.find2(destinationPath.getChildPath("file")));
    }

    @Test
    public void copyFolderWithFolderOverExistingFolderWithFileFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFolder(PRINCIPAL, sourcePath.getChildPath("child"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file"));
        filesystem.createFolder(PRINCIPAL, destinationPath);
        filesystem.createFile(PRINCIPAL, destinationPath.getChildPath("child"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);

        Assert.some(djfsResourceDao.find2(destinationPath));
        Assert.some(djfsResourceDao.find2(destinationPath.getChildPath("child")));
        Assert.none(djfsResourceDao.find2(destinationPath.getChildPath("file")));
    }

    @Test
    public void copyFileWithInsufficientSpaceFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        quotaManager.setLimit(UID, 100);
        filesystem.createFile(PRINCIPAL, sourcePath, x -> x.size(1000));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);

        Operation finishedOperation = operationDao.find(UID, operation.getId()).get();
        Operation.ErrorData error = finishedOperation.getData(CopyOperationData.B).getError().get();
        Assert.equals(507, error.getResponse());
        Assert.equals(59, error.getCode());
        Assert.equals("NoFreeSpaceCopyToDisk", error.getMessage());
    }

    @Test
    public void copyLockedResourceFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);
        lockManager.lock(sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);

        Operation finishedOperation = operationDao.find(UID, operation.getId()).get();
        Operation.ErrorData error = finishedOperation.getData(CopyOperationData.B).getError().get();
        Assert.equals(423, error.getResponse());
        Assert.equals(105, error.getCode());
        Assert.equals("ResourceLocked", error.getMessage());
    }

    @Test
    public void copyLockedResourceBySameLockerIdSucceeds() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFile(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        Assert.isTrue(lockManager.tryAcquireOrRenewLock(operation.getId(), operation.getId(), "", sourcePath));

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);
        Assert.some(djfsResourceDao.find(destinationPath));
    }

    @Test
    public void copyResourceInsideLockedFolderFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source/path");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath.getParent());
        filesystem.createFile(PRINCIPAL, sourcePath);
        lockManager.lock(sourcePath.getParent());

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);

        Operation finishedOperation = operationDao.find(UID, operation.getId()).get();
        Operation.ErrorData error = finishedOperation.getData(CopyOperationData.B).getError().get();
        Assert.equals(423, error.getResponse());
        Assert.equals(105, error.getCode());
        Assert.equals("ResourceLocked", error.getMessage());
    }

    @Test
    public void copyResourceInsideLockedFolderBySameLockerIdSucceeds() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source/path");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath.getParent());
        filesystem.createFile(PRINCIPAL, sourcePath);

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        Assert.isTrue(lockManager.tryAcquireOrRenewLock(operation.getId(), operation.getId(), "", sourcePath.getParent()));

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);
        Assert.some(djfsResourceDao.find(destinationPath));
    }

    @Test
    public void copyFolderWithLockedResourceInsideFails() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file"));
        lockManager.lock(sourcePath.getChildPath("file"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.FAIL, status);

        Operation finishedOperation = operationDao.find(UID, operation.getId()).get();
        Operation.ErrorData error = finishedOperation.getData(CopyOperationData.B).getError().get();
        Assert.equals(423, error.getResponse());
        Assert.equals(105, error.getCode());
        Assert.equals("ResourceLocked", error.getMessage());
    }

    @Test
    public void copyFolderWithLockedResourceInsideBySameLockerIdSucceeds() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        Assert.isTrue(lockManager.tryAcquireOrRenewLock(operation.getId(), operation.getId(), "", sourcePath.getChildPath("file")));

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);
        Assert.some(djfsResourceDao.find(destinationPath));
    }

    @Test
    public void copyFolderWithResourcesWithFailures() {
        DjfsResourcePath sourcePath = DjfsResourcePath.cons(UID, "/disk/source");
        DjfsResourcePath destinationPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(PRINCIPAL, sourcePath);
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file1"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file2"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("file3"));
        filesystem.createFolder(PRINCIPAL, sourcePath.getChildPath("subfolder"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("subfolder").getChildPath("file1"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("subfolder").getChildPath("file2"));
        filesystem.createFile(PRINCIPAL, sourcePath.getChildPath("subfolder").getChildPath("file3"));

        Operation operation = CopyOperation.create(UID, sourcePath, destinationPath);
        operationDao.insert(operation);

        randomFailuresProbabilitySource.setFailureProbability(0.4);

        int tryNumber = 0;
        while (tryNumber < 20000) {
            tryNumber += 1;
            try {
                operation = operationDao.find(UID, operation.getId()).get();
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

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("file1")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("file1")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("file2")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("file2")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("file3")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("file3")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("subfolder").getChildPath("file1")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("subfolder").getChildPath("file1")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("subfolder").getChildPath("file2")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("subfolder").getChildPath("file2")).get(),
                RESOURCE_EXCLUDED_FIELDS);

        Assert.reflectionEquals(djfsResourceDao.find2(sourcePath.getChildPath("subfolder").getChildPath("file3")).get(),
                djfsResourceDao.find2(destinationPath.getChildPath("subfolder").getChildPath("file3")).get(),
                RESOURCE_EXCLUDED_FIELDS);
    }

    @Test
    public void destinationAreaIsCorrect() {
        DjfsResourcePath sourceFolderPath = DjfsResourcePath.cons(UID, "/attach/folder");
        DjfsResourcePath sourceFilePath = sourceFolderPath.getChildPath("file.txt");
        DjfsResourcePath destinationFolderPath = DjfsResourcePath.cons(UID, "/disk/destination");

        filesystem.createFolder(DjfsPrincipal.SYSTEM, sourceFolderPath, x -> x.area(DjfsResourceArea.ATTACH));
        filesystem.createFile(PRINCIPAL, sourceFilePath, x -> x.area(DjfsResourceArea.ATTACH));

        Operation operation = CopyOperation.create(UID, sourceFolderPath, destinationFolderPath);
        operationDao.insert(operation);

        MpfsOperationHandler.Status status = sut.handle(operation, new AtomicBoolean());
        Assert.equals(MpfsOperationHandler.Status.DONE, status);

        Option<FolderDjfsResource> folder = filesystem.find(PRINCIPAL, destinationFolderPath, Option.of(ReadPreference.primary())).cast();
        Option<FileDjfsResource> file = filesystem.find(PRINCIPAL,
                sourceFilePath.changeParent(sourceFolderPath, destinationFolderPath), Option.of(ReadPreference.primary())).cast();

        Assert.isTrue(folder.isPresent());
        Assert.isTrue(file.isPresent());

        Assert.equals(DjfsResourceArea.DISK, folder.get().getArea().get());
        Assert.equals(DjfsResourceArea.DISK, file.get().getArea().get());
    }
}
