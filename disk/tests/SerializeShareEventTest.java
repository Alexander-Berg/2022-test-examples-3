package ru.yandex.chemodan.app.eventloader.serializer.tests;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.eventlog.events.EventMetadata;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.MpfsAddress;
import ru.yandex.chemodan.eventlog.events.UniverseInvite;
import ru.yandex.chemodan.eventlog.events.YandexCloudRequestId;
import ru.yandex.chemodan.eventlog.events.sharing.ShareData;
import ru.yandex.chemodan.eventlog.events.sharing.ShareEvents;
import ru.yandex.chemodan.eventlog.events.sharing.ShareRights;
import ru.yandex.chemodan.eventlog.events.sharing.ShareRightsChange;
import ru.yandex.chemodan.eventlog.events.sharing.ShareUserType;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class SerializeShareEventTest extends AbstractSerializeEventTest {
    private static final MpfsUid USER_UID = new MpfsUid(1L);

    private static final MpfsUid OWNER_UID = new MpfsUid(2L);

    private static final EventMetadata USER_METADATA = new EventMetadata(
            UID, Instant.now(), YandexCloudRequestId.parse("mac-1234567-localhost"));

    private static final EventMetadata OWNER_METADATA = new EventMetadata(
            OWNER_UID, Instant.now(), YandexCloudRequestId.parse("mac-1234567-localhost"));

    private static final ShareData OWNER_SHARE_DATA =
            new ShareData(OWNER_METADATA, OWNER_UID, "9bd4cad98040e171faf6addf08d97217",
                    MpfsAddress.parseFile(OWNER_UID + ":/disk/share"), false, Option.empty());

    private static final ShareData USER_SHARE_DATA =
            new ShareData(USER_METADATA, OWNER_UID, "9bd4cad98040e171faf6addf08d97217",
                    MpfsAddress.parseDir(USER_UID + ":/disk/dir/share/"), false, Option.empty());

    private static final ShareData INVITEE_SHARE_DATA =
            new ShareData(USER_METADATA, OWNER_UID, "9bd4cad98040e171faf6addf08d97217",
                    MpfsAddress.parseDir(USER_UID + ":/disk/dir/share/"), true, Option.empty());

    private static final ShareRightsChange SHARE_RIGHTS_CHANGE =
            new ShareRightsChange(ShareRights.READ_ONLY, ShareRights.WRITE);

    private static final UniverseInvite INVITE = new UniverseInvite("mpfs@yandex-team.ru", "email");

    @Test
    public void testCreateGroup() {
        new ExpectedJson()
                .withShareData(OWNER_SHARE_DATA)
                .serializeAndCheck(ShareEvents.createGroup(OWNER_METADATA, OWNER_SHARE_DATA));
    }

    @Test
    public void testUnshareFolderUser() {
        new ExpectedJson()
                .withEventType(EventType.SHARE_UNSHARE_FOLDER_USER)
                .withPerformer(OWNER_UID)
                .withUser(USER_UID)
                .withUserType(ShareUserType.SUBSCRIBER)
                .withShareData(OWNER_SHARE_DATA)
                .serializeAndCheck(ShareEvents.unshareFolderUser(OWNER_METADATA, OWNER_SHARE_DATA, USER_UID));
    }

    @Test
    public void testUnshareFolderInvitee() {
        new ExpectedJson()
                .withEventType(EventType.SHARE_UNSHARE_FOLDER_USER)
                .withPerformer(OWNER_UID)
                .withUser(USER_UID)
                .withUserType(ShareUserType.INVITEE)
                .withShareData(OWNER_SHARE_DATA)
                .serializeAndCheck(ShareEvents.unshareFolderInvitee(OWNER_METADATA, OWNER_SHARE_DATA, USER_UID));
    }

    @Test
    public void testUnshareFolderForOwner() {
        new ExpectedJson()
                .withEventType(EventType.SKIP)
                .withPerformer(OWNER_UID)
                .withShareData(OWNER_SHARE_DATA)
                .serializeAndCheck(ShareEvents.unshareFolder(OWNER_METADATA, OWNER_SHARE_DATA));
    }

    @Test
    public void testUnshareFolderForUser() {
        new ExpectedJson()
                .withEventType(EventType.SHARE_KICK_FROM_GROUP)
                .withPerformer(USER_UID)
                .withShareData(USER_SHARE_DATA)
                .serializeAndCheck(ShareEvents.unshareFolder(USER_METADATA, USER_SHARE_DATA));
    }

    @Test
    public void testUnshareFolderForInvitee() {
        new ExpectedJson()
                .withEventType(EventType.SHARE_REMOVE_INVITE)
                .withPerformer(USER_UID)
                .withShareData(INVITEE_SHARE_DATA)
                .serializeAndCheck(ShareEvents.unshareFolder(USER_METADATA, INVITEE_SHARE_DATA));
    }

    @Test
    public void testInviteUser() {
        new ExpectedJson()
                .withUser(USER_UID)
                .withUserType(ShareUserType.INVITEE)
                .withShareData(OWNER_SHARE_DATA)
                .withShareRights("rights", ShareRights.READ_ONLY)
                .serializeAndCheck(
                        ShareEvents.inviteUser(OWNER_METADATA, OWNER_SHARE_DATA, Either.left(USER_UID),
                                ShareRights.READ_ONLY)
                );
    }

    @Test
    public void testRemoveInvite() {
        new ExpectedJson()
                .withUserType(ShareUserType.INVITEE)
                .withShareData(OWNER_SHARE_DATA)
                .withInvite(INVITE)
                .serializeAndCheck(ShareEvents.removeInvite(OWNER_METADATA, OWNER_SHARE_DATA, Either.right(INVITE)));
    }

    @Test
    public void testChangeInviteRights() {
        new ExpectedJson()
                .withUser(USER_UID)
                .withUserType(ShareUserType.INVITEE)
                .withShareData(OWNER_SHARE_DATA)
                .withShareRightsChange(SHARE_RIGHTS_CHANGE)
                .serializeAndCheck(
                        ShareEvents.changeInviteRights(OWNER_METADATA, OWNER_SHARE_DATA, Either.left(USER_UID),
                                SHARE_RIGHTS_CHANGE)
                );
    }

    @Test
    public void testChangeRights() {
        new ExpectedJson()
                .withUser(USER_UID)
                .withUserType(ShareUserType.SUBSCRIBER)
                .withShareData(OWNER_SHARE_DATA)
                .withShareRightsChange(SHARE_RIGHTS_CHANGE)
                .serializeAndCheck(
                        ShareEvents.changeRights(OWNER_METADATA, OWNER_SHARE_DATA, USER_UID, SHARE_RIGHTS_CHANGE)
                );
    }

    @Test
    public void testKickFromGroup() {
        new ExpectedJson()
                .withUser(USER_UID)
                .withUserType(ShareUserType.SUBSCRIBER)
                .withShareData(OWNER_SHARE_DATA)
                .serializeAndCheck(ShareEvents.kickFromGroup(OWNER_METADATA, OWNER_SHARE_DATA, USER_UID));
    }

    @Test
    public void testChangeGroupOwner() {
        new ExpectedJson()
                .withUser(USER_UID)
                .withShareData(OWNER_SHARE_DATA)
                .serializeAndCheck(ShareEvents.changeGroupOwner(OWNER_METADATA, OWNER_SHARE_DATA, USER_UID));
    }

    @Test
    public void testActivateInvite() {
        new ExpectedJson()
                .withUser(USER_UID)
                .withUserType(ShareUserType.INVITEE)
                .withShareData(USER_SHARE_DATA)
                .withShareRights("rights", ShareRights.READ_ONLY)
                .serializeAndCheck(
                        ShareEvents.activateInvite(USER_METADATA, USER_SHARE_DATA, USER_UID, ShareRights.READ_ONLY)
                );
    }

    @Test
    public void testRejectInvite() {
        new ExpectedJson()
                .withUser(USER_UID)
                .withUserType(ShareUserType.INVITEE)
                .withShareData(USER_SHARE_DATA)
                .serializeAndCheck(ShareEvents.rejectInvite(USER_METADATA, USER_SHARE_DATA, USER_UID));
    }

    @Test
    public void testLeaveGroup() {
        new ExpectedJson()
                .withUser(USER_UID)
                .withUserType(ShareUserType.SUBSCRIBER)
                .withShareData(USER_SHARE_DATA)
                .serializeAndCheck(ShareEvents.leaveGroup(USER_METADATA, USER_SHARE_DATA, USER_UID));
    }
}
