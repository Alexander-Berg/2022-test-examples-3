package ru.yandex.calendar.logic.event.web;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class EventWebManagerPrimaryLayerTest extends AbstractConfTest {

    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private LayerRoutines layerRoutines;


    private final Instant startTs = MoscowTime.instant(2017, 10, 16, 20, 0);

    private TestUserInfo organizer;
    private TestUserInfo attendee;
    private TestUserInfo actor;

    @Before
    public void setup() {
        organizer = testManager.prepareRandomYaTeamUser(19200);
        attendee = testManager.prepareRandomYaTeamUser(19201);
        actor = testManager.prepareRandomYaTeamSuperUser(19202);

        layerRoutines.startNewSharing(actor.getUid(), organizer.getDefaultLayerId(), LayerActionClass.EDIT);
        layerRoutines.startNewSharing(actor.getUid(), attendee.getDefaultLayerId(), LayerActionClass.EDIT);
    }

    @Test
    public void eventFromMyLayer() {
        EventWithRelations event = createEvent(actor, createEventData(actor));

        assertIsPrimary(event, actor, Option.of(true));

        EventData update = createEventData(actor);
        update.setLayerId(organizer.getDefaultLayerId());

        event = updateEvent(actor, event, update);
        assertIsPrimary(event, actor, Option.empty(), organizer, Option.of(true));
    }

    @Test
    public void eventFromSomeoneLayer() {
        EventData data = createEventData(actor);
        data.setLayerId(organizer.getDefaultLayerId());

        EventWithRelations event = createEvent(actor, data);

        assertIsPrimary(event, organizer, Option.of(true), actor, Option.empty());

        EventData update = createEventData(actor);
        update.setLayerId(actor.getDefaultLayerId());

        event = updateEvent(actor, event, update);
        assertIsPrimary(event, organizer, Option.empty(), actor, Option.of(true));
    }

    @Test
    public void makeMeeting() {
        EventWithRelations event = createEvent(organizer, createEventData(organizer));

        EventData update = createEventData(actor);
        update.setInvData(attendee.getEmail());
        update.setLayerId(organizer.getDefaultLayerId());

        event = updateEvent(actor, event, update);

        assertIsPrimary(event, organizer, Option.of(true), attendee, Option.of(false), actor, Option.empty());
    }

    @Test
    public void createMeetingToMyLayer() {
        EventData data = createEventData(organizer, attendee);

        EventWithRelations event = createEvent(actor, data);

        assertIsPrimary(event, organizer, Option.of(true), attendee, Option.of(false), actor, Option.empty());
    }

    @Test
    public void createMeetingToMyLayerByAttendee() {
        EventData data = createEventData(organizer, attendee);

        EventWithRelations event = createEvent(attendee, data);

        assertIsPrimary(event, organizer, Option.of(true), attendee, Option.of(false));
    }

    @Test
    public void createMeetingToOrganizerLayer() {
        EventData data = createEventData(organizer, attendee);
        data.setLayerId(organizer.getDefaultLayerId());

        EventWithRelations event = createEvent(actor, data);

        assertIsPrimary(event, organizer, Option.of(true), attendee, Option.of(false), actor, Option.empty());
    }

    @Test
    public void createMeetingToAttendeeLayer() {
        EventData data = createEventData(organizer, attendee);
        data.setLayerId(attendee.getDefaultLayerId());

        EventWithRelations event = createEvent(actor, data);

        assertIsPrimary(event, organizer, Option.of(true), attendee, Option.of(false), actor, Option.empty());
    }


    private EventData createEventData(TestUserInfo organizer, TestUserInfo... attendees) {
        return createEventData("Single", startTs, organizer, attendees);
    }

    private EventData createEventData(String name, Instant startTs, TestUserInfo organizer, TestUserInfo... attendees) {
        EventData data = new EventData();
        data.setEvent(TestManager.createDefaultEventTemplate(organizer.getUid(), name, Option.of(startTs)));

        if (attendees.length > 0) {
            data.setInvData(Option.of(organizer.getEmail()), Cf.x(attendees).map(TestUserInfo::getEmail).toArray(Email.class));
        }
        return data;
    }

    private EventWithRelations createEvent(TestUserInfo actor, EventData eventData) {
        eventData.setTimeZone(MoscowTime.TZ);

        Event event = eventWebManager.createUserEvent(
                actor.getUid(), eventData, InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest(startTs)).getEvent();

        return eventDbManager.getEventWithRelationsByEvent(event);
    }

    private EventWithRelations updateEvent(TestUserInfo actor, EventWithRelations event, EventData updateData) {
        updateData.getEvent().setId(event.getId());
        updateData.setExternalId(Option.empty());

        return eventDbManager.getEventWithRelationsById(
                eventWebManager.update(actor.getUserInfo(), updateData, false, ActionInfo.webTest(startTs))
                        .getOrElse(event.getId()));
    }

    private static void assertIsPrimary(
            EventWithRelations event, TestUserInfo user1, Option<Boolean> primary1, Object... andSoOn)
    {
        Tuple2List.fromPairs(user1, primary1).plus(Tuple2List.fromPairs(andSoOn)).forEach((u, p) -> Assert.equals(
                p, event.findOwnEventLayer(u.getUid()).map(EventLayer::getIsPrimaryInst),
                "For user " + u.getUid() + " and event " + event.getId()));
    }

}
