package ru.yandex.calendar.logic.event.web;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventUserRoutines;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.model.WebReplyData;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class EventWebManagerNonAttendeeReplyTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventUserRoutines eventUserRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;

    private TestUserInfo organizer;
    private TestUserInfo attendee;
    private TestUserInfo actor;

    @Before
    public void setup() {
        organizer = testManager.prepareUser("yandex-team-mm-19200");
        attendee = testManager.prepareUser("yandex-team-mm-19201");
        actor = testManager.prepareUser("yandex-team-mm-19202");

        mailSender.clear();
    }

    @Test
    public void acceptEvent() {
        Event event = createUndecidedEvent();

        eventWebManager.handleEventInvitationDecision(actor.getUid(), event.getId(),
                new WebReplyData(Decision.YES), ActionInfo.webTest(event.getStartTs()));

        Assert.some(Decision.YES, eventUserRoutines.findEventUserDecision(actor.getUid(), event.getId()));
        Assert.some(false, eventUserRoutines.findEventUser(event.getId(), actor.getUid()).map(EventUser::getIsAttendee));

        Assert.isEmpty(mailSender.getEventMailTypes());
    }

    @Test
    public void declineEvent() {
        Event event = createUndecidedEvent();

        eventWebManager.handleEventInvitationDecision(actor.getUid(), event.getId(),
                new WebReplyData(Decision.NO), ActionInfo.webTest(event.getStartTs()));

        Assert.some(Decision.NO, eventUserRoutines.findEventUserDecision(actor.getUid(), event.getId()));
        Assert.some(false, eventUserRoutines.findEventUser(event.getId(), actor.getUid()).map(EventUser::getIsAttendee));

        Assert.none(eventLayerDao.findEventLayerByEventIdAndLayerId(event.getId(), actor.getDefaultLayerId()));

        Assert.isEmpty(mailSender.getEventMailTypes());
    }

    @Test
    public void acceptMeeting() {
        Event event = createUndecidedMeeting();

        eventWebManager.handleEventInvitationDecision(actor.getUid(), event.getId(),
                new WebReplyData(Decision.YES), ActionInfo.webTest(event.getStartTs()));

        Assert.some(Decision.YES, eventUserRoutines.findEventUserDecision(actor.getUid(), event.getId()));
        Assert.some(false, eventUserRoutines.findEventUser(event.getId(), actor.getUid()).map(EventUser::getIsAttendee));

        Assert.isEmpty(mailSender.getEventMailTypes());
    }

    @Test
    public void declineMeeting() {
        Event event = createUndecidedMeeting();

        eventWebManager.handleEventInvitationDecision(actor.getUid(), event.getId(),
                new WebReplyData(Decision.NO), ActionInfo.webTest(event.getStartTs()));

        Assert.some(Decision.NO, eventUserRoutines.findEventUserDecision(actor.getUid(), event.getId()));
        Assert.some(false, eventUserRoutines.findEventUser(event.getId(), actor.getUid()).map(EventUser::getIsAttendee));

        Assert.none(eventLayerDao.findEventLayerByEventIdAndLayerId(event.getId(), actor.getDefaultLayerId()));

        Assert.isEmpty(mailSender.getEventMailTypes());
    }


    private Event createUndecidedEvent() {
        Event eOverrides = new Event();
        EventUser euOverrides = new EventUser();
        euOverrides.setDecision(Decision.UNDECIDED);

        return testManager.createDefaultEventWithEventLayerAndEventUser(
                actor.getUid(), "Event", eOverrides, euOverrides);
    }

    private Event createUndecidedMeeting() {
        Event event = testManager.createDefaultEvent(organizer.getUid(), "Event");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.MAYBE, false);

        testManager.createEventLayer(actor.getDefaultLayerId(), event.getId());
        testManager.createEventUser(actor.getUid(), event.getId(), Decision.UNDECIDED, Option.empty());

        return event;
    }

}
