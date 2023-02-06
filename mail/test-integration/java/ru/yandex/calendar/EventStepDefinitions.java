package ru.yandex.calendar;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Value;
import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.DateTimeZone;
import ru.yandex.bolts.collection.CollectorsF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.definition.Meeting;
import ru.yandex.calendar.frontend.bender.RawJson;
import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.webNew.WebNewEventManager;
import ru.yandex.calendar.frontend.webNew.dto.in.WebEventData;
import ru.yandex.calendar.frontend.webNew.dto.in.WebEventUserData;
import ru.yandex.calendar.frontend.webNew.dto.out.WebEventInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.WebUserInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.WebUserParticipantInfo;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ExternalId;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.web.IdOrExternalId;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.support.ServiceStorage;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.calendar.definition.Constants.ADMIN_LOGIN;

public class EventStepDefinitions extends BaseStepDefinitions {
    @Value
    private static class EventDescription {
        Event event;
        WebEventInfo info;
    }

    @Value
    private static class MeetingDescription {
        EventDescription master;
        List<EventDescription> recurrences;
        MainEvent mainEvent;
    }

    @Inject
    WebNewEventManager eventNewManager;

    @Inject
    EventDao eventDao;

    @Inject
    MainEventDao mainEventDao;

    @Inject
    ServiceStorage serviceStorage;

    private EventDescription createDescription(WebEventInfo info) {
        val event = eventDao.findEventById(info.getId());
        return new EventDescription(event, info);
    }

    private MeetingDescription findMeeting(PassportUid uid, String meetingName) {
        val info = eventNewManager.getEvent(uid, meetingName, Language.ENGLISH, ActionInfo.webTest());
        val master = createDescription(info.getMainEvent());
        val recurrences = StreamEx.of(info.getRecurrentEvents()).map(this::createDescription).toImmutableList();
        val mainEvent = mainEventDao.findMainEventsByExternalId(new ExternalId(meetingName)).first();
        return new MeetingDescription(master, recurrences, mainEvent);
    }

    private MeetingDescription findMeeting(YandexUser user, String meetingName) {
        return findMeeting(user.getUid(), meetingName);
    }

    private MeetingDescription findMeeting(String meetingName) {
        val mainEvent = mainEventDao.findMainEventsByExternalId(new ExternalId(meetingName)).first();
        val master = eventDao.findMasterEventByMainId(mainEvent.getId()).first();
        return findMeeting(master.getCreatorUid(), meetingName);
    }

    private static WebEventData toWebEventData(EventDescription eventDescription, MainEvent mainEvent, WebEventUserData userData) {
        WebEventInfo info = eventDescription.getInfo();
        DateTimeZone tzId = DateTimeZone.forID(mainEvent.getTimezoneId());
        Option<Email> organizer = info.getOrganizer()
            .map(WebUserParticipantInfo::getUserInfo)
            .map(WebUserInfo::getEmail);
        ListF<Email> attendees = StreamEx.of(info.getAttendees())
            .map(WebUserParticipantInfo::getUserInfo)
            .map(WebUserInfo::getEmail)
            .collect(CollectorsF.toList());
        ListF<Email> optionalAttendees = StreamEx.of(info.getOptionalAttendees())
                .map(WebUserParticipantInfo::getUserInfo)
                .map(WebUserInfo::getEmail)
                .collect(CollectorsF.toList());
        return new WebEventData(
                eventDescription.getEvent(),
                tzId,
                organizer,
                Option.of(attendees),
                Option.of(optionalAttendees),
                info.getRepetition(),
                userData,
                info.getAttachments());
    }

    @Before
    public void before(Scenario scenario) {
        val adminUid = getUser(ADMIN_LOGIN).getUid();

        StreamEx.of(mainEventDao.findMainEvents())
            .map(MainEvent::getExternalId)
            .forEach(id -> eventNewManager.deleteEvent(
                    adminUid,
                    IdOrExternalId.externalId(id),
                    Option.empty(),
                    true,
                    Option.empty(),
                    ActionInfo.webTest()));
    }

    @Given("meeting")
    public void createMeeting(Meeting meeting) {
        val eventName = meeting.getName();
        val organizer = getUser(meeting.getOrganizer());
        val attendees = StreamEx.of(meeting.getAttendees())
            .map(this::getUser)
            .map(YandexUser::getEmail)
            .flatMap(StreamEx::of)
            .collect(CollectorsF.toList());
        Option<ListF<Email>> optionalAttendees = Option.empty();
        if (meeting.getOptionalAttendees().isPresent()) {
            optionalAttendees = Option.of(
                StreamEx.of(meeting.getOptionalAttendees().get())
                    .map(this::getUser)
                    .map(YandexUser::getEmail)
                    .flatMap(StreamEx::of)
                    .collect(CollectorsF.toList()));
        }

        val start = WebDateTime.dateTime(NOW);
        val end = WebDateTime.dateTime(NOW.plusHours(1));
        val userData = new WebEventUserData(Option.of(Availability.BUSY), Option.empty(), Option.empty());
        val eventData = new WebEventData(
                Option.of(EventType.USER),
                Option.empty(),
                Option.empty(),
                Option.of(start),
                Option.of(end),
                Option.of(eventName),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.of(false),
                Option.of(false),
                Option.of(true),
                organizer.getEmail(),
                Option.of(attendees),
                optionalAttendees,
                Option.empty(),
                userData,
                Option.x(meeting.getData().map(RawJson::new)),
                Option.empty());

        eventNewManager.createEvent(organizer.getUid(), Option.of(eventName), Option.empty(), eventData, ActionInfo.webTest());
    }

