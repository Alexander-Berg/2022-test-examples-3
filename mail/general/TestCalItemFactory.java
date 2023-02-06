package ru.yandex.calendar.frontend.ews.imp;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import com.microsoft.schemas.exchange.services._2006.types.AttendeeType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.DailyRecurrencePatternType;
import com.microsoft.schemas.exchange.services._2006.types.DayOfWeekIndexType;
import com.microsoft.schemas.exchange.services._2006.types.DayOfWeekType;
import com.microsoft.schemas.exchange.services._2006.types.DeletedOccurrenceInfoType;
import com.microsoft.schemas.exchange.services._2006.types.EndDateRecurrenceRangeType;
import com.microsoft.schemas.exchange.services._2006.types.NoEndRecurrenceRangeType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfAttendeesType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfDeletedOccurrencesType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfOccurrenceInfoType;
import com.microsoft.schemas.exchange.services._2006.types.NumberedRecurrenceRangeType;
import com.microsoft.schemas.exchange.services._2006.types.OccurrenceInfoType;
import com.microsoft.schemas.exchange.services._2006.types.RecurrenceType;
import com.microsoft.schemas.exchange.services._2006.types.RelativeMonthlyRecurrencePatternType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import com.microsoft.schemas.exchange.services._2006.types.SingleRecipientType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.ExchangeRepetitionEnd;
import ru.yandex.calendar.frontend.ews.exp.EventToCalendarItemConverter;
import ru.yandex.calendar.util.dates.AuxDateTime;
import ru.yandex.calendar.util.dates.WeekdayConv;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.random.Random2;

/**
 * @author akirakozov
 */
public class TestCalItemFactory {

    public static CalendarItemType createDefaultCalendarItemForExport(
            DateTime start, String subject)
            throws DatatypeConfigurationException
    {
        return createDefaultCalendarItem(start, subject, Option.<Instant>empty(), false);
    }

    public static CalendarItemType createDefaultCalendarItemForImport(
            DateTime start, String subject)
            throws DatatypeConfigurationException
    {
        return createDefaultCalendarItem(start, subject, Option.<Instant>empty(), true);
    }

    public static CalendarItemType createDefaultCalendarItemForImport(
            DateTime start, String subject, Instant lastModifiedTs)
            throws DatatypeConfigurationException
    {
        return createDefaultCalendarItem(start, subject, Option.of(lastModifiedTs), true);
    }

    private static CalendarItemType createDefaultCalendarItem(
            DateTime start, String subject, Option<Instant> lastModifiedTsO,
            boolean isImport) throws DatatypeConfigurationException
    {
        CalendarItemType calItem = new CalendarItemType();

        EwsUtils.setStartEndTimezone(calItem, start.getZone());

        DateTimeZone zone = EwsUtils.getOrDefaultZone(calItem);

        calItem.setSubject(subject);
        calItem.setStart(EwsUtils.instantToXMLGregorianCalendar(start.toInstant(), zone));
        calItem.setEnd(EwsUtils.instantToXMLGregorianCalendar(start.plusHours(1).toInstant(), zone));

        if (isImport) {
            calItem.setItemId(EwsUtils.createItemId(Random2.R.nextAlnum(8)));
            calItem.setIsMeeting(false);
            calItem.setAppointmentSequenceNumber(0);

            final Instant defaultLastModifiedTs =
                new DateTime(2010, 6, 7, 10, 0, 0, 0, start.getChronology()).toInstant();
            XMLGregorianCalendar lastModifiedTs = EwsUtils.instantToXMLGregorianCalendar(
                    lastModifiedTsO.getOrElse(defaultLastModifiedTs), zone);
            calItem.setDateTimeStamp(lastModifiedTs);
            calItem.setLastModifiedTime(lastModifiedTs);
        }
        return calItem;
    }

    public static void setOrganizer(CalendarItemType calItem, Email email) {
        SingleRecipientType organizer = new SingleRecipientType();
        organizer.setMailbox(EwsUtils.createEmailAddressType(email));
        calItem.setOrganizer(organizer);
    }

    public static void addAttendee(
            CalendarItemType calItem, Email email, ResponseTypeType decision)
    {
        if (calItem.getRequiredAttendees() == null) {
            calItem.setRequiredAttendees(new NonEmptyArrayOfAttendeesType());
        }
        AttendeeType attendee = new AttendeeType();
        attendee.setMailbox(EwsUtils.createEmailAddressType(email));
        attendee.setResponseType(decision);
        calItem.getRequiredAttendees().getAttendee().add(attendee);
    }


    private static RecurrenceType createDailyRecurrence() {
        DailyRecurrencePatternType daily = new DailyRecurrencePatternType();
        daily.setInterval(1);

        RecurrenceType recurrence = new RecurrenceType();
        recurrence.setDailyRecurrence(daily);
        return recurrence;
    }

