package ru.yandex.calendar.frontend.webNew;

import java.util.Optional;
import java.util.function.Supplier;

import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.CalendarRequest;
import ru.yandex.calendar.frontend.bender.FilterablePojo;
import ru.yandex.calendar.frontend.bender.WebDate;
import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.frontend.web.cmd.run.PermissionDeniedUserException;
import ru.yandex.calendar.frontend.web.cmd.run.Situation;
import ru.yandex.calendar.frontend.webNew.actions.LayerActions;
import ru.yandex.calendar.frontend.webNew.dto.in.ImportIcsData;
import ru.yandex.calendar.frontend.webNew.dto.in.LayerData;
import ru.yandex.calendar.frontend.webNew.dto.in.ReplyData;
import ru.yandex.calendar.frontend.webNew.dto.in.WebEventData;
import ru.yandex.calendar.frontend.webNew.dto.in.WebEventUserData;
import ru.yandex.calendar.frontend.webNew.dto.inOut.RepetitionData;
import ru.yandex.calendar.frontend.webNew.dto.out.EventsInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.ExdateBrief;
import ru.yandex.calendar.frontend.webNew.dto.out.LayerInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.ModifiedEventIdsInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.WebEventInfo;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventUserRoutines;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.event.web.IdOrExternalId;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.commune.a3.action.parameter.IllegalParameterException;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class WebNewEventManagerTest extends WebNewEventManagerTestBase {
    @Autowired
    private LayerActions layerActions;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventUserRoutines eventUserRoutines;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private WebNewEventManager webNewEventManager;

    @Test
    public void getAttendees() {
        val users = getUsers();
        val eventId = createEvent(users, Cf.list());
        val attendees = eventActions.getAttendees(uid, eventId, Option.empty()).getAttendees();
        validateAttendees(users, attendees);
    }

    @Test
    public void getUserEventsWithAllAttendees() {
        val allUsers = getUsers();
        val requiredUsers = allUsers.subList(0, allUsers.size() / 2);
        val optionalUsers = allUsers.subList(allUsers.size() / 2, allUsers.size());
        val eventId = createEvent(requiredUsers, optionalUsers);

        val userEvents = eventActions.getUserEvents(Option.empty(), Option.of(uid), Option.of(uid),
                WebDate.dateTime(WebDateTime.dateTime(NOW.minusHours(1))),
                WebDate.dateTime(WebDateTime.dateTime(NOW.plusHours(1))), Option.empty(), Option.empty(), Option.empty());
        val matchEvents = userEvents.getEvents().filter(x -> x.getId() == eventId);
        assertThat(matchEvents).hasSize(1);
        val event = matchEvents.first();
        validateAttendees(requiredUsers, event.getAttendees());
        validateAttendees(optionalUsers, event.getOptionalAttendees());
        assertThat(event.getTotalAttendees()).isEqualTo(requiredUsers.size());
        assertThat(event.getTotalOptionalAttendees()).isEqualTo(optionalUsers.size());
    }

    @Test
    public void getUserEventsLimitAttendees() {
        val allUsers = getUsers();
        val requiredUsers = allUsers.subList(0, allUsers.size() / 2);
        val optionalUsers = allUsers.subList(allUsers.size() / 2, allUsers.size());
        val eventId = createEvent(requiredUsers, optionalUsers);

        val userEvents = eventActions.getUserEvents(Option.empty(), Option.of(uid), Option.of(uid),
                WebDate.dateTime(WebDateTime.dateTime(NOW.minusHours(1))),
                WebDate.dateTime(WebDateTime.dateTime(NOW.plusHours(1))), Option.empty(), Option.of(true), Option.empty());
        val matchEvents = userEvents.getEvents().filter(x -> x.getId() == eventId);
        assertThat(matchEvents).hasSize(1);
        val event = matchEvents.first();
        assertThat(event.getTotalAttendees()).isEqualTo(requiredUsers.size());
        assertThat(event.getTotalOptionalAttendees()).isEqualTo(optionalUsers.size());
        assertThat(event.getAttendees()).hasSize(WebNewEventManager.ATTENDEE_LIMIT);
        assertThat(event.getOptionalAttendees()).hasSize(WebNewEventManager.ATTENDEE_LIMIT);

        val requiredUserEmails = getTestUserEmails(requiredUsers);
        val requiredParticipantEmails = getParticipantEmails(event.getAttendees());
        requiredParticipantEmails.removeAll(requiredUserEmails);
        assertThat(requiredParticipantEmails).isEmpty();

        val optionalUserEmails = getTestUserEmails(optionalUsers);
        val optionalParticipantEmails = getParticipantEmails(event.getOptionalAttendees());
        optionalParticipantEmails.removeAll(optionalUserEmails);
        assertThat(optionalParticipantEmails).isEmpty();
    }

    @Test
    public void getEventsOfTargetUserActorCannotView() {
        val events = createTestEventForGetEvents(user, user2);
        assertThat(events).hasSize(0);
    }

    @Test
    public void getEventsOfTargetUserActorCanView() {
        val su = testManager.prepareRandomYaTeamSuperUser(4321);
        val events = createTestEventForGetEvents(su, user2);
        assertThat(events).hasSize(1);
    }

    @Test
    public void createEventSingle() {
        val data = consBaseData();

        long eventId = createEvent(uid, data).asModified().getShowEventId();
        WebEventInfo event = getEvent(uid, eventId);

        assertThat(event.getName()).isEqualTo("Без названия");
        assertThat(event.isAllDay()).isFalse();

        assertThat(event.getStartTs()).isEqualTo(data.getStartTs().get());
        assertThat(event.getEndTs()).isEqualTo(data.getEndTs().get());

        data.setOrganizer(Option.of(user2.getEmail()));
        data.setAttendeeEmails(Option.of(Cf.list(user.getEmail(), resource1Email)));

        eventId = createEvent(uid, data).asModified().getShowEventId();
        event = getEvent(uid, eventId);

        assertThat(event.getOrganizer().get().getUserInfo().getEmail()).isEqualTo(user2.getEmail());
        assertThat(event.getAttendees().single().getUserInfo().getEmail()).isEqualTo(user.getEmail());
        assertThat(event.getResources().single().getEmail()).isEqualTo(resource1Email);
    }

    @Test
    public void createEventWithOneUserAsOptionalAndRequiredAttendee() {
        val data = consBaseData();
        data.setAttendeeEmails(Option.of(Cf.list(user2.getEmail())));
        data.setOptionalAttendeeEmails(Option.of(Cf.list(user2.getEmail())));

        long eventId = createEvent(uid, data).asModified().getShowEventId();
        WebEventInfo event = getEvent(uid, eventId);

        assertThat(event.getAttendees().single().getUserInfo().getEmail()).isEqualTo(user2.getEmail());
        assertThat(event.getOptionalAttendees()).isEmpty();
    }

    @Test
    public void createEventWithConference() {
        val data = consBaseData();


        long eventId = createEvent(uid, data).asModified().getShowEventId();
        WebEventInfo event = getEvent(uid, eventId);

        assertThat(event.getConferenceUrl()).isEqualTo("");


        data.setConferenceUrl(Option.of("http://conference"));

        eventId = createEvent(uid, data).asModified().getShowEventId();
        event = getEvent(uid, eventId);

        assertThat(event.getConferenceUrl()).isEqualTo("http://conference");
    }

    @Test
    public void updateEventWithConference() {
        val data = consBaseData();
        data.setDescription(Option.of("description"));
        val masterId = createEvent(uid, data).asModified().getShowEventId();
        WebEventInfo event = getEvent(uid, masterId);

        assertThat(event.getConferenceUrl()).isEqualTo("");

        data.setConferenceUrl(Option.of("http://conference"));
        val recurrenceId = eventActions.updateEvent(
                uid, Option.of(masterId), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), data, Option.empty(),
                Option.of(false), Option.empty()).asModified().getShowEventId();
        event = getEvent(uid, recurrenceId);

        assertThat(event.getConferenceUrl()).isEqualTo("http://conference");
    }

    @Test
    public void getModifiedWithConference() {

        ActionInfo actionInfo0 = CalendarRequest.getCurrent().getActionInfo().unfreezeNowForTest();
        val data = consBaseData();
        data.setConferenceUrl(Option.of("http://url"));
        val createdId = webNewEventManager
                .createEvent(uid, Option.empty(), Option.empty(), data, actionInfo0).asModified().getExternalIds().single();
        val since = actionInfo0.getNow().plus(1);

        Supplier<ModifiedEventIdsInfo> modifiedInfo = () -> eventActions.getModifiedEventIds(
                uid, since, Cf.list(), Option.empty());

        assertThat(modifiedInfo.get().getExternalIds().unique()).isEmpty();

        ActionInfo actionInfo1 = actionInfo0.withNow(actionInfo0.getNow().plus(2));
        webNewEventManager.updateEvent(uid, IdOrExternalId.fromOptions(Option.empty(), Option.of(createdId)), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(),
                data, Option.empty(), Option.empty(), Option.empty(), actionInfo1).asModified().getExternalIds().single();
        assertThat(modifiedInfo.get().getExternalIds().unique()).isEmpty();

        data.setConferenceUrl(Option.of("http://new_url"));
        val updatedId = webNewEventManager.updateEvent(uid, IdOrExternalId.fromOptions(Option.empty(), Option.of(createdId)), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(),
                data, Option.empty(), Option.empty(), Option.empty(), actionInfo1).asModified().getExternalIds().single();

        assertThat(modifiedInfo.get().getExternalIds().unique()).containsExactly(updatedId);
    }

    @Test
    public void getUserEventsWithConference() {
        val allUsers = getUsers();
        val requiredUsers = allUsers.subList(0, allUsers.size() / 2);
        val optionalUsers = allUsers.subList(allUsers.size() / 2, allUsers.size());
        val data = consBaseData(requiredUsers, optionalUsers);
        data.setConferenceUrl(Option.of("http://url"));
        val eventId = createEvent(uid, data).asModified().getShowEventId();
        val userEvents = eventActions.getUserEvents(Option.empty(), Option.of(uid), Option.of(uid),
                WebDate.dateTime(WebDateTime.dateTime(NOW.minusHours(1))),
                WebDate.dateTime(WebDateTime.dateTime(NOW.plusHours(1))), Option.empty(), Option.empty(), Option.empty());
        val matchEvents = userEvents.getEvents().filter(x -> x.getId() == eventId);
        assertThat(matchEvents).hasSize(1);
        val event = matchEvents.first();
        assertThat(event.getConferenceUrl()).isEqualTo("http://url");
    }

    @Test
    public void createEventAbsence() {
        val data = consBaseData();

        data.setType(Option.of(EventType.TRIP));

        runWithActionSource(ActionSource.WEB_FOR_STAFF,
                () -> eventActions.createEvent(uid, Option.of("007gap"), Option.empty(), data));

        val event = eventActions.getEvent(uid, Option.empty(), Option.empty(), Option.of("007gap"),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty());

        assertThat(event.getAvailability().toOptional()).hasValue(Availability.AVAILABLE);
        assertThat(layerActions.getLayer(Option.of(uid), event.getLayerId(), Option.empty(), Option.empty()).getType()).isEqualTo("absence");
    }

    @Test
    public void getLayerByPrivateToken() {
        val data = consBaseData();

        data.setType(Option.of(EventType.TRIP));

        runWithActionSource(ActionSource.WEB_FOR_STAFF,
                () -> eventActions.createEvent(uid, Option.of("007gap"), Option.empty(), data));


        val event = eventActions.getEvent(uid, Option.empty(), Option.empty(), Option.of("007gap"),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty());

        String layerToken = layerRoutines.obtainPtk(uid, event.getLayerId().get(), true);
        LayerInfo foundLayer = layerActions.getLayer(Option.of(uid), event.getLayerId(), Option.empty(),
                Option.of(layerToken));
        assertThat(foundLayer.getType()).isEqualTo("absence");
        assertThat(foundLayer.getName()).isEqualTo("Командировки");
        assertThat(foundLayer.getPerm()).isEqualTo(LayerActionClass.ADMIN);
    }

    @Test
    public void createEventBusyOverlap() {
        val data = consBaseData();

        data.setAttendeeEmails(Option.of(Cf.list(resource1Email)));
        data.setStartTs(Option.of(WebDateTime.dateTime(NOW.plusMinutes(30))));

        createEvent(uid2, data);

        data.setStartTs(Option.of(WebDateTime.dateTime(NOW)));

        val overlapped = createEvent(uid, data).asBusyOverlap();

        assertThat(overlapped.getResourceEmail()).isEqualTo(resource1Email);
        assertThat(overlapped.getInstanceStart()).isEqualTo(NOW.toLocalDateTime());
        assertThat(overlapped.getOverlapStart()).isEqualTo(NOW.plusMinutes(30).toLocalDateTime());
    }

    @Test
    public void updateEventChangeLayer() {
        val layerId = testManager.createLayer(uid);

        val data = consBaseData();
        data.getUserData().setLayerId(Option.of(layerId));

        val created = createEvent(uid, data).asModified();
        assertThat(getEvent(uid, created.getShowEventId()).getLayerId().toOptional()).hasValue(layerId);

        data.getUserData().setLayerId(Option.of(user.getDefaultLayerId()));

        eventActions.updateEvent(uid, Option.empty(), Option.of(layerId), Option.of(created.getExternalIds().single()),
                Option.empty(), Option.empty(), Option.empty(), data, Option.empty(), Option.empty(), Option.empty());

        assertThat(getEvent(uid, created.getShowEventId()).getLayerId().toOptional()).hasValue(user.getDefaultLayerId());
    }

    @Test
    public void updateEventInstance() {
        val data = consBaseData();

        data.setRepetition(Option.of(consDailyRepetition(Option.empty())));
        data.getUserData().setNotifications(Option.of(Cf.list(Notification.email(Duration.ZERO))));

        val masterId = createEvent(uid, data).asModified().getShowEventId();

        data.setStartTs(Option.of(WebDateTime.dateTime(data.getStartTs().get().getDateTime().plusDays(1))));
        data.setEndTs(Option.of(WebDateTime.dateTime(data.getEndTs().get().getDateTime().plusDays(1))));

        data.getUserData().setNotifications(Option.of(Cf.list(Notification.sms(Duration.ZERO))));

        val recurrenceId = eventActions.updateEvent(
                uid, Option.of(masterId), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), data, Option.empty(),
                Option.of(false), Option.of(data.getStartTs().get().getDateTimeUtc())).asModified().getShowEventId();

        val recurrence = getEvent(uid, recurrenceId);


        assertThat(recurrenceId).isNotEqualTo(masterId);
        assertThat(recurrence.getStartTs()).isEqualTo(data.getStartTs().get());
        assertThat(recurrence.getEndTs()).isEqualTo(data.getEndTs().get());
        assertThat(recurrence.getNotifications()).isEqualTo(data.getNotifications().get());
    }

    @Test
    public void updateEventBusyOverlap() {
        val data = consBaseData();

        val masterStart = NOW.toLocalDateTime();
        val conflictStart = NOW.plusHours(24).toLocalDateTime();

        data.setStartTs(Option.of(WebDateTime.localDateTime(conflictStart)));
        data.setEndTs(Option.of(WebDateTime.localDateTime(conflictStart.plusHours(1))));

        data.setAttendeeEmails(Option.of(Cf.list(resource2Email)));

        createEvent(uid2, data);

        data.setStartTs(Option.of(WebDateTime.localDateTime(masterStart)));
        data.setEndTs(Option.of(WebDateTime.localDateTime(masterStart.plusHours(1))));

        data.setAttendeeEmails(Option.empty());

        val eventId = createEvent(uid, data).asModified().getShowEventId();

        data.setAttendeeEmails(Option.of(Cf.list(resource2Email)));

        data.setRepetition(Option.of(consDailyRepetition(Option.empty())));

        val overlapped = eventActions.updateEvent(
                uid, Option.of(eventId), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), data, Option.empty(), Option.empty(), Option.empty()).asBusyOverlap();

        assertThat(overlapped.getResourceEmail()).isEqualTo(resource2Email);
        assertThat(overlapped.getInstanceStart()).isEqualTo(conflictStart);
        assertThat(overlapped.getOverlapStart()).isEqualTo(conflictStart);
    }

    @Test
    public void updateEventMakeMeetingFromNonMeeting() {
        val data = consBaseData();

        val eventId = createEvent(uid, data).asModified().getShowEventId();

        val layerId = user.getDefaultLayerId();

        data.setAttendeeEmails(Option.of(Cf.list(new Email("attendee@yandex.ru"))));

        val layerUser = new LayerUser();
        layerUser.setPerm(LayerActionClass.EDIT);

        try {
            layerRoutines.createLayerUserForUserAndLayer(uid2, layerId, layerUser, Cf.list());

            eventActions.updateEvent(uid2, Option.of(eventId), Option.of(layerId),
                    Option.empty(), Option.empty(), Option.of(0),
                    Option.empty(), data, Option.empty(), Option.empty(), Option.empty());
        } finally {
            layerRoutines.detach(uid, uid2, layerId, ActionInfo.webTest());
        }

        val event = eventDbManager.getEventWithRelationsById(eventId);
        assertThat(event.getOrganizerUidIfMeeting().toOptional()).hasValue(uid);
    }

    @Test
    public void updateEventMakeNonMeetingFromMeeting() {
        TestUserInfo user = getUsers().get(0);
        WebEventData data = consBaseData(Cf.list(user), Cf.list());

        long eventId = createEvent(uid, data).asModified().getShowEventId();
        long layerId = user.getDefaultLayerId();

        data.setAttendeeEmails(Option.of(Cf.list()));

        eventActions.updateEvent(uid, Option.of(eventId), Option.of(layerId),
                Option.empty(), Option.empty(), Option.of(0),
                Option.empty(), data, Option.empty(), Option.empty(), Option.empty());

        EventWithRelations event = eventDbManager.getEventWithRelationsById(eventId);
        assertThat(event.isMeeting()).isFalse();
    }

    @Test
    public void attachEvent() {
        val creator = testManager.prepareRandomYaTeamUser(13213132);

        layerActions.updateLayer(creator.getUid(), creator.getDefaultLayerId(), false,
                LayerData.empty().toBuilder().participants(Option.of(Cf.list())).build());

        val eventId = createUserEvent(NOW, creator, Option.empty()).getId();

        eventActions.attachEvent(uid, eventId, Option.empty());
        WebEventInfo info = getEvent(uid, eventId);

        assertThat(info.getAvailability().toOptional()).hasValue(Availability.BUSY);
        assertThat(info.getDecision().toOptional()).hasValue(Decision.YES);

        val availability = Availability.MAYBE;
        val notification = Notification.sms(Duration.ZERO);

        val layerData = LayerData.empty().toBuilder().name(Option.of("Name")).build();
        val layerId = layerActions.createLayer(uid2, layerData).getLayerId();

        eventActions.attachEvent(uid2, eventId, Option.of(WebEventUserData.empty().toBuilder()
                .availability(Option.of(availability))
                .layerId(Option.of(layerId))
                .notifications(Option.of(Cf.list(notification))).build()));

        info = getEvent(uid2, eventId);

        assertThat(info.getAvailability().toOptional()).hasValue(availability);
        assertThat(info.getLayerId().toOptional()).hasValue(layerId);
        assertThat(info.getNotifications()).containsExactly(notification);
    }

    @Test
    public void deleteEvent() {
        val master = createUserEvent(NOW, user, Option.empty(), user2);

        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        eventActions.deleteEvent(
                uid, Option.of(master.getId()), Option.empty(), Option.empty(), Option.empty(),
                Option.of(false), Option.of(master.getStartTs().toDateTime(DateTimeZone.UTC).toLocalDateTime()));

        assertThat(eventDbManager.getEventByIdSafe(master.getId()).toOptional()).isPresent();
        assertThat(eventDbManager.getEventAndRepetitionByEvent(master).getRepetitionInfo().getExdates()).hasSize(1);

        eventActions.deleteEvent(
                uid, Option.of(master.getId()), Option.empty(),
                Option.empty(), Option.empty(), Option.of(true), Option.empty());

        assertThat(eventDbManager.getEventByIdSafe(master.getId()).toOptional()).isEmpty();
    }

    @Test
    public void deleteFutureEvents() {
        val master = testManager.createDefaultEvent(uid, "Repeating", NOW.minusDays(1));

        testManager.createDailyRepetitionAndLinkToEvent(master.getId());
        testManager.addUserParticipantToEvent(master.getId(), uid, Decision.YES, true);

        val recurrence = testManager.createDefaultRecurrence(uid, master.getId(), NOW.plusDays(1));

        eventActions.deleteFutureEvents(uid, master.getId());

        assertThat(eventDbManager.getEventByIdSafe(master.getId()).toOptional()).isEmpty(); // since no cut modification
        assertThat(eventDbManager.getEventByIdSafe(recurrence.getId()).toOptional()).isEmpty();
    }

    @Test
    public void declineThenAccept() {
        val attendee = testManager.prepareUser("yandex-team-mm-19200");

        val event = testManager.createDefaultEvent(uid, "Repeating", NOW.minusDays(1));

        testManager.addUserParticipantToEvent(event.getId(), uid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee, Decision.UNDECIDED, false);

        eventActions.handleReply(attendee.getUid(), new ReplyData(event.getId(), Decision.NO));
        assertThat(eventUserRoutines.findEventUserDecision(attendee.getUid(), event.getId()).toOptional()).hasValue(Decision.NO);

        eventActions.handleReply(attendee.getUid(), new ReplyData(event.getId(), Decision.YES));
        assertThat(eventUserRoutines.findEventUserDecision(attendee.getUid(), event.getId()).toOptional()).hasValue(Decision.YES);
    }

    @Test
    public void detachSharedEvent() {
        layerActions.updateLayer(uid, user.getDefaultLayerId(), false,
                LayerData.empty().toBuilder().participants(Option.of(Cf.list())).build());

        val event = createUserEvent(NOW, user, Option.empty());

        val data = consBaseData();
        data.getUserData().setLayerId(Option.of(user2.getDefaultLayerId()));

        eventActions.updateEvent(uid2, Option.of(event.getId()), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                data, Option.empty(), Option.empty(), Option.empty());

        assertThat(getEvent(uid2, event.getId()).getLayerId().toOptional()).hasValue(user2.getDefaultLayerId());

        eventActions.detachEvent(uid2, event.getId(), Option.empty());
        assertThat(getEvent(uid2, event.getId()).getLayerId().toOptional()).isEmpty();

        assertThatExceptionOfType(CommandRunException.class)
                .isThrownBy(() -> eventActions.detachEvent(uid2, event.getId(), Option.empty()))
                .matches(e -> e.isSituation(Situation.EVENT_NOT_FOUND));

        eventActions.detachEvent(uid, event.getId(), Option.of(user.getDefaultLayerId()));
        assertThat(getEvent(uid, event.getId()).getLayerId().toOptional()).isEmpty();
    }

    @Test
    public void checkEventWithSeries() {
        val data = consBaseData();
        val base = DateTime.parse("2019-08-20T01:20+03:00");
        data.setStartTs(Option.of(WebDateTime.dateTime(base)));
        data.setEndTs(Option.of(WebDateTime.dateTime(base.plusHours(1))));

        val repetition = TestManager.createDailyRepetitionTemplate();
        repetition.setType(RegularRepetitionRule.WEEKLY);
        repetition.setRWeeklyDays("tu");

        data.setRepetition(Option.of(RepetitionData.fromRepetition(repetition, MoscowTime.TZ)));
        val created = createEvent(uid, data).asModified();
        val eventId = created.getShowEventId();

        val instantToRemove = base.plusWeeks(1).toInstant();
        eventActions.deleteEvent(uid, Option.of(eventId), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(),
                Option.of(instantToRemove).map(LocalDateTime::new));

        val instantToUpdate = base.plusWeeks(2).toInstant();
        val name = "Updated recurrent event";
        data.setName(Option.of(name));
        val recurrenceId = eventActions.updateEvent(
                uid, Option.of(eventId), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), data, Option.empty(),
                Option.of(false), Option.of(instantToUpdate).map(LocalDateTime::new)).asModified().getShowEventId();

        val event = eventActions.getEventWithSeries(uid, created.getExternalIds().first(), Option.empty());

        assertThat(event.getExdates()).hasSize(1).first()
                .isEqualTo(new ExdateBrief(instantToRemove, Optional.empty()));

        assertThat(event.getRecurrentEvents()).hasSize(1);
        val recurrentEvent = event.getRecurrentEvents().iterator().next();
        assertThat(recurrentEvent.getId()).isEqualTo(recurrenceId);
        assertThat(recurrentEvent.getName()).isEqualTo(name);
        assertThat(event.getMainEvent().getName()).isEqualTo("Без названия");
    }

    @Test
    public void getModifiedEventIds() {
        val since = NOW.toInstant().minus(1);

        val createdId = createEvent(uid, consBaseData()).asModified().getExternalIds().single();

        val updateId = createUserEvent(NOW, user, Option.empty()).getId();
        val declineId = createUserEvent(NOW, user2, Option.empty(), user).getId();
        val deleteId = createUserEvent(NOW, user, Option.empty()).getId();

        val extIds = mainEventDao.findExternalIdsByEventIds(
                Cf.list(updateId, declineId, deleteId)).unique();

        Supplier<ModifiedEventIdsInfo> modifiedInfo = () -> eventActions.getModifiedEventIds(
                uid, since, Cf.list(), Option.empty());

        assertThat(modifiedInfo.get().getExternalIds().unique()).containsExactly(createdId);

        eventActions.updateEvent(uid, Option.of(updateId), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(),
                consBaseData(), Option.empty(), Option.empty(), Option.empty());

        eventActions.handleReply(uid, new ReplyData(declineId, Decision.NO));

        eventActions.deleteEvent(uid, Option.of(deleteId),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty());

        val feed = layerActions.importIcs(uid, new ImportIcsData(
                Option.empty(), Option.of("holidays://russia"),
                new ImportIcsData.Layer("feed", LayerData.empty())));

        assertThat(modifiedInfo.get().getExternalIds().unique()).isEqualTo(extIds.plus(createdId));
        assertThat(modifiedInfo.get().getIcsFeedIds()).containsExactly(feed.getId());
    }

    @Test
    public void moveResourceOk() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, false, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test
    public void moveResourceOkOneUser() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid, false, false, NOW, Optional.of(0), Cf.list(), Cf.list());

        moveResourceCase(event1Id, event2Id, Optional.of(0), Optional.of(0), 0, 0, false, uid, Option.of(uid));
    }

    @Test(expected = PermissionDeniedUserException.class)
    public void moveResourceBadSource() {
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());
        moveResourceCase(event2Id, event2Id, 0, 0, false, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test(expected = PermissionDeniedUserException.class)
    public void moveResourceBadTarget() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        moveResourceCase(event1Id, event1Id, 0, 0, false, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test(expected = IllegalParameterException.class)
    public void moveResourceBadResource() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.of(0), Cf.list(resource2Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, false, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test(expected = IllegalParameterException.class)
    public void moveResourceBadStart() {
        val event1Id = constructAndCreateEvent(uid, true, false, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, false, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test(expected = IllegalParameterException.class)
    public void moveResourceBadStartAndEnd() {
        val event1Id = constructAndCreateEvent(uid, true, true, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, false, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test(expected = IllegalParameterException.class)
    public void moveResourceBadStartAndEndOfRepetition() {
        val event1Id = constructAndCreateEvent(uid, true, true, NOW, Optional.empty(), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.empty(), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 2, 2, false, Optional.empty(), Optional.empty(), Option.of(uid2));
    }

    @Test(expected = IllegalParameterException.class)
    public void moveResourceBadEnd() {
        val event1Id = constructAndCreateEvent(uid, false, true, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, false, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test
    public void moveResourceZeroRepetition() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.empty(), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, false, Optional.empty(), Optional.of(0), Option.of(uid2));
    }

    @Test
    public void moveResourceSecondRepetition() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.empty(), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(2), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 2, 0, false, Optional.empty(), Optional.of(2), Option.of(uid2));
    }

    @Test
    public void moveResourceDoubleZeroRepetition() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.empty(), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.empty(), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, false, Optional.empty(), Optional.empty(), Option.of(uid2));
    }

    @Test
    public void moveResourceDoubleSecondRepetition() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.empty(), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.empty(), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 2, 2, false, Optional.empty(), Optional.empty(), Option.of(uid2));
    }

    @Test
    public void moveResourceEmptyTarget() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, false, Optional.of(0), Optional.of(0), Option.empty());
    }

    @Test
    public void moveResourceDifferentTimeZones() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW_UTC, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, false, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test
    public void moveResourceDifferentTimeZonesWithRepetition() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW_UTC, Optional.empty(), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(2), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 2, 0, false, Optional.empty(), Optional.of(2), Option.of(uid2));
    }

    @Test
    public void moveResourceOkEmptyStart() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, true, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test(expected = IllegalParameterException.class)
    public void moveResourceBadStartAndEndEmptyStart() {
        val event1Id = constructAndCreateEvent(uid, true, true, NOW, Optional.of(0), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, true, Optional.of(0), Optional.of(0), Option.of(uid2));
    }

    @Test(expected = IllegalParameterException.class)
    public void moveResourceZeroRepetitionEmptyStart() {
        val event1Id = constructAndCreateEvent(uid, false, false, NOW, Optional.empty(), Cf.list(resource1Email), Cf.list());
        val event2Id = constructAndCreateEvent(uid2, false, false, NOW, Optional.of(0), Cf.list(user.getEmail()), Cf.list());

        moveResourceCase(event1Id, event2Id, 0, 0, true, Optional.empty(), Optional.of(0), Option.of(uid2));
    }

    @Test
    public void checkAdminAllResourcesIsEqualToCanEditFlag() {
        val users = getUsers();
        val eventId = createEvent(users, Cf.list());

        val event = getEvent(uid, eventId);
        assertThat(event.getActions().canEdit()).withFailMessage("canEdit").isTrue();
        assertThat(event.isCanAdminAllResources()).withFailMessage("canAdminAllResources").isTrue();
    }

    @Test
    public void checkAdminAllResourcesIsTrueIfOrganizerLetUsersEditTheEvent() {
        val users = getUsers();
        val data = consBaseData(users, Cf.list());
        data.setParticipantsCanEdit(Option.of(true));

        val eventId = createEvent(uid, data).asModified().getShowEventId();

        assertThat(StreamEx.of(users).map(user -> getEvent(user.getUid(), eventId)).toImmutableList()).allMatch(WebEventInfo::isCanAdminAllResources);
    }

    @Ignore
    @Test
    public void getEventWithoutUidByLayerToken() {
        TestUserInfo user1 = testManager.prepareRandomYaTeamUser(11411);
        TestUserInfo user2 = testManager.prepareRandomYaTeamUser(11412);
        PassportUid uid1 = user1.getUid();
        PassportUid uid2 = user2.getUid();
        testManager.createDefaultEventWithEventLayerAndEventUser(uid1, "event1");
        testManager.createDefaultEventWithEventLayerAndEventUser(uid2, "event2");
        Event event = testManager.createDefaultMeeting(uid1, "incomplete invitation");
        testManager.addUserParticipantToEvent(event.getId(), user1, Decision.UNDECIDED, true);
        testManager.addUserParticipantToEvent(event.getId(), user2, Decision.NO, false);
        Option<EventLayer> eventLayerForEventAndUser =
                eventDbManager.getEventLayerForEventAndUser(event.getId(), uid2);
        EventLayer eventLayer = eventLayerForEventAndUser.get();
        Layer layerById = layerRoutines.getLayerById(eventLayer.getEventId());
        Option<String> privateToken = layerById.getPrivateToken();

        FilterablePojo<EventsInfo> events = eventActions.getEvents(Option.empty(), Option.empty(), Option.empty(),
                Cf.list(layerById.getId().toString()), privateToken,
                Option.empty(), Option.of(WebDate.dateTime(WebDateTime.dateTime(NOW))),
                Option.of(WebDate.dateTime(WebDateTime.dateTime(NOW.plusHours(1)))),
                Option.empty(), Cf.list(), Option.empty(), Option.empty(), Option.empty(), Option.of(false),
                Option.empty(),
                Option.of(DateTimeZone.forID("Europe/Moscow")), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty());
        assertThat(events.getBendable().getEvents().length()).isEqualTo(0);
    }
}
