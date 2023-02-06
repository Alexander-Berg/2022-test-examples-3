package ru.yandex.calendar.logic.update;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.DatatypeConfigurationException;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.hook.EwsFirewallTestConfiguration;
import ru.yandex.calendar.frontend.ews.hook.EwsNtfContextConfiguration;
import ru.yandex.calendar.frontend.ews.imp.EwsImporter;
import ru.yandex.calendar.frontend.ews.imp.TestCalItemFactory;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventLayerFields;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ExternalId;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.imp.IcsImportMode;
import ru.yandex.calendar.logic.ics.imp.IcsImporter;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsPartStat;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAttendee;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

import static ru.yandex.calendar.test.auto.db.util.TestManager.NEXT_YEAR;

@ContextConfiguration(classes = {
        EwsNtfContextConfiguration.class,
        EwsFirewallTestConfiguration.class
})
public class MultiThreadedEventsImportTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private EwsImporter ewsImporter;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private EventResourceDao eventResourceDao;
    @Autowired
    private ResourceRoutines resourceRoutines;


    class IcsLoader implements Runnable {
        private final CountDownLatch doneSignal;
        private final CountDownLatch startSignal;
        private final PassportUid uid;
        private final IcsCalendar calendar;

        public IcsLoader(
                PassportUid uid, IcsCalendar calendar,
                CountDownLatch doneSignal, CountDownLatch startSignal)
        {
            this.doneSignal = doneSignal;
            this.startSignal = startSignal;
            this.uid = uid;
            this.calendar = calendar;
        }

        public void run() {
            try {
                startSignal.await();
                icsImporter.importIcsStuff(
                       uid, calendar, IcsImportMode.incomingEmailFromMailhook());
             } catch (InterruptedException e) {
                logger.warn(e, e);
            } finally {
                doneSignal.countDown();
            }
        }
    }

    class ExchangeLoader implements Runnable {
        private final CountDownLatch doneSignal;
        private final CountDownLatch startSignal;
        private final UidOrResourceId subjectId;
        private final CalendarItemType calendarItem;

        public ExchangeLoader(
                UidOrResourceId subjectId, CalendarItemType calendarItem,
                CountDownLatch doneSignal, CountDownLatch startSignal)
        {
           this.doneSignal = doneSignal;
           this.subjectId = subjectId;
           this.calendarItem = calendarItem;
           this.startSignal = startSignal;
        }

        public void run() {
            try {
                startSignal.await();
                ewsImporter.createOrUpdateEventForTest(
                        subjectId, calendarItem, ActionInfo.exchangeTest(), false);
            } catch (InterruptedException e) {
                logger.warn(e, e);
            } finally {
                doneSignal.countDown();
            }
        }
    }

    @Test
    public void multiThreadImportFromExchange() throws Exception {
        testMultiThreadImportInner(true, false);
    }

    @Test
    public void multiThreadImportFromIcs() throws Exception {
        testMultiThreadImportInner(false, true);
    }

    @Test
    public void multiThreadImportFromExchangeAndIcs() throws Exception {
        testMultiThreadImportInner(true, true);
    }

    private static final AtomicLong generationNumberHolder = new AtomicLong();

    private final long generationNumber = generationNumberHolder.incrementAndGet();

    public void testMultiThreadImportInner(
            boolean loadFromExchange, boolean loadFromIcs) throws Exception
    {
        final int userCounts = 10;
        TestUserInfo[] users = new TestUserInfo[userCounts];
        for (int i = 0; i < users.length; i++) {
            users[i] = testManager.prepareUser("yandex-team-mm-1152" + i);
            // run getters to initialize settings
            settingsRoutines.getSettingsByUid(users[i].getUid());
        }
        Resource r = testManager.cleanAndCreateThreeLittlePigs();

        String externalId = CalendarUtils.generateExternalId();
        DateTime startEvent = TestDateTimes.moscowDateTime(NEXT_YEAR, 11, 10, 18, 20);

        int threadsCount = 0;
        if (loadFromExchange)
            threadsCount += users.length + 1;
        if (loadFromIcs)
            threadsCount += users.length;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threadsCount);

        if (loadFromExchange) {
            CalendarItemType calItem =
                    createCalendarItemWithRandomExchangeId(users, r, externalId, startEvent);
            new Thread(new ExchangeLoader(UidOrResourceId.resource(r.getId()), calItem, doneSignal, startSignal), "ExchangeLoader-" + generationNumber + "-r").start();
            for (int i = 0; i < users.length; i++) {
                final PassportUid uid = users[i].getUid();
                CalendarItemType userCalItem =
                        createCalendarItemWithRandomExchangeId(users, r, externalId, startEvent);
                new Thread(new ExchangeLoader(UidOrResourceId.user(uid), userCalItem, doneSignal, startSignal), "ExchangeLoader-" + generationNumber + "-u-" + i).start();
            }
        }
        if (loadFromIcs) {
            IcsCalendar calendar = createIcsCalendar(users, r, externalId, startEvent);
            for (int i = 0; i < users.length; i++) {
                new Thread(new IcsLoader(users[i].getUid(), calendar, doneSignal, startSignal), "IcsLoader-" + generationNumber + "-" + i).start();
            }
        }

        startSignal.countDown();
        doneSignal.await();

        ListF<MainEvent> mainEventIds = mainEventDao.findMainEventsByExternalId(new ExternalId(externalId));
        Assert.A.hasSize(1, mainEventIds);
        final ListF<Event> events = eventDao.findEventsByMainId(mainEventIds.single().getId());
        Assert.A.hasSize(1, events);

        // check that all users and the resource have single event in all their layers
        SetF<Long> eventIds = Cf.hashSet();
        eventIds.addAll(eventResourceDao.findEventResourceEventIdsByResourceId(r.getId()));
        for (TestUserInfo user : users) {
            eventIds.addAll(
                    eventLayerDao.findEventLayersByLayerCreatorUid(user.getUid())
                    .map(EventLayerFields.EVENT_ID.getF())
            );
        }
        Assert.A.equals(events.single().getId(), eventIds.single());
    }

    private IcsCalendar createIcsCalendar(TestUserInfo[] users, Resource r, String externalId, DateTime startEvent) {
        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary("Event");
        vevent = vevent.withOrganizer(users[0].getEmail());
        for (int i = 0; i < users.length; i++) {
            vevent = vevent.addProperty(new IcsAttendee(users[i].getEmail(), IcsPartStat.ACCEPTED));
        }
        vevent = vevent.withUid(externalId);
        vevent = vevent.withDtStart(startEvent);
        vevent = vevent.withDtEnd(startEvent.plusHours(1));
        vevent = vevent.withSequenece(0);
        IcsCalendar calendar = vevent.makeCalendar();
        return calendar;
    }

    private CalendarItemType createCalendarItemWithRandomExchangeId(
            TestUserInfo[] users, Resource r, String externalId,
            DateTime startEvent) throws DatatypeConfigurationException
    {
        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForImport(startEvent, "createCalendarItemWithRandomExchangeId");
        calItem.setUID(externalId);
        TestCalItemFactory.setOrganizer(calItem, resourceRoutines.getExchangeEmail(r));
        for (int j = 0; j < users.length; j++) {
            TestCalItemFactory.addAttendee(calItem, users[j].getEmail(), ResponseTypeType.ACCEPT);
        }
        String exchangeId = Random2.R.nextAlnum(8);
        calItem.setItemId(EwsUtils.createItemId(exchangeId));
        return calItem;
    }
}
