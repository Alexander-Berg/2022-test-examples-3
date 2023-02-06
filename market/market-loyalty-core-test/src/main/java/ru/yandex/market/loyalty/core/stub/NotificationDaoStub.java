package ru.yandex.market.loyalty.core.stub;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.loyalty.api.model.notifications.NotificationType;
import ru.yandex.market.loyalty.core.dao.ydb.NotificationDao;
import ru.yandex.market.loyalty.core.model.notification.Notification;

public class NotificationDaoStub implements NotificationDao, StubDao {
    private final List<Notification> notifications = new LinkedList<>();

    public NotificationDaoStub() {
    }

    @Override
    public void clear() {
        notifications.clear();
    }

    @Override
    public List<Notification> findNotifications(long uid) {
        return notifications.stream()
                .filter(n -> n.getUid() == uid)
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }

    @Override
    public void insertNotification(Notification notification) {
        notifications.add(notification);
    }

    @Override
    public void markRead(long uid, Collection<String> notificationIds) {
        notifications.stream()
                .filter(n -> n.getUid() == uid)
                .filter(n -> notificationIds.contains(n.getId()))
                .forEach(n -> n.setRead(true));
    }

    @Override
    public long countNotifications(long uid, NotificationType type) {
        return notifications.stream()
                .filter(n -> n.getUid() == uid)
                .filter(n -> n.getType() == type)
                .count();
    }

    public List<Notification> getNotifications() {
        return notifications;
    }
}
