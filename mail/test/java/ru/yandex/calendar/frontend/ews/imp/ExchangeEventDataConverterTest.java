package ru.yandex.calendar.frontend.ews.imp;

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;

import com.microsoft.schemas.exchange.services._2006.types.AttendeeType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.EmailAddressType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfAttendeesType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.ExchangeRepetitionEnd;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantData;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.lang.Validate;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author ssytnik
 */
public class ExchangeEventDataConverterTest extends AbstractConfTest {

    @Autowired
    private ResourceRoutines resourceRoutines;

    @Test
    public void convertNumberedRecurrenceWithExdateAndRecurrenceId() throws DatatypeConfigurationException {
        DateTime start = TestDateTimes.moscowDateTime(2010, 12, 10, 15, 0);
        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                start, "Event with numbered recurrence, exdate and recurrence-id");
        int occurrences = 5;
        TestCalItemFactory.addDailyRecurrenceWithEnd(calItem, ExchangeRepetitionEnd.count(occurrences), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        TestCalItemFactory.addExdate(calItem, start.plusDays(1).toInstant());
        TestCalItemFactory.addRecurrenceId(calItem, start.plusDays(2).toInstant());

        UidOrResourceId subjectId = UidOrResourceId.user(PassportUid.cons(2909)); // yandex-team-mm-10549
        EventData data = ExchangeEventDataConverter.convert(
                calItem, subjectId, Option.<Email>empty(), resourceRoutines::selectResourceEmails);

        DateTime expectedLastItemDt = start.plusDays(occurrences - 1); // inclusive
        Instant expectedDueTs = expectedLastItemDt.plusDays(1).toInstant(); // exclusive
        Instant convertedDueTs = data.getRepetition().getDueTs().get(); // exclusive
        Assert.A.equals(expectedDueTs, convertedDueTs);
    }

    // exchangeId=AAMkAGQ2NWUzMTJmLWExYmYtNGE1MC04OWU1LTFjMzZjMmE2NTY3OQBGAAAAAACz1nTsaCPnRLiukBYD5mIhBwADgrhfiG9JRZA7CbDXJVkeAAAAajwAAADR/ho/IeDvR45XVeBF9OW2AAAFPwiNAAA=
    /**
     * @url http://wiki.yandex-team.ru/calendar/cantfix
     */
    @Ignore
    @Test
    public void convertAllDayEventWithDurationLessThan1Day() throws DatatypeConfigurationException {
        String name = "convertAllDayEventWithDurationLessThan1Day";
        DateTime start = TestDateTimes.moscowDateTime(2009, 8, 5, 0, 0);
        DateTime end = start.plusMinutes(5);

        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForImport(start, name);
        calItem.setIsAllDayEvent(true);
        DateTimeZone zone = EwsUtils.getOrDefaultZone(calItem);
        calItem.setEnd(EwsUtils.instantToXMLGregorianCalendar(end.toInstant(), zone));

        UidOrResourceId subjectId = UidOrResourceId.user(PassportUid.cons(2910)); // yandex-team-mm-10550
        ExchangeEventDataConverter.convert(
                calItem, subjectId, Option.<Email>empty(), resourceRoutines::selectResourceEmails);
    }

    private AttendeeType createAttendeeType(String email) {
        EmailAddressType emailAddressType = new EmailAddressType();
        emailAddressType.setEmailAddress(email);
        AttendeeType attendeeType = new AttendeeType();
        attendeeType.setMailbox(emailAddressType);
        return attendeeType;
    }

    @Test
    public void getAttendeeEmails() {
        NonEmptyArrayOfAttendeesType attendees = new NonEmptyArrayOfAttendeesType() {
            public List<AttendeeType> getAttendee() {
                return Cf.list(
                        createAttendeeType("correct1@yandex-team.ru"),
                        createAttendeeType("incorrect.yandex.ru"),
                        createAttendeeType("correct2@yandex-team.ru"),
                        createAttendeeType("/O=YANDEX/OU=FIRST ADMINISTRATIVE GROUP/CN=RECIPIENTS/CN=SMARGOLIN64932410"),
                        createAttendeeType(""),
                        createAttendeeType(null));
            }
        };
        CalendarItemType calItem = new CalendarItemType();
        calItem.setRequiredAttendees(attendees);

        SetF<Email> expected = Cf.set(new Email("correct1@yandex-team.ru"), new Email("correct2@yandex-team.ru"));
        SetF<Email> actual = ExchangeEventDataConverter.getAttendeeEmails(calItem).unique();
        Validate.V.equals(expected, actual);
    }

    @Test
    public void resolveAttendeesWithSameEmail() throws Exception {
        DateTime start = TestDateTimes.moscowDateTime(2017, 11, 13, 15, 0);
        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                start, "Event with crazy attendees");

        Email organizerEmail = new Email("organizer@yt.ru");
        Email attendee1Email = new Email("attendee1@yt.ru");
        Email attendee2Email = new Email("attendee2@yt.ru");

        TestCalItemFactory.setOrganizer(calItem, organizerEmail);
        TestCalItemFactory.addAttendee(calItem, organizerEmail, ResponseTypeType.ORGANIZER);

        calItem.setIsMeeting(true);

        TestCalItemFactory.addAttendee(calItem, attendee1Email, ResponseTypeType.ACCEPT);
        TestCalItemFactory.addAttendee(calItem, attendee1Email, ResponseTypeType.UNKNOWN);
        TestCalItemFactory.addAttendee(calItem, attendee1Email, ResponseTypeType.TENTATIVE);
        TestCalItemFactory.addAttendee(calItem, attendee1Email, ResponseTypeType.DECLINE);

        TestCalItemFactory.addAttendee(calItem, attendee2Email, ResponseTypeType.UNKNOWN);
        TestCalItemFactory.addAttendee(calItem, attendee2Email, ResponseTypeType.DECLINE);

        UidOrResourceId subjectId = UidOrResourceId.user(PassportUid.cons(2910)); // yandex-team-mm-10550

        EventData data = ExchangeEventDataConverter.convert(
                calItem, subjectId, Option.<Email>empty(), resourceRoutines::selectResourceEmails);

        Assert.equals(Tuple2List.fromPairs(
                attendee1Email, Decision.YES,
                attendee2Email, Decision.UNDECIDED,
                organizerEmail, Decision.YES),

                data.getParticipantsSafe().sortedBy(p -> p.getEmail().getEmail())
                        .toTuple2List(ParticipantData::getEmail, ParticipantData::getDecision)
        );
    }

    @Test
    public void convertEventWithConferenceUrl() throws DatatypeConfigurationException {
        DateTime start = TestDateTimes.moscowDateTime(2010, 12, 10, 15, 0);
        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                start, "Event with conference field");
        calItem.setMeetingWorkspaceUrl("http://url");
        UidOrResourceId subjectId = UidOrResourceId.user(PassportUid.cons(2909)); // yandex-team-mm-10549
        EventData data = ExchangeEventDataConverter.convert(
                calItem, subjectId, Option.<Email>empty(), resourceRoutines::selectResourceEmails);

        Assert.A.equals(data.getEvent().getConferenceUrl().getOrNull(), "http://url");
    }
}
