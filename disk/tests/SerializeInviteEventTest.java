package ru.yandex.chemodan.app.eventloader.serializer.tests;

import org.junit.Test;

import ru.yandex.chemodan.eventlog.events.UniverseInvite;
import ru.yandex.chemodan.eventlog.events.invite.ActivationInviteEvent;
import ru.yandex.chemodan.eventlog.events.invite.SentInviteEvent;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class SerializeInviteEventTest extends AbstractSerializeEventTest {
    private static final UniverseInvite INVITE = new UniverseInvite("mpfs@yandex-team.ru", "email");

    private static final MpfsUid OWNER_UID = new MpfsUid(2L);

    @Test
    public void testSending() {
        new ExpectedJson()
                .withInvite(INVITE)
                .serializeAndCheck(new SentInviteEvent(METADATA, "mpfs", INVITE));
    }

    @Test
    public void testActivationWithoutOwner() {
        new ExpectedJson()
                .serializeAndCheck(new ActivationInviteEvent(METADATA, "mpfs"));
    }

    @Test
    public void testActivationWithOwner() {
        new ExpectedJson()
                .withOwner(OWNER_UID)
                .serializeAndCheck(new ActivationInviteEvent(METADATA, "mpfs").withOwner(OWNER_UID));
    }

    @Test
    public void testActivationForOwner() {
        new ExpectedJson()
                .withUser(UID)
                .forUid(OWNER_UID)
                .serializeAndCheck(new ActivationInviteEvent(METADATA, "mpfs").withOwner(OWNER_UID));
    }
}
