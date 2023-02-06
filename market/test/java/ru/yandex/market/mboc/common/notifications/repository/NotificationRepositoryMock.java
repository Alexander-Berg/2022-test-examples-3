package ru.yandex.market.mboc.common.notifications.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;

import ru.yandex.market.mbo.lightmapper.test.LongGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.notifications.model.Notification;

/**
 * @author prediger
 */
public class NotificationRepositoryMock
    extends LongGenericMapperRepositoryMock<Notification>
    implements NotificationRepository {

    public static final Comparator<Notification> NOTIFICATION_COMPARATOR = (o1, o2) ->
        equalsIgnoringIdAndStatusTimestamp(o1, o2) ? 0 : 1;

    public NotificationRepositoryMock() {
        super(Notification::setId, Notification::getId);

    }

    public static boolean equalsIgnoringIdAndStatusTimestamp(Notification offer1, Notification offer2) {
        boolean equals = EqualsBuilder.reflectionEquals(offer1, offer2, "id", "statusTs", "data");
        boolean dataEquals = EqualsBuilder.reflectionEquals(offer1.getData(), offer2.getData());
        return equals && dataEquals && ((offer1.getStatusTs() == null) == (offer2.getStatusTs() == null));
    }

    @Override
    public List<Notification> findNewNotifications() {
        return findWhere(notification -> notification.getStatus() == Notification.Status.NEW);
    }

    @Override
    public List<Notification> findRecentProcessedNotifications(LocalDateTime statusTs,
                                                               Collection<Integer> supplierIds) {
        return findWhere(notification -> notification.getStatusTs().isAfter(statusTs) &&
            Notification.NotificationType.SUPPLIER_OFFERS_PROCESSED.equals(notification.getNotificationType()) &&
            supplierIds.contains(notification.getSupplierId()));
    }

    @Override
    public long countUnsentNotifications(LocalDateTime statusTs) {
        return countWhere(notification -> notification.getStatusTs().isBefore(statusTs) &&
            Notification.Status.NEW.equals(notification.getStatus()));
    }

    @Override
    public List<Notification> findUnsentNotifications(LocalDateTime statusTs) {
        return findWhere(notification -> notification.getStatusTs().isBefore(statusTs) &&
            Notification.Status.NEW.equals(notification.getStatus()));
    }
}
