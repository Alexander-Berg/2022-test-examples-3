package ru.yandex.calendar.logic.event.web;

import lombok.val;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.frontend.ews.imp.ExchangeEventDataConverter;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class EventWebManagerChangeOrganizerTest extends AbstractConfTest {
    private final LocalDate startDate = new LocalDate(2017, 9, 14);
    private final Instant startTs = startDate.toDateTime(new LocalTime(12, 0), MoscowTime.TZ).toInstant();
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private MailSenderMock mailSenderMock;
    @Value("${ews.domain}")
    private String ewsDomain;
    private TestUserInfo ewser1;
    private TestUserInfo ewser2;

    private TestUserInfo yndxoid1;
    private TestUserInfo yndxoid2;

    private Resource resource;
    private Email resourceEmail;

    @Before
    public void setup() {
        ewser1 = testManager.prepareRandomYaTeamUser(32211, "testuser2013");
        ewser2 = testManager.prepareRandomYaTeamUser(32212, "calendartestuser");

        yndxoid1 = testManager.prepareRandomYaTeamUser(32213);
        yndxoid2 = testManager.prepareRandomYaTeamUser(32214);

        resource = testManager.cleanAndCreateResourceWithNoExchSync("conf_xx", "XX", ResourceType.ROOM);
        resourceEmail = ResourceRoutines.getResourceEmail(resource);

        testManager.updateIsEwser(ewser1, ewser2);

        Cf.list(ewser1, ewser2).forEach(u ->
                ewsProxyWrapper.cancelMasterAndSingleMeetings(new Email(u.getLogin().toString() + "@" + ewsDomain),
                        startDate.toDateTimeAtStartOfDay(MoscowTime.TZ).toInstant(),
                        startDate.toDateTimeAtStartOfDay(MoscowTime.TZ).plusDays(4).toInstant()));
    }

    @Test
    public void singleFromYndxoidToYndxoid() {
        EventWithRelations event = createEvent(yndxoid1, createSingleEventData(yndxoid1, ewser1));

        event = updateEvent(yndxoid1, event, createSingleEventData(yndxoid2, ewser1));

        Assert.some(yndxoid2.getParticipantId(), event.getOrganizerIdSafe());

        Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(yndxoid1.getEmail()));
        Assert.some(MailType.EVENT_INVITATION, mailSenderMock.findEventMailType(yndxoid2.getEmail()));
        Assert.some(MailType.EVENT_UPDATE, mailSenderMock.findEventMailType(ewser1.getEmail()));
    }

    @Test
    public void singleFromYndxoidToEwser() {
        val created = createEvent(yndxoid1, createSingleEventData(yndxoid1, yndxoid2));

        val updated = updateEvent(yndxoid1, created, createSingleEventData(ewser1, yndxoid2));

        Assert.some(ewser1.getParticipantId(), updated.getOrganizerIdSafe());
        Assert.some(ewser1.getEmail(), findInExchange(ewser1, updated).get().getOrganizerEmail());

        Assert.none(eventDbManager.getEventByIdSafe(created.getId()));
        Assert.isTrue(updated.isExportedWithEws());

        Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(yndxoid1.getEmail()));
        Assert.some(MailType.EVENT_REORGANIZATION, mailSenderMock.findEventMailType(yndxoid2.getEmail()));
        Assert.some(mailSenderMock.findEventMailType(ewser1.getEmail()));
    }

    @Test
    public void singleFromEwserToEwser() {
        val created = createEvent(ewser1, createSingleEventData(ewser1, yndxoid1));

        Assert.some(findInExchange(ewser1, created));
        Assert.isTrue(created.isExportedWithEws());

        EventWithRelations updated = updateEvent(ewser1, created, createSingleEventData(ewser2, yndxoid1));

        Assert.some(ewser2.getParticipantId(), updated.getOrganizerIdSafe());
        Assert.some(ewser2.getEmail(), findInExchange(ewser2, updated).get().getOrganizerEmail());

        Assert.none(eventDbManager.getEventByIdSafe(created.getId()));
        Assert.none(findInExchange(ewser1, created));

        Assert.isTrue(updated.isExportedWithEws());
        Assert.sizeIs(1, mailSenderMock.getEventMailTypes());
    }

    @Test
    public void singleFromEwserToYndxoid() {
        val created = createEvent(ewser1, createSingleEventData(ewser1, yndxoid2));

        Assert.some(findInExchange(ewser1, created));
        Assert.isTrue(created.isExportedWithEws());

        EventWithRelations updated = updateEvent(ewser1, created, createSingleEventData(yndxoid1, yndxoid2));

        Assert.some(yndxoid1.getParticipantId(), updated.getOrganizerIdSafe());
        Assert.isFalse(updated.isExportedWithEws());

        Assert.none(eventDbManager.getEventByIdSafe(created.getId()));
        Assert.none(findInExchange(ewser1, created));

        Assert.some(MailType.EVENT_INVITATION, mailSenderMock.findEventMailType(yndxoid1.getEmail()));
        Assert.some(MailType.EVENT_UPDATE, mailSenderMock.findEventMailType(yndxoid2.getEmail()));
    }

    @Test
    public void recurrenceFromYndxoidToYndxoid() {
        val master = createEvent(yndxoid1, createRepeatingEventData(yndxoid1, ewser1));

        val recurrence = updateEvent(yndxoid1, master, createRecurrenceEventData(yndxoid2, ewser1));

        Assert.some(yndxoid2.getParticipantId(), recurrence.getOrganizerIdSafe());
        Assert.equals(master.getMainEventId(), recurrence.getMainEventId());

        Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(yndxoid1.getEmail()));
        Assert.some(MailType.EVENT_INVITATION, mailSenderMock.findEventMailType(yndxoid2.getEmail()));
        Assert.some(MailType.EVENT_UPDATE, mailSenderMock.findEventMailType(ewser1.getEmail()));
    }

    @Test
    public void recurrenceFromYndxoidToEwser() {
        val master = createEvent(yndxoid1, createRepeatingEventData(yndxoid1, yndxoid2));

        val recurrence = updateEvent(yndxoid1, master, createRecurrenceEventData(ewser1, yndxoid2));

        Assert.some(ewser1.getParticipantId(), recurrence.getOrganizerIdSafe());
        Assert.some(ewser1.getEmail(), findInExchange(ewser1, recurrence).get().getOrganizerEmail());

        Assert.equals(Cf.list(recurrence.getStartTs()),
                eventDbManager.getEventAndRepetitionByEvent(master.getEvent()).getRepetitionInfo().getExdateStarts());

        Assert.isTrue(recurrence.isExportedWithEws());
        Assert.notEquals(master.getMainEventId(), recurrence.getMainEventId());

        Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(yndxoid1.getEmail()));
        Assert.some(MailType.EVENT_REORGANIZATION, mailSenderMock.findEventMailType(yndxoid2.getEmail()));
        Assert.some(mailSenderMock.findEventMailType(ewser1.getEmail()));
    }

    @Test
    public void recurrenceFromEwserToEwser() {
        val master = createEvent(ewser1, createRepeatingEventData(ewser1, yndxoid1));

        Assert.isTrue(master.isExportedWithEws());

        val recurrence = updateEvent(ewser1, master, createRecurrenceEventData(ewser2, yndxoid1));

        Assert.some(ewser2.getParticipantId(), recurrence.getOrganizerIdSafe());
        Assert.some(ewser2.getEmail(), findInExchange(ewser2, recurrence).get().getOrganizerEmail());

        Assert.equals(Cf.list(recurrence.getStartTs()),
                findInExchange(ewser1, master).get().getRdates().map(Rdate::getStartTs));

        Assert.equals(Cf.list(recurrence.getStartTs()),
                eventDbManager.getEventAndRepetitionByEvent(master.getEvent()).getRepetitionInfo().getExdateStarts());

        Assert.isTrue(recurrence.isExportedWithEws());
        Assert.notEquals(master.getMainEventId(), recurrence.getMainEventId());

        Assert.sizeIs(1, mailSenderMock.getEventMailTypes());
    }

    @Test
    public void recurrenceFromEwserToYndxoid() {
        val master = createEvent(ewser1, createRepeatingEventData(ewser1, yndxoid2));

        Assert.isTrue(master.isExportedWithEws());

        EventWithRelations recurrence = updateEvent(ewser1, master, createRecurrenceEventData(yndxoid1, yndxoid2));

        Assert.some(yndxoid1.getParticipantId(), recurrence.getOrganizerIdSafe());

        Assert.equals(Cf.list(recurrence.getStartTs()),
                findInExchange(ewser1, master).get().getRdates().map(Rdate::getStartTs));

        Assert.equals(Cf.list(recurrence.getStartTs()),
                eventDbManager.getEventAndRepetitionByEvent(master.getEvent()).getRepetitionInfo().getExdateStarts());

        Assert.isFalse(recurrence.isExportedWithEws());
        Assert.notEquals(master.getMainEventId(), recurrence.getMainEventId());

        Assert.some(MailType.EVENT_INVITATION, mailSenderMock.findEventMailType(yndxoid1.getEmail()));
        Assert.some(MailType.EVENT_UPDATE, mailSenderMock.findEventMailType(yndxoid2.getEmail()));
    }

    @Test
    public void repeatingWithResourceFromEwserToYndxoid() {
        val data = createRepeatingEventData(ewser1, yndxoid2);
        data.setInvData(resourceEmail, data.getInvData().getParticipantEmails().toArray(Email.class));

        val created = createEvent(ewser1, data);
        Assert.isTrue(created.isExportedWithEws());

        data.setInvData(Option.of(yndxoid1.getEmail()), data.getInvData().getParticipantEmails().toArray(Email.class));

        val updated = updateEvent(ewser1, created, data);
        Assert.isFalse(updated.isExportedWithEws());

        Assert.none(eventDbManager.getEventByIdSafe(created.getId()));
        Assert.none(findInExchange(ewser1, created));

        Assert.some(MailType.EVENT_INVITATION, mailSenderMock.findEventMailType(yndxoid1.getEmail()));
        Assert.some(MailType.EVENT_UPDATE, mailSenderMock.findEventMailType(yndxoid2.getEmail()));
        Assert.some(MailType.EVENT_UPDATE, mailSenderMock.findEventMailType(ewser1.getEmail()));
    }

    @Test
    public void repeatingWithOnetimeParticipantFromYndxoidToEwser() {
        val data = createRepeatingEventData(yndxoid1, ewser1);

        val master = createEvent(yndxoid1, data);
        val recurrence = updateEvent(yndxoid1, master, createRecurrenceEventData(yndxoid1, yndxoid2, ewser1));

        data.setInvData(Option.of(ewser1.getEmail()), yndxoid1.getEmail());

        EventWithRelations updated = updateEvent(yndxoid1, master, data);

        Assert.isTrue(updated.isExportedWithEws());
        Assert.notEquals(master.getMainEventId(), updated.getMainEventId());

        Assert.some(findInExchange(ewser1, updated));

        Assert.some(MailType.EVENT_REORGANIZATION, mailSenderMock.findEventMailType(yndxoid1.getEmail()));
        Assert.some(MailType.EVENT_REORGANIZATION, mailSenderMock.findEventMailType(ewser1.getEmail()));

        Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(yndxoid2.getEmail()));
        Assert.some(mailSenderMock.findEventMailType(recurrence.getEvent().getId(), yndxoid2.getEmail()));
    }

    @Test
    public void tailFromYndxoidToEwser() {
        val created = createEvent(yndxoid1, createRepeatingEventData(yndxoid1, yndxoid2));
        Assert.isFalse(created.isExportedWithEws());

        val instanceStart = created.getStartTs().plus(Duration.standardDays(3));

        val data = createRepeatingEventData(ewser1, yndxoid2);
        data.setInstanceStartTs(instanceStart);
        data.getEvent().setStartTs(instanceStart);
        data.getEvent().setEndTs(instanceStart);

        val updated = updateEvent(yndxoid1, created, data, true);
        Assert.isTrue(updated.isExportedWithEws());

        Assert.some(eventDbManager.getEventByIdSafe(created.getId()));
        Assert.some(findInExchange(ewser1, updated));

        Assert.some(MailType.EVENT_UPDATE, mailSenderMock.findEventMailType(yndxoid1.getEmail()));
        Assert.some(MailType.EVENT_UPDATE, mailSenderMock.findEventMailType(yndxoid2.getEmail()));
        Assert.some(mailSenderMock.findEventMailType(ewser1.getEmail()));
    }


    private Option<EventData> findInExchange(TestUserInfo user, EventWithRelations event) {
        val exchangeId = event.getEventUsers().find(eu -> eu.getUid().sameAs(user.getUid()))
                .getOrThrow(user.getUid() + " not found for " + event.getId())
                .getExchangeId().getOrThrow("No exchange id for " + Tuple2.tuple(user.getUid(), event.getId()));

        return ewsProxyWrapper.getEvent(exchangeId).map(
                i -> ExchangeEventDataConverter.convert(i, UidOrResourceId.user(user.getUid()), Option.empty(), es -> es));
    }

    private EventData createSingleEventData(TestUserInfo organizer, TestUserInfo... attendees) {
        return createEventData("Single", startTs, organizer, attendees);
    }

    private EventData createRepeatingEventData(TestUserInfo organizer, TestUserInfo... attendees) {
        val data = createEventData("Repeating", startTs, organizer, attendees);
        data.setRepetition(TestManager.createDailyRepetitionTemplate());

        return data;
    }

    private EventData createRecurrenceEventData(TestUserInfo organizer, TestUserInfo... attendees) {
        val data = createEventData("Recurrence", startTs.plus(Duration.standardDays(1)), organizer, attendees);
        data.setInstanceStartTs(data.getEvent().getStartTs());

        return data;
    }

    private EventData createEventData(String name, Instant startTs, TestUserInfo organizer, TestUserInfo... attendees) {
        val data = new EventData();
        data.setEvent(TestManager.createDefaultEventTemplate(organizer.getUid(), name, Option.of(startTs)));
        data.setInvData(Option.of(organizer.getEmail()), Cf.x(attendees).map(TestUserInfo::getEmail).toArray(Email.class));

        return data;
    }

    private EventWithRelations createEvent(TestUserInfo actor, EventData eventData) {
        eventData.setTimeZone(MoscowTime.TZ);

        mailSenderMock.clear();

        val event = eventWebManager.createUserEvent(
                actor.getUid(), eventData, InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest(startTs)).getEvent();

        return eventDbManager.getEventWithRelationsByEvent(event);
    }

    private EventWithRelations updateEvent(TestUserInfo actor, EventWithRelations event, EventData updateData) {
        return updateEvent(actor, event, updateData, false);
    }

    private EventWithRelations updateEvent(
            TestUserInfo actor, EventWithRelations event, EventData updateData, boolean applyToFuture) {
        updateData.getEvent().setId(event.getId());
        updateData.setExternalId(Option.empty());

        mailSenderMock.clear();

        return eventDbManager.getEventWithRelationsById(
                eventWebManager.update(actor.getUserInfo(), updateData, applyToFuture, ActionInfo.webTest(startTs))
                        .getOrElse(event.getId()));
    }
}
