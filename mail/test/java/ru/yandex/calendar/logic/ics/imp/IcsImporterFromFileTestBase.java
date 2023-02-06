package ru.yandex.calendar.logic.ics.imp;

import org.springframework.beans.factory.annotation.Autowired;

import Yandex.RequestPackage.RequestData;

import ru.yandex.bolts.collection.SetF;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.RequestEventDataConverter;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.util.data.AliasedRequestDataProvider;
import ru.yandex.calendar.util.data.DataProvider;
import ru.yandex.calendar.util.idlent.RequestDataFactory;
import ru.yandex.calendar.util.idlent.RequestWrapper;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public abstract class IcsImporterFromFileTestBase extends AbstractDbDataTest {

    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    protected IcsImporter icsImporter;

    protected long importOneEventIcs(String icsFilename, PassportUid uid) throws Exception {
        IcsImportStats stats = importIcsByUid(icsFilename, uid);
        SetF<Long> eIds = stats.getProcessedEventIds();
        Assert.assertNotEmpty(eIds);
        // XXX: upyachka: returning random event id
        return eIds.iterator().next();
    }

    protected IcsImportStats importIcsByUid(String icsFilename, PassportUid uid) throws Exception {
        return importCalendar(uid, loadCalendar(icsFilename));
    }

    protected IcsCalendar loadCalendar(String icsFilename) {
        return IcsCalendar.parse(new File2("unittest_data/db/ics/" + icsFilename));
    }

    protected IcsImportStats importCalendar(PassportUid uid, IcsCalendar c) {
        return icsImporter.importIcsStuff(uid, c, IcsImportMode.importFile(LayerReference.byCategory()));
    }

    protected IcsImportStats importIcsByUidWithStatCheck(String fileName, PassportUid uid, long[] expectedStat) throws Exception {
        IcsImportStats stats = importIcsByUid(fileName, uid);
        Assert.assertEquals(expectedStat[0], stats.getProcessedCount());
        Assert.assertEquals(expectedStat[1], stats.getTotalCount());
        return stats;
    }

    protected CreateInfo createEventFromXml(String xmlFileName, InvitationProcessingMode mode, PassportUid uid) {
        byte[] xml = new File2("unittest_data/db/" + xmlFileName).readBytes();
        RequestData rd = RequestDataFactory.createFromXml(xml, true);
        DataProvider dp = new AliasedRequestDataProvider(new RequestWrapper(rd));
        EventData eventData = RequestEventDataConverter.convertAndValidate(MoscowTime.TZ, MoscowTime.TZ, dp);
        long mainEventId = eventRoutines.createMainEvent(uid, eventData, ActionInfo.webTest());
        return eventRoutines.createUserOrFeedEvent(
                UidOrResourceId.user(uid), EventType.USER, mainEventId, eventData,
                NotificationsData.create(eventData.getEventUserData().getNotifications()), mode, ActionInfo.webTest());
    }

}
