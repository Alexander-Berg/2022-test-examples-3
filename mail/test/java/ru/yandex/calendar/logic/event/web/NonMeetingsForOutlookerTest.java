package ru.yandex.calendar.logic.event.web;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class NonMeetingsForOutlookerTest extends AbstractConfTest {

    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventWebUpdater eventWebUpdater;
    @Autowired
    private TestManager testManager;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private LayerRoutines layerRoutines;

    private TestUserInfo outlooker;
    private TestUserInfo nonOutlooker;

    @Before
    public void setup() {
        outlooker = testManager.prepareRandomYaTeamUser(12801);
        nonOutlooker = testManager.prepareRandomYaTeamUser(12802);

        SettingsYt data = new SettingsYt();
        data.setIsOutlooker(true);
        settingsRoutines.saveEmptySettingsForUid(outlooker.getUid());
        settingsRoutines.updateSettingsYtByUid(data, outlooker.getUid());

        layerRoutines.startNewSharing(outlooker.getUid(), nonOutlooker.getDefaultLayerId(), LayerActionClass.EDIT);
        layerRoutines.startNewSharing(nonOutlooker.getUid(), outlooker.getDefaultLayerId(), LayerActionClass.EDIT);

        mailSender.clear();
    }

    @Test
    public void onForeignLayerByOutlooker() {
        createAndUpdateNonMeeting(outlooker, nonOutlooker, false);
    }

    @Test
    public void onOwnLayerByOutlooker() {
        createAndUpdateNonMeeting(outlooker, outlooker, true);
    }

    @Test
    public void onOutlookerForeignLayer() {
        createAndUpdateNonMeeting(nonOutlooker, outlooker, true);
    }

    @Test
    public void onOwnLayerForNonOutlooker() {
        createAndUpdateNonMeeting(nonOutlooker, nonOutlooker, false);
    }

    @Test
    public void onOwnLayerWithAnotherOrganizer() {
        createEvent(nonOutlooker.getUid(), nonOutlooker.getDefaultLayerId(), Option.of(outlooker.getEmail()));
        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_INVITATION, mailSender.findEventMailType(outlooker.getEmail()));
    }

    @Test
    public void onDismissedOutlookerForeignLayer() {
        SettingsYt settings = new SettingsYt();
        settings.setIsDismissed(true);
        settingsRoutines.updateSettingsYtByUid(settings, outlooker.getUid());

        long layerId = outlooker.getDefaultLayerId();

        EventData eventData = createEvent(nonOutlooker.getUid(), layerId, Option.empty());

        Assert.hasSize(0, mailSender.getEventMessageParameters());

        mailSender.clear();
        updateEventName(nonOutlooker.getUserInfo(), eventData, "updatedEvent");

        Assert.hasSize(0, mailSender.getEventMessageParameters());
    }

    private void createAndUpdateNonMeeting(TestUserInfo actor, TestUserInfo layerOwner, boolean shouldReceiveMail) {
        long layerId = layerOwner.getDefaultLayerId();

        EventData eventData = createEvent(actor.getUid(), layerId, Option.empty());

        if (shouldReceiveMail) {
            Assert.hasSize(1, mailSender.getEventMessageParameters());
            Assert.some(MailType.EVENT_INVITATION, mailSender.findEventMailType(outlooker.getEmail()));
        } else {
            Assert.hasSize(0, mailSender.getEventMessageParameters());
        }

        mailSender.clear();
        updateEventName(actor.getUserInfo(), eventData, "updatedEvent");

        if (shouldReceiveMail) {
            Assert.hasSize(1, mailSender.getEventMessageParameters());
            Assert.some(MailType.EVENT_UPDATE, mailSender.findEventMailType(outlooker.getEmail()));
        } else {
            Assert.hasSize(0, mailSender.getEventMessageParameters());
        }
    }

    private EventData createEvent(PassportUid creator, long layerId, Option<Email> organizer, Email... attendees) {
        EventData eventData = testManager.createDefaultEventData(creator, "event");
        eventData.setLayerId(layerId);
        eventData.setInvData(organizer, attendees);

        eventData.setEvent(eventWebManager.createUserEvent(
                creator, eventData, InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()).getEvent());

        return eventData;
    }

    private void updateEventName(UserInfo user, EventData eventData, String newName) {
        Event event = eventData.getEvent().copy();
        event.setName(newName);
        eventData.setEvent(event);
        eventWebUpdater.update(user, eventData, NotificationsData.notChanged(), true, ActionInfo.webTest());
    }

}
