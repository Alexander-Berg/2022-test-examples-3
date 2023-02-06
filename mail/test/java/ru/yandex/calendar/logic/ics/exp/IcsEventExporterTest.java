package ru.yandex.calendar.logic.ics.exp;

import java.util.List;
import java.util.function.BiFunction;

import lombok.val;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Conference;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.caldav.proto.caldav.report.TimeRange;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.domain.PassportAuthDomainsHolder;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.imp.IcsImportMode;
import ru.yandex.calendar.logic.ics.imp.IcsImporter;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsSuitableTimeZoneFinder;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsComponent;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVTimeZone;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.util.base.Cf2;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.regex.Pattern2;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

public class IcsEventExporterTest extends AbstractDbDataTest {
    @Autowired
    private IcsEventExporter icsEventExporter;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private PassportAuthDomainsHolder passportAuthDomainsHolder;

    @Test
    public void eventWithRdate() {
        val t = testManager.createEventWithRdate(TestManager.UID);
        val event = t._1;

        val layerId = eventLayerDao.findEventLayersByEventId(t._1.getId()).single().getLayerId();
        val res = icsEventExporter.exportCalendar(
                TestManager.UID, layerId, TestDateTimes.moscow(2011, 7, 6, 15, 36), ActionSource.WEB);
        val ve = res.getEvents().first();
        val tzs = IcsVTimeZones.cons(res.getTimezones(), MoscowTime.TZ, false);

        assertThat(ve.getStart().getInstant(tzs)).isEqualTo(event.getStartTs());
        assertThat(ve.getEnd().getInstant(tzs)).isEqualTo(event.getEndTs());

        val rdate = ve.getRDates().single();
        val actual = rdate.getIntervals().single().getStartIcsDateTime().getInstant(tzs);
        assertThat(actual).isEqualTo(t._2.getStartTs());
        assertThat(ve.getExDates()).isEmpty();

        val icsRecurrenceId = ve.getRecurrenceId();
        assertThat(icsRecurrenceId).isEmpty();
    }

    @Test
    public void eventWithExdate() {
        val t = testManager.createEventWithRepetitionAndExdate(TestManager.UID);
        val event = t._1;
        val exdate = t._2;

        val layerId = eventLayerDao.findEventLayersByEventId(event.getId()).single().getLayerId();
        val res = icsEventExporter.exportCalendar(
                TestManager.UID, layerId, TestDateTimes.moscow(2011, 7, 6, 15, 36), ActionSource.WEB);
        val ve = res.getEvents().first();
        val tzs = IcsVTimeZones.cons(res.getTimezones(), MoscowTime.TZ, false);

        assertThat(ve.getStart().getInstant(tzs)).isEqualTo(event.getStartTs());
        assertThat(ve.getEnd().getInstant(tzs)).isEqualTo(event.getEndTs());
        assertThat(ve.getRDates()).isEmpty();

        val icsExdate = ve.getExDates().single();
        val actual = icsExdate.getIcsDateTimes().single().getInstant(tzs);
        assertThat(icsExdate.getTzId().toOptional()).hasValue(MoscowTime.TZ.getID());
        assertThat(actual).isEqualTo(exdate.getStartTs());

        val icsRecurrenceId = ve.getRecurrenceId();
        assertThat(icsRecurrenceId).isEmpty();
    }

    @Test
    public void eventWithRecurInst() {
        val t = testManager.createEventWithRepetitionAndRecurrence(TestManager.UID);
        val recurrence = t._2;
        val tz = DateTimeZone.forID("Asia/Yekaterinburg");
        mainEventDao.updateTimezoneIdById(recurrence.getMainEventId(), tz.getID());

        val recurrenceWithRelations = eventDbManager.getEventWithRelationsById(recurrence.getId());
        val repetitionInfo = RepetitionInstanceInfo.noRepetition(
                new InstantInterval(recurrence.getStartTs(), recurrence.getEndTs()));

        val calendar = icsEventExporter.exportEvent(
                TestManager.UID, recurrenceWithRelations, repetitionInfo, Option.empty(),
                EventInstanceParameters.fromEvent(recurrence), exportParams());

        val tzs = IcsVTimeZones.cons(calendar.getTimezones(), tz, false);

        val recurId = recurrence.getRecurrenceId().get();

        val ve = calendar.getEvents().first();
        assertThat(ve.getDtStart().get().getInstant(tzs)).isEqualTo(recurrence.getStartTs());
        assertThat(ve.getDtEnd().get().getInstant(tzs)).isEqualTo(recurrence.getEndTs());
        assertThat(ve.getStart().getTzId().toOptional()).hasValue(tz.getID());

        val icsRecurrenceId = ve.getRecurrenceId();
        assertThat(icsRecurrenceId.get().getInstant(tzs)).isEqualTo(recurId);
        assertThat(icsRecurrenceId.get().getIcsDateTime().getTzId().toOptional()).hasValue(tz.getID());
    }

