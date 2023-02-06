package ru.yandex.calendar.frontend.ews.proxy;

import javax.xml.datatype.DatatypeConfigurationException;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.ItemChangeDescriptionType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.exp.EventToCalendarItemConverter;
import ru.yandex.calendar.frontend.ews.exp.EwsModifyingItemId;
import ru.yandex.calendar.frontend.ews.imp.ExchangeEventDataConverter;
import ru.yandex.calendar.frontend.ews.imp.TestCalItemFactory;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.beans.generated.RepetitionFields;
import ru.yandex.calendar.logic.beans.generated.RepetitionHelper;
import ru.yandex.calendar.logic.event.EventChangesInfo;
import ru.yandex.calendar.logic.event.EventChangesInfoForExchange;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventChangesInfo.EventChangesInfoFactory;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.EventParticipantsChangesInfo;
import ru.yandex.calendar.logic.sharing.ParticipantChangesInfo;
import ru.yandex.calendar.logic.sharing.participant.ParticipantData;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class EwsProxyWrapperUpdateTest extends AbstractConfTest {
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EventToCalendarItemConverter eventToCalendarItemConverter;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private RepetitionRoutines repetitionRoutines;
    @Autowired
    private UserManager userManager;
    @Autowired
    private ResourceRoutines resourceRoutines;

    @Test
    public void updateEventFieldsTest() throws DatatypeConfigurationException {
        Email subscribedEmail = TestManager.testExchangeUserEmail;
        CalendarItemType calendarItem = TestCalItemFactory.createDefaultCalendarItemForExport(
                TestDateTimes.moscowDateTime(2009, 12, 1, 10, 0), "Subject (updateEventFieldsTest)");

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                subscribedEmail,
                EwsUtils.xmlGregorianCalendarInstantToInstant(calendarItem.getStart()),
                EwsUtils.xmlGregorianCalendarInstantToInstant(calendarItem.getEnd()));

        // don't forget to clean interval where updated events go to
        Instant newStart = TestDateTimes.moscow(2010, 11, 2, 10, 0).toInstant();
        Instant newEnd = TestDateTimes.moscow(2010, 11, 2, 12, 0).toInstant();
        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                subscribedEmail,
                newStart,
                newEnd);


        String exchangeId = ewsProxyWrapper.createEventSafe(subscribedEmail, calendarItem, EwsActionLogData.test()).get();

        Event eventChanges = new Event();
        eventChanges.setName("New subject (updateEventFieldsTest)");
        eventChanges.setStartTs(newStart);
        eventChanges.setEndTs(newEnd);
        eventChanges.setIsAllDay(false);
        eventChanges.setDescription("New description (updateEventFieldsTest)");
        eventChanges.setLocation("New location (updateEventFieldsTest)");

        EventChangesInfoFactory factory = new EventChangesInfoFactory();
        factory.setEventChanges(eventChanges);
        EventChangesInfo eventChangesInfo = factory.create();

        PassportUid organizerUid = testManager.prepareUser("yandex-team-mm-10210").getUid();
        long eventId = testManager.createDefaultEvent(organizerUid, "updateEventFieldsTest").getId();

        update(eventId, exchangeId, eventChangesInfo.toEventChangesInfoForExchange());

        CalendarItemType updatedCalendarItem = ewsProxyWrapper.getEvent(exchangeId).get();
        Assert.A.equals(eventChanges.getName(), updatedCalendarItem.getSubject());
        Assert.A.equals(eventChanges.getStartTs(), EwsUtils.xmlGregorianCalendarInstantToInstant(updatedCalendarItem.getStart()));
        Assert.A.equals(eventChanges.getEndTs(), EwsUtils.xmlGregorianCalendarInstantToInstant(updatedCalendarItem.getEnd()));
        Assert.A.equals(eventChanges.getIsAllDay(), updatedCalendarItem.isIsAllDayEvent());
        Assert.A.equals(eventChanges.getLocation(), updatedCalendarItem.getLocation());
        Assert.A.equals(eventChanges.getDescription(), updatedCalendarItem.getBody().getValue());
    }

    @Test
    public void updateAttendeesTest() throws DatatypeConfigurationException {
        Email subscribedEmail = TestManager.testExchangeUserEmail;
        CalendarItemType calendarItem = TestCalItemFactory.createDefaultCalendarItemForExport(
                TestDateTimes.moscowDateTime(2009, 12, 1, 10, 0), "updateAttendeesTest");

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                subscribedEmail,
                EwsUtils.xmlGregorianCalendarInstantToInstant(calendarItem.getStart()),
                EwsUtils.xmlGregorianCalendarInstantToInstant(calendarItem.getEnd()));

        TestUserInfo organizer = testManager.prepareYandexUser(TestManager.createYashunsky());
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10212");

        final Email attendeeEmail = attendee.getEmail();
        final Email organizerEmail = organizer.getEmail();
        final Email organizerExchangeEmail = userManager.getLdEmailByUid(organizer.getUid());

        String exchangeId = ewsProxyWrapper.createEventSafe(organizerExchangeEmail, calendarItem, EwsActionLogData.test()).get();

        long eventId = testManager.createDefaultEvent(organizer.getUid(), "Meeting (updateAttendeesTest)").getId();
        testManager.addUserParticipantToEvent(eventId, organizer.getLogin(), Decision.YES, true);
        testManager.addUserParticipantToEvent(eventId, attendee.getLogin(), Decision.UNDECIDED, false);

        Tuple2List<ParticipantId, ParticipantData> newParticipants = Tuple2List.arrayList();
        newParticipants.add(
                eventInvitationManager.getParticipantIdByEmail(attendeeEmail),
                new ParticipantData(attendeeEmail, null, Decision.UNDECIDED, true, false, false));
        EventParticipantsChangesInfo participantsChanges = EventParticipantsChangesInfo.changes(
                newParticipants,
                Tuple2List.<ParticipantId, ParticipantChangesInfo>tuple2List(),
                Cf.<ParticipantInfo>list());

        EventChangesInfoFactory factory = new EventChangesInfoFactory();
        factory.setEventParticipantsChangesInfo(participantsChanges);
        EventChangesInfo eventChangesInfo = factory.create();

        update(eventId, exchangeId, eventChangesInfo.toEventChangesInfoForExchange());

        CalendarItemType updatedCalendarItem = ewsProxyWrapper.getEvent(exchangeId).get();
        SetF<Email> actualEmails = ExchangeEventDataConverter.getAttendeeEmails(updatedCalendarItem).unique();
        Assert.equals(Cf.set(attendeeEmail), actualEmails);

        actualEmails = ExchangeEventDataConverter.getOrganizerEmailSafe(updatedCalendarItem).unique();
        Assert.equals(Cf.set(organizerEmail), actualEmails);
    }

    @Test
    public void updateToNotMeetingTest() throws DatatypeConfigurationException {
        Email subscribedEmail = TestManager.testExchangeUserEmail;
        CalendarItemType calItemWithAttendee = TestCalItemFactory.createDefaultCalendarItemForExport(
                TestDateTimes.moscowDateTime(2009, 12, 1, 10, 0), "updateToNotMeetingTest");
        final Email attendeeEmail = new Email("yandex-team-mm-10214@yandex.ru");
        TestCalItemFactory.addAttendee(calItemWithAttendee, attendeeEmail, ResponseTypeType.ACCEPT);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                subscribedEmail,
                EwsUtils.xmlGregorianCalendarInstantToInstant(calItemWithAttendee.getStart()),
                EwsUtils.xmlGregorianCalendarInstantToInstant(calItemWithAttendee.getEnd()));

        String exchangeId = ewsProxyWrapper.createEventSafe(subscribedEmail, calItemWithAttendee, EwsActionLogData.test()).get();

        PassportUid organizerUid = testManager.prepareUser("yandex-team-mm-10213").getUid();

        long eventId = testManager.createDefaultEvent(organizerUid, "None meeting").getId();

        update(eventId, exchangeId, new EventChangesInfoForExchange(new Event(), new Repetition(), true, Cf.<Instant>list()));

        CalendarItemType calendarItem = ewsProxyWrapper.getEvent(exchangeId).get();
        SetF<Email> emails = ExchangeEventDataConverter.getAttendeeEmails(calendarItem).unique();
        Assert.assertTrue(emails.isEmpty());
    }

    @Test
    public void updateRepetition() throws DatatypeConfigurationException {
        PassportUid organizerUid = testManager.prepareUser("yandex-team-mm-10213").getUid();

        long eventId = testManager.createDefaultEvent(organizerUid, "updateRepetition").getId();
        testManager.createDailyRepetitionAndLinkToEvent(eventId);
        Event event = eventDao.findEventById(eventId);
        Repetition repetition = eventDao.findRepetitionById(event.getRepetitionId().get()).copy();
        repetition.unsetField(RepetitionFields.ID);

        Email subscribedEmail = TestManager.testExchangeUserEmail;
        CalendarItemType calendarItem = TestCalItemFactory.createDefaultCalendarItemForExport(
                new DateTime(event.getStartTs(), MoscowTime.TZ), "updateRepetition");

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                subscribedEmail, event.getStartTs(), event.getEndTs());

        String exchangeId = ewsProxyWrapper.createEventSafe(subscribedEmail, calendarItem, EwsActionLogData.test()).get();

        EventChangesInfoFactory factory = new EventChangesInfoFactory();
        factory.setRepetitionChanges(repetition);
        EventChangesInfo eventChangesInfo = factory.create();

        update(eventId, exchangeId, eventChangesInfo.toEventChangesInfoForExchange());

        CalendarItemType updatedCalendarItem = ewsProxyWrapper.getEvent(exchangeId).get();
        Repetition updatedRepetition = ExchangeEventDataConverter
                .convert(updatedCalendarItem, UidOrResourceId.user(organizerUid),
                        Option.empty(), resourceRoutines::selectResourceEmails)
                .getRepetition();
        Repetition changes = RepetitionHelper.INSTANCE.findChanges(repetition, updatedRepetition);
        Assert.assertTrue(changes.isEmpty());
        Assert.equals(event.getStartTs(), EwsUtils.xmlGregorianCalendarInstantToInstant(updatedCalendarItem.getStart()));
    }

    @Test
    public void updateToNoneRepetition() throws DatatypeConfigurationException {
        Email subscribedEmail = TestManager.testExchangeUserEmail;
        CalendarItemType calendarItem = TestCalItemFactory.createDefaultCalendarItemForExport(
                TestDateTimes.moscowDateTime(2009, 12, 1, 10, 0), "updateToNoneRepetition");
        TestCalItemFactory.addDailyRecurrence(calendarItem);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                subscribedEmail,
                EwsUtils.xmlGregorianCalendarInstantToInstant(calendarItem.getStart()),
                EwsUtils.xmlGregorianCalendarInstantToInstant(calendarItem.getEnd()));

        String exchangeId = ewsProxyWrapper.createEventSafe(subscribedEmail, calendarItem, EwsActionLogData.test()).get();

        PassportUid organizerUid = testManager.prepareUser("yandex-team-mm-10216").getUid();

        Event event = testManager.createDefaultEvent(organizerUid, "updateToNoneRepetition");

        EventChangesInfoFactory factory = new EventChangesInfoFactory();
        factory.setRepetitionChanges(RepetitionRoutines.createNoneRepetition());
        EventChangesInfo eventChangesInfo = factory.create();

        update(event.getId(), exchangeId, eventChangesInfo.toEventChangesInfoForExchange());

        CalendarItemType updatedCalendarItem = ewsProxyWrapper.getEvent(exchangeId).get();
        Assert.assertTrue(updatedCalendarItem.getRecurrence() == null);
    }

    private void update(long eventId, String exchangeId, EventChangesInfoForExchange changesInfo) {
        EventWithRelations event = eventDbManager.getEventWithRelationsById(eventId);
        RepetitionInstanceInfo repetitionInfo = repetitionRoutines.getRepetitionInstanceInfo(event);

        ListF<ItemChangeDescriptionType> changeDescriptions = eventToCalendarItemConverter.convertToChangeDescriptions(
                event, repetitionInfo, changesInfo);

        ewsProxyWrapper.updateItem(EwsModifyingItemId.fromExchangeId(exchangeId), changeDescriptions, EwsActionLogData.test());
    }
}
