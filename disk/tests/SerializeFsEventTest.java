package ru.yandex.chemodan.app.eventloader.serializer.tests;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.MpfsAddress;
import ru.yandex.chemodan.eventlog.events.Resource;
import ru.yandex.chemodan.eventlog.events.ResourceLocation;
import ru.yandex.chemodan.eventlog.events.fs.FsEvents;
import ru.yandex.chemodan.eventlog.events.fs.MksysdirFsEvent;
import ru.yandex.chemodan.eventlog.events.fs.StoreTypeSubtype;
import ru.yandex.chemodan.eventlog.events.fs.TemporaryFsEventGroup;
import ru.yandex.chemodan.eventlog.events.fs.TrashDropAllFsEvent;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class SerializeFsEventTest extends AbstractSerializeEventTest {
    private static final MpfsUid UID2 = new MpfsUid(2L);

    private static final ResourceLocation FILE1_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/disk/dir/file.txt"));

    private static final ResourceLocation RENAMED_FILE1_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/disk/dir/renamed_file.txt"));

    private static final ResourceLocation TRASH_FILE1_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/trash/file.txt"));

    private static final ResourceLocation FILE2_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/disk/otherdir/file.txt"));

    private static final ResourceLocation DIR_ADDRESS = loc(MpfsAddress.parseDir(UID + ":/disk/dir/subdir/"));

    private static final ResourceLocation IMAGE_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/disk/dir/image.jpg"));

    private static final ResourceLocation FOREIGN_FILE_ADDRESS = loc(MpfsAddress.parseFile(UID2 + ":/disk/dir/file.txt"));

    private static final Resource DIRECTORY_RESOURCE = Resource.directory(
            "c4a194dde77a397a3c007fe46b6e014b8bb199b3abbe7c73e90b9e3fb8953f8c", UID);

    private static final Resource FILE_RESOURCE = Resource.file("text",
            "f9e57cec71b0eec7bfea167b37e7093eede04e607a98ceeaf01e7a4df5b7555f", UID);

    private static final Resource OVERWRITTEN_FILE_RESOURCE = FILE_RESOURCE.withOverwritten(false);

    private static final Resource PUBLIC_FILE_RESOURCE = FILE_RESOURCE.withKeyAndUrl("123", "https://dummy.ya.ru/123");

    private static final Resource IMAGE_RESOURCE = Resource.file("image",
            "15fb390c6edbcd473878a71aeb092b4e29bb8058dc3a4d5e977e43b52179c448", UID);

    private static ResourceLocation loc(MpfsAddress address) {
        return new ResourceLocation(address, Option.empty());
    }

    @Test
    public void testMkdirsysdir() {
        new ExpectedJson()
                .with("type", "fotki")
                .serializeAndCheck(new MksysdirFsEvent(METADATA, "fotki"));
    }

    @Test
    public void testMkdir() {
        new ExpectedJson()
                .withTarget(DIR_ADDRESS)
                .withResource(DIRECTORY_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(FsEvents.mkdir(METADATA, DIR_ADDRESS, DIRECTORY_RESOURCE));
    }

    @Test
    public void testStore() {
        createBaseStoreJson(EventType.FS_STORE, new StoreTypeSubtype("store", "disk"))
                .serializeAndCheck(
                        FsEvents.store(METADATA, IMAGE_ADDRESS, IMAGE_RESOURCE, "store", "disk")
                );
    }

    @Test
    public void testPhotostream() {
        createBaseStoreJson(EventType.FS_STORE_PHOTOSTREAM, new StoreTypeSubtype("store", "photostream"))
                .serializeAndCheck(
                        FsEvents.store(METADATA, IMAGE_ADDRESS, IMAGE_RESOURCE, "store", "photostream")
                );
    }

    @Test
    public void testSocialImport() {
        createBaseStoreJson(EventType.SOCIAL_IMPORT, new StoreTypeSubtype("social_copy", "vkontakte_disk"))
                .withProviderId("vkontakte")
                .serializeAndCheck(
                        FsEvents.store(METADATA, IMAGE_ADDRESS, IMAGE_RESOURCE, "social_copy", "vkontakte_disk")
                );
    }

    @Test
    public void testStoreUnknown() {
        new ExpectedJson()
                .withEventType(EventType.SKIP)
                .withTypeSubtype(new StoreTypeSubtype("unknown", "unknown"))
                .withTarget(IMAGE_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(
                        FsEvents.store(METADATA, IMAGE_ADDRESS, IMAGE_RESOURCE, "unknown", "unknown")
                );
    }

    private ExpectedJson createBaseStoreJson(EventType eventType, StoreTypeSubtype typeSubtype) {
        ListF<Object> groupKey = Cf.list(
                eventType,
                UID,
                typeSubtype.getType(),
                typeSubtype.getSubtype(),
                IMAGE_ADDRESS.getParentAddressPath(),
                TemporaryFsEventGroup.IMAGE);

        if (!eventType.isGroup()) {
            groupKey = groupKey.plus(IMAGE_ADDRESS.address.getName(), METADATA.getTimestampInSeconds());
        }

        return new ExpectedJson()
                .withEventType(eventType)
                .withGroupKey(groupKey.toArray())
                .withTarget(IMAGE_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .withPerformer(UID)
                .withTypeSubtype(typeSubtype);
    }

    @Test
    public void testCopy() {
        testCopyBasic(FILE_RESOURCE);
    }

    @Test
    public void testCopyOverwritten() {
        testCopyBasic(OVERWRITTEN_FILE_RESOURCE);
    }

    private void testCopyBasic(Resource resource) {
        new ExpectedJson()
                .withGroupKey(
                        EventType.FS_COPY,
                        UID,
                        FILE1_ADDRESS.getParentAddressPath(),
                        FILE2_ADDRESS.getParentAddressPath(),
                        TemporaryFsEventGroup.FILE)
                .withSource(FILE1_ADDRESS)
                .withTarget(FILE2_ADDRESS)
                .withResource(resource)
                .withPerformer(UID)
                .serializeAndCheck(
                        FsEvents.copy(METADATA, FILE1_ADDRESS, FILE2_ADDRESS, resource, false)
                );
    }

    @Test
    public void testCopyFromForeignSource() {
        new ExpectedJson()
                .withGroupKey(
                        EventType.FS_STORE_DOWNLOAD,
                        UID,
                        FOREIGN_FILE_ADDRESS.getParentAddressPath(),
                        FILE2_ADDRESS.getParentAddressPath(),
                        TemporaryFsEventGroup.FILE)
                .withTarget(FILE2_ADDRESS)
                .withResource(FILE_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(
                        FsEvents.copy(METADATA, FOREIGN_FILE_ADDRESS, FILE2_ADDRESS, FILE_RESOURCE, false)
                );
    }

    @Test
    public void testMove() {
        new ExpectedJson()
                .withSource(FILE1_ADDRESS)
                .withTarget(FILE2_ADDRESS)
                .withResource(FILE_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(
                        FsEvents.move(METADATA, FILE1_ADDRESS, FILE2_ADDRESS, FILE_RESOURCE, false)
                );
    }

    @Test
    public void testRename() {
        new ExpectedJson()
                .withSource(FILE1_ADDRESS)
                .withTarget(RENAMED_FILE1_ADDRESS)
                .withResource(FILE_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(FsEvents.move(METADATA, FILE1_ADDRESS, RENAMED_FILE1_ADDRESS, FILE_RESOURCE, false));
    }

    @Test
    public void testRm() {
        new ExpectedJson()
                .withGroupKey(EventType.FS_RM, UID, DIR_ADDRESS.getParentAddressPath(), TemporaryFsEventGroup.DIRECTORY)
                .withTarget(DIR_ADDRESS)
                .withResource(DIRECTORY_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(FsEvents.rm(METADATA, DIR_ADDRESS, DIRECTORY_RESOURCE));
    }

    @Test
    public void testTrashAppend() {
        new ExpectedJson()
                .withSource(FILE1_ADDRESS)
                .withTarget(TRASH_FILE1_ADDRESS)
                .withResource(FILE_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(FsEvents.trashAppend(METADATA, FILE1_ADDRESS, TRASH_FILE1_ADDRESS, FILE_RESOURCE));
    }

    @Test
    public void testTrashRestore() {
        new ExpectedJson()
                .withSource(TRASH_FILE1_ADDRESS)
                .withTarget(FILE1_ADDRESS)
                .withResource(FILE_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(FsEvents.trashRestore(METADATA, TRASH_FILE1_ADDRESS, FILE1_ADDRESS, FILE_RESOURCE,
                        Option.empty(), Option.empty()));
    }

    @Test
    public void testTrashDrop() {
        new ExpectedJson()
                .withTarget(FILE1_ADDRESS)
                .withResource(FILE_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(FsEvents.trashDrop(METADATA, FILE1_ADDRESS, FILE_RESOURCE));
    }

    @Test
    public void testTrashDropAll() {
        new ExpectedJson()
                .withOwner(new MpfsUid(1L))
                .serializeAndCheck(new TrashDropAllFsEvent(METADATA, UID));
    }

    @Test
    public void testSetPublic() {
        new ExpectedJson()
                .withTarget(DIR_ADDRESS)
                .withResource(DIRECTORY_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(FsEvents.setPublic(METADATA, DIR_ADDRESS, DIRECTORY_RESOURCE));
    }

    @Test
    public void testSetPublicWithKeyAndUrl() {
        new ExpectedJson()
                .withTarget(FILE1_ADDRESS)
                .withResource(PUBLIC_FILE_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(FsEvents.setPublic(METADATA, FILE1_ADDRESS, PUBLIC_FILE_RESOURCE));
    }

    @Test
    public void testSetPrivate() {
        new ExpectedJson()
                .withTarget(DIR_ADDRESS)
                .withResource(DIRECTORY_RESOURCE)
                .withPerformer(UID)
                .serializeAndCheck(FsEvents.setPrivate(METADATA, DIR_ADDRESS, DIRECTORY_RESOURCE));
    }

    @Test
    public void testAviaryRender() {
        createBaseStoreJson(EventType.FS_AVIARY_RENDER, new StoreTypeSubtype("social_copy", "aviary_disk"))
                .withProviderId("aviary")
                .serializeAndCheck(FsEvents.aviaryRender(METADATA, IMAGE_ADDRESS, IMAGE_RESOURCE));
    }
}
