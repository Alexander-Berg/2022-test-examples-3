package ru.yandex.calendar.logic.notification;

import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventNotification;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.repetition.EventAndRepetition;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

/**
 * @author dbrylev
 */
public class NotificationRoutinesTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private NotificationRoutines notificationRoutines;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private LayerRoutines layerRoutines;

    @Test
    public void allDayInUserTz() {
        DateTimeZone eventTz = DateTimeZone.forOffsetHours(3);
        DateTimeZone userTz = DateTimeZone.forOffsetHours(2);

        LocalDateTime start = LocalDate.now(eventTz).plusDays(1).toLocalDateTime(LocalTime.MIDNIGHT);

        EventAndRepetition event = createAllDayEventWithYearlyRepetition(start.toLocalDate(), eventTz);
        EventNotification notification = new EventNotification();
        notification.setOffsetMinute(-15);

        Option<Instant> nextSendTs = NotificationRoutines.recalcNextSendTs(
                notification, event.getEvent(), event.getRepetitionInfo(), userTz, Instant.now());

        Assert.some(start.toDateTime(userTz).minusMinutes(15).toInstant(), nextSendTs);

        nextSendTs = NotificationRoutines.recalcNextSendTs(
                notification, event.getEvent(), event.getRepetitionInfo(), userTz, nextSendTs.get().plus(1));

        Assert.some(start.toDateTime(userTz).plusYears(1).minusMinutes(15).toInstant(), nextSendTs);
    }

    @Test
    public void allDayOffsetPointsToLocalTime() {
        DateTimeZone tz = DateTimeZone.forID("Europe/Kiev");

        LocalDateTime localStart = new LocalDateTime(2014, 3, 30, 0, 0);
        LocalDateTime localGap = localStart.withTime(3, 30, 0, 0);
        LocalDateTime localReminder = localStart.withTime(5, 0, 0, 0);

        EventAndRepetition event = createAllDayEventWithYearlyRepetition(localStart.toLocalDate(), tz);

        EventNotification notification = new EventNotification();
        notification.setOffsetMinute((int) new Period(localStart, localReminder).toStandardDuration().getStandardMinutes());

        Option<Instant> nextSendTs = NotificationRoutines.recalcNextSendTs(
                notification, event.getEvent(), event.getRepetitionInfo(), tz,
                localStart.minusDays(1).toDateTime(tz).toInstant());

        Assert.isTrue(tz.isLocalDateTimeGap(localGap));
        Assert.some(localReminder.toDateTime(tz).toInstant(), nextSendTs);
    }

    @Test
    public void unstoppableAllDayReminder() { // CAL-6659
        DateTimeZone tz = DateTimeZone.forID("Europe/Tallinn");

        LocalDate dateOfTzTransition = new LocalDate(2014, 3, 30);
        EventAndRepetition event = createAllDayEventWithYearlyRepetition(dateOfTzTransition, tz);

        EventNotification notification = new EventNotification();
        notification.setOffsetMinute(1080);

        Instant now = dateOfTzTransition.minusDays(1).toDateTimeAtStartOfDay(tz).toInstant();

        Instant nextSendTs = NotificationRoutines.recalcNextSendTs(
                notification, event.getEvent(), event.getRepetitionInfo(), tz, now).get();

        Instant nextNextSendTs = NotificationRoutines.recalcNextSendTs(
                notification, event.getEvent(), event.getRepetitionInfo(), tz, nextSendTs.plus(1)).get();

        Assert.equals(2014, nextSendTs.get(DateTimeFieldType.year()));
        Assert.equals(2015, nextNextSendTs.get(DateTimeFieldType.year()));
    }

    // CAL-6480
    @Test
    public void dontSendNotificationInThePast() {
        EventNotification en = new EventNotification();
        en.setOffsetMinute(-60);
        Event e = new Event();
        e.setIsAllDay(false);
        Instant now = Instant.now();
        RepetitionInstanceInfo rii = RepetitionInstanceInfo.noRepetition(
                new InstantInterval(now.plus(Duration.standardMinutes(30)), Duration.standardHours(2)));
        Option<Instant> nextSendTs = NotificationRoutines.recalcNextSendTs(en, e, rii, DateTimeZone.UTC, now);
        // none cause we never send notification whose next_send_ts is already in the past
        Assert.none(nextSendTs);

        en.setOffsetMinute(-10);
        Option<Instant> nextSendTs2 = NotificationRoutines.recalcNextSendTs(en, e, rii, DateTimeZone.UTC, now);
        // not none, cause next_send_ts is in the future
        Assert.some(now.plus(Duration.standardMinutes(20)), nextSendTs2);
    }

    @Test
    public void keepNextSendTs() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(12).getUid();
        Event event = testManager.createDefaultEvent(uid, "Event");
        long eventUserId = addUserToEventGetEventUserId(uid, event);

        notificationRoutines.recalcAndSaveEventNotifications(eventUserId,
                Cf.list(Notification.email(minutes(-25)), Notification.email(minutes(-5))),
                eventDbManager.getEventAndRepetitionByEvent(event),
                ActionInfo.webTest(event.getStartTs().plus(minutes(-10))));

        ListF<EventNotification> notifications = getNotificationsSortedByOffset(uid, event);
        Assert.none(notifications.get(0).getNextSendTs());
        Assert.some(event.getStartTs().plus(minutes(-5)), notifications.get(1).getNextSendTs());

        notificationRoutines.recalcNextSendTs(eventUserId, ActionInfo.webTest(event.getStartTs().plus(minutes(-30))));
        notifications = getNotificationsSortedByOffset(uid, event);
        Assert.some(event.getStartTs().plus(minutes(-25)), notifications.get(0).getNextSendTs());
        Assert.some(event.getStartTs().plus(minutes(-5)), notifications.get(1).getNextSendTs());

        notificationRoutines.recalcNextSendTs(eventUserId, ActionInfo.webTest(event.getStartTs()));
        notifications = getNotificationsSortedByOffset(uid, event);
        Assert.some(event.getStartTs().plus(minutes(-25)), notifications.get(0).getNextSendTs());
        Assert.some(event.getStartTs().plus(minutes(-5)), notifications.get(1).getNextSendTs());

        Event update = event.copy();
        update.setStartTs(event.getStartTs().plus(minutes(10)));
        update.setEndTs(event.getEndTs().plus(minutes(10)));
        eventDao.updateEvent(update);

        notificationRoutines.recalcNextSendTs(eventUserId, ActionInfo.webTest(update.getStartTs().plus(minutes(-10))));
        notifications = getNotificationsSortedByOffset(uid, event);
        Assert.none(notifications.get(0).getNextSendTs());
        Assert.some(update.getStartTs().plus(minutes(-5)), notifications.get(1).getNextSendTs());
    }

    @Test
    public void recalcForNewRecurrence() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(13).getUid();
        Event master = testManager.createDefaultEventWithDailyRepetition(uid, "Repeating");
        long masterEventUserId = addUserToEventGetEventUserId(uid, master);

        notificationDbManager.saveEventNotifications(masterEventUserId, Cf.list(
                Notification.email(minutes(-25)),
                Notification.email(minutes(-5)),
                Notification.email(minutes(0))));

        notificationRoutines.recalcNextSendTs(
                masterEventUserId, ActionInfo.webTest(master.getStartTs().plus(minutes(-10))));

        ListF<EventNotification> notifications = getNotificationsSortedByOffset(uid, master);
        Assert.some(master.getStartTs().plus(days(1)).plus(minutes(-25)), notifications.get(0).getNextSendTs()); // sent
        Assert.some(master.getStartTs().plus(minutes(-5)), notifications.get(1).getNextSendTs());
        Assert.some(master.getStartTs().plus(minutes(0)), notifications.get(2).getNextSendTs());

        Event recurrence = testManager.createDefaultRecurrence(uid, master.getId(), master.getStartTs());
        long recurrenceEventUserId = addUserToEventGetEventUserId(uid, recurrence);

        notificationDbManager.saveEventNotifications(recurrenceEventUserId, Cf.list(
                Notification.email(minutes(-25)),
                Notification.email(minutes(-25)),
                Notification.email(minutes(-5)),
                Notification.email(minutes(-3))));

        notificationRoutines.recalcNextSendTsForNewRecurrenceOrTail(
                master.getId(), recurrence.getId(), ActionInfo.webTest(master.getStartTs()));

        notifications = getNotificationsSortedByOffset(uid, master);
        Assert.some(master.getStartTs().plus(days(1)).plus(minutes(-25)), notifications.get(0).getNextSendTs());
        Assert.some(master.getStartTs().plus(days(1)).plus(minutes(-5)), notifications.get(1).getNextSendTs());
        Assert.some(master.getStartTs().plus(days(1)), notifications.get(2).getNextSendTs());

        notifications = getNotificationsSortedByOffset(uid, recurrence);
        Assert.none(notifications.get(0).getNextSendTs()); // sent for master
        Assert.none(notifications.get(1).getNextSendTs()); // expired
        Assert.some(recurrence.getStartTs().plus(minutes(-5)), notifications.get(2).getNextSendTs()); // not sent for master
        Assert.none(notifications.get(3).getNextSendTs()); // expired
    }

    // CAL-6869
    @Test
    public void moveRepeatingEventToPast() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(14).getUid();
        Event event = testManager.createDefaultEventWithDailyRepetition(uid, "Repeating");

        Instant movedStart = event.getStartTs().minus(Duration.standardDays(1));

        long eventUserId = addUserToEventGetEventUserId(uid, event);
        notificationDbManager.saveEventNotifications(eventUserId, Cf.list(Notification.email(minutes(0))));

        notificationRoutines.recalcNextSendTs(eventUserId, ActionInfo.webTest(movedStart.plus(minutes(-5))));
        ListF<EventNotification> notifications = getNotificationsSortedByOffset(uid, event);
        Assert.some(event.getStartTs(), notifications.single().getNextSendTs());

        event.setStartTs(movedStart);
        event.setEndTs(movedStart.plus(minutes(60)));
        eventDao.updateEvent(event);

        notificationRoutines.recalcNextSendTs(eventUserId, ActionInfo.webTest(movedStart.plus(minutes(-5))));
        notifications = getNotificationsSortedByOffset(uid, event);
        Assert.some(movedStart, notifications.single().getNextSendTs());
    }

    @Test
    public void sortNotificationsForEvent() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(15).getUid();
        Event event = testManager.createDefaultEvent(uid, "Single");
        long eventUserId = addUserToEventGetEventUserId(uid, event);
        notificationDbManager.saveEventNotifications(eventUserId, Cf.list(
                Notification.email(minutes(-3)),
                Notification.email(minutes(0)),
                Notification.email(minutes(-25)),
                Notification.sms(minutes(-25)),
                Notification.sms(minutes(-10)),
                Notification.email(minutes(-10))
        ));

        ListF<EventNotification> notifications =
                notificationDbManager.getNotificationsByEventUserId(eventUserId).getEventNotifications();

        ListF<Integer> offsets = notifications.map(EventNotification::getOffsetMinute);
        ListF<Channel> channels = notifications.map(EventNotification::getChannel);

        Assert.equals(Cf.list(-25, -25, -10, -10, -3, -0), offsets);
        Assert.equals(Cf.list(Channel.EMAIL, Channel.SMS, Channel.EMAIL, Channel.SMS, Channel.EMAIL, Channel.EMAIL),
                channels);
    }

    @Test
    public void sortNotificationsForLayer() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(16).getUid();
        long layerId = testManager.createLayer(uid);
        long layerUserId = layerRoutines.getLayerUser(layerId, uid).get().getId();

        notificationDbManager.saveLayerNotifications(layerUserId, Cf.list(
                Notification.email(minutes(-3)),
                Notification.email(minutes(0)),
                Notification.email(minutes(-25)),
                Notification.sms(minutes(-25)),
                Notification.sms(minutes(-10)),
                Notification.email(minutes(-10))
        ));

        ListF<Notification> notifications =
                notificationDbManager.getNotificationsByLayerUserId(layerUserId);

        ListF<Long> offsets = notifications.map(n -> n.getOffset().getStandardMinutes());
        ListF<Channel> channels = notifications.map(Notification::getChannel);

        Assert.equals(Cf.list(-25L, -25L, -10L, -10L, -3L, -0L), offsets);
        Assert.equals(Cf.list(Channel.EMAIL, Channel.SMS, Channel.EMAIL, Channel.SMS, Channel.EMAIL, Channel.EMAIL),
                channels);
    }

    private static Duration minutes(long minutes) {
        return Duration.standardMinutes(minutes);
    }

    private static Duration days(long days) {
        return Duration.standardDays(days);
    }

    private ListF<EventNotification> getNotificationsSortedByOffset(PassportUid uid, Event event) {
        return notificationDbManager.getEventUserWithNotificationsByUidAndEventId(uid, event.getId()).get()
                .getNotifications().getEventNotifications()
                .sorted(EventNotification.getOffsetMinuteF().andThenNaturalComparator());
    }

    private long addUserToEventGetEventUserId(PassportUid uid, Event event) {
        testManager.addUserParticipantToEvent(event.getId(), uid, Decision.YES, false);
        return notificationDbManager.getEventUserWithNotificationsByUidAndEventId(
                uid, event.getId()).get().getEventUser().getId();
    }

    private static EventAndRepetition createAllDayEventWithYearlyRepetition(LocalDate date, DateTimeZone tz) {
        Event event = new Event();
        event.setStartTs(date.toDateTimeAtStartOfDay(tz).toInstant());
        event.setEndTs(date.toDateTimeAtStartOfDay(tz).plusDays(1).toInstant());
        event.setIsAllDay(true);

        Repetition repetition = new Repetition();
        repetition.setType(RegularRepetitionRule.YEARLY);
        repetition.setREach(1);
        repetition.setDueTsNull();

        RepetitionInstanceInfo repetitionInfo = RepetitionInstanceInfo.create(
                new InstantInterval(event.getStartTs(), event.getEndTs()), tz, repetition);

        return new EventAndRepetition(event, repetitionInfo);
    }
}
