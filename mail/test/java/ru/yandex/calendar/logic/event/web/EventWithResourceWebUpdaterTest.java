package ru.yandex.calendar.logic.event.web;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.frontend.ews.imp.ExchangeEventDataConverter;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationUpdateData;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ResourceParticipantInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

public class EventWithResourceWebUpdaterTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EventWebUpdater eventWebUpdater;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private EventResourceDao eventResourceDao;

    @Test
    public void updateEventWithInvitedResourceWithoutExchangeId() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11110");

        Event event = testManager.createDefaultEvent(user.getUid(), "updateEventWithInvitedResourceWithoutExchangeId");
        testManager.addUserParticipantToEvent(event.getId(), user.getLogin(), Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        ensureAllMeetingInRoomCancelled(event.getStartTs(), event.getEndTs());

        // update first occurrence of event
        EventData eventData = new EventData();
        Event updatingEvent = new Event();
        updatingEvent.setId(event.getId());
        updatingEvent.setName("New event name");
        eventData.setEvent(updatingEvent);
        eventData.setInstanceStartTs(event.getStartTs());
        eventData.setInvData(EventInvitationUpdateData.EMPTY);
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebUpdater.update(user.getUserInfo(), eventData, NotificationsData.notChanged(), true,
                ActionInfo.webTest(event.getStartTs().minus(Duration.standardHours(1))));

        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        CalendarItemType updatedItem = ewsProxyWrapper.getEvent(exchangeId).get();
        Assert.A.equals(updatingEvent.getName(), updatedItem.getSubject());
    }

    @Test
    public void updateOccurrence() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11111");

        Event event = testManager.createDefaultEvent(user.getUid(), "updateOccurrence");
        Instant dueTs = TestDateTimes.plusDays(event.getStartTs(), 2);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(event.getId(), dueTs);
        testManager.addUserParticipantToEvent(event.getId(), user.getLogin(), Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        ensureAllMeetingInRoomCancelled(event.getStartTs(), dueTs);

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest());
        ResourceParticipantInfo resourceParticipant =
            resourceDao.findResourceParticipants(Cf.list(event.getId())).single();
        Assert.A.isTrue(resourceParticipant.getExchangeId().isPresent());

        // update first occurrence of event
        EventData eventData = new EventData();
        Event updatingEvent = new Event();
        updatingEvent.setId(event.getId());
        updatingEvent.setStartTs(TestDateTimes.plusHours(event.getStartTs(), 1));
        updatingEvent.setEndTs(TestDateTimes.plusHours(event.getEndTs(), 1));
        eventData.setEvent(updatingEvent);
        eventData.setInstanceStartTs(event.getStartTs());
        eventData.setInvData(EventInvitationUpdateData.EMPTY);
        eventData.setTimeZone(MoscowTime.TZ);

        long recurrenceEventId = eventWebUpdater.update(user.getUserInfo(), eventData, NotificationsData.notChanged(),
                false, ActionInfo.webTest()).getNewEventId().get();

        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        String recurrenceExchangeId = eventResourceDao.findExchangeIds(Cf.list(recurrenceEventId)).single();
        Assert.A.notEquals(exchangeId, recurrenceExchangeId);

        CalendarItemType updatedItem = ewsProxyWrapper.getEvent(recurrenceExchangeId).get();
        Assert.A.equals(removeSeconds(updatingEvent.getStartTs()), EwsUtils.xmlGregorianCalendarInstantToInstant(updatedItem.getStart()));
        Assert.A.equals(removeSeconds(updatingEvent.getEndTs()), EwsUtils.xmlGregorianCalendarInstantToInstant(updatedItem.getEnd()));
        Assert.A.equals(removeSeconds(event.getStartTs()), EwsUtils.xmlGregorianCalendarInstantToInstant(updatedItem.getRecurrenceId()));
    }

    @Test
    public void inviteResourceOnOccurrence() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11112");

        Event event = testManager.createDefaultEvent(user.getUid(), "inviteResourceOnOccurrence");
        Instant dueTs = TestDateTimes.plusDays(event.getStartTs(), 2);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(event.getId(), dueTs);
        testManager.addUserParticipantToEvent(event.getId(), user.getLogin(), Decision.UNDECIDED, true);

        InstantInterval interval = new InstantInterval(event.getStartTs(), dueTs);
        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                TestManager.testExchangeThreeLittlePigsEmail, interval);

        // update first occurrence of event
        EventData eventData = new EventData();
        Event updatingEvent = new Event();
        updatingEvent.setId(event.getId());
        eventData.setEvent(updatingEvent);
        eventData.setInstanceStartTs(event.getStartTs());
        EventInvitationUpdateData eventInvitationUpdateData = new EventInvitationUpdateData(
                Cf.list(ResourceRoutines.getResourceEmail(r)),
                Cf.list());
        eventData.setInvData(eventInvitationUpdateData);
        long recurrenceEventId = eventWebUpdater.update(user.getUserInfo(), eventData, NotificationsData.notChanged(),
                false, ActionInfo.webTest()).getNewEventId().get();
        String recurrenceExchangeId = eventResourceDao.findExchangeIds(Cf.list(recurrenceEventId)).single();
        CalendarItemType updatedItem = ewsProxyWrapper.getEvent(recurrenceExchangeId).get();
        SetF<Email> emails = ExchangeEventDataConverter.getAttendeeEmails(updatedItem).unique();
        Assert.assertTrue(emails.containsTs(ResourceRoutines.getResourceEmail(r)));
    }

    private Instant removeSeconds(Instant instant) {
        DateTime dt = new DateTime(instant, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        return dt.minusSeconds(dt.getSecondOfMinute()).toInstant();
    }

    private void ensureAllMeetingInRoomCancelled(Instant startTs, Instant endTs) {
        InstantInterval interval = new InstantInterval(startTs, endTs);

        while (ewsProxyWrapper
                .findInstanceEventIds(TestManager.testExchangeThreeLittlePigsEmail, interval)
                .isNotEmpty()) {
            ewsProxyWrapper.cancelMeetings(
                    TestManager.testExchangeThreeLittlePigsEmail, startTs, endTs);
        }
    }
}