    private static RecurrenceType createMonthlyDayWeeknoRecurrence(CalendarItemType calItem) {
        Instant start = EwsUtils.xmlGregorianCalendarInstantToInstant(calItem.getStart());
        DateTimeZone tz = EwsUtils.getOrDefaultZone(calItem);
        LocalDate startDate = start.toDateTime(tz).toLocalDate();
        DayOfWeekType dayOfWeek = WeekdayConv.jodaToExchange(startDate.getDayOfWeek());
        int weekOfMonth = AuxDateTime.getWeekOfMonth(startDate);
        DayOfWeekIndexType dayOfWeekIndex = EwsUtils.getDayOfWeekIndexTypeByWeekOfMonth(weekOfMonth); // 5 => last

        RelativeMonthlyRecurrencePatternType relMonthly = new RelativeMonthlyRecurrencePatternType();
        relMonthly.setInterval(1);
        relMonthly.setDaysOfWeek(dayOfWeek);
        relMonthly.setDayOfWeekIndex(dayOfWeekIndex);

        RecurrenceType recurrence = new RecurrenceType();
        recurrence.setRelativeMonthlyRecurrence(relMonthly);
        return recurrence;
    }

    private static NoEndRecurrenceRangeType createNoEndRecurrenceRange(CalendarItemType calItem) {
        NoEndRecurrenceRangeType endRange = new NoEndRecurrenceRangeType();
        endRange.setStartDate(calItem.getStart());
        return endRange;
    }

    private static NumberedRecurrenceRangeType createNumberedRecurrenceRange(CalendarItemType calItem, int count) {
        NumberedRecurrenceRangeType endRange = new NumberedRecurrenceRangeType();
        endRange.setStartDate(calItem.getStart());
        endRange.setNumberOfOccurrences(count);
        return endRange;
    }

    private static EndDateRecurrenceRangeType createEndDateRecurrenceRange(CalendarItemType calItem, Instant instant, DateTimeZone tz) {
        EndDateRecurrenceRangeType endRange = new EndDateRecurrenceRangeType();
        endRange.setStartDate(calItem.getStart());
        endRange.setEndDate(EventToCalendarItemConverter.convertDueDate(instant, tz));
        return endRange;
    }


    public static void addDailyRecurrence(CalendarItemType calItem) {
        RecurrenceType recurrence = createDailyRecurrence();
        recurrence.setNoEndRecurrence(createNoEndRecurrenceRange(calItem));
        calItem.setRecurrence(recurrence);
        // TODO calculate firstOccurrence
    }

    // XXX this might not correct, as well as createResource()
    // XXX why do we need a start (== event start)?
    public static void addDailyRecurrenceWithEnd(CalendarItemType calItem, ExchangeRepetitionEnd end, DateTimeZone tz) {
        RecurrenceType recurrence = createDailyRecurrence();
        if (end.isCount()) {
            recurrence.setNumberedRecurrence(createNumberedRecurrenceRange(calItem, end.getCount()));
        } else {
            recurrence.setEndDateRecurrence(createEndDateRecurrenceRange(calItem, end.getInstant(), tz));
        }
        calItem.setRecurrence(recurrence);
        // TODO calculate firstOccurrence, lastOccurrence
    }

    public static void addMonthlyDayWeeknoRecurrence(CalendarItemType calItem) {
        RecurrenceType recurrence = createMonthlyDayWeeknoRecurrence(calItem);
        recurrence.setNoEndRecurrence(createNoEndRecurrenceRange(calItem));
        calItem.setRecurrence(recurrence);
        // TODO calculate firstOccurrence
    }


    // import only
    public static void addExdate(CalendarItemType calItem, Instant exdateTs) {
        if (calItem.getDeletedOccurrences() == null) {
             calItem.setDeletedOccurrences(new NonEmptyArrayOfDeletedOccurrencesType());
        }
        DeletedOccurrenceInfoType occurrence = new DeletedOccurrenceInfoType();
        DateTimeZone zone = EwsUtils.getOrDefaultZone(calItem);
        occurrence.setStart(EwsUtils.instantToXMLGregorianCalendar(exdateTs, zone));
        calItem.getDeletedOccurrences().getDeletedOccurrence().add(occurrence);
        // TODO recalculate firstOccurrence, lastOccurrence
    }

    // import only
    public static void addRecurrenceId(CalendarItemType calItem, Instant recurrenceIdTs) {
        if (calItem.getModifiedOccurrences() == null) {
            calItem.setModifiedOccurrences(new NonEmptyArrayOfOccurrenceInfoType());
        }
        OccurrenceInfoType occurrence = new OccurrenceInfoType();
        DateTimeZone zone = EwsUtils.getOrDefaultZone(calItem);
        final Instant originalStartTs = recurrenceIdTs;
        final Instant startTs = recurrenceIdTs.plus(Duration.standardMinutes(30));
        final Instant endTs = recurrenceIdTs.plus(Duration.standardHours(1));
        occurrence.setOriginalStart(EwsUtils.instantToXMLGregorianCalendar(originalStartTs, zone));
        occurrence.setStart(EwsUtils.instantToXMLGregorianCalendar(startTs, zone));
        occurrence.setEnd(EwsUtils.instantToXMLGregorianCalendar(endTs, zone));
        occurrence.setItemId(EwsUtils.createItemId(Random2.R.nextAlnum(8)));
        calItem.getModifiedOccurrences().getOccurrence().add(occurrence);
        // TODO recalculate firstOccurrence, lastOccurrence
    }

    public static void increaseDtstamp(CalendarItemType calItem, Duration duration) {
        Instant oldDtstamp = EwsUtils.xmlGregorianCalendarInstantToInstant(calItem.getDateTimeStamp());
        Instant newDtstamp = oldDtstamp.plus(duration);
        DateTimeZone zone = EwsUtils.getOrDefaultZone(calItem);
        calItem.setDateTimeStamp(EwsUtils.instantToXMLGregorianCalendar(newDtstamp, zone));
    }
}
