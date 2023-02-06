package ru.yandex.calendar.logic.event.meeting;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.web.EventWebUpdater;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class MeetingPrimaryLayersTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private EventWebUpdater eventWebUpdater;

    private TestUserInfo creator;
    private TestUserInfo layerOwner;
    private TestUserInfo attendee;

    @Before
    public void prepare() {
        creator = testManager.prepareRandomYaTeamUser(144);
        layerOwner = testManager.prepareRandomYaTeamUser(145);
        attendee = testManager.prepareRandomYaTeamUser(146);

        LayerUser layerUser = new LayerUser();
        layerUser.setPerm(LayerActionClass.EDIT);

        Cf.list(layerOwner, attendee).map(TestUserInfo::getDefaultLayerId).forEach(layerId ->
                layerRoutines.createLayerUserForUserAndLayer(creator.getUid(), layerId, layerUser, Cf.list()));
    }

    @Test
    public void makeMeetingFromNotMeetingOnForeignLayerAndChangeOrganizerTwice() {
        EventData eventData = createEvent(Option.empty());
        long eventId = eventData.getEvent().getId();
        ListF<EventLayer> eventLayers = getEventLayers(eventId);
        Assert.hasSize(1, eventLayers);
        assertPrimaryLayer(eventLayers, layerOwner.getDefaultLayerId());

        updateInvData(eventData, Option.of(creator.getEmail()), attendee.getEmail());
        eventLayers = getEventLayers(eventId);
        Assert.hasSize(2, eventLayers);
        assertPrimaryLayer(eventLayers, layerOwner.getDefaultLayerId());

        updateInvData(eventData, Option.of(attendee.getEmail()), creator.getEmail());
        eventLayers = getEventLayers(eventId);
        Assert.hasSize(2, eventLayers);
        assertPrimaryLayer(eventLayers, layerOwner.getDefaultLayerId());
    }

    @Test
    public void changeMeetingOnOrganizerLayer() {
        EventData eventData = createEvent(Option.of(layerOwner.getEmail()), attendee.getEmail());
        long eventId = eventData.getEvent().getId();
        ListF<EventLayer> eventLayers = getEventLayers(eventId);
        Assert.hasSize(2, eventLayers);
        assertPrimaryLayer(eventLayers, layerOwner.getDefaultLayerId());

        updateInvData(eventData, Option.of(creator.getEmail()), attendee.getEmail());
        eventLayers = getEventLayers(eventId);
        Assert.hasSize(2, eventLayers);
        assertPrimaryLayer(eventLayers, creator.getDefaultLayerId());
    }

    @Test
    public void removeLayerOwnerFromMeeting() {
        EventData eventData = createEvent(Option.of(creator.getEmail()), layerOwner.getEmail());
        long eventId = eventData.getEvent().getId();
        ListF<EventLayer> eventLayers = getEventLayers(eventId);
        Assert.hasSize(1, eventLayers);
        assertPrimaryLayer(eventLayers, layerOwner.getDefaultLayerId());

        updateInvData(eventData, Option.of(creator.getEmail()), attendee.getEmail());
        eventLayers = getEventLayers(eventId);
        Assert.hasSize(2, eventLayers);
        assertPrimaryLayer(eventLayers, creator.getDefaultLayerId());
    }

    private ListF<EventLayer> getEventLayers(long eventId) {
        return eventLayerDao.findEventLayersByEventId(eventId);
    }

    private void updateInvData(EventData eventData, Option<Email> organizer, Email... attendees) {
        eventData.setInvData(organizer, attendees);
        eventWebUpdater.update(
                creator.getUserInfo(), eventData, NotificationsData.notChanged(), true, ActionInfo.webTest());
    }

    private void assertPrimaryLayer(ListF<EventLayer> eventLayers, long layerId) {
        Assert.equals(layerId, eventLayers.filter(EventLayer::getIsPrimaryInst).single().getLayerId());
    }

    private EventData createEventData(Option<Email> organizer, Email... attendees) {
        EventData eventData = testManager.createDefaultEventData(creator.getUid(), "Event on foreign layer");
        eventData.setLayerId(layerOwner.getDefaultLayerId());
        eventData.setInvData(organizer, attendees);
        return eventData;
    }

    private EventData createEvent(Option<Email> organizer, Email... attendees) {
        EventData eventData = createEventData(organizer, attendees);
        return createEvent(eventData);
    }

    private EventData createEvent(EventData eventData) {
        ActionInfo actionInfo = ActionInfo.webTest();

        CreateInfo created = eventRoutines.createUserOrFeedEvent(
                UidOrResourceId.user(creator.getUid()), EventType.USER,
                eventRoutines.createMainEvent(creator.getUid(), eventData, actionInfo), eventData,
                NotificationsData.createEmpty(),
                InvitationProcessingMode.SAVE_ATTACH_SEND, actionInfo);

        eventData.setEvent(created.getEvent());

        return eventData;
    }
}
