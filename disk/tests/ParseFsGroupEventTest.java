package ru.yandex.chemodan.eventlog.log.tests;

import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.MpfsAddress;
import ru.yandex.chemodan.eventlog.events.Resource;
import ru.yandex.chemodan.eventlog.events.ResourceLocation;
import ru.yandex.chemodan.eventlog.events.fs.FsEvents;
import ru.yandex.chemodan.mpfs.MpfsResourceId;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class ParseFsGroupEventTest extends AbstractParseEventTest {

    private static final MpfsResourceId FOLDER_ID = new MpfsResourceId(UID, "/disk");

    private static final MpfsUid PERFORMER_UID = new MpfsUid(2L);

    private static final MpfsAddress DIR_ADDRESS = MpfsAddress.parseDir(UID + ":/disk/target");

    private static final MpfsAddress SOURCE_ADDRESS = MpfsAddress.parseFile(UID + ":/disk/source");

    private static final MpfsAddress TARGET_ADDRESS = MpfsAddress.parseFile(UID + ":/disk/target");

    private static final MpfsAddress TRASH_ADDRESS = MpfsAddress.parseFile(UID + ":/trash/target");

    private static final String PERFORMER_TSKV_LINE = "user_uid=" + PERFORMER_UID;

    private static final String FILE_TSKV_LINE = PERFORMER_TSKV_LINE + "\t" + FILE_RESOURCE_LINE;

    private static final Resource DIRECTORY_RESOURCE = Resource.directory("1", UID);

    private static final String TGT_FOLDER_ID_LOG = "\ttgt_folder_id=" + FOLDER_ID;

    private static final String SRC_FOLDER_ID_LOG = "\tsrc_folder_id=" + FOLDER_ID;

    private static ResourceLocation loc(MpfsAddress address) {
        return new ResourceLocation(address, Option.of(FOLDER_ID));
    }

    @Test
    public void testMkdir() {
        assertParseEquals(
                UID, "fs-mkdir",
                PERFORMER_TSKV_LINE + "\ttgt_rawaddress=" + DIR_ADDRESS + TGT_FOLDER_ID_LOG + "\t" +
                        "resource_type=dir\tresource_file_id=1\towner_uid=" + UID,
                FsEvents.mkdir(EVENT_METADATA, loc(DIR_ADDRESS), DIRECTORY_RESOURCE).withPerformer(PERFORMER_UID)
        );
    }

    @Test
    public void testStore() {
        assertParseEquals(
                UID, "fs-store", FILE_TSKV_LINE + "\ttype=store\tsubtype=disk"
                        + "\ttgt_rawaddress=" + TARGET_ADDRESS + TGT_FOLDER_ID_LOG,
                FsEvents.store(
                        EVENT_METADATA.subtractFromCommitTime(Duration.millis(1)),
                        loc(TARGET_ADDRESS), FILE_RESOURCE, "store", "disk")
                        .withPerformer(PERFORMER_UID)
        );
    }

    @Test
    public void testCopy() {
        assertParseEquals(
                UID, "fs-copy", FILE_TSKV_LINE + "\tforce=False\ttgt_rawaddress=" + TARGET_ADDRESS + TGT_FOLDER_ID_LOG,
                FsEvents.copy(EVENT_METADATA, loc(SOURCE_ADDRESS), loc(TARGET_ADDRESS), FILE_RESOURCE, false)
                        .withoutSource()
                        .withPerformer(PERFORMER_UID)
        );
    }

    @Test
    public void testSourceMove() {
        assertParseEquals(
                UID, "fs-move", FILE_TSKV_LINE + "\tforce=False\tsrc_rawaddress=" + SOURCE_ADDRESS + SRC_FOLDER_ID_LOG,
                FsEvents.move(EVENT_METADATA, loc(SOURCE_ADDRESS), loc(TARGET_ADDRESS), FILE_RESOURCE, false)
                        .withoutTarget()
                        .withPerformer(PERFORMER_UID)
        );
    }

    @Test
    public void testTargetMove() {
        assertParseEquals(
                UID, "fs-move", FILE_TSKV_LINE + "\tforce=False\ttgt_rawaddress=" + TARGET_ADDRESS + TGT_FOLDER_ID_LOG,
                FsEvents.move(EVENT_METADATA, loc(SOURCE_ADDRESS), loc(TARGET_ADDRESS), FILE_RESOURCE, false)
                        .withoutSource()
                        .withPerformer(PERFORMER_UID)
        );
    }

    @Test
    public void testRm() {
        assertParseEquals(
                UID, "fs-rm", FILE_TSKV_LINE + "\ttgt_rawaddress=" + TARGET_ADDRESS + TGT_FOLDER_ID_LOG,
                FsEvents.rm(EVENT_METADATA, loc(TARGET_ADDRESS), FILE_RESOURCE).withPerformer(PERFORMER_UID)
        );
    }

    @Test
    public void testTrashAppend() {
        assertParseEquals(
                UID, "fs-trash-append", FILE_TSKV_LINE + "\tsrc_rawaddress=" + SOURCE_ADDRESS + SRC_FOLDER_ID_LOG,
                FsEvents.trashAppend(EVENT_METADATA, loc(SOURCE_ADDRESS), loc(TRASH_ADDRESS), FILE_RESOURCE)
                        .withoutTarget()
                        .withPerformer(PERFORMER_UID)
        );
    }

    @Test
    public void testTrashRestore() {
        assertParseEquals(
                UID, "fs-trash-restore", FILE_TSKV_LINE + "\ttgt_rawaddress=" + TARGET_ADDRESS + TGT_FOLDER_ID_LOG,
                FsEvents.trashRestore(EVENT_METADATA, loc(TRASH_ADDRESS), loc(TARGET_ADDRESS), FILE_RESOURCE,
                        Option.empty(), Option.empty())
                        .withoutSource()
                        .withPerformer(PERFORMER_UID)
        );
    }

    @Test
    public void testSetPrivate() {
        assertParseEquals(
                UID, "fs-set-private", FILE_TSKV_LINE + "\ttgt_rawaddress=" + TARGET_ADDRESS + TGT_FOLDER_ID_LOG,
                FsEvents.setPrivate(EVENT_METADATA, loc(TARGET_ADDRESS), FILE_RESOURCE).withPerformer(PERFORMER_UID)
        );
    }

    @Test
    public void testSetPublic() {
        assertParseEquals(
                UID, "fs-set-public", FILE_TSKV_LINE + "\ttgt_rawaddress=" + TARGET_ADDRESS + TGT_FOLDER_ID_LOG,
                FsEvents.setPublic(EVENT_METADATA, loc(TARGET_ADDRESS), FILE_RESOURCE).withPerformer(PERFORMER_UID)
        );
    }
}
