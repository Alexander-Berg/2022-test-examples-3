package ru.yandex.calendar.frontend.ews.proxy;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;

import com.microsoft.schemas.exchange.services._2006.messages.GetEventsResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.types.BaseNotificationEventType;
import com.microsoft.schemas.exchange.services._2006.types.BaseObjectChangedEventType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.ItemChangeDescriptionType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseClassType;
import com.microsoft.schemas.exchange.services._2006.types.SensitivityChoicesType;
import com.microsoft.schemas.exchange.services._2006.types.UnindexedFieldURIType;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.EwsErrorCodes;
import ru.yandex.calendar.frontend.ews.EwsException;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.ExchangeRepetitionEnd;
import ru.yandex.calendar.frontend.ews.exp.EwsModifyingItemId;
import ru.yandex.calendar.frontend.ews.imp.ExchangeEventDataConverter;
import ru.yandex.calendar.frontend.ews.imp.TestCalItemFactory;
import ru.yandex.calendar.frontend.ews.subscriber.EwsSubscribeResult;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class EwsProxyWrapperTest extends AbstractConfTest {
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;

    @Before
    public void setup() {
        val interval = new InstantInterval(
                new DateTime(2007, 10, 10, 15, 0, 0, 0, DateTimeZone.UTC),
                new DateTime(2030, 10, 10, 16, 0, 0, 0, DateTimeZone.UTC));
        ewsProxyWrapper.cancelMasterAndSingleMeetings(TestManager.testExchangeSmolnyEmail, interval);
        ewsProxyWrapper.cancelMasterAndSingleMeetings(TestManager.testExchangeThreeLittlePigsEmail, interval);
        registry.getMeters().forEach(meter -> registry.remove(meter));
    }

    @Test
    public void createEvent() {
        // create event
        val exchangeIdO = ewsProxyWrapper.createEventSafe(TestManager.testExchangeUserEmail,
                new CalendarItemType(), EwsActionLogData.test());
        assertThat(exchangeIdO).isNotEmpty();
        // check, that event exists in exchange
        assertThat(ewsProxyWrapper.getEvent(exchangeIdO.get())).isNotEmpty();
        // remove event
        ewsProxyWrapper.cancelMeetingsSafe(exchangeIdO.map(ExchangeIdLogData::test), ActionInfo.exchangeTest());
    }

    @Test
    public void updatePrivateEventUpdate() {
        val item = new CalendarItemType();
        item.setSensitivity(SensitivityChoicesType.PRIVATE);

        val exchangeIdO = ewsProxyWrapper.createEventSafe(TestManager.testExchangeConfRr21, item, EwsActionLogData.test());
        assertThat(exchangeIdO).isNotEmpty();

        val changes = Cf.<ItemChangeDescriptionType>arrayList();
        val calendarItem = new CalendarItemType();
        calendarItem.setSubject("New event name");
        changes.add(EwsUtils.createSetItemField(calendarItem, UnindexedFieldURIType.ITEM_SUBJECT));

        ewsProxyWrapper.updateItem(EwsModifyingItemId.fromExchangeId(exchangeIdO.get()), changes, EwsActionLogData.test());
        checkCounterValue("application.exchange.success", 3);
        checkCounterValue("application.exchange.createItems.success", 1);
        checkTimer("application.exchange.time.createItems.success");
        checkTimer("application.exchange.time.success");
    }

    @Test
    public void updateRecurrenceIdExtendedProperty() {
        Instant recurrenceId = MoscowTime.instant(2012, 7, 15, 10, 39);

        CalendarItemType item = new CalendarItemType();
        item.getExtendedProperty().add(EwsUtils.createRecurrenceIdExtendedProperty(recurrenceId));

        Option<String> exchangeIdO = ewsProxyWrapper.createEventSafe(TestManager.testExchangeConfRr21, item, EwsActionLogData.test());
        assertThat(exchangeIdO).isNotEmpty();

        item = ewsProxyWrapper.getEvent(exchangeIdO.get()).get();
        assertThat(EwsUtils.convertExtendedProperties(item.getExtendedProperty()).getRecurrenceId().toOptional()).hasValue(recurrenceId);

        recurrenceId = MoscowTime.instant(2012, 7, 16, 22, 37);

        CalendarItemType change = new CalendarItemType();
        change.getExtendedProperty().add(EwsUtils.createRecurrenceIdExtendedProperty(recurrenceId));

        val changes = Cf.<ItemChangeDescriptionType>list(EwsUtils.createSetItemField(change, EwsUtils.EXTENDED_PROPERTY_RECURRENCE_ID));

        exchangeIdO = ewsProxyWrapper.updateItem(EwsModifyingItemId.fromExchangeId(exchangeIdO.get()), changes, EwsActionLogData.test());
        assertThat(exchangeIdO).isNotEmpty();

        item = ewsProxyWrapper.getEvent(exchangeIdO.get()).get();
        assertThat(EwsUtils.convertExtendedProperties(item.getExtendedProperty()).getRecurrenceId().toOptional()).hasValue(recurrenceId);
    }

    @Test
    public void removePrivateEvent() {
        val item = new CalendarItemType();
        item.setSensitivity(SensitivityChoicesType.PRIVATE);

        val exchangeIdO = ewsProxyWrapper.createEventSafe(TestManager.testExchangeConfRr21, item, EwsActionLogData.test());
        assertThat(exchangeIdO).isNotEmpty();

        ewsProxyWrapper.cancelMeetingsSafe(exchangeIdO.map(ExchangeIdLogData::test), ActionInfo.exchangeTest());

        assertThat(ewsProxyWrapper.getEvent(exchangeIdO.get())).isEmpty();
        checkCounterValue("application.exchange.getEvents.success", 2);
        checkCounterValue("application.exchange.success", 4);
        checkCounterValue("application.exchange.createItems.success", 2);
        checkTimer("application.exchange.time.getEvents.success");
        checkTimer("application.exchange.time.success");
        checkTimer("application.exchange.time.createItems.success");
    }

    @Test
    public void createEventWithSeconds() {
        // create event
        val start = new DateTime(2010, 12, 16, 10, 1, 1, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        val end = start.plusHours(1);
        val item = new CalendarItemType();
        item.setStart(EwsUtils.instantToXMLGregorianCalendar(start.toInstant(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE));
        item.setEnd(EwsUtils.instantToXMLGregorianCalendar(end.toInstant(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE));

        val interval = new InstantInterval(start.toInstant(), end.toInstant());
        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                TestManager.testExchangeThreeLittlePigsEmail, interval);

        val exchangeId = ewsProxyWrapper.createEventSafe(TestManager.testExchangeThreeLittlePigsEmail, item, EwsActionLogData.test()).get();
        val createdItem = ewsProxyWrapper.getEvent(exchangeId).get();
        assertThat(item.getStart()).isEqualTo(createdItem.getStart());

        val updatingStart = new DateTime(2010, 12, 16, 10, 1, 2, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        val updatingItem = new CalendarItemType();
        updatingItem.setStart(EwsUtils.instantToXMLGregorianCalendar(updatingStart.toInstant(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE));
        val changeDescriptions = Cf.list(EwsUtils.createSetItemField(
                updatingItem, UnindexedFieldURIType.CALENDAR_START));

        ewsProxyWrapper.updateItem(EwsModifyingItemId.fromExchangeId(exchangeId), changeDescriptions, EwsActionLogData.test());
        val updatedItem = ewsProxyWrapper.getEvent(exchangeId).get();
        assertThat(updatingItem.getStart()).isEqualTo(updatedItem.getStart());
        checkCounterValue("application.exchange.success", 6);
        checkTimer("application.exchange.time.success");
    }

    @Test
    public void findEventsByEmail() {
        val interval = new InstantInterval(
                new DateTime(2020, 10, 10, 15, 0, 0, 0, DateTimeZone.UTC),
                new DateTime(2020, 10, 10, 16, 0, 0, 0, DateTimeZone.UTC));

        val before = new CalendarItemType(); // no timezone => default
        val beforeZone = EwsUtils.getOrDefaultZone(before);
        before.setStart(EwsUtils.instantToXMLGregorianCalendar(interval.getStart().minus(Duration.standardHours(3)), beforeZone));
        before.setEnd(EwsUtils.instantToXMLGregorianCalendar(interval.getStart().minus(Duration.standardHours(2)), beforeZone));

        val during = new CalendarItemType(); // no timezone => default
        val duringZone = EwsUtils.getOrDefaultZone(during);
        during.setStart(EwsUtils.instantToXMLGregorianCalendar(interval.getStart().minus(Duration.standardMinutes(15)), duringZone));
        during.setEnd(EwsUtils.instantToXMLGregorianCalendar(interval.getEnd().minus(Duration.standardMinutes(15)), duringZone));
        String smolnyDuring = ewsProxyWrapper.createEventSafe(TestManager.testExchangeSmolnyEmail, during, EwsActionLogData.test()).get();

        val after = new CalendarItemType(); // no timezone => default
        val afterZone = EwsUtils.getOrDefaultZone(after);
        after.setStart(EwsUtils.instantToXMLGregorianCalendar(interval.getEnd().plus(Duration.standardHours(5)), afterZone));
        after.setEnd(EwsUtils.instantToXMLGregorianCalendar(interval.getEnd().plus(Duration.standardHours(6)), afterZone));
        val threeLittlePigsDuring = ewsProxyWrapper.createEventSafe(TestManager.testExchangeThreeLittlePigsEmail, during, EwsActionLogData.test()).get();

        val idsInSmolny = ewsProxyWrapper.findInstanceEventIds(TestManager.testExchangeSmolnyEmail, interval);
        val idsInThreeLittlePigs = ewsProxyWrapper.findInstanceEventIds(TestManager.testExchangeThreeLittlePigsEmail, interval);
        assertThat(smolnyDuring).isEqualTo(idsInSmolny.single());
        assertThat(threeLittlePigsDuring).isEqualTo(idsInThreeLittlePigs.single());
    }

    @Test
    public void timezonesInCalItemAndItsPureDateFieldsMatch() throws DatatypeConfigurationException {
        val tz = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;
        val searchStart = new DateTime(2010, 11, 3, 9, 0, 0, 0, tz);
        val searchEnd = new DateTime(2010, 11, 12, 11, 0, 0, 0, tz);

        val interval = new InstantInterval(searchStart, searchEnd);
        ewsProxyWrapper.cancelMasterAndSingleMeetings(TestManager.testExchangeSmolnyEmail, interval);

        val subject = "Repeating event (tz/dates match?)";
        val eventStart = new DateTime(2010, 11, 10, 10, 0, 0, 0, tz);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForExport(eventStart, subject);
        TestCalItemFactory.addDailyRecurrenceWithEnd(calItem, ExchangeRepetitionEnd.count(3), tz);
        calItem.setUID(Random2.R.nextAlnum(10));

        val exchangeId = ewsProxyWrapper.createEvent(TestManager.testExchangeSmolnyEmail, calItem, EwsActionLogData.test());

        val createdItem = ewsProxyWrapper.getEvent(exchangeId).get(); // get our calItem back from exchange
        assertThat(eventStart.toInstant()).isEqualTo(EwsUtils.xmlGregorianCalendarInstantToInstant(createdItem.getStart()));

        val recurrence = createdItem.getRecurrence();
        val recurrenceRange = recurrence.getNumberedRecurrence();
        assertThat(recurrenceRange).isNotNull();
        val expectedRecurrenceStartDate = eventStart.toLocalDate(); // == start of recurrence in calItem
        val actualRecurrenceStartDate = EwsUtils.xmlGregorianCalendarLocalDateToLocalDate(recurrenceRange.getStartDate());
        assertThat(recurrenceRange.getNumberOfOccurrences()).isEqualTo(3);
        assertThat(expectedRecurrenceStartDate).isEqualTo(actualRecurrenceStartDate);
        assertThat(eventStart.toInstant()).isEqualTo(EwsUtils.xmlGregorianCalendarInstantToInstant(createdItem.getFirstOccurrence().getStart()));
        assertThat(eventStart.plusDays(2).toInstant()).isEqualTo(EwsUtils.xmlGregorianCalendarInstantToInstant(createdItem.getLastOccurrence().getStart()));
    }

    @Test
    public void ews2010To2013() throws DatatypeConfigurationException {
        val eventStart = new DateTime(2014, 7, 10, 10, 0, 0, 0, MoscowTime.TZ);

        val email2010 = new Email("conf_rr_3_1_2010@msft.yandex-team.ru");
        val email2013 = new Email("conf_rr_3_1_2013@msft.yandex-team.ru");

        val subject = "ews2010To2013";
        val changedSubject = "ews2013To2010";

        val change = new CalendarItemType();
        change.setSubject(changedSubject);
        val changeSubject = Cf.list(
                EwsUtils.createSetItemField(change, UnindexedFieldURIType.ITEM_SUBJECT));

        ewsProxyWrapper.cancelMeetings(email2010, eventStart.toInstant(), eventStart.plusHours(1).toInstant());
        ewsProxyWrapper.cancelMeetings(email2013, eventStart.toInstant(), eventStart.plusHours(1).toInstant());

        val exchangeId2010 = ewsProxyWrapper.createEvent(
                email2010, TestCalItemFactory.createDefaultCalendarItemForExport(eventStart, subject), EwsActionLogData.test());
        val exchangeId2013 = ewsProxyWrapper.createEvent(
                email2013, TestCalItemFactory.createDefaultCalendarItemForExport(eventStart, subject), EwsActionLogData.test());

        assertThat(ewsProxyWrapper.updateItem(EwsModifyingItemId.fromExchangeId(exchangeId2010), changeSubject, EwsActionLogData.test()))
                .isNotEmpty();
        assertThat(ewsProxyWrapper.updateItem(EwsModifyingItemId.fromExchangeId(exchangeId2013),
                                              changeSubject, EwsActionLogData.test())).isNotEmpty();

        val exchangeIds = Cf.list(exchangeId2010, exchangeId2013);

        assertThat(ewsProxyWrapper.getEvents(exchangeIds)).hasSize(2);
        ewsProxyWrapper.cancelOrDeclineMeetingSafe(exchangeIds.map(ExchangeIdLogData::test), ActionInfo.exchangeTest());
        assertThat(ewsProxyWrapper.getEvents(exchangeIds)).isEmpty();
    }


    @Test
    public void utcTimezonesInCalItemAndItsDueTsMatch() throws DatatypeConfigurationException {
        timezonesInCalItemAndItsDueTsMatch(DateTimeZone.UTC, new LocalTime(0, 0));
        timezonesInCalItemAndItsDueTsMatch(DateTimeZone.UTC, new LocalTime(2, 0));
        timezonesInCalItemAndItsDueTsMatch(DateTimeZone.UTC, new LocalTime(5, 0));
        timezonesInCalItemAndItsDueTsMatch(DateTimeZone.UTC, new LocalTime(12, 0));
        timezonesInCalItemAndItsDueTsMatch(DateTimeZone.UTC, new LocalTime(19, 0));
        timezonesInCalItemAndItsDueTsMatch(DateTimeZone.UTC, new LocalTime(23, 0));
    }

    @Test
    public void moscowTimezonesInCalItemAndItsDueTsMatch() throws DatatypeConfigurationException {
        timezonesInCalItemAndItsDueTsMatch(TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalTime(0, 0));
        timezonesInCalItemAndItsDueTsMatch(TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalTime(2, 0));
        timezonesInCalItemAndItsDueTsMatch(TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalTime(5, 0));
        timezonesInCalItemAndItsDueTsMatch(TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalTime(12, 0));
        timezonesInCalItemAndItsDueTsMatch(TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalTime(19, 0));
        timezonesInCalItemAndItsDueTsMatch(TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalTime(23, 0));
    }

    private void timezonesInCalItemAndItsDueTsMatch(DateTimeZone tz, LocalTime startTime) throws DatatypeConfigurationException {
        val subject = "Repeating event (tz/due match?)";
        val eventStart = new DateTime(2010, 11, 10, startTime.getHourOfDay(), startTime.getMinuteOfHour(), 0, 0, tz);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForExport(eventStart, subject);
        val lastInstanceTs = eventStart.plusDays(2);
        val dueTs = lastInstanceTs.plusDays(1).toInstant();

        TestCalItemFactory.addDailyRecurrenceWithEnd(calItem, ExchangeRepetitionEnd.instant(dueTs), tz);
        calItem.setUID(Random2.R.nextAlnum(10));

        val exchangeId = ewsProxyWrapper.createEvent(TestManager.testExchangeSmolnyEmail, calItem, EwsActionLogData.test());

        CalendarItemType createdItem = ewsProxyWrapper.getEvent(exchangeId).get();
        val dueTsFromExchange = ExchangeEventDataConverter.getRecurrenceDueTs(createdItem.getRecurrence(), eventStart.toInstant(), tz);
        assertThat(dueTs).isEqualTo(dueTsFromExchange);
    }

    @Test
    public void differentCalItemZonesPointToTheSameInstants() throws DatatypeConfigurationException {
        val start = new DateTime(2010, 11, 14, 8, 0, 0, 0, DateTimeZone.UTC);

        ListF<DateTimeZone> testZones = Cf.list(
            DateTimeZone.UTC, TimeUtils.EUROPE_MOSCOW_TIME_ZONE,
            DateTimeZone.forID("Asia/Yekaterinburg"), DateTimeZone.forID("America/Regina")
        );

        for (DateTimeZone zone : testZones) {
            val subject = "Event (timezone = " + zone.getID() + ")";
            val calItem = TestCalItemFactory.createDefaultCalendarItemForExport(start.withZone(zone), subject);

            val exchangeId = ewsProxyWrapper.createEvent(TestManager.testExchangeSmolnyEmail, calItem, EwsActionLogData.test());
            val createdItem = ewsProxyWrapper.getEvent(exchangeId).get();

            assertThat(zone.getID()).isEqualTo(EwsUtils.getOrDefaultZone(createdItem).getID());
            assertThat(start.getMillis()).isEqualTo(EwsUtils.xmlGregorianCalendarInstantToInstant(createdItem.getStart()).getMillis());
        }
    }

    @Test
    public void searchInInterval() {
        val start = new DateTime(2010, 12, 16, 10, 1, 1, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        val end = start.plusHours(1);

        val item = new CalendarItemType();
        item.setStart(EwsUtils.instantToXMLGregorianCalendar(start.toInstant(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE));
        item.setEnd(EwsUtils.instantToXMLGregorianCalendar(end.toInstant(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE));

        val overlaps = new InstantInterval(start.toInstant().minus(Duration.standardMinutes(30)), start.plus(Duration.standardMinutes(30)));
        val inside = new InstantInterval(start.toInstant().plus(Duration.standardMinutes(30)), start.plus(Duration.standardMinutes(35)));

        val before = new InstantInterval(start.toInstant().minus(Duration.standardHours(2)), start.minus(Duration.standardHours(1)));
        val after = new InstantInterval(end.toInstant().plus(Duration.standardMinutes(30)), end.plus(Duration.standardMinutes(35)));

        val exchangeId = ewsProxyWrapper.createEventSafe(TestManager.testExchangeThreeLittlePigsEmail, item, EwsActionLogData.test()).get();

        assertThat(ewsProxyWrapper.findInstanceEventIds(TestManager.testExchangeThreeLittlePigsEmail, overlaps)).contains(exchangeId);
        assertThat(ewsProxyWrapper.findInstanceEventIds(TestManager.testExchangeThreeLittlePigsEmail, inside)).contains(exchangeId);

        assertThat(ewsProxyWrapper.findInstanceEventIds(TestManager.testExchangeThreeLittlePigsEmail, before)).doesNotContain(exchangeId);
        assertThat(ewsProxyWrapper.findInstanceEventIds(TestManager.testExchangeThreeLittlePigsEmail, after)).doesNotContain(exchangeId);
    }

    @Test
    public void eventWithExtendedProperties() {
        val withoutExtProp = new CalendarItemType();
        val withoutExtPropId = ewsProxyWrapper.createEvent(TestManager.testExchangeConfRr21, withoutExtProp, EwsActionLogData.test());

        val withExtProp = new CalendarItemType();
        val properties = Cf.list(
                EwsUtils.createOrganizerExtendedProperty(TestManager.testExchangeThreeLittlePigsEmail),
                EwsUtils.createSourceExtendedProperty());
        withExtProp.getExtendedProperty().addAll(properties);
        val withExtPropId = ewsProxyWrapper.createEvent(TestManager.testExchangeThreeLittlePigsEmail, withExtProp, EwsActionLogData.test());

        val withoutExtPropO = ewsProxyWrapper.getEvent(withoutExtPropId);
        val withExtPropO = ewsProxyWrapper.getEvent(withExtPropId);

        assertThat(withoutExtPropO).isNotEmpty();
        assertThat(withExtPropO).isNotEmpty();

        val withoutProps = EwsUtils.convertExtendedProperties(withoutExtPropO.get().getExtendedProperty());
        val withProps = EwsUtils.convertExtendedProperties(withExtPropO.get().getExtendedProperty());

        assertThat(withoutProps.getOrganizerEmail()).isEmpty();
        assertThat(withoutProps.getWasCreatedFromYaTeamCalendar()).isFalse();

        assertThat(withProps.getOrganizerEmail().toOptional()).hasValue(TestManager.testExchangeThreeLittlePigsEmail);
        assertThat(withProps.getWasCreatedFromYaTeamCalendar()).isTrue();
    }

    @Test
    public void pullReplay() throws DatatypeConfigurationException {
        val email = TestManager.testExchangeThreeLittlePigsEmail;
        val eventStart = MoscowTime.dateTime(2015, 2, 24, 21, 0);

        EwsSubscribeResult subscription = ewsProxyWrapper.subscribeToPull(email, Minutes.minutes(5), Option.empty());
        assertThat(subscription.getSubscriptionId()).isNotEmpty();
        assertThat(subscription.getWatermark()).isNotEmpty();

        val watermark = subscription.getWatermark().get();

        val exchangeId = ewsProxyWrapper.createEvent(
                email, TestCalItemFactory.createDefaultCalendarItemForExport(eventStart, "Pull subscription test"), EwsActionLogData.test());

        BaseObjectChangedEventType notification = pull(subscription);
        assertThat(exchangeId).isEqualTo(notification.getItemId().getId());

        GetEventsResponseMessageType response = ewsProxyWrapper.pull(subscription.getSubscriptionId().get(), watermark);
        assertThat(response.getResponseCode()).isEqualTo(EwsErrorCodes.INVALID_WATERMARK);

        ewsProxyWrapper.unsubscribeSafe(subscription.getSubscriptionId().get());

        subscription = ewsProxyWrapper.subscribeToPull(email, Minutes.minutes(5), Option.of(watermark));
        notification = pull(subscription);
        assertThat(notification.getItemId().getId()).isEqualTo(exchangeId);

        ewsProxyWrapper.unsubscribeSafe(subscription.getSubscriptionId().get());
    }

    private BaseObjectChangedEventType pull(EwsSubscribeResult subscription) {
        Option<BaseObjectChangedEventType> itemNotification;
        String watermark = subscription.getWatermark().get();
        do {
            ThreadUtils.sleep(Duration.standardSeconds(5));

            val response = ewsProxyWrapper.pull(
                    subscription.getSubscriptionId().get(), watermark);

            if (response.getResponseClass() == ResponseClassType.ERROR) {
                throw new EwsException(response.getMessageText());
            }
            ListF<BaseNotificationEventType> notifications = Cf.x(response.getNotification()
                    .getCopiedEventOrCreatedEventOrDeletedEvent())
                    .map(JAXBElement::getValue);

            itemNotification = notifications
                    .filterByType(BaseObjectChangedEventType.class).find(ntf -> ntf.getItemId() != null);

            watermark = notifications.last().getWatermark();

        } while (!itemNotification.isPresent());

        return itemNotification.get();
    }
}
