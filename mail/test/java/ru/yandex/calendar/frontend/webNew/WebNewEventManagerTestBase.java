package ru.yandex.calendar.frontend.webNew;

import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.bender.WebDate;
import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.webNew.actions.EventActions;
import ru.yandex.calendar.frontend.webNew.dto.in.WebEventData;
import ru.yandex.calendar.frontend.webNew.dto.inOut.RepetitionData;
import ru.yandex.calendar.frontend.webNew.dto.out.*;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.MoscowTime;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

abstract public class WebNewEventManagerTestBase extends WebNewTestWithResourcesBase {
    protected static final int NUMBER_OF_USERS = WebNewEventManager.ATTENDEE_LIMIT * 4;
    protected static final DateTime NOW = MoscowTime.dateTime(2018, 1, 9, 20, 0);
    protected static final DateTime NOW_UTC = NOW.withZone(DateTimeZone.UTC);

    @Autowired
    protected EventActions eventActions;

    @Override
    protected ReadableInstant now() {
        return NOW;
    }

    protected long constructAndCreateEvent(PassportUid uid, boolean shiftEventStart, boolean shiftEventEnd, DateTime startTime,
                                           Optional<Integer> shiftInWeeks, ListF<Email> attendeeEmails, ListF<Email> optionalAttendeeEmails) {

        val weeksShiftDuration = Duration.standardDays(shiftInWeeks.orElse(0) * 7L);
        val startMinutesShiftDuration = Duration.standardMinutes(shiftEventStart ? 5 : 0);
        val endMinutesShiftDuration = Duration.standardMinutes(shiftEventEnd ? 5 : 0);
        val startShift = weeksShiftDuration.plus(startMinutesShiftDuration);
        val endShift = weeksShiftDuration.plus(endMinutesShiftDuration);

        val data = consBaseData(startShift, endShift, shiftInWeeks.isEmpty(), startTime, attendeeEmails, optionalAttendeeEmails );
        return createEvent(uid, data).asModified().getShowEventId();
    }

    protected long createEvent(List<TestUserInfo> attendees, List<TestUserInfo> optionalAttendees) {
        val data = consBaseData(attendees, optionalAttendees);
        return createEvent(uid, data).asModified().getShowEventId();
    }

    protected ModifyEventResult createEvent(PassportUid uid, WebEventData data) {
        return eventActions.createEvent(uid, Option.empty(), Option.empty(), data);
    }

    protected WebEventData consBaseData(List<TestUserInfo> attendees, List<TestUserInfo> optionalAttendees) {
        val data = consBaseData();
        val attendeesEmails = Cf.toList(attendees).map(TestUserInfo::getEmail);
        val optionalAttendeesEmails = Cf.list(optionalAttendees).map(TestUserInfo::getEmail);
        data.setAttendeeEmails(wrapEmails(attendeesEmails));
        data.setOptionalAttendeeEmails(wrapEmails(optionalAttendeesEmails));
        return data;
    }

    protected WebEventData consBaseData() {
        return consBaseData(Duration.ZERO, Duration.ZERO, false, NOW, Cf.list(), Cf.list());
    }

    protected WebEventData consBaseData(Duration startShift, Duration endShift, boolean addRepetition, DateTime now, ListF<Email> attendeeEmails,  ListF<Email> optionalAttendeeEmails) {
        val data = WebEventData.empty();

        data.setStartTs(wrapDate(now.plus(startShift)));
        data.setEndTs(wrapDate(now.plusHours(1).plus(endShift)));
        data.setAttendeeEmails(wrapEmails(attendeeEmails));
        data.setOptionalAttendeeEmails(wrapEmails(optionalAttendeeEmails));

        if (addRepetition) {
            val repetitionData = new RepetitionData();
            repetitionData.setType(RegularRepetitionRule.WEEKLY);
            repetitionData.setWeeklyDays(Option.of("Tue"));
            repetitionData.setEach(1);
            repetitionData.setMonthlyLastweek(Option.empty());
            repetitionData.setDueDate(Option.empty());
            data.setRepetition(Option.of(repetitionData));
        }

        return data;
    }

