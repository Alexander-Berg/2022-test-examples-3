package ru.yandex.calendar.logic.ics.feed;

import net.fortuna.ical4j.model.property.Transp;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.frontend.web.cmd.run.Situation;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.IcsFeedFields;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.event.EventInfoDbLoader;
import ru.yandex.calendar.logic.event.EventLoadLimits;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.repetition.EventAndRepetition;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.exp.IcsHolidaysExporter;
import ru.yandex.calendar.logic.ics.imp.IcsImportMode;
import ru.yandex.calendar.logic.ics.imp.IcsImportStats;
import ru.yandex.calendar.logic.ics.imp.IcsImporter;
import ru.yandex.calendar.logic.ics.imp.LayerReference;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsTransp;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.layer.LayerType;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.web.servletContainer.SingleWarJetty;

public class IcsFeedManagerTest extends AbstractConfTest {
    @Autowired
    private IcsFeedManager icsFeedManager;
    @Autowired
    private IcsFeedDao icsFeedDao;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventInfoDbLoader eventInfoDbLoader;

    private SingleWarJetty jetty;
    private String exportUrl;

    @Before
    public void setup() {
        jetty = testManager.startFileResourceJetty();
        exportUrl = "http://localhost:" + jetty.getActualHttpPort() + "/unittest_data/db/ics/import/eduboard_fixed.ics";
    }

    @After
    public void stop() {
        jetty.stop();
    }

    @Test
    public void repeatedSubscription() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-19300");

        Assert.isEmpty(icsFeedDao.findIcsFeeds(IcsFeedFields.UID.eq(user.getUid())));
        Assert.isEmpty(layerDao.findLayersByTypesAndLayerUserUid(user.getUid(), Cf.list(LayerType.FEED)));

        icsFeedManager.subscribe(user.getUid(), exportUrl, "XXX");

        Assert.hasSize(1, icsFeedDao.findIcsFeeds(IcsFeedFields.UID.eq(user.getUid())));
        Assert.hasSize(1, layerDao.findLayersByTypesAndLayerUserUid(user.getUid(), Cf.list(LayerType.FEED)));

        try {
            icsFeedManager.subscribe(user.getUid(), exportUrl, "YYY");
            Assert.fail("CommandRunException expected");
        } catch (CommandRunException e) {
            Assert.isTrue(e.isSituation(Situation.ICS_FEED_ALREADY_SUBSCRIBED_URL));
        }
    }

    @Test
    public void webcalScheme() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-19301");

        Assert.isEmpty(eventLayerDao.findEventLayersByLayerId(user.getDefaultLayerId()));

        icsFeedManager.importContent(user.getUid(), exportUrl, "ZZZ", LayerReference.defaultLayer());

        Assert.notEmpty(eventLayerDao.findEventLayersByLayerId(user.getDefaultLayerId()));
    }

    @Test
    public void holidays() {
        Assert.equals(
                IcsFeedUpdater.md5Hash(IcsHolidaysExporter.export("holidays://russia").serializeToBytes()),
                IcsFeedUpdater.md5Hash(IcsHolidaysExporter.export("holidays://russia").serializeToBytes()));

        TestUserInfo user = testManager.prepareUser("yandex-team-mm-19305");
        icsFeedManager.importContent(user.getUid(), "holidays://russia", "ZZZ", LayerReference.defaultLayer());

        Function<LocalDate, Option<EventAndRepetition>> find = date ->
                eventInfoDbLoader.getEventsOnLayers(Cf.list(user.getDefaultLayerId()),
                        EventLoadLimits.intersectsInterval(new InstantInterval(
                                date.toDateTime(new LocalTime(12, 0), MoscowTime.TZ).toInstant(),
                                date.toDateTime(new LocalTime(12, 0), MoscowTime.TZ).toInstant())))
                        .singleO();

        Option<EventAndRepetition> found = find.apply(new LocalDate(2017, 1, 1));
        Assert.some("Новогодние каникулы", found.map(e -> e.getEvent().getName()));
        Assert.equals(RegularRepetitionRule.YEARLY, found.get().getRepetitionInfo().getRepetition().get().getType());

        Assert.some("Новый год", find.apply(new LocalDate(2004, 1, 1)).map(e -> e.getEvent().getName()));

        icsFeedManager.importContent(user.getUid(), "holidays://tatarstan", "ZZZ", LayerReference.defaultLayer());
        found = find.apply(new LocalDate(2017, 9, 1));

        Assert.some("Курбан-Байрам", found.map(e -> e.getEvent().getName()));
        Assert.none(found.get().getRepetitionInfo().getRepetition());

        Assert.none(find.apply(new LocalDate(2017, 11, 26)));

        icsFeedManager.importContent(user.getUid(), "holidays://russia?labels=1", "ZZZ", LayerReference.defaultLayer());
        found = find.apply(new LocalDate(2017, 11, 26));

        Assert.some("День матери", found.map(e -> e.getEvent().getName()));
        Assert.equals(RegularRepetitionRule.MONTHLY_DAY_WEEKNO, found.get().getRepetitionInfo().getRepetition().get().getType());
        Assert.some(true, found.get().getRepetitionInfo().getRepetition().get().getRMonthlyLastweek());
    }

    @Test
    public void userAvailabilityAfterImportSomeonesMeeting() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-19302");

        icsFeedManager.subscribe(user.getUid(), exportUrl, "VVV");
        Layer feedLayer = layerDao.findLayersByTypesAndLayerUserUid(user.getUid(), Cf.list(LayerType.FEED)).single();


        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-19303");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-19304");

        String externalId = CalendarUtils.generateExternalId();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary("checkUserAvailability");
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail());
        vevent = vevent.withUid(externalId);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 10, 11, 22, 52));
        vevent = vevent.withTransp(new IcsTransp(Transp.OPAQUE.getValue()));
        vevent = vevent.withSequenece(0);

        IcsImportStats stats = icsImporter.importIcsStuff(
                user.getUid(), vevent.makeCalendar(), IcsImportMode.updateFeed(feedLayer.getId()));
        Assert.hasSize(1, stats.getNewEventIds());

        EventUser eventUser = eventUserDao.findEventUserByEventIdAndUid(stats.getNewEventIds().single(), user.getUid()).get();
        Assert.A.equals(Availability.AVAILABLE, eventUser.getAvailability());
    }

}
