package ru.yandex.chemodan.eventlog.log.tests;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.chemodan.eventlog.events.EventMetadata;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.lenta.LentaTskvEventType;
import ru.yandex.chemodan.eventlog.events.lenta.actiontype.BlockCreatePinnedActionEventType;
import ru.yandex.chemodan.eventlog.events.lenta.actiontype.BlockTaskMergedActionEventType;
import ru.yandex.chemodan.eventlog.events.lenta.share.SharedFolderInviteEvent;
import ru.yandex.chemodan.eventlog.events.lenta.share.SharedFolderInviteEventType;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author bursy
 */
public class ParseSharedFolderInviteEventTest extends AbstractParseEventTest {
    private static final MpfsUid OWNER_UID = new MpfsUid(3001197905L);
    private static final MpfsUid INVITEE_UID = new MpfsUid(2001197904L);
    private static final String LENTA_BLOCK_ID = "000000150477432602804860000001504624621829";

    private static final EventMetadata INVITE_RECEIVED_EVENT_METADATA =
            new EventMetadata(INVITEE_UID, new Instant(TIME * 1000), YANDEX_CLOUD_REQUEST_ID);
    private static final EventMetadata INVITE_ACCEPTED_EVENT_METADATA =
            new EventMetadata(OWNER_UID, new Instant(TIME * 1000), YANDEX_CLOUD_REQUEST_ID);

    @Test
    public void testInviteReceived() {
        assertParseEquals(UID, LentaTskvEventType.BLOCK_CREATE_PINNED.value(), ""
                        + "action_event=" + BlockCreatePinnedActionEventType.SHARE_INVITE_USER.value() + "\t"
                        + "uid=" + INVITEE_UID + "\t"
                        + "block_id=" + LENTA_BLOCK_ID,

                new SharedFolderInviteEvent(
                        SharedFolderInviteEventType.RECEIVED, INVITE_RECEIVED_EVENT_METADATA, LENTA_BLOCK_ID),
                EventType.SHARE_INVITE_USER);
    }

    @Test
    public void testInviteAccepted() {
        checkInviteAccepted(LentaTskvEventType.BLOCK_TASK_MERGED);
        checkInviteAccepted(LentaTskvEventType.BLOCK_TASK_SCHEDULED);
        checkInviteAccepted(LentaTskvEventType.BLOCK_UPDATE_AND_UP);
    }

    private void checkInviteAccepted(LentaTskvEventType eventType) {
        assertParseEquals(UID, eventType.value(), ""
                        + "action_event=" + BlockTaskMergedActionEventType.SHARE_ACTIVATE_INVITE.value() + "\t"
                        + "uid=" + OWNER_UID + "\t"
                        + "block_id=" + LENTA_BLOCK_ID,

                new SharedFolderInviteEvent(
                        SharedFolderInviteEventType.ACCEPTED, INVITE_ACCEPTED_EVENT_METADATA, LENTA_BLOCK_ID),
                EventType.SHARE_ACTIVATE_INVITE);
    }
}