    @When("{string} invites {string} to {string}")
    public void userInvitesNewAttendee(String inviterLogin, String attendeeLogin, String meetingName) {
        val inviter = getUser(inviterLogin);
        val attendee = getUser(attendeeLogin);
        val meeting = findMeeting(inviter, meetingName);

        val oldAttendees = meeting
            .getMaster()
            .getInfo()
            .getAttendees();

        val newAttendees = StreamEx.of(oldAttendees)
            .map(WebUserParticipantInfo::getUserInfo)
            .map(WebUserInfo::getEmail)
            .append(attendee.getEmail())
            .collect(CollectorsF.toList());

        val eventData = toWebEventData(meeting.getMaster(), meeting.getMainEvent(), WebEventUserData.empty());
        eventData.setAttendeeEmails(Option.of(newAttendees));
        eventData.setOrganizer(Option.empty());

        eventNewManager.updateEvent(inviter.getUid(), IdOrExternalId.externalId(meetingName), Option.empty(), Option.empty(),
            Option.empty(), Option.empty(), eventData, Option.of(true), Option.empty(), Option.of(false), ActionInfo.webTest());
    }

    private void setEventJsonData(String userLogin, String meetingName, Optional<String> json, OptionalLong requestTvmId) {
        val user = getUser(userLogin);
        val meeting = findMeeting(user, meetingName);

        val webEventData = toWebEventData(meeting.getMaster(), meeting.getMainEvent(), WebEventUserData.empty());
        webEventData.setEventData(Option.x(json));
        webEventData.setOrganizer(Option.empty());

        val actionInfo = ActionInfo.webTest().withTvmId(requestTvmId);
        eventNewManager.updateEvent(user.getUid(), IdOrExternalId.externalId(meetingName), Option.empty(), Option.empty(),
            Option.empty(), Option.empty(), webEventData, Option.empty(), Option.empty(), Option.of(false), actionInfo);
    }

    @When("{string} set {string} event json data to {string}")
    public void userSetMeetEventJsonData(String userLogin, String meetingName, String json) {
        setEventJsonData(userLogin, meetingName, Optional.of(json), OptionalLong.empty());
    }

    @When("{string} set {string} event json data to {string} using {string} service")
    public void userSetMeetEventJsonData(String userLogin, String meetingName, String json, String serviceName) {
        val tvmId = serviceStorage.find(serviceName);
        setEventJsonData(userLogin, meetingName, Optional.of(json), OptionalLong.of(tvmId));
    }

    @When("{string} reset {string} event json data")
    public void userResetMeetEventJsonData(String userLogin, String meetingName) {
        setEventJsonData(userLogin, meetingName, Optional.of("{}"), OptionalLong.empty());
    }

    @When("{string} reset {string} event json data using {string} service")
    public void userResetMeetEventJsonData(String userLogin, String meetingName, String serviceName) {
        val tvmId = serviceStorage.find(serviceName);
        setEventJsonData(userLogin, meetingName, Optional.of("{}"), OptionalLong.of(tvmId));
    }

    @When("{string} allow attendees can edit meeting {string}")
    public void orgAllowAttendeesCanEditMeeting(String actorLogin, String meetingName) {
        val actor = getUser(actorLogin);
        val meeting = findMeeting(actor, meetingName);

        val webEventData = toWebEventData(meeting.getMaster(), meeting.getMainEvent(), WebEventUserData.empty());
        webEventData.setParticipantsCanEdit(Option.of(true));
        webEventData.setOrganizer(Option.empty());

        eventNewManager.updateEvent(actor.getUid(), IdOrExternalId.externalId(meetingName), Option.empty(), Option.empty(),
            Option.empty(), Option.empty(), webEventData, Option.empty(), Option.empty(), Option.of(false), ActionInfo.webTest());
    }

    @Then("meeting {string} should contain the following attendees: {}")
    public void meetingShouldContainTheFollowingAttendees(String meetingName, List<String> expectedAttendees) {
        val actualAttendees = findMeeting(meetingName)
            .getMaster()
            .getInfo()
            .getAttendees();

        val actualAttendeeLogins = StreamEx.of(actualAttendees)
            .map(WebUserParticipantInfo::getUserInfo)
            .map(WebUserInfo::getEmail)
            .map(Email::getLocalPart)
            .toImmutableList();

        assertThat(actualAttendeeLogins)
            .containsExactlyInAnyOrderElementsOf(expectedAttendees);
    }

    @Then("meeting {string} should contain the following json data: {string}")
    public void meetingMeetShouldContainTheFollowingJsonData(String meetingName, String expectedJson) {
        val data = findMeeting(meetingName)
            .getMaster()
            .getEvent()
            .getData()
            .toOptional();

        assertThat(data)
            .contains(new RawJson(expectedJson));
    }

    @Then("meeting {string} should not contain json data")
    public void meetingMeetShouldNotContainJsonData(String meetingName) {
        val data = findMeeting(meetingName)
            .getMaster()
            .getEvent()
            .getData()
            .toOptional();

        assertThat(data)
            .contains(new RawJson("{}"));
    }
}
