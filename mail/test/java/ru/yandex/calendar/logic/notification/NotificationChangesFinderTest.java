package ru.yandex.calendar.logic.notification;

import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.beans.generated.EventNotification;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author Daniel Brylev
 */
public class NotificationChangesFinderTest extends CalendarTestBase {

    @Test
    public void clearChannels() {
        EventNotifications notifications = new EventNotifications(0, Cf.list(
                eventNotification(2, Channel.EMAIL, -20),
                eventNotification(4, Channel.EMAIL, -15),
                eventNotification(6, Channel.SMS, -10),
                eventNotification(8, Channel.SMS, -15),
                eventNotification(10, Channel.DISPLAY, -5),
                eventNotification(12, Channel.AUDIO, -10)));

        NotificationsData.Update updates = NotificationsData.updateChannels(
                Cf.set(Channel.EMAIL, Channel.SMS, Channel.AUDIO),
                Cf.list(Notification.email(Duration.standardMinutes(-20)), Notification.sms(Duration.standardMinutes(-15))));

        EventNotificationChangesInfo changes = NotificationChangesFinder.changes(notifications, updates);
        Assert.equals(Cf.set(4L, 6L, 12L), changes.getDeleteNotificationIds().unique());
        Assert.isEmpty(changes.getCreateNotifications());
    }

    @Test
    public void saveDuplicates() {
        EventNotifications notifications = new EventNotifications(0, Cf.list(
                eventNotification(2, Channel.EMAIL, -15),
                eventNotification(4, Channel.SMS, 10),
                eventNotification(6, Channel.EMAIL, -15)));

        NotificationsData.Update updates = NotificationsData.updateChannels(Cf.toSet(Channel.R.valuesList()), Cf.list(
                Notification.sms(Duration.standardMinutes(10)),
                Notification.email(Duration.standardMinutes(-15)),
                Notification.sms(Duration.standardMinutes(10))));

        EventNotificationChangesInfo changes = NotificationChangesFinder.changes(notifications, updates);
        Assert.hasSize(1, changes.getDeleteNotificationIds());
        Assert.hasSize(1, changes.getCreateNotifications());

        Assert.in(changes.getDeleteNotificationIds().single(), Cf.set(2L, 6L));
        Assert.equals(Channel.SMS, changes.getCreateNotifications().single().getChannel());
        Assert.equals(10, changes.getCreateNotifications().single().getOffsetMinute());
    }

    @Test
    public void emptyUpdates() {
        EventNotifications emptyNotifications = new EventNotifications(0, Cf.<EventNotification>list());
        NotificationsData.Update emptyUpdate = NotificationsData.updateChannels(
                Cf.<Channel>set(), Cf.<Notification>list());
        EventNotifications nonEmptyNotifications = new EventNotifications(0, Cf.list(
                eventNotification(2, Channel.EMAIL, -15)));
        NotificationsData.Update nonEmptyUpdate = NotificationsData.updateChannels(
                Channel.R.valuesList().unique(), Cf.list(Notification.email(Duration.standardMinutes(-15))));

        EventNotificationChangesInfo changes;

        changes = NotificationChangesFinder.changes(nonEmptyNotifications, emptyUpdate);
        Assert.isEmpty(changes.getCreateNotifications());
        Assert.isEmpty(changes.getDeleteNotificationIds());

        changes = NotificationChangesFinder.changes(emptyNotifications, nonEmptyUpdate);
        Assert.hasSize(1, changes.getCreateNotifications());
        Assert.isEmpty(changes.getDeleteNotificationIds());
    }

    private EventNotification eventNotification(long id, Channel channel, int offset) {
        EventNotification notification = new EventNotification();
        notification.setId(id);
        notification.setChannel(channel);
        notification.setOffsetMinute(offset);
        notification.setEventUserId(0);
        return notification;
    }

}