    protected ListF<WebEventInfo> createTestEventForGetEvents(TestUserInfo actor, TestUserInfo target) {
        val actorUid = actor.getUid();
        val targetUid = target.getUid();

        val data = consBaseData();
        data.setOthersCanView(Option.of(false));
        data.setStartTs(Option.of(WebDateTime.dateTime(NOW)));
        data.setEndTs(Option.of(WebDateTime.dateTime(NOW.plusHours(1))));

        val eventId = createEvent(targetUid, data).asModified().getShowEventId();
        val layerId = Long.toString(target.getDefaultLayerId());
        val from = Option.of(WebDate.dateTime(WebDateTime.dateTime(NOW.minusDays(1))));
        val till = Option.of(WebDate.dateTime(WebDateTime.dateTime(NOW.plusDays(1))));

        val events = eventActions.getEvents(
                Option.empty(), Option.of(actorUid), Option.of(targetUid), Cf.list(layerId), Option.empty(),
                Option.empty(), from, till, Option.of(eventId),
                Cf.list(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty());

        return events.getBendable().getEvents();
    }

    protected List<WebEventInfo> getEvents(PassportUid uid, long eventId, Optional<Integer> shift) {
        val start = shift.orElse(0);
        val count = shift.isEmpty() ? 5 : 1;
        return StreamEx.iterate(start, Math::incrementExact)
                .limit(count)
                .map(NOW::plusWeeks)
                .map(this::wrapLocalDate)
                .map(x -> getEvent(uid, eventId, Option.x(x)))
                .toImmutableList();
    }

    protected WebEventInfo getEvent(PassportUid uid, long eventId) {
        return getEvent(uid, eventId, Option.empty());
    }

    protected WebEventInfo getEvent(PassportUid uid, long eventId, Option<LocalDateTime> instanceStartTs) {
        return eventActions.getEvent(
                uid, Option.of(eventId), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), instanceStartTs, Option.empty(), Option.empty(), Option.empty(), Option.empty());
    }

    protected void compareEventsLists(List<WebEventInfo> oldEvents, List<WebEventInfo> newEvents, int changedIndex, List<Email> addToOld, List<Email> addToNew) {
        for (int i = 0; i < oldEvents.size(); i++) {
            List<Email> emailsForOldEvent = i == changedIndex ? addToOld : emptyList();
            List<Email> emailsForNewEvent = i == changedIndex ? addToNew : emptyList();
            val equalId = oldEvents.size() == 1 || i != changedIndex;

            compareEvents(oldEvents.get(i), newEvents.get(i), emailsForOldEvent, emailsForNewEvent, equalId);
        }
    }

    protected void compareEvents(WebEventInfo newEvent, WebEventInfo oldEvent, List<Email> newEmails, List<Email> oldEmails, boolean equalId) {
        assertThat(newEvent).isEqualToIgnoringGivenFields(oldEvent, "isRecurrence", "repetition", "id", "actions", "organizer", "resources", "sequence");

        if (equalId) {
            assertThat(newEvent.isRecurrence()).isEqualTo(oldEvent.isRecurrence());
            assertThat(newEvent.getRepetition()).isEqualTo(oldEvent.getRepetition());
            assertThat(newEvent.getId()).isEqualTo(oldEvent.getId());
        } else {
            assertThat(newEvent.isRecurrence()).isNotEqualTo(oldEvent.isRecurrence());
            assertThat(newEvent.getRepetition()).isNotEqualTo(oldEvent.getRepetition());
            assertThat(newEvent.getId()).isNotEqualTo(oldEvent.getId());
        }

        val resourcesOld = StreamEx.of(newEvent.getResources())
                .map(WebResourceInfo::getEmail)
                .append(oldEmails)
                .toImmutableSet();
        val resourcesNew = StreamEx.of(oldEvent.getResources())
                .map(WebResourceInfo::getEmail)
                .append(newEmails)
                .toImmutableSet();
        assertThat(resourcesOld).isEqualTo(resourcesNew);
    }

