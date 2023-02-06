package ru.yandex.market.mboc.tms.executors.notifications.creators;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepository;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.OfferStatService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;


public class NotifyCatMansAboutNeedActionOffersExecutorTest extends BaseDbTestClass {

    @Autowired
    private OfferRepositoryImpl offerRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    private CategoryCachingServiceMock categoryCachingServiceMock;

    private NotifyCatMansAboutNeedActionOffersExecutor executor;

    @Before
    public void setUp() {
        OfferStatService offerStatService = new OfferStatService(namedParameterJdbcTemplate,
            slaveNamedParameterJdbcTemplate, null, transactionHelper, offerRepository, storageKeyValueService);
        offerStatService.subscribe();

        categoryCachingServiceMock = new CategoryCachingServiceMock();
        YamlTestUtil.readCategoriesFromResources("notifications/catman/need_action/category-tree.yml")
            .forEach(category -> categoryCachingServiceMock.addCategory(category));

        supplierRepository.insertBatch(
            YamlTestUtil.readSuppliersFromResource("notifications/tms-suppliers.yml"));
        offerRepository.insertOffers(
            YamlTestUtil.readOffersFromResources("notifications/catman/need_action/offers.yml"));

        offerStatService.updateOfferStat();

        executor = new NotifyCatMansAboutNeedActionOffersExecutor(notificationRepository, jdbcTemplate,
            categoryCachingServiceMock);

    }

    @Test
    public void testNotifications() {
        executor.execute();

        Notification expected = YamlTestUtil
            .readNotificationsFromResources("notifications/catman/need_action/notification-result.yml")
            .get(0);
        List<Notification> actual = notificationRepository.findNewNotifications();
        Assertions.assertThat(actual)
            .usingElementComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .containsExactlyInAnyOrder(expected);
    }
}
