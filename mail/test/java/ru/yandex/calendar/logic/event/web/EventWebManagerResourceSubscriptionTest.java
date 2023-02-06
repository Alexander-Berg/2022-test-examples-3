package ru.yandex.calendar.logic.event.web;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.RoomsSubscribers;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.user.LoginOrEmail;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class EventWebManagerResourceSubscriptionTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private MailSenderMock mailSender;


    private Resource resource;
    private Email resourceEmail;

    private TestUserInfo user;
    private final Email subscriberEmail = new Email("subscriber@yandex-team.ru");

    @Before
    public void setup() {
        resource = testManager.cleanAndCreateResourceWithNoExchSync("resource_1", "Resource 1", ResourceType.ROOM);
        resourceEmail = ResourceRoutines.getResourceEmail(resource);

        user = testManager.prepareRandomYaTeamUser(12345);
        mailSender.clear();

        testManager.setValue(eventInvitationManager.resourceSubscribers,
                Cf.list(new RoomsSubscribers(Cf.list(resourceEmail.getLocalPart()), Cf.list(LoginOrEmail.email(subscriberEmail)))));
    }

    @Test
    public void add() {
        EventData data = testManager.createDefaultEventData(user.getUid(), "Event");

        eventWebManager.createUserEvent(user.getUid(), data,
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()).getEventId();

        data.setInvData(user.getEmail(), resourceEmail);

        eventWebManager.update(user.getUserInfo(), data, false, ActionInfo.webTest());

        Assert.some(MailType.EVENT_RESOURCE_ADDED, mailSender.findEventMailType(subscriberEmail));
    }

    @Test
    public void attend() {
        EventData data = testManager.createDefaultEventData(user.getUid(), "Event");

        data.setInvData(user.getEmail(), resourceEmail, subscriberEmail);

        eventWebManager.createUserEvent(user.getUid(), data,
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()).getEventId();

        Assert.some(MailType.EVENT_INVITATION, mailSender.findEventMailType(subscriberEmail));
    }

    @Test
    public void update() {
        EventData data = testManager.createDefaultEventData(user.getUid(), "Event");
        data.setInvData(user.getEmail(), resourceEmail);

        eventWebManager.createUserEvent(user.getUid(), data,
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()).getEventId();

        Assert.some(MailType.EVENT_RESOURCE_ADDED, mailSender.findEventMailType(subscriberEmail));
        mailSender.clear();

        data.getEvent().setStartTs(data.getEvent().getStartTs().plus(Duration.standardHours(1)));
        data.getEvent().setEndTs(data.getEvent().getEndTs().plus(Duration.standardHours(1)));

        eventWebManager.update(user.getUserInfo(), data, false, ActionInfo.webTest());

        Assert.some(MailType.EVENT_RESOURCE_UPDATED, mailSender.findEventMailType(subscriberEmail));
    }

    @Test
    public void remove() {
        EventData data = testManager.createDefaultEventData(user.getUid(), "Event");
        data.setInvData(user.getEmail(), resourceEmail);

        eventWebManager.createUserEvent(user.getUid(), data,
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()).getEventId();

        mailSender.clear();

        data.setInvData(user.getEmail());
        eventWebManager.update(user.getUserInfo(), data, false, ActionInfo.webTest());

        Assert.some(MailType.EVENT_RESOURCE_CANCELLED, mailSender.findEventMailType(subscriberEmail));
    }

    @Test
    public void delete() {
        EventData data = testManager.createDefaultEventData(user.getUid(), "Event");
        data.setInvData(user.getEmail(), resourceEmail);

        long eventId = eventWebManager.createUserEvent(user.getUid(), data,
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()).getEventId();

        mailSender.clear();

        eventWebManager.deleteEvent(user.getUserInfo(), eventId, Option.empty(), true, ActionInfo.webTest());

        Assert.some(MailType.EVENT_RESOURCE_CANCELLED, mailSender.findEventMailType(subscriberEmail));
    }
}
