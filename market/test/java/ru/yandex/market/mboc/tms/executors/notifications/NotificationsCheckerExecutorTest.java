package ru.yandex.market.mboc.tms.executors.notifications;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepository;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

public class NotificationsCheckerExecutorTest extends BaseDbTestClass {

    @Autowired
    private NotificationRepository repository;
    @Autowired
    private SupplierRepository supplierRepository;

    private NotificationsCheckerExecutor checkerExecutor;

    @Before
    public void setUp() throws Exception {
        checkerExecutor = new NotificationsCheckerExecutor(repository);
        supplierRepository.insertBatch(
            YamlTestUtil.readSuppliersFromResource("notifications/tms-suppliers.yml"));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void doRealJob() throws Exception {
        List<Notification> notifications = YamlTestUtil
            .readNotificationsFromResources("notifications/tms-notifications-checker.yml");

        repository.insert(notifications.get(0));
        checkerExecutor.execute();

        repository.insert(notifications.get(1));
        checkerExecutor.execute();

        Assertions.assertThat(repository.findById(notifications.get(1).getId()).getStatus())
            .isEqualByComparingTo(Notification.Status.FAILED);

        notifications.get(2).setStatusTsInternal(DateTimeUtils.dateTimeNow().minusHours(5));
        repository.insert(notifications.get(2));

        Assertions.assertThatThrownBy(() -> checkerExecutor.execute())
            .isInstanceOf(RuntimeException.class)
            .hasMessage(String.format("Found old notifications, total: %s", 1));
    }
}
