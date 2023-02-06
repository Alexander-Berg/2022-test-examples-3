package ru.yandex.market.vendor.controllers;

import java.util.Collections;

import com.google.common.collect.Sets;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.model.notifications.NotificationSubscriberEventIdentifiersView;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.notification.VendorNotificationParameterFormatter;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
class VendorNotificationsControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final VendorNotificationParameterFormatter vendorNotificationParameterFormatter;

    @Autowired
    public VendorNotificationsControllerFunctionalTest(
            VendorNotificationParameterFormatter vendorNotificationParameterFormatter) {
        this.vendorNotificationParameterFormatter = vendorNotificationParameterFormatter;
    }


    @BeforeEach
    void beforeEachTest() {
        Mockito.doReturn("2019").when(vendorNotificationParameterFormatter).year();
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testGetCountOfUnreadNotifications/before.csv"
    )
    void testGetCountOfUnreadNotifications() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/notifications/countUnread?uid=849589468");
        String expected = getStringResource("/testGetCountOfUnreadNotifications/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testGetCountOfUnreadNotificationsForNull/before.csv"
    )
    void testGetCountOfUnreadNotificationsForNull() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/notifications/countUnread?uid=0");
        String expected = getStringResource("/testGetCountOfUnreadNotificationsForNull/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testGetCountOfUnreadNotificationsForReadNotifications/before.csv"
    )
    void testGetCountOfUnreadNotificationsForReadNotifications() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/notifications/countUnread?uid=1");
        String expected = getStringResource("/testGetCountOfUnreadNotificationsForReadNotifications/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Отмечает список уведомлений центра уведомлений как прочитанный
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testMarkReadLoginNotifications/before.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testMarkReadLoginNotifications/after.csv"
    )
    void testMarkReadLoginNotifications() {
        String response = FunctionalTestHelper.post(
                baseUrl + "/vendors/321/notifications/markRead?uid=849589468",
                new NotificationSubscriberEventIdentifiersView(Sets.newHashSet(1L, 2L))
        );
        String expected = getStringResource("/testMarkReadLoginNotifications/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Отмечает список несуществующих уведомлений центра уведомлений как прочитанный
     */
    @Test
    void testMarkReadNotExistingLoginNotifications() {
        String response = FunctionalTestHelper.post(
                baseUrl + "/vendors/321/notifications/markRead?uid=849589468",
                new NotificationSubscriberEventIdentifiersView(Sets.newHashSet(1L, 2L, 3L, 4L))
        );
        String expected = getStringResource("/testMarkReadNotExistingLoginNotifications/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Отмечает все уведомления подписчика как прочитанные
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testMarkReadAllLoginNotifications/before.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testMarkReadAllLoginNotifications/after.csv"
    )
    void testMarkReadAllLoginNotifications() {
        String response = FunctionalTestHelper.post(
                baseUrl + "/vendors/321/notifications/markRead?uid=849589468",
                new NotificationSubscriberEventIdentifiersView(Collections.emptySet())
        );
        String expected = getStringResource("/testMarkReadAllLoginNotifications/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Возвращает существующие непрочитанные уведомления c:
     * <ul>
     * <li>Тип "Рекомендованные магазины"</li>
     * <li>C 2019-04-01T00:00:00.00</li>
     * <li>По 2019-05-30T00:00:00.00</li>
     * <li>Статус "Непрочитанные"</li>
     * <li>Заголовок или тело включает текст "ПРОДАВЕЦ"</li>
     * <li>Размер страницы: 2 элемента</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testGetExistingNotifications/before.csv"
    )
    void testGetExistingNotifications() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/notifications?uid=849589468&type" +
                "=RECOMMENDED&from=2019-04-01T00:00:00.00Z&to=2019-05-30T00:00:00" +
                ".00Z&status=UNREAD&searchText=ПРОДАВЕЦ&pageSize=2");
        String expected = getStringResource("/testGetExistingNotifications/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Возвращает существующие непрочитанные уведомления c:
     * <ul>
     * <li>Тип "Рекомендованные магазины"</li>
     * <li>По 2019-05-30T00:00:00.00</li>
     * <li>Статус "Непрочитанные"</li>
     * <li>Заголовок или тело включает текст "ПРОДАВЕЦ"</li>
     * <li>Размер страницы: 2 элемента</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testGetExistingNotificationsToDate/before.csv"
    )
    void testGetExistingNotificationsToDate() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/notifications?uid=849589468&type" +
                "=RECOMMENDED&to=2019-05-30T00:00:00.00Z&status=UNREAD&searchText=ПРОДАВЕЦ&pageSize=2");
        String expected = getStringResource("/testGetExistingNotificationsToDate/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Возвращает существующее уведомление
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testGetExistingNotification/before.csv"
    )
    void testGetExistingNotification() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/notifications/1?uid=849589468");
        String expected = getStringResource("/testGetExistingNotification/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Получение шаблона из рассылятора завершилось с ошибкой
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorNotificationsControllerFunctionalTest" +
                    "/testGetNotificationEmailSenderFailure/before.csv"
    )
    void testGetNotificationEmailSenderFailure() {
        assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/vendors/321/notifications/1?uid=849589468")
        );
    }
}
