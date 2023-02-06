package ru.yandex.calendar.frontend.caldav.proto.facade;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.caldav.proto.tree.CollectionId;
import ru.yandex.calendar.frontend.caldav.userAgent.UserAgentType;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class CaldavCalendarFacadeImplTest extends AbstractCaldavTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private CaldavCalendarFacade caldavCalendarFacade;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private EventRoutines eventRoutines;

    @Test
    public void getEventWithData() {
        getEvent(true);
    }

    @Test
    public void getEventWithoutData() {
        getEvent(false);
    }

    private void getEvent(boolean withData) {
        TestUserInfo user = testManager.prepareUser(withData ? "yandex-team-mm-12901" : "yandex-team-mm-12902");
        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "getEventWithData");
        long layerId = eventLayerDao.findEventLayersByEventId(event.getId()).single().getLayerId();
        String externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();
        Option<CalendarComponent> gotEvent = caldavCalendarFacade.getUserCalendarEvent(
                user.getUid(), CollectionId.events(user.getEmail().getEmail(), Long.toString(layerId), user.getUid()),
                externalId + ".ics", withData, UserAgentType.ICAL);

        Assert.A.some(gotEvent);
        if (withData) {
            Assert.A.some(gotEvent.get().getIcal());
        } else {
            Assert.A.none(gotEvent.get().getIcal());
        }
    }

    @Test
    public void getEventByFilename() {
        String externalId = "getEventByFilenameyandex.ru";
        String filename = "getEventByFilenameyandex.ru.ics";

        TestUserInfo user = testManager.prepareUser("yandex-team-mm-12911");
        long mainEventId = eventRoutines.createMainEvent(externalId, MoscowTime.TZ, ActionInfo.webTest());

        Event eventOverrides = new Event();
        eventOverrides.setSequence(0);
        Event event = testManager.createDefaultEvent(user.getUid(), "getEventByFilename", eventOverrides, mainEventId);
        testManager.createEventUser(user.getUid(), event.getId(), Decision.YES, Option.<Boolean>empty());
        testManager.createEventLayer(layerRoutines.getOrCreateDefaultLayer(user.getUid()), event.getId());

        long layerId = eventLayerDao.findEventLayersByEventId(event.getId()).single().getLayerId();
        CalendarComponent gotEvent = caldavCalendarFacade.getUserCalendarEvent(
                user.getUid(), CollectionId.events(user.getEmail().getEmail(), Long.toString(layerId), user.getUid()),
                filename, true, UserAgentType.ICAL).get();

        Assert.A.equals(filename, gotEvent.getFileName());
    }
}