    protected RepetitionData consDailyRepetition(Option<Instant> dueTs) {
        val repetition = TestManager.createDailyRepetitionTemplate();
        repetition.setDueTs(dueTs);

        return RepetitionData.fromRepetition(repetition, MoscowTime.TZ);
    }

    protected List<TestUserInfo> getUsers() {
        val maxExistingUid = Math.max(uid.getUid(), uid2.getUid());
        return IntStream.rangeClosed(1, NUMBER_OF_USERS)
                .mapToObj(i -> testManager.prepareRandomYaTeamUser(maxExistingUid + i))
                .collect(Collectors.toList());
    }

    protected Set<String> getTestUserEmails(List<TestUserInfo> users) {
        return users.stream().map(TestUserInfo::getEmail)
                .map(Email::getEmail)
                .collect(Collectors.toSet());
    }

    protected Set<String> getParticipantEmails(List<WebUserParticipantInfo> attendees) {
        return attendees.stream().map(WebUserParticipantInfo::getUserInfo)
                .map(WebUserInfo::getEmail)
                .map(Email::getEmail)
                .collect(Collectors.toSet());
    }

    protected void validateAttendees(List<TestUserInfo> users, List<WebUserParticipantInfo> attendees) {
        assertThat(attendees.size()).isEqualTo(users.size());
        assertThat(getParticipantEmails(attendees)).isEqualTo(getTestUserEmails(users));
    }

    public void moveResourceCase(long sourceEventId, long targetEventId, int sourceChangedIndex, int targetChangedIndex, boolean emptyInstanceTs,
                                 Optional<Integer> sourceShift, Optional<Integer> targetShift, Option<PassportUid> targetUid) {
        moveResourceCase(sourceEventId, targetEventId, sourceShift, targetShift, sourceChangedIndex, targetChangedIndex, emptyInstanceTs, uid2, targetUid);
    }

    public void moveResourceCase(long sourceEventId, long targetEventId, Optional<Integer> sourceShift, Optional<Integer> targetShift,
                                 int sourceChangedIndex, int targetChangedIndex, boolean emptyInstanceTs,
                                 PassportUid targetEventOwnerUid, Option<PassportUid> targetUid) {
        val oldEvents = getEvents(uid, sourceEventId, sourceShift);
        val oldEvents2 = getEvents(targetEventOwnerUid, targetEventId, targetShift);

        val instanceTs = emptyInstanceTs ? Option.<LocalDateTime>empty() : Option.of(oldEvents.get(sourceChangedIndex).getInstanceStartTs());
        val result = eventActions.moveResource(uid, uid, sourceEventId, targetEventId, resource1Email.getEmail(),
                targetUid, instanceTs);

        if (sourceShift.isEmpty()) {
            assertThat(result.getSourceEventId()).isNotEqualTo(sourceEventId);
        } else {
            assertThat(result.getSourceEventId()).isEqualTo(sourceEventId);
        }
        if (targetShift.isEmpty()) {
            assertThat(result.getTargetEventId()).isNotEqualTo(targetEventId);
        } else {
            assertThat(result.getTargetEventId()).isEqualTo(targetEventId);
        }

        val newEvents = getEvents(uid, sourceEventId, sourceShift);
        val newEvents2 = getEvents(targetEventOwnerUid, targetEventId, targetShift);

        compareEventsLists(oldEvents, newEvents, sourceChangedIndex, List.of(resource1Email), emptyList());
        compareEventsLists(oldEvents2, newEvents2, targetChangedIndex, emptyList(), List.of(resource1Email));
    }

    protected Option<ListF<Email>> wrapEmails(ListF<Email> emails) {
        return Option.of(emails);
    }

    protected Option<WebDateTime> wrapDate(DateTime dateTime) {
        return Option.of(WebDateTime.dateTime(dateTime));
    }

    protected Optional<LocalDateTime> wrapLocalDate(DateTime dateTime) {
        return Optional.of(new LocalDateTime(dateTime));
    }
}
