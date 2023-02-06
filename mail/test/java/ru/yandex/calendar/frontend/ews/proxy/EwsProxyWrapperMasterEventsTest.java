package ru.yandex.calendar.frontend.ews.proxy;

import javax.xml.datatype.DatatypeConfigurationException;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemTypeType;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.ExchangeRepetitionEnd;
import ru.yandex.calendar.frontend.ews.hook.EwsFirewallTestConfiguration;
import ru.yandex.calendar.frontend.ews.hook.EwsNtfContextConfiguration;
import ru.yandex.calendar.frontend.ews.imp.TestCalItemFactory;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.time.InstantInterval;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {
        EwsNtfContextConfiguration.class,
        EwsFirewallTestConfiguration.class
})
public class EwsProxyWrapperMasterEventsTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private ResourceRoutines resourceRoutines;

    @Before
    public void setup() {
        val cleanupStart = new DateTime(2010, 11, 1, 10, 0, 0, 0, DateTimeZone.UTC);
        val cleanupEnd = new DateTime(2010, 11, 30, 10, 0, 0, 0, DateTimeZone.UTC);
        ewsProxyWrapper.cancelMasterAndSingleMeetings(TestManager.testExchangeSmolnyEmail, new InstantInterval(cleanupStart, cleanupEnd));
        registry.getMeters().forEach(meter -> registry.remove(meter));
    }

    @Test
    public void getMasterAndSingleEventsInInterval() throws DatatypeConfigurationException {
        val user = new PassportLogin("yandex-team-mm-10600");
        val uid = userManager.getUidByLoginForTest(user);
        testManager.cleanUser(uid);

        val smolnyResource = testManager.cleanAndCreateSmolny();
        val email = resourceRoutines.getExchangeEmail(smolnyResource);
        val recurEventStart = new DateTime(2010, 11, 5, 10, 0, 0, 0, DateTimeZone.UTC);
        val recurEventDueTs = new DateTime(2010, 11, 8, 10, 0, 0, 0, DateTimeZone.UTC);
        val exclusiveDueTs = recurEventDueTs.plusDays(1);

        // Create recurring event
        val recurCalItem = TestCalItemFactory.createDefaultCalendarItemForExport(recurEventStart, "Repeating event (findMasterAndSingleEvents)");
        TestCalItemFactory.addDailyRecurrenceWithEnd(recurCalItem, ExchangeRepetitionEnd.instant(exclusiveDueTs.toInstant()), DateTimeZone.UTC);
        recurCalItem.setUID(Random2.R.nextAlnum(10));
        ewsProxyWrapper.createEvent(email, recurCalItem, EwsActionLogData.test());

        // Create single event
        val singleEventStart = recurEventStart.plusDays(1).plusHours(2);
        CalendarItemType singleCalItem = TestCalItemFactory.createDefaultCalendarItemForExport(singleEventStart, "Single event");
        singleCalItem.setUID(Random2.R.nextAlnum(10));
        ewsProxyWrapper.createEvent(email, singleCalItem, EwsActionLogData.test());

        // Lookup for single + occurrences of recurring events
        val searchStart = recurEventStart;
        val searchEnd = recurEventDueTs.plusHours(1);
        val searchInterval = new InstantInterval(searchStart, searchEnd);

        val instanceIds = ewsProxyWrapper.findInstanceEventIds(email, searchInterval);
        assertThat(instanceIds).hasSize(5);
        val filledInstances = ewsProxyWrapper.getEvents(instanceIds);
        int occurrenceCount = 0;
        int singleCount = 0;
        for (CalendarItemType itemType : filledInstances) {
            val type = itemType.getCalendarItemType();
            if (type == CalendarItemTypeType.OCCURRENCE) {
                occurrenceCount++;
            } else if (type == CalendarItemTypeType.SINGLE) {
                singleCount++;
            }
        }
        assertThat(occurrenceCount).isEqualTo(4);
        assertThat(singleCount).isEqualTo(1);

        // Lookup for single + master recurring events
        val masterAndSingleEvents = ewsProxyWrapper.getMasterAndSingleEvents(email, Option.of(searchInterval));
        assertThat(masterAndSingleEvents).hasSize(2);

        // Event order in unspecified. Determine ourselves.
        val singleEventIndex = masterAndSingleEvents.get(0).getCalendarItemType() == CalendarItemTypeType.SINGLE ? 0 : 1;

        val singleEvent = masterAndSingleEvents.get(singleEventIndex);
        assertThat(singleEvent.getCalendarItemType()).isEqualTo(CalendarItemTypeType.SINGLE);
        assertThat(singleCalItem.getSubject()).isEqualTo(singleEvent.getSubject());

        val masterEvent = masterAndSingleEvents.get(1 - singleEventIndex);
        assertThat(masterEvent.getCalendarItemType()).isEqualTo(CalendarItemTypeType.RECURRING_MASTER);
        assertThat(recurCalItem.getSubject()).isEqualTo(masterEvent.getSubject());

        checkCounterValue("application.exchange.getEvents.success", 2);
        checkCounterValue("application.exchange.success", 6);
        checkCounterValue("application.exchange.createItems.success", 2);
        checkCounterValue("application.exchange.findInstanceEvents.success", 1);
        checkTimer("application.exchange.time.findInstanceEvents.success");
        checkTimer("application.exchange.time.success");
        checkTimer("application.exchange.time.getEvents.success");
        checkTimer("application.exchange.time.createItems.success");
    }

    @Test
    public void getMasterEventsByOccurrences() throws DatatypeConfigurationException {
        val user = new PassportLogin("yandex-team-mm-10601");
        val uid = userManager.getUidByLoginForTest(user);
        testManager.cleanUser(uid);

        val r = testManager.cleanAndCreateSmolny();
        val eventStart = new DateTime(2010, 11, 4,  5, 0, 0, 0, DateTimeZone.UTC);
        val eventDueTs = new DateTime(2010, 11, 5,  5, 0, 0, 0, DateTimeZone.UTC);
        val exclusiveDueTs = eventDueTs.plusDays(1);

        val calItem = TestCalItemFactory.createDefaultCalendarItemForExport(
                eventStart, "Repeating event (getMasterEvent)");
        TestCalItemFactory.addDailyRecurrenceWithEnd(calItem, ExchangeRepetitionEnd.instant(exclusiveDueTs.toInstant()), DateTimeZone.UTC);
        calItem.setUID(Random2.R.nextAlnum(10));

        val email = resourceRoutines.getExchangeEmail(r);
        ewsProxyWrapper.createEvent(email, calItem, EwsActionLogData.test());

        val searchInterval = new InstantInterval(eventStart, eventDueTs.plusHours(1));
        val occurrenceIds = ewsProxyWrapper.findInstanceEventIds(email, searchInterval);
        assertThat(occurrenceIds).hasSize(2);

        val masterEvents = ewsProxyWrapper.getMasterAndSingleEvents(occurrenceIds);
        assertThat(masterEvents).hasSize(1);
        checkCounterValue("application.exchange.getEvents.success", 2);
        checkCounterValue("application.exchange.createItems.success", 1);
        checkCounterValue("application.exchange.findInstanceEvents.success", 1);
        checkCounterValue("application.exchange.success", 4);
        checkTimer("application.exchange.time.findInstanceEvents.success");
        checkTimer("application.exchange.time.success");
        checkTimer("application.exchange.time.createItems.success");
        checkTimer("application.exchange.time.getEvents.success");
    }

    @Test
    public void getOccurrenceByIndex() throws DatatypeConfigurationException {
        val user = new PassportLogin("yandex-team-mm-10602");
        val uid = userManager.getUidByLoginForTest(user);
        testManager.cleanUser(uid);

        val r = testManager.cleanAndCreateSmolny();
        val email = resourceRoutines.getExchangeEmail(r);

        val occurrences = 4;
        val eventStart = new DateTime(2010, 11, 15,  9, 0, 0, 0, DateTimeZone.UTC);
        val finalOccurrenceStart = eventStart.plusDays(occurrences - 1);

        val subject = "Numbered recurrence" /* TODO + "with exceptions" */;
        val calItem = TestCalItemFactory.createDefaultCalendarItemForExport(eventStart, subject);
        TestCalItemFactory.addDailyRecurrenceWithEnd(calItem, ExchangeRepetitionEnd.count(occurrences), DateTimeZone.UTC);
        /* TODO add following exdate and recurrence-id when we know exactly how to do that:
        DateTime recurEventStart = eventStart.plusDays(1);
        TestCalItemFactory.addRecurrenceId(calItem, recurEventStart, recurItem);
        DateTime exdateEventStart = eventStart.plusDays(2);
        TestCalItemFactory.addExdate(calItem, exdateEventStart.toInstant());
        */

        val recurringMasterExchangeId = ewsProxyWrapper.createEvent(email, calItem, EwsActionLogData.test());

        val finalOccurrence = ewsProxyWrapper.getOccurrenceByIndex(recurringMasterExchangeId, occurrences).get();
        assertThat(subject).isEqualTo(finalOccurrence.getSubject());
        assertThat(finalOccurrenceStart.toInstant()).isEqualTo(EwsUtils.xmlGregorianCalendarInstantToInstant(finalOccurrence.getStart()));
        checkCounterValue("application.exchange.getEvents.success", 1);
        checkCounterValue("application.exchange.success", 2);
        checkCounterValue("application.exchange.createItems.success", 1);
        checkTimer("application.exchange.time.createItems.success");
        checkTimer("application.exchange.time.getEvents.success");
        checkTimer("application.exchange.time.success");
    }
}
