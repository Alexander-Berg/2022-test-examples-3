package ru.yandex.calendar.logic.ics.imp;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsRRule;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author gutman
 */
public class IcsImporterDeleteMeetingTest extends AbstractConfTest {

    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private TestManager testManager;
    @Autowired
    private TestStatusChecker testStatusChecker;
    @Autowired
    private EventDao eventDao;

    @Test
    public void attendeeCantDeleteRecurrence() {
        deletingRecurrence(true);
    }

    @Test
    public void organizerDeletesRecurrence() {
        deletingRecurrence(false);
    }

    private void deletingRecurrence(boolean attendeeDeletes) {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-13801");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-13802");

        Tuple2<Event,Event> t = testManager.createEventWithRepetitionAndRecurrence(organizer.getUid());
        Event master = t._1;
        Event recurrence = t._2;

        testManager.createEventUser(organizer.getUid(), master.getId(), Decision.YES, Option.of(true));
        testManager.createEventUser(attendee.getUid(), master.getId(), Decision.MAYBE, Option.of(false));
        testManager.createEventUser(organizer.getUid(), recurrence.getId(), Decision.YES, Option.of(true));
        testManager.createEventUser(attendee.getUid(), recurrence.getId(), Decision.MAYBE, Option.of(false));

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(master.getId()));
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail());
        vevent = vevent.withSummary("");
        vevent = vevent.withDtStart(master.getStartTs());
        vevent = vevent.withDtEnd(master.getEndTs());
        vevent = vevent.withSequenece(1);
        vevent = vevent.addProperty(new IcsRRule("FREQ=DAILY"));

        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest();
        PassportUid actorUid = attendeeDeletes ? attendee.getUid() : organizer.getUid();
        IcsImportStats icsImportStats = icsImporter.importIcsStuff(actorUid, vevent.makeCalendar(), mode);

        if (attendeeDeletes) {
            Assert.A.isEmpty(icsImportStats.getDeletedEventIds());
            testStatusChecker.checkForAttendeeOnVEventUpdateOrDelete(actorUid, master, vevent, mode, true);
        } else {
            Assert.A.equals(recurrence.getId(), icsImportStats.getDeletedEventIds().single());
        }
    }
}
