package ru.yandex.calendar.logic.notification.xiva;

import javax.annotation.PostConstruct;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.webNew.dto.in.WebEventData;
import ru.yandex.calendar.frontend.webNew.dto.inOut.RepetitionData;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.ModificationInfo;
import ru.yandex.calendar.logic.event.WebNewEventDataConverter;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.repetition.EventAndRepetition;
import ru.yandex.calendar.logic.event.web.EventWebManager;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;

class TestEventManager {
    @Autowired
    public EventWebManager eventWebManager;
    @Autowired
    public TestManager testManager;
    @Autowired
    private EventTestConverter converter;

    public DateTimeZone defaultTimeZone = DateTimeZone.UTC;
    public TestUserInfo defaultOrganizer;
    public TestUserInfo defaultCreator;
    public final String defaultEventName = "test_event_name";

    @PostConstruct
    public void setup() {
        defaultOrganizer = testManager.prepareRandomYaTeamUser(1, "default_organizer");
        defaultCreator = testManager.prepareRandomYaTeamUser(2, "default_creator");
    }

    public CreateInfo createAbsence(DateTime startTs) {
        return createSingleEvent(startTs, EventType.DUTY);
    }

    public CreateInfo createSingleEvent(DateTime startTs) {
        return createSingleEvent(startTs, EventType.USER);
    }

    public CreateInfo createSingleEvent(DateTime startTs, EventType type) {
        DateTime endTs = startTs.plusHours(1);
        WebDateTime webDateStartTs = WebDateTime.dateTime(startTs);
        WebDateTime webDateEndTs = WebDateTime.dateTime(endTs);

        WebEventData webData = WebEventData.empty();
        webData.setName(Option.of(defaultEventName));
        webData.setStartTs(Option.of(webDateStartTs));
        webData.setEndTs(Option.of(webDateEndTs));
        webData.setOrganizer(Option.of(defaultOrganizer.getEmail()));
        webData.setType(Option.of(type));
        EventData data = WebNewEventDataConverter.convert(webData, defaultTimeZone, defaultTimeZone);

        return eventWebManager.createUserEvent(defaultCreator.getUid(), data, InvitationProcessingMode.SAVE_ONLY, ActionInfo.webTest());
    }

    public CreateInfo createSeriesEvent(DateTime startTs, Repetition repetition) {
        DateTime endTs = startTs.plusHours(1);
        WebDateTime webDateTimeStartTs = WebDateTime.dateTime(startTs);
        WebDateTime webDateTimeEndTs = WebDateTime.dateTime(endTs);

        RepetitionData repetitionData = RepetitionData.fromRepetition(repetition, defaultTimeZone);

        WebEventData webData = WebEventData.empty();
        webData.setName(Option.of(defaultEventName));
        webData.setStartTs(Option.of(webDateTimeStartTs));
        webData.setEndTs(Option.of(webDateTimeEndTs));
        webData.setOrganizer(Option.of(defaultOrganizer.getEmail()));
        webData.setRepetition(Option.of(repetitionData));
        EventData data = WebNewEventDataConverter.convert(webData, defaultTimeZone, defaultTimeZone);

        return eventWebManager.createUserEvent(defaultCreator.getUid(), data, InvitationProcessingMode.SAVE_ONLY, ActionInfo.webTest());
    }

    public ModificationInfo moveSingleEventTo(CreateInfo createdEvent, DateTime targetStartTs) {
        EventData eventData = converter.convert(createdEvent);
        DateTime targetEndTs = targetStartTs.plusHours(1);

        Event event = eventData.getEvent();
        event.setStartTs(targetStartTs.toInstant());
        event.setEndTs(targetEndTs.toInstant());

        return eventWebManager.update(defaultOrganizer.getUserInfo(), eventData, Option.empty(), false, ActionInfo.webTest());
    }

    public ModificationInfo changeRepetition(CreateInfo createdEvent, Repetition repetition) {
        EventData eventData = converter.convert(createdEvent);
        eventData.setRepetition(repetition);
        eventData.setInstanceStartTs(createdEvent.getEvent().getStartTs());

        return eventWebManager.update(defaultOrganizer.getUserInfo(), eventData, Option.empty(), true, ActionInfo.webTest());
    }

    public ModificationInfo moveOccurenceTo(CreateInfo createdEvent, DateTime instanceStartTs, DateTime targetStartTs) {
        EventData eventData = converter.convert(createdEvent);
        DateTime targetEndTs = targetStartTs.plusHours(1);

        Event event = eventData.getEvent();
        event.setStartTs(targetStartTs.toInstant());
        event.setEndTs(targetEndTs.toInstant());
        eventData.setInstanceStartTs(instanceStartTs.toInstant());

        return eventWebManager.update(defaultOrganizer.getUserInfo(), eventData, Option.empty(), false, ActionInfo.webTest());
    }

    public ModificationInfo moveRecurrenceTo(EventAndRepetition createdEvent, DateTime targetStartTs) {
        EventData eventData = converter.convert(createdEvent);
        DateTime targetEndTs = targetStartTs.plusHours(1);

        Event event = eventData.getEvent();
        event.setStartTs(targetStartTs.toInstant());
        event.setEndTs(targetEndTs.toInstant());

        return eventWebManager.update(defaultOrganizer.getUserInfo(), eventData, Option.empty(), false, ActionInfo.webTest());
    }

    public ModificationInfo changeOccurenceEventName(CreateInfo createdEvent, DateTime instanceStartTs, String newName) {
        EventData eventData = converter.convert(createdEvent);

        Event event = eventData.getEvent();
        event.setName(newName);
        eventData.setInstanceStartTs(instanceStartTs.toInstant());

        return eventWebManager.update(defaultOrganizer.getUserInfo(), eventData, Option.empty(), false, ActionInfo.webTest());
    }

    public ModificationInfo deleteSingleEvent(long eventId) {
        return eventWebManager.deleteEvent(defaultOrganizer.getUserInfo(), eventId, Option.empty(), false, ActionInfo.webTest());
    }

    public ModificationInfo deleteOccurence(long eventId, DateTime instanceStartTs) {
        return eventWebManager.deleteEvent(defaultOrganizer.getUserInfo(), eventId, Option.of(instanceStartTs.toInstant()), false, ActionInfo.webTest());
    }

    public ModificationInfo deleteSeriesEvent(long eventId) {
        return eventWebManager.deleteEvent(defaultOrganizer.getUserInfo(), eventId, Option.empty(), true, ActionInfo.webTest());
    }
}
