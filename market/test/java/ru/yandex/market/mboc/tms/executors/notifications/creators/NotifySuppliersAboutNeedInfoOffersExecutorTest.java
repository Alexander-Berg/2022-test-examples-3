package ru.yandex.market.mboc.tms.executors.notifications.creators;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepository;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

@SuppressWarnings("checkstyle:magicnumber")
public class NotifySuppliersAboutNeedInfoOffersExecutorTest extends BaseDbTestClass {
    protected NotifySuppliersAboutNeedInfoOffersExecutor executor;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @Before
    public void setUp() {
        executor = new NotifySuppliersAboutNeedInfoOffersExecutor(offerRepository,
            notificationRepository);
        supplierRepository.insertBatch(
            YamlTestUtil.readSuppliersFromResource("notifications/tms-suppliers.yml"));
    }

    @Test
    public void testSupplierNeedInfoRepeatedOffers() throws Exception {
        notificationRepository.insertBatch(YamlTestUtil
            .readNotificationsFromResources("notifications/tms-repeat-supplier-notifications-input.yml"));
        offerRepository.insertOffers(YamlTestUtil
            .readOffersFromResources("notifications/tms-repeat-supplier-notifications-offers.yml"));

        executor.execute();

        Notification[] correctNotifications = YamlTestUtil
            .readNotificationsFromResources("notifications/tms-repeat-supplier-notifications-result.yml")
            .toArray(new Notification[0]);
        List<Notification> newNotifications = notificationRepository.findNewNotifications();

        Assertions.assertThat(newNotifications)
            .usingElementComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .containsExactlyInAnyOrder(correctNotifications);
    }
}
