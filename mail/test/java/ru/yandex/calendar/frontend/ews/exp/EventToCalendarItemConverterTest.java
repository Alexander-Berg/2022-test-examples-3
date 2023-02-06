package ru.yandex.calendar.frontend.ews.exp;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.microsoft.schemas.exchange.services._2006.types.ItemChangeDescriptionType;
import com.microsoft.schemas.exchange.services._2006.types.SetItemFieldType;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.event.EventChangesInfoForExchange;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

import static org.assertj.core.api.Assertions.assertThat;

public class EventToCalendarItemConverterTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventToCalendarItemConverter eventToCalendarItemConverter;

    @Test
    public void doNotAddLocationToChangesIfNothingElseChanged() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(454).getUid();
        Event event = testManager.createDefaultEvent(uid, "useless event");
        ListF<ItemChangeDescriptionType> changes =
                eventToCalendarItemConverter.convertToChangeDescriptions(
                        eventDbManager.getEventWithRelationsByEvent(event),
                        eventDbManager.getEventAndRepetitionByEvent(event).getRepetitionInfo(),
                        new EventChangesInfoForExchange(new Event(), new Repetition(), false, Cf.list()));
        Assert.isEmpty(changes);
    }

    /**
     * If we change textual location it should be sent to exchange.
     */
    @Test
    public void changeTextualLocation() {
        val initialEvent = prepareInitialEvent("Before", 0);
        val newLocationText = "After";
        val eventChangesInfoForExchange = prepareChanges(Optional.of(newLocationText), false);

        val changes = convertChanges(initialEvent, eventChangesInfoForExchange);

        assertThat(extractLocationFromListOfChanges(changes)).contains(newLocationText);
    }

    /**
     * No changes in textual field, but were changes in a list of meeting rooms => set location as a list of the rooms.
     */
    @Test
    public void addMeetingRoomsOrDeletePartOfMeetingRooms() {
        val initialEvent = prepareInitialEvent("", 2);
        val eventChangesInfoForExchange = prepareChanges(Optional.empty(), true);

        val changes = convertChanges(initialEvent, eventChangesInfoForExchange);

        assertThat(extractLocationFromListOfChanges(changes)).contains("meetingroom0, meetingroom1");
    }

    /**
     * In case all meeting rooms are deleted and no textual location submitted we should leave location field empty.
     */
    @Test
    public void deleteAllMeetingRooms() {
        val initialEvent = prepareInitialEvent("", 0);
        val eventChangesInfoForExchange = prepareChanges(Optional.empty(), true);

        val changes = convertChanges(initialEvent, eventChangesInfoForExchange);

        assertThat(extractLocationFromListOfChanges(changes)).contains("");
    }

    /**
     * Textual location is not set and participants are the same => do not send location field to Exchange.
     * (we should not bother user without cause see CAL-10815)
     */
    @Test
    public void noLocationChanges() {
        val initialEvent = prepareInitialEvent("Before", 2);
        val eventChangesInfoForExchange = prepareChanges(Optional.empty(), false);

        val changes = convertChanges(initialEvent, eventChangesInfoForExchange);

        assertThat(extractLocationFromListOfChanges(changes)).isNotPresent(); //Location changes should not appear
    }

    /**
     * It should be possible to delete all meeting rooms and describe location textually.
     */
    @Test
    public void replaceMeetingRoomByTextualLocation() {
        val initialEvent = prepareInitialEvent("", 0);
        val newLocationText = "After";
        val eventChangesInfoForExchange = prepareChanges(Optional.of(newLocationText), true);

        val changes = convertChanges(initialEvent, eventChangesInfoForExchange);

        assertThat(extractLocationFromListOfChanges(changes)).contains(newLocationText);
    }

    /**
     * For consistency with Calendar. Now it do not store textual location for event with meeting rooms (see CAL-6280, CAL-7947).
     */
    @Test
    public void replaceTextualLocationByMeetingRooms() {
        val initialEvent = prepareInitialEvent("Before", 1);
        val eventChangesInfoForExchange = prepareChanges(Optional.empty(), true);

        val changes = convertChanges(initialEvent, eventChangesInfoForExchange);

        assertThat(extractLocationFromListOfChanges(changes)).contains("meetingroom0");
    }

    @Test
    public void withConferenceUrl() {
        String conferenceUrl = "http://url";
        val initialEvent = prepareInitialEvent("Before", 2);
        val eventChangesInfoForExchange = prepareChanges(Optional.empty(), true);
        eventChangesInfoForExchange.getEventChanges().setConferenceUrl(conferenceUrl);

        val changes = convertChanges(initialEvent, eventChangesInfoForExchange);

        Optional<String> changedConferenceUrl =  StreamEx.of(changes)
                .select(SetItemFieldType.class)
                .map(change -> change.getCalendarItem().getMeetingWorkspaceUrl())
                .nonNull().findFirst();
        assertThat(changedConferenceUrl.orElse(null)).isEqualTo(conferenceUrl);

        initialEvent.setConferenceUrl(conferenceUrl);
        val cal = eventToCalendarItemConverter.convertToCalendarItem(
                eventDbManager.getEventWithRelationsByEvent(initialEvent),
                eventDbManager.getEventAndRepetitionByEvent(initialEvent).getRepetitionInfo());

        assertThat(cal.getMeetingWorkspaceUrl()).isEqualTo(conferenceUrl);
    }

    /**
     * Extract new value of location field from prepared for Exchange information
     * @param changes list of changes for Exchange
     * @return new value of location field or null if it should not be updated now
     */
    private Optional<String> extractLocationFromListOfChanges(List<ItemChangeDescriptionType> changes) {
        return StreamEx.of(changes)
                .select(SetItemFieldType.class)
                .map(change -> change.getCalendarItem().getLocation())
                .nonNull().findFirst();
    }

    /**
     * Prepare event which was changed and should be passed to {@link EventToCalendarItemConverter#convertToChangeDescriptions(EventWithRelations, RepetitionInstanceInfo, EventChangesInfoForExchange)}.
     * @param textualLocation current textual location value
     * @param meetingRoomsAmount amount of meeting rooms to be attached to the event
     * @return prepared event
     */
    private Event prepareInitialEvent(String textualLocation, int meetingRoomsAmount) {
        val uid = testManager.prepareRandomYaTeamUser(454).getUid();
        val initialEvent = testManager.createDefaultEvent(uid, "useless event");
        initialEvent.setLocation(textualLocation);
        testManager.addUserParticipantToEvent(initialEvent.getId(), uid, Decision.YES, true);
        IntStream.range(0, meetingRoomsAmount)
                .mapToObj(i->"meetingroom"+i)
                .map(resourceName -> testManager.cleanAndCreateResource(resourceName, resourceName))
                .forEach(resource -> testManager.addResourceParticipantToEvent(initialEvent.getId(), resource));
        return initialEvent;
    }

    /**
     * Prepare description of changes for call of {@link EventToCalendarItemConverter#convertToChangeDescriptions(EventWithRelations, RepetitionInstanceInfo, EventChangesInfoForExchange)}
     * @param locationName new textual location (optional)
     * @param participantsChanged list of participants (which include meeting rooms) was changed
     * @return description of changes
     */
    private EventChangesInfoForExchange prepareChanges(Optional<String> locationName, boolean participantsChanged) {
        val changesEvent = new Event();
        locationName.ifPresent(changesEvent::setLocation);
        return new EventChangesInfoForExchange(changesEvent, new Repetition(), participantsChanged, Cf.list());
    }

    /**
     * Convert changes' description to list of field to be sent to Exchange.
     * @param initialEvent event to change
     * @param eventChangesInfoForExchange description of changes
     * @return description of changes to send to Exchange
     */
    private List<ItemChangeDescriptionType> convertChanges(Event initialEvent, EventChangesInfoForExchange eventChangesInfoForExchange) {
        return eventToCalendarItemConverter.convertToChangeDescriptions(
                eventDbManager.getEventWithRelationsByEvent(initialEvent),
                eventDbManager.getEventAndRepetitionByEvent(initialEvent).getRepetitionInfo(),
                eventChangesInfoForExchange);
    }
}
