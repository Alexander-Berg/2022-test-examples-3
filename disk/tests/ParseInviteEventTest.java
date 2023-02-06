package ru.yandex.chemodan.eventlog.log.tests;

import org.junit.Test;

import ru.yandex.chemodan.eventlog.events.UniverseInvite;
import ru.yandex.chemodan.eventlog.events.invite.ActivationInviteEvent;
import ru.yandex.chemodan.eventlog.events.invite.SentInviteEvent;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class ParseInviteEventTest extends AbstractParseEventTest {
    private static final UniverseInvite UNIVERSE_INVITE = new UniverseInvite("mpfs@yandex-team.ru", "email");

    @Test
    public void testSending() {
        assertParseEquals(
                UID, "invite-sent",
                "project=mpfs\t" +
                        "provider=" + UNIVERSE_INVITE.getService() + "\t" + "address=" + UNIVERSE_INVITE.getLogin(),
                new SentInviteEvent(EVENT_METADATA, "mpfs", UNIVERSE_INVITE)
        );
    }

    @Test
    public void testActivationWithNoneOwner() {
        assertParseEquals(
                UID, "invite-activation", "project=mpfs\towner_uid=None",
                new ActivationInviteEvent(EVENT_METADATA, "mpfs")
        );
    }

    @Test
    public void testActivationWithOwner() {
        assertParseEquals(
                UID, "invite-activation", "project=mpfs\towner_uid=1",
                new ActivationInviteEvent(EVENT_METADATA, "mpfs").withOwner(new MpfsUid(1L))
        );
    }
}
