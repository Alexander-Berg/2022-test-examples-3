package ru.yandex.calendar.frontend.webNew;

import java.util.List;

import lombok.val;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.EventInvitation;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ExternalUserParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.UserParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.YandexUserParticipantInfo;
import ru.yandex.calendar.logic.user.SettingsInfo;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.PassportUids;
import ru.yandex.misc.email.Email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.calendar.frontend.webNew.WebNewUserManager.getTopParticipants;


public class WebNewUserManagerSmallTest {
    private static EventUser makeEventUser(long eventId, PassportUid uid) {
        val eu = new EventUser();
        eu.setEventId(eventId);
        eu.setUid(uid);
        eu.setDecision(Decision.YES);
        return eu;
    }

    private static Settings makeSettings(EventUser eventUser, String email) {
        val settings = new Settings();
        settings.setUid(eventUser.getUid());
        val emailObject = new Email(email);
        settings.setEmail(emailObject);
        settings.setUserLogin(emailObject.getLocalPart());
        return settings;
    }

    private static SettingsYt makeSettingsYt(EventUser eventUser) {
        val settingsYt = new SettingsYt();
        settingsYt.setUid(eventUser.getUid());
        settingsYt.setIsDismissed(false);
        return settingsYt;
    }

    private static UserParticipantInfo makeYandexUserParticipantInfo(EventUser eventUser, String email) {
        val settings = makeSettings(eventUser, email);
        val settingsYt = makeSettingsYt(eventUser);
        val settingsInfo = new SettingsInfo(settings, Option.of(settingsYt));
        return new YandexUserParticipantInfo(eventUser, settingsInfo);
    }

    private static UserParticipantInfo makeExternalUserParticipantInfo(String email) {
        val invitation = new EventInvitation();
        invitation.setEmail(new Email(email));
        invitation.setDecision(Decision.YES);
        return new ExternalUserParticipantInfo(invitation);
    }

    @Test
    public void topParticipantsAreInCorrectOrderGivenDomainAndExternalUsers() {
        val uidBase = PassportUids.FIRST_DOMAIN_MAIL;
        val actorUid = PassportUid.cons(uidBase);
        val uid1 = PassportUid.cons(uidBase + 1);
        val uid2 = PassportUid.cons(uidBase + 2);
        val uid3 = PassportUid.cons(uidBase + 3);
        val recentEventsParticipants = List.of(
                makeYandexUserParticipantInfo(makeEventUser(1L, uid3), "u3@abc.de"),
                makeYandexUserParticipantInfo(makeEventUser(1L, uid1), "u1@abc.de"),
                makeExternalUserParticipantInfo("u1@abc.de"),
                makeExternalUserParticipantInfo("u2@abc.de"),
                makeYandexUserParticipantInfo(makeEventUser(1L, uid2), "u2@abc.de"),
                makeYandexUserParticipantInfo(makeEventUser(2L, uid1), "u1@abc.de"),
                makeYandexUserParticipantInfo(makeEventUser(3L, uid1), "u1@abc.de")
        );
        val topParticipants = getTopParticipants(actorUid, 2, recentEventsParticipants.stream());
        assertThat(topParticipants).containsExactly(
                entry(new Email("u1@abc.de"), Option.of("u1")),
                entry(new Email("u2@abc.de"), Option.of("u2"))
        );
    }
}