    @Test
    public void resourceNamesAsLocation() {
        val organizer = testManager.prepareUser("yandex-team-mm-12310");

        val event = testManager.createDefaultEvent(organizer.getUid(), "Exported event");

        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        val abc = testManager.cleanAndCreateResource("acb", "abc") ;
        val def = testManager.cleanAndCreateResource("def", "def") ;
        testManager.addResourceParticipantToEvent(event.getId(), abc);
        testManager.addResourceParticipantToEvent(event.getId(), def);

        val eventWithRelations = eventDbManager.getEventWithRelationsById(event.getId());
        val repetitionInfo = RepetitionInstanceInfo.noRepetition(
                new InstantInterval(event.getStartTs(), event.getEndTs()));

        val calendar = icsEventExporter.exportEvent(
                organizer.getUid(), eventWithRelations, repetitionInfo, Option.empty(),
                EventInstanceParameters.fromEvent(event), exportParams());

        val vevent = calendar.getEvents().first();
        assertThat(vevent.getLocation()).contains("abc, def");
    }

    @Test
    public void resourceNamesWithLocation() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(787);

        Resource resource1 = testManager.cleanAndCreateResource("resource1", "Resource 1");
        Resource resource2 = testManager.cleanAndCreateResource("resource2", "Resource 2");

