package ru.yandex.chemodan.eventlog.log.tests;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.EventMetadata;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.MpfsAddress;
import ru.yandex.chemodan.eventlog.events.MpfsPath;
import ru.yandex.chemodan.eventlog.events.UniverseInvite;
import ru.yandex.chemodan.eventlog.events.sharing.ShareData;
import ru.yandex.chemodan.eventlog.events.sharing.ShareEvents;
import ru.yandex.chemodan.eventlog.events.sharing.ShareRights;
import ru.yandex.chemodan.eventlog.events.sharing.ShareRightsChange;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class ParseShareEventTest extends AbstractParseEventTest {
    private static final MpfsPath TARGET_PATH = MpfsPath.parseDir("/disk/target");

    private static final MpfsUid OWNER_UID = new MpfsUid(4001282897L);

    private static final MpfsUid USER_UID = new MpfsUid(4001197903L);

    private static final MpfsUid USER2_UID = new MpfsUid(4001197904L);

    private static final EventMetadata OWNER_EVENT_METADATA =
            new EventMetadata(OWNER_UID, new Instant(TIME * 1000), YANDEX_CLOUD_REQUEST_ID);

    private static final EventMetadata USER_EVENT_METADATA =
            new EventMetadata(USER_UID, new Instant(TIME * 1000), YANDEX_CLOUD_REQUEST_ID);

    private static final MpfsAddress OWNER_TARGET_ADDRESS = new MpfsAddress(OWNER_UID, TARGET_PATH);

    private static final MpfsAddress USER_TARGET_ADDRESS = new MpfsAddress(USER_UID, TARGET_PATH);

    private static final ShareData OWNER_SHARE_DATA =
            new ShareData(OWNER_EVENT_METADATA, OWNER_UID, "9bd4cad98040e171faf6addf08d97217", OWNER_TARGET_ADDRESS,
                    false, Option.empty());

    private static final ShareData USER_SHARE_DATA =
            new ShareData(USER_EVENT_METADATA, OWNER_UID, "9bd4cad98040e171faf6addf08d97217", USER_TARGET_ADDRESS,
                    false, Option.empty());

    private static final UniverseInvite UNIVERSE_INVITE = new UniverseInvite("mpfs@yandex-team.ru", "email");

    private static final String BASE_TSKV_LINE = "gid=" + OWNER_SHARE_DATA.groupId + "\t" +
            "owner_uid=" + OWNER_UID + "\t" +
            "path=/disk/target\t" +
            "is_invite=False";

    private static final String USER_TSKV_LINE = BASE_TSKV_LINE + "\tuser_uid=" + USER2_UID;


    @Test
    public void testCreateGroup() {
        assertParseEquals(
                OWNER_UID, "share-create-group", BASE_TSKV_LINE + "\tuid=" + OWNER_UID,
                ShareEvents.createGroup(OWNER_EVENT_METADATA, OWNER_SHARE_DATA)
        );
    }

    @Test
    public void testUnshareFolderForOwner() {
        assertParseEquals(
                OWNER_UID, "share-unshare-folder", BASE_TSKV_LINE + "\tuid=" + OWNER_UID,
                ShareEvents.unshareFolder(OWNER_EVENT_METADATA, OWNER_SHARE_DATA),
                EventType.SKIP
        );
    }

    @Test
    public void testUnshareFolderForUser() {
        assertParseEquals(
                OWNER_UID, "share-unshare-folder", BASE_TSKV_LINE + "\tuid=" + USER_UID,
                ShareEvents.unshareFolder(USER_EVENT_METADATA, USER_SHARE_DATA),
                EventType.SHARE_KICK_FROM_GROUP
        );
    }

    @Test
    public void testInviteUser() {
        assertParseEquals(
                OWNER_UID, "share-invite-user",
                BASE_TSKV_LINE + "\tuid" + OWNER_UID + "\trights=640\t" +
                        "user_universe_login=" + UNIVERSE_INVITE.getLogin() + "\t" +
                        "user_universe_service=" + UNIVERSE_INVITE.getService(),
                ShareEvents.inviteUser(OWNER_EVENT_METADATA, OWNER_SHARE_DATA, Either.right(UNIVERSE_INVITE),
                        ShareRights.READ_ONLY)
        );
    }

    @Test
    public void testRemoveInvite() {
        assertParseEquals(
                OWNER_UID, "share-remove-invite", USER_TSKV_LINE + "\tuid=" + OWNER_UID,
                ShareEvents.removeInvite(OWNER_EVENT_METADATA, OWNER_SHARE_DATA, Either.left(USER2_UID))
        );
    }

    @Test
    public void testChangeInviteRights() {
        assertParseEquals(
                OWNER_UID, "share-change-invite-rights",
                USER_TSKV_LINE + "\trights=640\tprev_rights=660\tuid=" + OWNER_UID,
                ShareEvents.changeInviteRights(OWNER_EVENT_METADATA, OWNER_SHARE_DATA, Either.left(USER2_UID),
                        new ShareRightsChange(ShareRights.READ_ONLY, ShareRights.WRITE)
                )
        );
    }

    @Test
    public void testChangeRights() {
        assertParseEquals(
                OWNER_UID, "share-change-rights",
                USER_TSKV_LINE + "\trights=640\tprev_rights=660\tuid=" + OWNER_UID,
                ShareEvents.changeRights(OWNER_EVENT_METADATA, OWNER_SHARE_DATA, USER2_UID,
                        new ShareRightsChange(ShareRights.READ_ONLY, ShareRights.WRITE))
        );
    }

    @Test
    public void testKickFromGroup() {
        assertParseEquals(
                OWNER_UID, "share-kick-from-group", USER_TSKV_LINE + "\tuid=" + OWNER_UID,
                ShareEvents.kickFromGroup(OWNER_EVENT_METADATA, OWNER_SHARE_DATA, USER2_UID)
        );
    }

    @Test
    public void testChangeGroupOwner() {
        assertParseEquals(
                OWNER_UID, "share-change-group-owner",
                USER_TSKV_LINE + "\tuid=" + OWNER_UID,
                ShareEvents.changeGroupOwner(OWNER_EVENT_METADATA, OWNER_SHARE_DATA, USER2_UID)
        );
    }

    @Test
    public void testActivateInvite() {
        assertParseEquals(
                USER_UID, "share-activate-invite", USER_TSKV_LINE + "\tuid=" + USER_UID + "\trights=660",
                ShareEvents.activateInvite(USER_EVENT_METADATA, USER_SHARE_DATA, USER2_UID, ShareRights.WRITE)
        );
    }

    @Test
    public void testRejectInvite() {
        assertParseEquals(
                USER_UID, "share-reject-invite", USER_TSKV_LINE + "\tuid=" + USER_UID,
                ShareEvents.rejectInvite(USER_EVENT_METADATA, USER_SHARE_DATA, USER2_UID)
        );
    }

    @Test
    public void testLeaveGroup() {
        assertParseEquals(
                USER_UID, "share-leave-group", USER_TSKV_LINE + "\tuid=" + USER_UID,
                ShareEvents.leaveGroup(USER_EVENT_METADATA, USER_SHARE_DATA, USER2_UID)
        );
    }
}
