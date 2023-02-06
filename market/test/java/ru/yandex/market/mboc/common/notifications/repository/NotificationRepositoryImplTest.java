package ru.yandex.market.mboc.common.notifications.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.mboc.common.dict.SupplierRepositoryImpl;
import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author prediger
 */
public class NotificationRepositoryImplTest extends BaseDbTestClass {

    @Autowired
    private NotificationRepositoryImpl repository;

    @Autowired
    private SupplierRepositoryImpl supplierRepository;

    @Before
    public void setUp() {
        supplierRepository
            .insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
        repository.insertBatch(YamlTestUtil
            .readNotificationsFromResources("notifications/sample-notifications-batch.yml"));
    }

    @Test
    public void testForeignKeyDeletion() {
        Notification sample = YamlTestUtil
            .readFromResources("notifications/sample-notification.yml", Notification.class);
        repository.insert(sample);
        Assertions.assertThatThrownBy(() -> supplierRepository
            .delete(supplierRepository.findById(sample.getSupplierId())))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testNotificationNotFound() {
        assertThatThrownBy(() -> repository.findById(-1L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testInsertNotification() {
        Notification sample = YamlTestUtil
            .readFromResources("notifications/sample-notification.yml", Notification.class);
        Notification inserted = repository.insert(sample);

        Assertions.assertThat(repository.findById(inserted.getId()))
            .usingComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .isEqualTo(inserted);
    }

    @Test
    public void testUpdateNotification() {
        Notification sample = YamlTestUtil
            .readFromResources("notifications/sample-notification.yml", Notification.class);
        Notification inserted = repository.insert(sample);

        Notification updatedValues = YamlTestUtil
            .readFromResources("notifications/sample-notification-update.yml", Notification.class);
        updatedValues.setId(inserted.getId());
        repository.update(updatedValues);

        Assertions.assertThat(repository.findById(inserted.getId()))
            .usingComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .isEqualTo(updatedValues);
    }

    @Test
    public void testFindNewNotifications() {
        Notification[] newSamples = YamlTestUtil
            .readNotificationsFromResources("notifications/sample-notifications-batch.yml")
            .stream()
            .filter(notification -> notification.getStatus() == Notification.Status.NEW)
            .toArray(Notification[]::new);

        Assertions.assertThat(repository.findNewNotifications())
            .usingElementComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .containsExactlyInAnyOrder(newSamples);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testFindRecentSupplierProcessedNotifications() {
        LocalDateTime statusTs = DateTimeUtils.dateTimeNow().minusHours(1);
        List<Integer> supplierIds = Arrays.asList(42, 43);
        Notification[] recentNotifications = YamlTestUtil
            .readNotificationsFromResources("notifications/sample-notifications-batch.yml")
            .stream()
            .filter(notification -> notification.getStatusTs().isAfter(statusTs) &&
                Notification.NotificationType.SUPPLIER_OFFERS_PROCESSED.equals(notification.getNotificationType()) &&
                supplierIds.contains(notification.getSupplierId()))
            .toArray(Notification[]::new);

        Assertions.assertThat(repository.findRecentProcessedNotifications(statusTs, supplierIds))
            .usingElementComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .containsExactlyInAnyOrder(recentNotifications);
    }

    @Test
    public void testDifferentToDataTypesInsertAndSelect() {
        Notification[] dataSamples = YamlTestUtil
            .readNotificationsFromResources("notifications/sample-notifications-data-test.yml")
            .toArray(new Notification[0]);

        List<Notification> notifications = repository.insertBatch(Arrays.asList(dataSamples));

        Assertions.assertThat(
            repository.findByIds(notifications.stream()
                .map(Notification::getId)
                .collect(Collectors.toList())))
            .usingElementComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .containsExactlyInAnyOrder(dataSamples);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testCountNewNotifications() {
        Assertions.assertThat(repository.countUnsentNotifications(DateTimeUtils.dateTimeNow().minusMinutes(10L)))
            .isEqualTo(2L);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testFindOldNewNotifications() {
        LocalDateTime statusTs = DateTimeUtils.dateTimeNow().minusMinutes(10);

        Notification[] newSamples = YamlTestUtil
            .readNotificationsFromResources("notifications/sample-notifications-batch.yml")
            .stream()
            .filter(notification -> notification.getStatus() == Notification.Status.NEW &&
                notification.getStatusTs().isBefore(statusTs))
            .toArray(Notification[]::new);

        Assertions.assertThat(repository.findUnsentNotifications(statusTs))
            .usingElementComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .containsExactlyInAnyOrder(newSamples);
    }

}
