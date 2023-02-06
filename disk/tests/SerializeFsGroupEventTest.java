package ru.yandex.chemodan.app.eventloader.serializer.tests;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.MpfsAddress;
import ru.yandex.chemodan.eventlog.events.Resource;
import ru.yandex.chemodan.eventlog.events.ResourceLocation;
import ru.yandex.chemodan.eventlog.events.fs.FsEvents;
import ru.yandex.chemodan.eventlog.events.fs.StoreTypeSubtype;
import ru.yandex.chemodan.eventlog.events.fs.TemporaryFsEventGroup;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class SerializeFsGroupEventTest extends AbstractSerializeEventTest {
    private static final MpfsUid PERFORMER_UID = new MpfsUid(3L);

    private static final ResourceLocation DIR_ADDRESS = loc(MpfsAddress.parseDir(UID + ":/disk/dir/subdir/"));

    private static final ResourceLocation IMAGE1_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/disk/dir1/image.png"));

    private static final ResourceLocation IMAGE2_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/disk/dir2/image.png"));

    private static final ResourceLocation RENAMED_IMAGE1_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/disk/dir1/image_new.png"));

    private static final ResourceLocation TRASH_IMAGE1_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/trash/image.png"));

    private static final Resource IMAGE_RESOURCE = Resource.file("image", "123", UID);

    private static final Resource DIRECTORY_RESOURCE = Resource.directory("123", UID);

    private static ResourceLocation loc(MpfsAddress address) {
        return new ResourceLocation(address, Option.empty());
    }

    @Test
    public void testMkdir() {
        new ExpectedJson()
                .withUser(PERFORMER_UID)
                .withTarget(DIR_ADDRESS)
                .withResource(DIRECTORY_RESOURCE)
                .serializeAndCheck(
                        FsEvents.mkdir(METADATA, DIR_ADDRESS, DIRECTORY_RESOURCE)
                                .withPerformer(PERFORMER_UID)
                );
    }

    @Test
    public void testStore() {
        new ExpectedJson()
                .withGroupKey(
                        EventType.FS_STORE,
                        PERFORMER_UID,
                        "store",
                        "disk",
                        IMAGE1_ADDRESS.getParentAddressPath(),
                        TemporaryFsEventGroup.IMAGE)
                .withUser(PERFORMER_UID)
                .withTypeSubtype(new StoreTypeSubtype("store", "disk"))
                .withTarget(IMAGE1_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .serializeAndCheck(
                        FsEvents.store(METADATA, IMAGE1_ADDRESS, IMAGE_RESOURCE, "store", "disk")
                                .withPerformer(PERFORMER_UID)
                );
    }

    @Test
    public void testCopy() {
        new ExpectedJson()
                .withGroupKey(
                        EventType.FS_COPY,
                        PERFORMER_UID,
                        IMAGE2_ADDRESS.getParentAddressPath(),
                        TemporaryFsEventGroup.IMAGE)
                .withUser(PERFORMER_UID)
                .withTarget(IMAGE2_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .serializeAndCheck(
                        FsEvents.copy(METADATA, IMAGE1_ADDRESS, IMAGE2_ADDRESS, IMAGE_RESOURCE, false)
                                .withoutSource()
                                .withPerformer(PERFORMER_UID)
                );
    }

    @Test
    public void testSourceMove() {
        new ExpectedJson()
                .withUser(PERFORMER_UID)
                .withSource(IMAGE1_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .serializeAndCheck(
                        FsEvents.move(METADATA, IMAGE1_ADDRESS, IMAGE2_ADDRESS, IMAGE_RESOURCE, false)
                                .withoutTarget()
                                .withPerformer(PERFORMER_UID)
                );
    }

    @Test
    public void testTargetMove() {
        new ExpectedJson()
                .withUser(PERFORMER_UID)
                .withTarget(IMAGE2_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .serializeAndCheck(
                        FsEvents.move(METADATA, IMAGE1_ADDRESS, IMAGE2_ADDRESS, IMAGE_RESOURCE, false)
                                .withoutSource()
                                .withPerformer(PERFORMER_UID)
                );
    }

    @Test
    public void testRm() {
        new ExpectedJson()
                .withUser(PERFORMER_UID)
                .withSource(IMAGE1_ADDRESS)
                .withTarget(RENAMED_IMAGE1_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .serializeAndCheck(
                        FsEvents.move(METADATA, IMAGE1_ADDRESS, RENAMED_IMAGE1_ADDRESS, IMAGE_RESOURCE, false)
                                .withPerformer(PERFORMER_UID)
                );
    }

    @Test
    public void testTrashAppend() {
        new ExpectedJson()
                .withUser(PERFORMER_UID)
                .withSource(IMAGE1_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .serializeAndCheck(
                        FsEvents.trashAppend(METADATA, IMAGE1_ADDRESS, TRASH_IMAGE1_ADDRESS, IMAGE_RESOURCE)
                                .withoutTarget()
                                .withPerformer(PERFORMER_UID)
                );
    }

    @Test
    public void testTrashRestore() {
        new ExpectedJson()
                .withUser(PERFORMER_UID)
                .withTarget(IMAGE1_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .serializeAndCheck(
                        FsEvents.trashRestore(METADATA, TRASH_IMAGE1_ADDRESS, IMAGE1_ADDRESS,
                                IMAGE_RESOURCE, Option.empty(), Option.empty())
                                .withoutSource()
                                .withPerformer(PERFORMER_UID)
                );
    }

    @Test
    public void testSetPrivate() {
        new ExpectedJson()
                .withUser(PERFORMER_UID)
                .withTarget(IMAGE1_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .serializeAndCheck(
                        FsEvents.setPrivate(METADATA, IMAGE1_ADDRESS, IMAGE_RESOURCE).withPerformer(PERFORMER_UID)
                );
    }

    @Test
    public void testSetPublic() {
        new ExpectedJson()
                .withUser(PERFORMER_UID)
                .withTarget(IMAGE1_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .serializeAndCheck(
                        FsEvents.setPublic(METADATA, IMAGE1_ADDRESS, IMAGE_RESOURCE).withPerformer(PERFORMER_UID)
                );
    }
}
