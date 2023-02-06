package ru.yandex.market.mboc.tms.executors.notifications.creators;

import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.notifications.model.data.catmans.NewOffersData;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepositoryMock;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static ru.yandex.market.mboc.common.utils.MbocConstants.PROTO_API_USER;


public class NotifyCatMansAboutNewOffersExecutorTest
    extends TimestampExecutorTest {

    private CategoryCachingServiceMock categoryCachingServiceMock;

    @Before
    public void setUp() {
        super.setUp();
        storageKey = NotifyCatMansAboutNewOffersExecutor.CREATED_TS_STORAGE_KEY;
        storageService.putValue(storageKey, DateTimeUtils.dateTimeNow()
            .minusMinutes(NotifyCatMansAboutNewOffersExecutor.MINUTES_BEFORE_NOW));
        categoryCachingServiceMock = new CategoryCachingServiceMock().enableAuto(false);
        executor = new NotifyCatMansAboutNewOffersExecutor(offerRepository,
            supplierRepository, categoryCachingServiceMock,
            notificationRepository, storageService);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testInsertingNewOffers() throws Exception {
        offerRepository.insertOffers(YamlTestUtil
            .readOffersFromResources("notifications/catman/new_offers/offers.yml"));

        categoryCachingServiceMock.addCategory(1, "category 1")
            .addCategory(2, "category 2");
        setUpBeforeRun();

        executor.execute();

        Notification[] correctNotifications = YamlTestUtil
            .readNotificationsFromResources("notifications/catman/new_offers/notification-result.yml")
            .toArray(new Notification[0]);
        List<Notification> newNotifications = notificationRepository.findNewNotifications();

        assertTimestampChanged();
        Assertions.assertThat(newNotifications)
            .usingElementComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .containsExactlyInAnyOrder(correctNotifications);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenSortingCategoryNamesShouldNotThrowNullPointerException() throws Exception {
        int supplierId = 1;
        offerRepository.insertOffers(ImmutableSet.of(new Offer().setId(1)
                .setBusinessId(supplierId)
                .setCategoryIdForTests(null, Offer.BindingKind.SUGGESTED)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setCreatedByLogin(PROTO_API_USER)
                .setMappingDestination(Offer.MappingDestination.BLUE)
                .setShopSku("hello-there-1")
                .setShopCategoryName("shop category name 1")
                .setIsOfferContentPresent(true)
                .storeOfferContent(OfferContent.builder().build())
                .setTitle("This is a title 1")
                .setGolden(false),
            new Offer().setId(2)
                .setBusinessId(supplierId)
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setCreatedByLogin(PROTO_API_USER)
                .setMappingDestination(Offer.MappingDestination.BLUE)
                .setShopSku("hello-there-2")
                .setShopCategoryName("shop category name 2")
                .setIsOfferContentPresent(true)
                .storeOfferContent(OfferContent.builder().build())
                .setTitle("This is a title 2")
                .setGolden(false),
            new Offer().setId(3)
                .setBusinessId(supplierId)
                .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setCreatedByLogin(PROTO_API_USER)
                .setMappingDestination(Offer.MappingDestination.BLUE)
                .setShopSku("hello-there-3")
                .setShopCategoryName("shop category name 3")
                .setIsOfferContentPresent(true)
                .storeOfferContent(OfferContent.builder().build())
                .setTitle("This is a title 3")
                .setGolden(false),
            new Offer().setId(4)
                .setBusinessId(supplierId)
                .setCategoryIdForTests(3L, Offer.BindingKind.SUGGESTED)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setCreatedByLogin(PROTO_API_USER)
                .setMappingDestination(Offer.MappingDestination.BLUE)
                .setShopSku("hello-there-4")
                .setShopCategoryName("shop category name 4")
                .setIsOfferContentPresent(true)
                .storeOfferContent(OfferContent.builder().build())
                .setTitle("This is a title 4")
                .setGolden(false)));
        categoryCachingServiceMock
            .addCategory(1, "some cat name")
            .addCategory(3, null);

        executor.execute();

        List<Notification> newNotifications = notificationRepository.findNewNotifications();
        Assertions.assertThat(newNotifications).hasSize(1);
        Assertions.assertThat(((NewOffersData) newNotifications.get(0).getData())
            .getNewOffersBySuppliers().get(supplierId).getCategoryNames())
            .containsExactly("some cat name", "Категория 2 не найдена",
                "Категория 3 не найдена", "Категория не указана");
    }
}
