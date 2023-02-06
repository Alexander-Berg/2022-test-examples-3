package ru.yandex.market.mboc.tms.executors.notifications.creators;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepositoryMock;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

public class NotifySuppliersAboutProcessedOffersExecutorTest
    extends TimestampExecutorTest {

    @Before
    public void setUp() {
        super.setUp();
        storageKey = NotifySuppliersAboutProcessedOffersExecutor.STATUS_TS_STORAGE_KEY;
        storageService.putValue(storageKey, DateTimeUtils.dateTimeNow()
            .minusMinutes(NotifySuppliersAboutProcessedOffersExecutor.MINUTES_BEFORE_NOW));
        executor = new NotifySuppliersAboutProcessedOffersExecutor(offerRepository,
            notificationRepository, storageService);

    }

    @Test
    public void testSupplierProcessedOffers() throws Exception {
        List<Offer> offers = YamlTestUtil
            .readOffersFromResources("notifications/tms-create-supplier-notifications-offers.yml");
        setUpBeforeRun();
        offerRepository.insertOffers(offers);

        executor.execute();

        Notification[] correctNotifications = YamlTestUtil
            .readNotificationsFromResources("notifications/tms-create-supplier-notifications-result.yml")
            .toArray(new Notification[0]);
        List<Notification> newNotifications = notificationRepository.findNewNotifications();

        assertTimestampChanged();
        Assertions.assertThat(newNotifications)
            .usingElementComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .containsExactlyInAnyOrder(correctNotifications);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testSupplierProcessedOffersWithNullAcceptanceStatusTs() throws Exception {
        List<Offer> offers = YamlTestUtil
            .readOffersFromResources("notifications/tms-create-supplier-notifications-offers-null-ts.yml");

        setUpBeforeRun();
        offerRepository.insertOffers(offers);

        executor.execute();

        assertTimestampChanged();
    }
}
