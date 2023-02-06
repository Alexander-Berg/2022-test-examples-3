package ru.yandex.chemodan.eventlog.log.tests;

import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.AbstractEvent;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.MpfsAddress;
import ru.yandex.chemodan.eventlog.events.Resource;
import ru.yandex.chemodan.eventlog.events.ResourceLocation;
import ru.yandex.chemodan.eventlog.events.fs.DeleteSubdirFsEvent;
import ru.yandex.chemodan.eventlog.events.fs.FsEvents;
import ru.yandex.chemodan.eventlog.events.fs.MksysdirFsEvent;
import ru.yandex.chemodan.eventlog.events.fs.TrashDropAllFsEvent;
import ru.yandex.chemodan.eventlog.log.TskvEventLogLine;
import ru.yandex.chemodan.mpfs.MpfsResourceId;
import ru.yandex.misc.test.Assert;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class ParseFsEventTest extends AbstractParseEventTest {

    private static final MpfsResourceId FOLDER_ID = new MpfsResourceId(UID, "/disk");

    private static final ResourceLocation DIR_ADDRESS = loc(MpfsAddress.parseDir(UID + ":/disk/dir/"));

    private static final ResourceLocation SOURCE_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/disk/source"));

    private static final ResourceLocation TARGET_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/disk/target"));

    private static final ResourceLocation TRASH_ADDRESS = loc(MpfsAddress.parseFile(UID + ":/trash/target"));

    private static final String TGT_FOLDER_ID_LOG = "\ttgt_folder_id=" + FOLDER_ID;

    private static final String SRC_FOLDER_ID_LOG = "\tsrc_folder_id=" + FOLDER_ID;

    private static ResourceLocation loc(MpfsAddress address) {
        return new ResourceLocation(address, Option.of(FOLDER_ID));
    }

    @Test
    public void testMksysdir() {
        assertParseEquals(
                UID, "fs-mksysdir", "type=fotki",
                new MksysdirFsEvent(EVENT_METADATA, "fotki")
        );
    }

    @Test
    public void testMkdir() {
        assertParseEquals(
                UID, "fs-mkdir",
                "tgt_rawaddress=4001210263:/disk/dir\tresource_type=dir\tresource_file_id=123\towner_uid=" + UID
                        + TGT_FOLDER_ID_LOG,
                FsEvents.mkdir(EVENT_METADATA, DIR_ADDRESS, Resource.directory("123", UID))
        );
    }

    @Test
    public void testStore() {
        assertParseEquals(
                UID, "fs-store",
                "type=store\tsubtype=disk\ttgt_rawaddress=4001210263:/disk/target\t"
                        + FILE_RESOURCE_LINE + TGT_FOLDER_ID_LOG,
                FsEvents.store(
                        EVENT_METADATA.subtractFromCommitTime(Duration.millis(1)),
                        TARGET_ADDRESS,
                        FILE_RESOURCE, "store", "disk")
        );
    }

    @Test
    public void testSocialImport() {
        assertParseEquals(
                UID, "fs-store",
                "type=social_copy\tsubtype=instagram_disk\ttgt_rawaddress=4001210263:/disk/target\t" +
                        FILE_RESOURCE_LINE + TGT_FOLDER_ID_LOG,
                FsEvents.socialImport(EVENT_METADATA, TARGET_ADDRESS, FILE_RESOURCE, "instagram_disk"),
                EventType.SOCIAL_IMPORT
        );
    }

    @Test
    public void testCopy() {
        testCopyBasic(FILE_RESOURCE, FILE_RESOURCE_LINE);
    }

    @Test
    public void testCopyOverwritten() {
        testCopyBasic(OVERWRITTEN_FILE_RESOURCE, OVERWRITTEN_FILE_RESOURCE_LINE);
    }

    private void testCopyBasic(Resource resource, String resourceLine) {
        assertParseEquals(
                UID, "fs-copy",
                "src_rawaddress=4001210263:/disk/source\ttgt_rawaddress=4001210263:/disk/target\tforce=False\t"
                        + resourceLine + TGT_FOLDER_ID_LOG + SRC_FOLDER_ID_LOG + TGT_FOLDER_ID_LOG,
                FsEvents.copy(EVENT_METADATA, SOURCE_ADDRESS, TARGET_ADDRESS, resource, false)
        );
    }

    @Test
    public void testMove() {
        String srcAddress = "4001210263:/disk/dir1/file.txt";
        String tgtAddress = "4001210263:/disk/dir2/file.txt";
        assertParseEquals(
                UID, "fs-move",
                "src_rawaddress=" + srcAddress + "\ttgt_rawaddress=" + tgtAddress + "\tforce=False\t"
                        + FILE_RESOURCE_LINE + SRC_FOLDER_ID_LOG + TGT_FOLDER_ID_LOG,
                FsEvents.move(EVENT_METADATA,
                        loc(MpfsAddress.parseFile(srcAddress)),
                        loc(MpfsAddress.parseFile(tgtAddress)),
                        FILE_RESOURCE,
                        false)
        );
    }

    @Test
    public void testRename() {
        String srcAddress = "4001210263:/disk/dir/old_name";
        String tgtAddress = "4001210263:/disk/dir/new_name";
        assertParseEquals(
                UID, "fs-move",
                "src_rawaddress=" + srcAddress + "\ttgt_rawaddress=" + tgtAddress + "\tforce=False\t"
                        + FILE_RESOURCE_LINE + SRC_FOLDER_ID_LOG + TGT_FOLDER_ID_LOG,
                FsEvents.move(EVENT_METADATA,
                        loc(MpfsAddress.parseFile(srcAddress)),
                        loc(MpfsAddress.parseFile(tgtAddress)),
                        FILE_RESOURCE,
                        false)
        );
    }

    @Test
    public void testRm() {
        assertParseEquals(
                UID, "fs-rm", "tgt_rawaddress=4001210263:/disk/target\t" + FILE_RESOURCE_LINE + TGT_FOLDER_ID_LOG,
                FsEvents.rm(EVENT_METADATA, TARGET_ADDRESS, FILE_RESOURCE)
        );
    }

    @Test
    public void testTrashAppend() {
        assertParseEquals(
                UID, "fs-trash-append",
                "src_rawaddress=4001210263:/disk/source\ttgt_rawaddress=4001210263:/trash/target\t"
                        + FILE_RESOURCE_LINE + SRC_FOLDER_ID_LOG + TGT_FOLDER_ID_LOG,
                FsEvents.trashAppend(EVENT_METADATA, SOURCE_ADDRESS, TRASH_ADDRESS, FILE_RESOURCE)
        );
    }

    @Test
    public void testTrashAppendGuestNotFiltered() {
        AbstractEvent abstractEvent = TskvEventLogLine.parse("unixtime=1511530486\tevent_type=fs-trash-append\t"
                + "req_id=web-8f63c46ef1a47e5ef7c9a4cd3ac52fa9-ufo04f\t"
                + "tskv_format=ydisk-event-history-log\tuser_uid=559649406\t"
                + "uid=331894720\tresource_file_id=ea9c503beb9774e72ad38d80d746e7a9527c50f4353ed7edf15e3453cf02698c\t"
                + "owner_uid=559649406\t"
                + "src_folder_id=331894720:d9c6996cb27cf8cf2c9a72b5404bcca29b585a09b153ec87f2d22a0d5fc57b09\t"
                + "lenta_media_type=document\tresource_media_type=document\t"
                + "src_rawaddress=331894720:/disk/Тесты/FA папка 3/lkjadrskldjhflajkdfncjfjhdgsbfchasgkcnfhasgfknjhagsdcknhagskdcfgaksfgkansfgkjasgndcfkjaghsdfnkcahgscfjkagsjknfagskjdfxgaksdjfhaksdjfh.txt\t"
                + "resource_type=file").toEvent().get();
        Assert.assertFalse(abstractEvent.reject());
    }

    @Test
    public void testTrashRestore() {
        assertParseEquals(
                UID, "fs-trash-restore",
                "type=trash_restore\tsubtype=disk\tsrc_rawaddress=4001210263:/trash/target\ttgt_rawaddress=4001210263:/disk/target\t"
                        + FILE_RESOURCE_LINE + SRC_FOLDER_ID_LOG + TGT_FOLDER_ID_LOG,
                FsEvents.trashRestore(EVENT_METADATA, TRASH_ADDRESS, TARGET_ADDRESS, FILE_RESOURCE,
                        TYPE_TRASH_RESTORE, SUBTYPE_DISK)
        );
    }

    @Test
    public void testTrashDrop() {
        assertParseEquals(
                UID, "fs-trash-drop", "tgt_rawaddress=4001210263:/trash/target\t"
                        + FILE_RESOURCE_LINE + TGT_FOLDER_ID_LOG,
                FsEvents.trashDrop(EVENT_METADATA, TRASH_ADDRESS, FILE_RESOURCE));
    }

    @Test
    public void testTrashDropAll() {
        assertParseEquals(
                UID, "fs-trash-drop-all", "owner=4001210263",
                new TrashDropAllFsEvent(EVENT_METADATA, UID));
    }

    @Test
    public void testDeleteSubdir() {
        assertParseEquals(
                UID, "fs-delete-subdir", "resource_id=" + FOLDER_ID,
                new DeleteSubdirFsEvent(EVENT_METADATA, FOLDER_ID));
    }

    @Test
    public void testSetPublic() {
        assertParseEquals(
                UID, "fs-set-public", "tgt_rawaddress=4001210263:/disk/target\t"
                        + FILE_RESOURCE_LINE + TGT_FOLDER_ID_LOG,
                FsEvents.setPublic(EVENT_METADATA, TARGET_ADDRESS, FILE_RESOURCE)
        );
    }

    @Test
    public void testSetPublicWithKeyAndUrl() {
        assertParseEquals(
                UID, "fs-set-public", "tgt_rawaddress=4001210263:/disk/target\t"
                        + PUBLIC_FILE_RESOURCE_LINE + TGT_FOLDER_ID_LOG,
                FsEvents.setPublic(EVENT_METADATA, TARGET_ADDRESS, PUBLIC_FILE_RESOURCE)
        );
    }

    @Test
    public void testSetPrivate() {
        assertParseEquals(
                UID, "fs-set-private", "tgt_rawaddress=4001210263:/disk/target\t"
                        + FILE_RESOURCE_LINE + TGT_FOLDER_ID_LOG,
                FsEvents.setPrivate(EVENT_METADATA, TARGET_ADDRESS, FILE_RESOURCE)
        );
    }

    @Test
    public void testLegacyAviaryRender() {
        assertSkipped(UID, "fs-aviary-render", "tgt_rawaddress=4001210263:/disk/target\t"
                + FILE_RESOURCE_LINE + TGT_FOLDER_ID_LOG);
    }

    @Test
    public void testAviaryRender() {
        assertParseEquals(
                UID, "fs-store",
                "type=social_copy\tsubtype=aviary_disk\ttgt_rawaddress=4001210263:/disk/target\t"
                        + FILE_RESOURCE_LINE + TGT_FOLDER_ID_LOG,
                FsEvents.aviaryRender(EVENT_METADATA, TARGET_ADDRESS, FILE_RESOURCE),
                EventType.FS_AVIARY_RENDER
        );
    }
}
