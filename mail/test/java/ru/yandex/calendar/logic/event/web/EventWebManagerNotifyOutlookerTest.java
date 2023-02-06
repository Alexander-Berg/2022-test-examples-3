package ru.yandex.calendar.logic.event.web;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function0V;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * CAL-6985
 *
 * @author dbrylev
 */
public class EventWebManagerNotifyOutlookerTest extends AbstractConfTest {

    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private MailSenderMock mailSender;


    private TestUserInfo outlooker;
    private TestUserInfo organizer;
    private TestUserInfo attendee;

    private final DateTime now = new DateTime(2015, 3, 17, 23, 30, MoscowTime.TZ);
    private final ActionInfo actionInfo = ActionInfo.webTest(now.toInstant());

    @Before
    public void cleanBeforeTest() {
        outlooker = testManager.prepareRandomYaTeamUser(12801);
        organizer = testManager.prepareRandomYaTeamUser(12802);
        attendee = testManager.prepareRandomYaTeamUser(12803);

        SettingsYt data = new SettingsYt();
        data.setIsOutlooker(true);
        settingsRoutines.saveEmptySettingsForUid(outlooker.getUid());
        settingsRoutines.updateSettingsYtByUid(data, outlooker.getUid());

        mailSender.clear();
    }

    @Test
    public void attachEvent() {
        long eventId = testManager.createDefaultEvent(organizer.getUid(), "For outlooker", now.toInstant()).getId();
        testManager.addUserParticipantToEvent(eventId, organizer.getUid(), Decision.YES, true);

        Function0V attach = () -> eventWebManager.attachEvent(
                outlooker.getUserInfo(), eventId, Option.empty(),
                new EventUser(), NotificationsData.create(Cf.list()), actionInfo);

        attach.apply();
        Assert.some(MailType.EVENT_INVITATION, mailSender.findEventMailType(outlooker.getEmail()));

        mailSender.clear();

        attach.apply();
        Assert.none(mailSender.findEventMailType(outlooker.getEmail()));
    }

    @Test
    public void attachUsingUpdate() {
        Event event = testManager.createDefaultEvent(organizer.getUid(), "For outlooker", now.toInstant());
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, false);

        EventData data = new EventData();
        data.setEvent(event.<Event>copy());

        Function0V update = () -> eventWebManager.update(outlooker.getUserInfo(), data, false, actionInfo);

        update.apply();
        Assert.some(MailType.EVENT_INVITATION, mailSender.findEventMailType(outlooker.getEmail()));

        mailSender.clear();

        update.apply();
        Assert.none(mailSender.findEventMailType(outlooker.getEmail()));
    }

    @Test
    public void updateAndCancelMeeting() {
        Event event = testManager.createDefaultEvent(organizer.getUid(), "For outlooker", now.toInstant());
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);
        testManager.addSubscriberToEvent(event.getId(), outlooker.getUid());

        EventData data = new EventData();
        data.setEvent(event.<Event>copy());
        data.setInvData(attendee.getEmail());
        data.getEvent().setStartTs(now.plusHours(1).toInstant());
        data.getEvent().setEndTs(now.plusHours(2).toInstant());

        eventWebManager.update(organizer.getUserInfo(), data, false, actionInfo);
        Assert.some(MailType.EVENT_UPDATE, mailSender.findEventMailType(outlooker.getEmail()));

        mailSender.clear();

        eventWebManager.deleteEvent(organizer.getUserInfo(), event.getId(), Option.empty(), false, actionInfo);
        Assert.some(MailType.EVENT_CANCEL, mailSender.findEventMailType(outlooker.getEmail()));
    }

    @Test
    public void updateOutlookerAttendee() {
        Event event = testManager.createDefaultEvent(organizer.getUid(), "For outlooker", now.toInstant());
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), outlooker.getUid(), Decision.UNDECIDED, false);

        EventData data = new EventData();
        data.setEvent(event.<Event>copy());
        data.setInvData(outlooker.getEmail());
        data.getEvent().setStartTs(now.plusHours(1).toInstant());
        data.getEvent().setEndTs(now.plusHours(2).toInstant());

        eventWebManager.update(organizer.getUserInfo(), data, false, actionInfo);

        Assert.some(MailType.EVENT_UPDATE, mailSender.findEventMailType(outlooker.getEmail()));
    }

    @Test
    public void eventWithNoInvitations() {
        EventData data = testManager.createDefaultEventData(outlooker.getUid(), "For outlooker", now.toInstant());
        CreateInfo createInfo = eventWebManager.createUserEvent(
                outlooker.getUid(), data, InvitationProcessingMode.SAVE_ATTACH_SEND, actionInfo);

        Assert.some(MailType.EVENT_INVITATION, mailSender.findEventMailType(outlooker.getEmail()));

        data.setEvent(createInfo.getEvent().copy());
        data.getEvent().setId(createInfo.getEventId());
        data.setExternalId(Option.empty());

        data.getEvent().setStartTs(now.plusHours(1).toInstant());
        data.getEvent().setEndTs(now.plusHours(2).toInstant());

        mailSender.clear();
        eventWebManager.update(outlooker.getUserInfo(), data, false, actionInfo);

        Assert.some(MailType.EVENT_UPDATE, mailSender.findEventMailType(outlooker.getEmail()));

        mailSender.clear();

        eventWebManager.deleteEvent(outlooker.getUserInfo(), createInfo.getEventId(), Option.empty(), false, actionInfo);
        Assert.some(MailType.EVENT_CANCEL, mailSender.findEventMailType(outlooker.getEmail()));
    }
}
