package ru.yandex.chemodan.app.djfs.core.filesystem;

import java.util.UUID;

import com.mongodb.ReadPreference;
import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.FilenamePreviewStidMimetypeVersionFileId;
import ru.yandex.chemodan.app.djfs.core.db.EntityAlreadyExistsException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.AntiVirusScanStatus;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderType;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.MediaType;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class DjfsResourceDaoTest extends DjfsSingleUserTestBase {
    @Test
    public void folderRoundtrip() {
        DjfsResource parent = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK)).get();

        FolderDjfsResource expected = FolderDjfsResource.builder()
                .id(UuidUtils.fromHex("66f1148f825b35985fb3c87376de6601"))
                .parentId(parent.getId())
                .fileId(DjfsFileId.cons("03054de9d14b41c1b8bc339792a33f1626ea883d3458366e48ce1cb4a7f5f68e"))
                .path(DjfsResourcePath.cons(UID, "/disk/folder"))
                .modifyUid(DjfsUid.cons("54321"))
                .version(1510761634748146L)
                .uploadTime(InstantUtils.fromSeconds(1510755749))
                .trashAppendTime(InstantUtils.fromSeconds(1510761646))
                .hiddenAppendTime(InstantUtils.fromSeconds(1510761757))
                .trashAppendOriginalPath("/trash/append/original/path")
                .isVisible(true)
                .isPublic(true)
                .isBlocked(true)
                .isPublished(true)
                .hasYarovayaMark(true)
                .publicHash("specific public hash")
                .shortUrl("specific short url")
                .symlink("specific symlink")
                .folderUrl("specific folder url")
                .downloadCounter(34)
                .customProperties("specific custrom properties")
                .creationTime(InstantUtils.fromSeconds(1510761634))
                .modificationTime(InstantUtils.fromSeconds(1510761635))
                .folderType(FolderType.PHOTOSTREAM)
                .lastImportTime(InstantUtils.fromSeconds(1510761899))
                .externalProperty("first", "specific value 1")
                .externalProperty("second", "specific value 2")
                .build();

        djfsResourceDao.insert2(expected);
        FolderDjfsResource actual = (FolderDjfsResource) djfsResourceDao.find2(UID, DjfsResourceArea.DISK,
                UuidUtils.fromHex("66f1148f825b35985fb3c87376de6601")).get();

        Assert.reflectionEquals(expected, actual);
    }

    @Test
    public void fileRoundtrip() {
        String videoInfo = "{\"format\": \"mov,mp4,m4a,3gp,3g2,mj2\", \"bitRate\": 724992, \"streams\": [{\"id\": \"0\", \"type\": \"videoFormat\", \"codec\": \"h264\", \"bitRate\": 624640, \"dimension\": {\"width\": 480, \"height\": 360}, \"frameRate\": 29.97}, {\"id\": \"1\", \"type\": \"audioFormat\", \"codec\": \"aac\", \"stereo\": true, \"bitRate\": 97280, \"channelsCount\": 2, \"sampleFrequency\": 44100}], \"duration\": 1421850, \"startTime\": 0, \"creationTime\": 1327609746000}";

        DjfsResource parent = djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK)).get();

        FileDjfsResource expected = FileDjfsResource.builder()
                .id(UuidUtils.fromHex("66f1148f825b35985fb3c87376de6601"))
                .parentId(parent.getId())
                .fileId(DjfsFileId.cons("03054de9d14b41c1b8bc339792a33f1626ea883d3458366e48ce1cb4a7f5f68e"))
                .path(DjfsResourcePath.cons(UID, "/disk/file"))
                .modifyUid(DjfsUid.cons("54321"))
                .version(1510761634748146L)
                .uploadTime(InstantUtils.fromSeconds(1510755749))
                .trashAppendTime(InstantUtils.fromSeconds(1510761646))
                .hiddenAppendTime(InstantUtils.fromSeconds(1510761757))
                .trashAppendOriginalPath("/trash/append/original/path")
                .isVisible(true)
                .isPublic(true)
                .isBlocked(true)
                .isPublished(true)
                .hasYarovayaMark(true)
                .publicHash("specific public hash")
                .shortUrl("specific short url")
                .symlink("specific symlink")
                .folderUrl("specific folder url")
                .downloadCounter(34)
                .customProperties("specific custrom properties")
                .size(3000)
                .hid("ed3fff9ddfe15d2b319d28e6a4d9c86e")
                .md5("faa87dd3a0e961df7474310035dd6935")
                .sha256("963beffa7b86a10365dcebf23fe1deae29e7fa27d1072bd07df6d50bc7ee15c6")
                .fileStid("320.yadisk:547130119.E63516:4065349937144441621134816553043")
                .digestStid("320.yadisk:547130119.E50511:4065349937141120666638229405779")
                .previewStid("320.yadisk:547130119.E235461:4065349937145297819961940985939")
                .creationTime(InstantUtils.fromSeconds(1510761634))
                .modificationTime(InstantUtils.fromSeconds(1510761635))
                .exifTime(InstantUtils.fromSeconds(1391751002))
                .antiVirusScanStatus(AntiVirusScanStatus.CLEAN)
                .source("specific source")
                .mimetype("specific mimetype")
                .mediaType(MediaType.DEVELOPMENT)
                .fotkiTags("specific fotki tags")
                .externalUrl("specific external url")
                .height(1024)
                .width(768)
                .angle(180)
                .videoInfo(videoInfo)
                .externalProperty("first", "specific value 1")
                .externalProperty("second", "specific value 2")
                .isLivePhoto(false)
                .build();

        djfsResourceDao.insert2(expected);
        FileDjfsResource actual = (FileDjfsResource) djfsResourceDao.find2(UID, DjfsResourceArea.DISK,
                UuidUtils.fromHex("66f1148f825b35985fb3c87376de6601")).get();

        Assert.reflectionEquals(expected, actual);
    }

    @Test
    public void insertFolderWithSameNameAsFileFails() {
        djfsResourceDao.insert2(FileDjfsResource.random(UID, "/disk/resource"));
        Assert.assertThrows(() -> djfsResourceDao.insert2(FolderDjfsResource.cons(UID, "/disk/resource")),
                EntityAlreadyExistsException.class);
    }

    @Test
    public void insertFileWithSameNameAsFolderFails() {
        djfsResourceDao.insert2(FolderDjfsResource.cons(UID, "/disk/resource"));
        Assert.assertThrows(() -> djfsResourceDao.insert2(FileDjfsResource.random(UID, "/disk/resource")),
                EntityAlreadyExistsException.class);
    }

    @Test
    public void livePhotoRoundtrip() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        filesystem.initializeArea(UID, DjfsResourceArea.ADDITIONAL_DATA);

        FolderDjfsResource livePhotoParent = (FolderDjfsResource) djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.DISK)).get();
        FolderDjfsResource liveVideoParent = (FolderDjfsResource) djfsResourceDao.find(DjfsResourcePath.areaRoot(UID, DjfsResourceArea.ADDITIONAL_DATA)).get();

        FileDjfsResource livePhotoFile = FileDjfsResource.random(livePhotoParent, "live_photo", x -> x.isLivePhoto(true));
        FileDjfsResource liveVideoFile = FileDjfsResource.random(liveVideoParent, "live_video");

        transactionUtils.executeInNewOrCurrentTransaction(UID, () -> {
            djfsResourceDao.insert2(liveVideoFile);
            djfsResourceDao.insert2AndLinkTo(livePhotoFile, liveVideoFile);
        });

        FileDjfsResource actualLivePhotoFile = (FileDjfsResource) djfsResourceDao.find2(livePhotoFile.getPath()).get();
        FileDjfsResource actualLiveVideoFile = djfsResourceDao.find2AdditionalFileFor(actualLivePhotoFile).get();

        Assert.reflectionEquals(livePhotoFile, actualLivePhotoFile, Cf.list("liveVideoId"));
        Assert.reflectionEquals(liveVideoFile, actualLiveVideoFile);
    }

    @Test
    public void findPreviewDownloadUrlMetatada() {
        DjfsFileId fileId = DjfsFileId.random();
        String previewStid = UuidUtils.randomToHexString();
        String filename = "resource_1";
        String mimetype = "image/jpeg";
        long version = 100500;
        UUID id = UUID.randomUUID();

        FileDjfsResource resource = FileDjfsResource.random(DjfsResourcePath.cons(UID, "/disk/" + filename),
                x -> x.fileId(fileId).previewStid(previewStid).mimetype(mimetype).version(version).id(id));

        djfsResourceDao.insert2(resource);

        ListF<FilenamePreviewStidMimetypeVersionFileId> filenamePreviewUrlGeneratorMetadata =
                djfsResourceDao.find2FilenamePreviewStidMimetypeVersionFid(
                        UID,
                        Cf.list(fileId)
                );

        Assert.isTrue(filenamePreviewUrlGeneratorMetadata.length() == 1);
        FilenamePreviewStidMimetypeVersionFileId first = filenamePreviewUrlGeneratorMetadata.get(0);

        Assert.equals(first.getFileId().get(), fileId);
        Assert.equals(first.getPreviewStid().get(), previewStid);
        Assert.equals(first.getMimetype().get(), mimetype);
        Assert.equals(first.getFilename(), filename);
        Assert.equals(first.getId(), id);
        Assert.equals(first.getVersion().get(), version);
    }

    @Test
    public void findFolderByPath() {
        DjfsResourcePath folder1 = DjfsResourcePath.cons(UID, "/disk/f1");
        DjfsResourcePath folder2 = folder1.getChildPath("f2");
        DjfsResourcePath folder3 = folder2.getChildPath("f3");

        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder1);
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder2);
        DjfsFileId folder3fileId = DjfsFileId.random();
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder3, x -> x.fileId(folder3fileId));

        Option<DjfsResource> resource = filesystem.find(DjfsPrincipal.SYSTEM, folder3, Option.of(ReadPreference.primary()));
        Assert.isTrue(resource.isPresent());
        Assert.isTrue(resource.get().getFileId().isPresent());
        Assert.equals(resource.get().getFileId().get(), folder3fileId);
    }

    @Test
    public void findFileByPath() {
        DjfsResourcePath folder1 = DjfsResourcePath.cons(UID, "/disk/f1");
        DjfsResourcePath folder2 = folder1.getChildPath("f2");
        DjfsResourcePath file = folder2.getChildPath("f3");

        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder1);
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder2);
        DjfsFileId fileId = DjfsFileId.random();
        filesystem.createFile(DjfsPrincipal.SYSTEM, file, x -> x.fileId(fileId));

        Option<DjfsResource> resource = filesystem.find(DjfsPrincipal.SYSTEM, file, Option.of(ReadPreference.primary()));
        Assert.isTrue(resource.isPresent());
        Assert.isTrue(resource.get().getFileId().isPresent());
        Assert.equals(resource.get().getFileId().get(), fileId);
    }

    @Test
    public void findNotExistingResourceByPath() {
        DjfsResourcePath folder1 = DjfsResourcePath.cons(UID, "/disk/f1");
        DjfsResourcePath folder2 = folder1.getChildPath("f2");
        DjfsResourcePath folder3 = folder2.getChildPath("f3");

        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder1);
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder2);

        Option<DjfsResource> resource = filesystem.find(DjfsPrincipal.SYSTEM, folder3, Option.of(ReadPreference.primary()));
        Assert.isFalse(resource.isPresent());
    }

    @Test
    public void findFolderByPathWithNotExistingMiddleFolderPart() {
        DjfsResourcePath folder1 = DjfsResourcePath.cons(UID, "/disk/f1");
        DjfsResourcePath folder2 = folder1.getChildPath("f2");
        DjfsResourcePath folder3 = folder2.getChildPath("f3");
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder1);
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder2);
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder3);

        DjfsResourcePath notExistingPath1 = folder2.getChildPath("anything").getChildPath(folder3.getName());
        Option<DjfsResource> resource = filesystem.find(DjfsPrincipal.SYSTEM, notExistingPath1, Option.of(ReadPreference.primary()));
        Assert.isFalse(resource.isPresent());
    }

    @Test
    public void findFolderByPathWithNotExistingLongMiddlePart() {
        DjfsResourcePath folder1 = DjfsResourcePath.cons(UID, "/disk/f1");
        DjfsResourcePath folder2 = folder1.getChildPath("f2");
        DjfsResourcePath folder3 = folder2.getChildPath("f3");
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder1);
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder2);
        filesystem.createFolder(DjfsPrincipal.SYSTEM, folder3);

        DjfsResourcePath notExistingPath2 = folder2
                .getMultipleChildPath("anything/infinitely/many/times")
                .getChildPath(folder3.getName());
        Option<DjfsResource> resource = filesystem.find(DjfsPrincipal.SYSTEM, notExistingPath2, Option.of(ReadPreference.primary()));
        Assert.isFalse(resource.isPresent());
    }

    @Test
    public void testYarovayaMarkForSomeParent() {
        DjfsResourcePath folder1 = DjfsResourcePath.cons(UID, "/disk/folder");
        DjfsResourcePath folder2 = folder1.getChildPath("subfolder");
        DjfsResourcePath folder3 = folder2.getChildPath("deep-inside");
        DjfsResourcePath folder4 = folder3.getChildPath("we-need-to-go-deeper");
        DjfsResourcePath file = folder4.getChildPath("file.txt");

        filesystem.createFolder(PRINCIPAL, folder1);
        filesystem.createFolder(PRINCIPAL, folder2);
        filesystem.createFolder(PRINCIPAL, folder3, x -> x.hasYarovayaMark(true));
        filesystem.createFolder(PRINCIPAL, folder4);
        filesystem.createFile(PRINCIPAL, file);

        Assert.isTrue(djfsResourceDao.hasAnyParentWithYarovayaMark(file));
        Assert.isFalse(djfsResourceDao.hasAnyParentWithYarovayaMark(folder3));
    }

    @Test
    public void testCountFiles() {
        final long filesCount = 42;
        for (int i = 0; i < filesCount; i++) {
            filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, "/disk/file-" + i + ".txt"));
        }
        Assert.equals(filesCount, djfsResourceDao.countAllFiles(UID));
    }
}
