package ru.yandex.calendar.logic.event;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function0V;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.web.EventWebManager;
import ru.yandex.calendar.logic.event.web.EventWebUpdater;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class DismissedInvitationsTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventWebUpdater eventWebUpdater;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private SettingsRoutines settingsRoutines;

    @Test
    public void sendNoMailsToDismissedUsers() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(593);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(594);
        TestUserInfo dismissedAttendee = testManager.prepareRandomYaTeamUser(595);

        Function0V assertNoMailsForDismissed = () -> {
            Assert.notEmpty(mailSender.getEventMessageParameters());
            Assert.isEmpty(mailSender.findEventMailType(dismissedAttendee.getEmail()));
        };

        SettingsYt settings = new SettingsYt();
        settings.setIsDismissed(true);
        settingsRoutines.updateSettingsYtByUid(settings, dismissedAttendee.getUid());

        EventData eventData = testManager.createDefaultEventData(organizer.getUid(), "event with dismissed attendee");
        eventData.setInvData(Option.of(organizer.getEmail()), attendee.getEmail(), dismissedAttendee.getEmail());

        mailSender.clear();
        eventData.setEvent(eventWebManager.createUserEvent(organizer.getUid(), eventData,
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()).getEvent());
        assertNoMailsForDismissed.apply();

        Event event = eventData.getEvent().copy();
        event.setName("updated event");
        eventData.setEvent(event);

        mailSender.clear();
        eventWebUpdater.update(
                organizer.getUserInfo(), eventData, NotificationsData.notChanged(), true, ActionInfo.webTest());
        assertNoMailsForDismissed.apply();

        eventData.setInvData(Option.of(organizer.getEmail()));

        mailSender.clear();
        eventWebUpdater.update(
                organizer.getUserInfo(), eventData, NotificationsData.notChanged(), true, ActionInfo.webTest());
        assertNoMailsForDismissed.apply();
    }
}
