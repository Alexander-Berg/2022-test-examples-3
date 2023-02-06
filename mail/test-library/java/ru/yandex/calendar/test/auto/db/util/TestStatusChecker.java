package ru.yandex.calendar.test.auto.db.util;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventInstanceStatusChecker;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.ics.imp.IcsImportMode;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class TestStatusChecker {

    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private MainEventDao mainEventDao;


    /** does checks for attendee when he updates or deletes event on web */
    public void checkForAttendeeOnWebUpdateOrDelete(
            PassportUid attendeeUid, Event oldEvent, ActionInfo actionInfo, boolean eventLastUpdateShouldUpdate)
    {
        long eventId = oldEvent.getId();
        Event newEvent = eventDao.findEventById(eventId);
        EventUser newEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, attendeeUid).get();

        if (eventLastUpdateShouldUpdate) {
            checkEventLastUpdateIsSetOrUpdatedToRequestNow(eventId, actionInfo);
        } else {
            checkEventLastUpdateIsPreserved(oldEvent);
        }
        checkMainEventLastUpdateIsUpdated(newEvent, actionInfo);

        checkUserSequenceAndDtStampOnWebUpdateOrDelete(newEvent, newEventUser, actionInfo);
    }

    private void checkUserSequenceAndDtStampOnWebUpdateOrDelete(Event newEvent, EventUser newEventUser, ActionInfo actionInfo) {
        Assert.A.equals(newEvent.getSequence(), newEventUser.getSequence());
        compareIgnoreMillis(actionInfo.getNow(), newEventUser.getDtstamp().getOrElse(new Instant(0)));
    }



    /** does checks for attendee when he creates RECURRENCE event using any ics source. */
    public void checkForAttendeeOnVEventCreateRecurrence(
            PassportUid attendeeUid, long recurEventId, IcsVEvent recurVevent, IcsImportMode mode)
    {
        checkUserSequenceAndDtStampOnVEventAction(attendeeUid, recurEventId, recurVevent, mode, true);
    }

    /** does checks for attendee when he updates or deletes event using any ics source */
    public void checkForAttendeeOnVEventUpdateOrDelete(
            PassportUid attendeeUid, Event oldEvent, IcsVEvent vevent, IcsImportMode mode, boolean userVersionShouldUpdate)
    {
        checkEventLastUpdateIsPreserved(oldEvent);
        checkMainEventLastUpdateIsUpdated(oldEvent, mode.getActionInfo());

        checkUserSequenceAndDtStampOnVEventAction(attendeeUid, oldEvent.getId(), vevent, mode, userVersionShouldUpdate);
    }

    // TODO can also provide old event user to
    // 1) verify provided versionShouldUpdate value;
    // 2) if it's false, ensure that old value really did not change for 'versions equal' case
    /** does checks for attendee when he does any action (create, update or delete event) using any ics source */
    private void checkUserSequenceAndDtStampOnVEventAction(
            PassportUid attendeeUid, long eventId, IcsVEvent vevent, IcsImportMode mode, boolean versionShouldUpdate)
    {
        int veventSequence = vevent.getSequence().getOrElse(0);
        // what should we do if DTSTAMP is absent in VEVENT (RFC 2445 says it's optional)? ssytnik@
        //Instant veventDtStamp = vevent.getDtStampInstant(DateTimeZone.UTC).getOrElse(mode.getActionInfo().getNow());
        Instant veventDtStamp = vevent.getDtStampInstant(IcsVTimeZones.fallback(DateTimeZone.UTC)).getOrElse(
                new Instant(0));

        EventUser newEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, attendeeUid).get();
        int userSequence = newEventUser.getSequence();
        Instant userDtStamp = newEventUser.getDtstamp().getOrElse(new Instant(0));

        if (versionShouldUpdate) {
            Assert.A.equals(veventSequence, userSequence);
            compareIgnoreMillis(veventDtStamp, userDtStamp);
        } else {
            // XXX this is wrong if 'versions are equal'; in this case, use checkUserSequenceAndDtStampArePreserved()
            Assert.A.isTrue(EventInstanceStatusChecker.isVersionGreater(userSequence, userDtStamp, veventSequence, veventDtStamp));
        }
    }


    public void checkForAttendeeOnEwsCreateRecurrenceOrUpdateDecision(
            PassportUid attendeeUid, long recurEventId, CalendarItemType recurCalItem)
    {
        checkUserSequenceAndDtStampOnEwsAction(attendeeUid, recurEventId, recurCalItem, true);
    }

    public void checkForAttendeeOnEwsUpdateOrDelete(
            PassportUid attendeeUid, Event oldEvent, CalendarItemType calItem, ActionInfo actionInfo, boolean userVersionShouldUpdate)
    {
        checkEventLastUpdateIsPreserved(oldEvent);
        checkMainEventLastUpdateIsUpdated(oldEvent, actionInfo);

        checkUserSequenceAndDtStampOnEwsAction(attendeeUid, oldEvent.getId(), calItem, userVersionShouldUpdate);
    }

    public void checkForEventUpdaterOnEwsUpdate( // OrDelete ?
            PassportUid updaterUid, Event oldEvent, EventUser oldEventUser, CalendarItemType calItem, ActionInfo actionInfo)
    {
        checkEventLastUpdateIsUpdatedToIncomingValue(oldEvent.getId(), EwsUtils.xmlGregorianCalendarInstantToInstant(calItem.getLastModifiedTime()));
        checkMainEventLastUpdateIsUpdated(oldEvent, actionInfo);

        checkUserSequenceAndDtStampOnEwsAction(updaterUid, oldEvent.getId(), calItem, true);
    }


    private void checkUserSequenceAndDtStampOnEwsAction(
            PassportUid uid, long eventId, CalendarItemType calItem, boolean versionShouldUpdate)
    {
        int calItemSequence = Option.ofNullable(calItem.getAppointmentSequenceNumber()).getOrElse(0);
        Instant calItemDtStamp = EwsUtils.toInstantO(calItem.getDateTimeStamp()).getOrElse(new Instant(0));

        EventUser newEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, uid).get();
        int userSequence = newEventUser.getSequence();
        Instant userDtStamp = newEventUser.getDtstamp().getOrElse(new Instant(0));

        if (versionShouldUpdate) {
            Assert.A.equals(calItemSequence, userSequence);
/*          CAL-7906
            compareIgnoreMillis(calItemDtStamp, userDtStamp);
*/
        } else {
            // XXX this is wrong if 'versions are equal'; in this case, use checkUserSequenceAndDtStampArePreserved()
            Assert.A.isTrue(EventInstanceStatusChecker.isVersionGreater(userSequence, userDtStamp, calItemSequence, calItemDtStamp));
        }
    }


    public void checkUserSequenceAndDtStampArePreserved(PassportUid uid, long eventId, EventUser oldEventUser) {
        EventUser newEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, uid).get();
        Assert.A.equals(oldEventUser.getSequence(), newEventUser.getSequence());
        compareIgnoreMillis(oldEventUser.getDtstamp(), newEventUser.getDtstamp());
    }


    public void checkEventLastUpdateIsPreserved(Event oldEvent) {
        Event newEvent = eventDao.findEventById(oldEvent.getId());
        compareIgnoreMillis(oldEvent.getLastUpdateTs(), newEvent.getLastUpdateTs());
    }

    /** applicable for any create or WEB update */
    public void checkEventLastUpdateIsSetOrUpdatedToRequestNow(long eventId, ActionInfo info) {
        Event newEvent = eventDao.findEventById(eventId);
        compareIgnoreMillis(info.getNow(), newEvent.getLastUpdateTs());
    }

    public void checkEventLastUpdateIsUpdatedToIncomingValue(long eventId, Instant incomingLastUpdate) {
        Event newEvent = eventDao.findEventById(eventId);
        compareIgnoreMillis(incomingLastUpdate, newEvent.getLastUpdateTs());
    }

    public void checkMainEventLastUpdateIsUpdated(Event oldOrNewEvent, ActionInfo info) {
        MainEvent me = mainEventDao.findMainEventById(oldOrNewEvent.getMainEventId());
        compareIgnoreMillis(info.getNow(), me.getLastUpdateTs());
    }

    public void checkUserSequenceAndDtStampAreInInitialState(EventUser eventUser) {
        Assert.A.equals(0, eventUser.getSequence());
        compareIgnoreMillis(Option.<Instant>empty(), eventUser.getDtstamp());
    }


    public void compareIgnoreMillis(Instant a, Instant b) {
        Assert.A.equals(withNoMillis(a), withNoMillis(b));
    }

    public void compareIgnoreMillis(Option<Instant> a, Option<Instant> b) {
        Assert.A.equals(a.map(withNoMillisF()), b.map(withNoMillisF()));
    }

    public static Instant withNoMillis(Instant instant) {
        return instant.withMillis(instant.getMillis() / 1000L * 1000L);
    }

    public static Function<Instant, Instant> withNoMillisF() {
        return new Function<Instant, Instant>() {
            public Instant apply(Instant a) { return withNoMillis(a); }
        };
    }

}