        Event data = new Event();
        data.setLocation("Location");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "Exported event", data);
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        testManager.addResourceParticipantToEvent(event.getId(), resource1);
        testManager.addResourceParticipantToEvent(event.getId(), resource2);

        IcsCalendar calendar = passportAuthDomainsHolder.withDomainsForTest("public",
                () -> icsEventExporter.exportEvent(organizer.getUid(),
                        eventDbManager.getEventWithRelationsById(event.getId()),
                        RepetitionInstanceInfo.noRepetition(EventRoutines.getInstantInterval(event)),
                        Option.empty(), EventInstanceParameters.fromEvent(event), exportParams()));

        assertThat(calendar.getEvents().single().getLocation())
                .contains("Resource 1, Resource 2, Location");
    }

    // CAL-6524
    @Test
    public void timeZoneDefinitionFirst() {
        val user = testManager.prepareUser("yandex-team-mm-12321");

        val event = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "timeZoneDefinitionFirst");
        val eventWithRelations = eventDbManager.getEventWithRelationsById(event.getId());
        val repetitionInfo = RepetitionInstanceInfo.noRepetition(
                new InstantInterval(event.getStartTs(), event.getEndTs()));

        val calendar = icsEventExporter.exportEvent(
                user.getUid(), eventWithRelations, repetitionInfo, Option.empty(),
                EventInstanceParameters.fromEvent(event), exportParams());

        assertThat(calendar.getEvents()).hasSize(1);
        assertThat(calendar.getComponents().map(IcsComponent::getName)).containsExactly("VTIMEZONE", "VEVENT");
    }

    @Test
    public void etcGmt() {
        val user = testManager.prepareUser("yandex-team-mm-12321");
        val tzOffsetHours = 3;
        val tz = DateTimeZone.forOffsetHours(tzOffsetHours);

        val event = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "etcGmt");
        testManager.updateEventTimezone(event.getId(), tz);

        val eventWithRelations = eventDbManager.getEventWithRelationsById(event.getId());
        val repetitionInfo = RepetitionInstanceInfo.noRepetition(
                new InstantInterval(event.getStartTs(), event.getEndTs()));

        val calendar = icsEventExporter.exportEvent(
                user.getUid(), eventWithRelations, repetitionInfo, Option.empty(),
                EventInstanceParameters.fromEvent(event), exportParams());

        val expectedTzId = "Etc/GMT" + -tzOffsetHours;
        assertThat(calendar.getEvents().single().getDtStart().get().getTzId().toOptional()).hasValue(expectedTzId);

        val icsTz = calendar.getTimezones().find(
                Cf2.f(IcsVTimeZone::getTzId).andThenEquals(expectedTzId));
        assertThat(icsTz).isNotEmpty();
        assertThat(IcsSuitableTimeZoneFinder.getOffset(icsTz.get().toComponent(), LocalDateTime.now(MoscowTime.TZ)))
                .isEqualTo(tzOffsetHours * DateTimeConstants.MILLIS_PER_HOUR);
    }

    private static IcsExportParameters exportParams() {
        return new IcsExportParameters(
                IcsExportMode.CALDAV, IcsMethod.PUBLISH, true, TestDateTimes.moscow(2011, 7, 6, 15, 36));
    }

    @Test
    public void serializeAllDayEvent() {
        val user = testManager.prepareUser("yandex-team-mm-12331");

        val allDay = new Event();
        allDay.setStartTs(TestDateTimes.moscow(2011, 4, 21, 0, 0));
        allDay.setEndTs(TestDateTimes.moscow(2011, 4, 22, 0, 0));
        allDay.setIsAllDay(true);

        val event = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "allDayEvent", allDay);

        val layerId = eventLayerDao.findEventLayersByEventId(event.getId()).single().getLayerId();
        val calendar = icsEventExporter.exportCalendar(
                user.getUid(), layerId, TestDateTimes.moscow(2011, 7, 6, 15, 36), ActionSource.WEB);

        val dtStart = Pattern2.compile("\\r\\nDTSTART;VALUE=DATE:(.*?)\\r\\n")
                .findFirstGroup(calendar.serializeToString()).get();
        val dtEnd = Pattern2.compile("\\r\\nDTEND;VALUE=DATE:(.*?)\\r\\n")
                .findFirstGroup(calendar.serializeToString()).get();

        assertThat(dtStart).isEqualTo("20110421");
        assertThat(dtEnd).isEqualTo("20110422");
    }

    @Test
    public void eventWithConferenceUrl() {
        val eventOverrides = new Event();
        eventOverrides.setConferenceUrl(Option.of("http://url"));
        val event = testManager.createDefaultEventWithEventLayerAndEventUser(TestManager.UID, "test", eventOverrides);
        final ListF<EventLayer> eventLayersByEventId = eventLayerDao.findEventLayersByEventId(event.getId());
        val layerId = eventLayersByEventId.single().getLayerId();
        val res = icsEventExporter.exportCalendar(
                TestManager.UID, layerId, TestDateTimes.moscow(2011, 7, 6, 15, 36), ActionSource.WEB);
        val ve = res.getEvents().first();

        assertThat(ve.getPropertyValue(Conference.PROPERTY_NAME).getOrNull()).isEqualTo("http://url");
    }

    // CAL-6294
    @Test
    public void importExportedEventUrlAndLocationAndRoomPhones() {
        val user = testManager.prepareRandomYaTeamUser(589);
        val resourceWithPhone = testManager.cleanAndCreateResourceWithNoSyncWithExchange("rr_1", "Resource 1" );
        val resourceWithNoPhone = testManager.cleanAndCreateResourceWithNoSyncWithExchange("rr_2", "Resource 2" );
        val resourceWithVideo = testManager.cleanAndCreateResourceWithNoSyncWithExchange("rr_3", "Resource 3" );
        val resourceAllInclusive = testManager.cleanAndCreateResourceWithNoSyncWithExchange("rr_4", "Resource 4" );

        val e = testManager.createDefaultEvent(user.getUid(), "Event with resources");
        testManager.addUserParticipantToEvent(e.getId(), user.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(e.getId(), resourceWithPhone);
        testManager.addResourceParticipantToEvent(e.getId(), resourceWithNoPhone);
        testManager.addResourceParticipantToEvent(e.getId(), resourceAllInclusive);
        testManager.addResourceParticipantToEvent(e.getId(), resourceWithVideo);

        resourceWithNoPhone.setPhoneNull();
        resourceWithPhone.setPhone(589);
        resourceWithVideo.setVideo(127);
        resourceAllInclusive.setPhone(259);
        resourceAllInclusive.setVideo(119);

        resourceDao.updateResource(resourceWithNoPhone);
        resourceDao.updateResource(resourceWithPhone);
        resourceDao.updateResource(resourceWithVideo);
        resourceDao.updateResource(resourceAllInclusive);

        e.setUrlNull();
        e.setLocation("");
        e.setDescription("Some description");
        eventDao.updateEvent(e);

        val event = eventDbManager.getEventWithRelationsById(e.getId());
        val repetition = RepetitionInstanceInfo.noRepetition(EventRoutines.getInstantInterval(e));

        val calendar = icsEventExporter.exportEvent(
                user.getUid(), event, repetition, Option.empty(), EventInstanceParameters.fromEvent(e),
                new IcsExportParameters(
                        IcsExportMode.CALDAV, IcsMethod.PUBLISH, true, event.getEvent().getLastUpdateTs().plus(589)));

        val stats = icsImporter.importIcsStuff(
                user.getUid(), calendar, IcsImportMode.caldavPutToDefaultLayerForTest());

        val vevent = calendar.getEvents().single();
        val updatedEvent = eventDao.findEventById(event.getId());

        assertThat(stats.getUpdatedEventIds()).hasSize(1);

        assertThat(vevent.getPropertyValue(Property.URL)).isNotEqualTo(event.getEvent().getUrl());
        assertThat(updatedEvent.getUrl()).isEqualTo(event.getEvent().getUrl());

        assertThat(vevent.getLocation().get()).isNotEqualTo(event.getEvent().getLocation());
        assertThat(updatedEvent.getLocation()).isEqualTo(event.getEvent().getLocation());

        assertThat(vevent.getDescription().get()).isNotEqualTo(event.getEvent().getDescription());
        assertThat(updatedEvent.getDescription()).isEqualTo(event.getEvent().getDescription());
    }

    @Test
    public void excludedRecurrence() {
        val user1 = testManager.prepareRandomYaTeamUser(589);
        val user2 = testManager.prepareRandomYaTeamUser(689);

        val masterAndRecur = testManager.createEventWithRepetitionAndRecurrence(TestManager.UID);
        testManager.addUserParticipantToEvent(masterAndRecur.get1().getId(), user1.getUid(), Decision.YES, false);

        val opts = new IcsExportOptions(true, true);

        BiFunction<PassportUid, Long, List<IcsEventGroupExportData>> exportF = (uid, layerId) ->
                icsEventExporter.exportEventsOnLayerForCaldav(uid, layerId, opts, TimeRange.unlimited(), Instant.now());


        val heavy1 = exportF.apply(user1.getUid(), user1.getDefaultLayerId()).iterator().next().heavy();
        assertThat(heavy1.getVevents()).hasSize(1);
        assertThat(heavy1.getVevents().single().getExDates()).hasSize(1);

        testManager.createEventUser(user1.getUid(), masterAndRecur.get2().getId(), Decision.NO, Option.empty());

        val heavy2 = exportF.apply(user1.getUid(), user1.getDefaultLayerId()).iterator().next().heavy();

        assertThat(heavy2.getVevents()).hasSize(1);
        assertThat(heavy2.getVevents().single().getExDates()).hasSize(1);

        testManager.addUserParticipantToEvent(masterAndRecur.get1().getId(), user2.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(masterAndRecur.get2().getId(), user2.getUid(), Decision.YES, false);

        assertThat(exportF.apply(user2.getUid(), user1.getDefaultLayerId()).iterator().next().heavy().getVevents()).hasSize(1);
        assertThat(exportF.apply(user2.getUid(), user2.getDefaultLayerId()).iterator().next().heavy().getVevents()).hasSize(2);
    }

    private List<IcsEventGroupExportData> export(PassportUid uid, long layerId) {
        val opts = new IcsExportOptions(false, true);
        return icsEventExporter.exportEventsOnLayerForCaldav(uid, layerId, opts, TimeRange.unlimited(), Instant.now());
    }

    @Test
    public void invisibleLightGroups() {
        val organizer = testManager.prepareRandomYaTeamUser(589);
        val attendee = testManager.prepareRandomYaTeamUser(689);
        val subscriber = testManager.prepareRandomYaTeamUser(789);

        val event = testManager.createDefaultEvent(organizer.getUid(), "excluded", new Event(), true);

        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        assertThat(export(organizer.getUid(), organizer.getDefaultLayerId())).hasSize(1);
        assertThat(export(subscriber.getUid(), organizer.getDefaultLayerId())).isEmpty();

        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.NO, false);

        assertThat(export(attendee.getUid(), attendee.getDefaultLayerId())).hasSize(1);
        assertThat(export(organizer.getUid(), attendee.getDefaultLayerId())).isEmpty();

        testManager.createEventUser(subscriber.getUid(), event.getId(), Decision.NO, Option.empty());
        testManager.createEventLayer(subscriber.getDefaultLayerId(), event.getId());

        assertThat(export(subscriber.getUid(), organizer.getDefaultLayerId())).hasSize(1);
        assertThat(export(subscriber.getUid(), subscriber.getDefaultLayerId())).isEmpty();
        assertThat(export(organizer.getUid(), subscriber.getDefaultLayerId())).isEmpty();
    }
}
