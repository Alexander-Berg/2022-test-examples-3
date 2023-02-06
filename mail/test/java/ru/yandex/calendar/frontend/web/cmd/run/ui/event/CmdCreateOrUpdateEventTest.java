package ru.yandex.calendar.frontend.web.cmd.run.ui.event;

import org.jdom.Element;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.web.cmd.ctx.XmlCmdContext;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationsData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.model.EventUserData;
import ru.yandex.calendar.logic.event.web.EventWebManager;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.validation.Captcha;
import ru.yandex.misc.test.Assert;

/**
 * @author gutman
 */
public class CmdCreateOrUpdateEventTest extends AbstractConfTest {

    @Autowired
    private EventCmdManager eventCmdManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private TestStatusChecker testStatusChecker;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventLayerDao eventLayerDao;

    @Test
    public void create() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-12001");
        XmlCmdContext ctx = new XmlCmdContext(new Element("test"));
        EventData eventData = testManager.createDefaultEventData(creator.getUid(), "create");
        DateTimeZone tz = DateTimeZone.UTC;
        eventCmdManager.createEvent(
                ctx, eventData, Option.<Captcha.CaptchaData>empty(), tz, creator.getUid(), ActionInfo.webTest());
    }

    @Test
    public void update() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-12011");
        EventData eventData = testManager.createDefaultEventData(creator.getUid(), "create-update");

        CreateInfo info = createEvent(creator, eventData, ActionInfo.webTest());

        eventData.getEvent().setId(info.getEvent().getId());
        eventData.getEvent().setName("updated");
        eventWebManager.update(creator.getUserInfo(), eventData, true, ActionInfo.webTest());

        Event updated = eventDao.findEventById(info.getEvent().getId());
        Assert.A.equals("updated", updated.getName());
    }

    @Test
    public void updateByAttendee() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-12021");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-12022");

        EventData eventData = testManager.createDefaultEventData(creator.getUid(), "update-by-attendee");
        eventData.setInvData(new EventInvitationsData(Cf.list(attendee.getEmail())));

        ActionInfo actionInfo = ActionInfo.webTest();
        CreateInfo info = createEvent(creator, eventData, ActionInfo.webTest());

        long createdEventId = info.getEvent().getId();
        eventData.getEvent().setId(createdEventId);

        EventUser eventUser = new EventUser();
        eventUser.setDecision(Decision.NO);
        eventData.setEventUserData(new EventUserData(eventUser, Cf.list()));

        EventLayer attendeeEventLayer = eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(createdEventId, attendee.getUid()).get();
        eventData.setLayerId(attendeeEventLayer.getLayerId());

        eventWebManager.update(attendee.getUserInfo(), eventData, true, actionInfo);

        EventUser attendeeEventUser = eventUserDao.findEventUserByEventIdAndUid(createdEventId, attendee.getUid()).get();
        Assert.A.equals(Decision.NO, attendeeEventUser.getDecision());

        testStatusChecker.checkForAttendeeOnWebUpdateOrDelete(attendee.getUid(), info.getEvent(), actionInfo, false);
    }

    @Test
    public void updateByAttendeeOnlyProcessParticipationChanges() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-12031");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-12032");

        EventData eventData = testManager.createDefaultEventData(creator.getUid(), "update-by-attendee");
        eventData.setInvData(new EventInvitationsData(Cf.list(attendee.getEmail())));

        ActionInfo actionInfo = ActionInfo.webTest();
        CreateInfo info = createEvent(creator, eventData, actionInfo);

        long createdEventId = info.getEvent().getId();
        eventData.getEvent().setId(createdEventId);

        EventUser eventUser = new EventUser();
        eventUser.setDecision(Decision.NO);
        eventData.setEventUserData(new EventUserData(eventUser, Cf.<Notification>list()));

        EventLayer attendeeEventLayer = eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(createdEventId, attendee.getUid()).get();
        eventData.setLayerId(attendeeEventLayer.getLayerId());

        eventData.getEvent().setName("try-to-update-name");

        eventWebManager.update(attendee.getUserInfo(), eventData, true, actionInfo);

        Event updated = eventDao.findEventById(createdEventId);
        EventUser attendeeEventUser = eventUserDao.findEventUserByEventIdAndUid(updated.getId(), attendee.getUid()).get();

        Assert.A.equals(Decision.NO, attendeeEventUser.getDecision());
        Assert.A.equals("update-by-attendee", updated.getName());

        testStatusChecker.checkForAttendeeOnWebUpdateOrDelete(attendee.getUid(), info.getEvent(), actionInfo, false);
    }

    private CreateInfo createEvent(TestUserInfo creator, EventData eventData, ActionInfo actionInfo) {

        return eventRoutines.createUserOrFeedEvent(
                UidOrResourceId.user(creator.getUid()), EventType.USER,
                eventRoutines.createMainEvent(creator.getUid(), eventData, actionInfo), eventData,
                NotificationsData.createEmpty(),
                InvitationProcessingMode.SAVE_ATTACH, actionInfo);
    }

}
