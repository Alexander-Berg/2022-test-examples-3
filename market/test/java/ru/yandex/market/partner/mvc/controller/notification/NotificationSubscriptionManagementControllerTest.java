package ru.yandex.market.partner.mvc.controller.notification;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link NotificationSubscriptionManagementController}
 */
public class NotificationSubscriptionManagementControllerTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    private static final long NEW_ORDER_NOTIFICATION = 1612872409L;
    private static final long DECLINED_ORDER_NOTIFICATION = 1634797551L;
    private static final long THEME_ID = 1L;
    private static final long NOT_MANAGED_THEME_ID = 6L;
    private static final long NOT_MANAGED_NOTIFICATION_ID = 778L;
    private static final String ERROR_MESSAGE_UNSUBSCRIBED = "[{\"code\":\"BAD_PARAM\"," +
            "\"message\":\"Unsubscription is forbidden " +
            "for notification " + NOT_MANAGED_NOTIFICATION_ID + "\",\"details\":{}}]";
    private static final String ERROR_MESSAGE_SUBSCRIBED = "[{\"code\":\"BAD_PARAM\"," +
            "\"message\":\"Subscription is forbidden " +
            "for notification " + NOT_MANAGED_NOTIFICATION_ID + "\",\"details\":{}}]";

    @BeforeEach
    void setUp() {
        environmentService.setValue("notification.managed_notifications", "true");
    }

    @Test
    @DisplayName("Успешная подписка на управляемое уведомление (был отписан) - только e-mail")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.noEmailSubscription.csv",
            after = "NotificationSubscriptionManagementControllerTest.subscribedAll.csv"
    )
    void testReSubscribeEmailOnly() {
        doPostSubscribe(NEW_ORDER_NOTIFICATION);
    }

    @Test
    @DisplayName("Успешная подписка на уведомление об отмене заказа (был отписан) по e-mail и телеграмм")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.subscribedAll.csv",
            after = "NotificationSubscriptionManagementControllerTest.declinedOfferNoEmail.csv"
    )
    void testResubscribeDeclinedOrder() {
        doPostUnsubscribeAllTransports(DECLINED_ORDER_NOTIFICATION);
    }

    @Test
    @DisplayName("Успешная подписка на управляемое уведомление (был отписан)")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.unsubscribeAll.csv",
            after = "NotificationSubscriptionManagementControllerTest.subscribedAll.csv"
    )
    void testReSubscribe() {
        doPostSubscribeAllTransports();
    }

    @Test
    @DisplayName("Подписка на уведомление, которым партнер не может управлять")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.noEmailSubscription.csv"
    )
    void testSubscribeNotManagedNotification() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> doPostSubscribe(NOT_MANAGED_NOTIFICATION_ID));
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage(ERROR_MESSAGE_SUBSCRIBED, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Успешная отписка пользователя от уведомления,которым партнер может управлять, по e-mail и телеграмм")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.subscribedAll.csv",
            after = "NotificationSubscriptionManagementControllerTest.unsubscribeAll.csv"
    )
    void testUnsubscribe() {
        doPostUnsubscribeAllTransports(NEW_ORDER_NOTIFICATION);
    }

    @Test
    @DisplayName("Успешная отписка пользователя от уведомления,которым партнер может управлять, по e-mail" +
            "(остается активная подписка в телеграмме)")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.subscribedAll.csv",
            after = "NotificationSubscriptionManagementControllerTest.noEmailSubscription.csv"
    )
    void testUnsubscribeEmailOnly() {
        doPostUnsubscribe(NEW_ORDER_NOTIFICATION);
    }

    @Test
    @DisplayName("Успешная отписка пользователя от уведомления,партнер подписан только по имейлу")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.unsubscribe.noTelegramSubscription.csv",
            after = "NotificationSubscriptionManagementControllerTest.unsubscribeAll.csv"
    )
    void testUnsubscribeIfPartlyNotSubscribed() {
        doPostUnsubscribeAllTransports(NEW_ORDER_NOTIFICATION);
    }

    @Test
    @DisplayName("Отписка от уведомления, которым партнер не может управлять")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.subscribedAll.csv"
    )
    void testUnsubscribeNotManagedNotification() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> doPostUnsubscribe(NOT_MANAGED_NOTIFICATION_ID));
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage(ERROR_MESSAGE_UNSUBSCRIBED, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Получить список доступных для управления нотификаций партнера")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.subscribedAll.csv"
    )
    void getManagedNotificationsList() {
        //language=json
        String expected = "{\n" +
                "    \"notificationsMeta\": [\n" +
                "        {\n" +
                "            \"id\": 1612872409,\n" +
                "            \"name\": \"о новых заказах\",\n" +
                "            \"isActive\": true\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 1634797551,\n" +
                "            \"name\": \"об отмене заказов до отгрузки\",\n" +
                "            \"isActive\": true\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        ResponseEntity<String> response = getResponse(THEME_ID);
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Получить список доступных для управления нотификаций партнера -  у юзера несколько кампаний," +
            " и по всем он отписан по e-mail и переподписан в телеграмме")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.noEmailAndTgResubscribedMultipleCampaigns.csv"
    )
    void getManagedNotificationsListNoEmailSubscriptionTgResubscribed() {
        assertNoEmailSubscriptionMultipleCampaigns();
    }

    @Test
    @DisplayName("Получить список доступных для управления нотификаций партнера -  у юзера несколько кампаний," +
            " и по всем он отписан по e-mail, в тг подписан по умолчанию (в базе нет записи)")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.noEmailSubscriptionMultipleCampaigns.csv"
    )
    void getManagedNotificationsListNoEmailSubscription() {
        assertNoEmailSubscriptionMultipleCampaigns();
    }

    @Test
    @DisplayName("Получить список доступных для управления нотификаций партнера - в запросе тема," +
            " которой партнер не может управлять")
    @DbUnitDataSet(
            before = "NotificationSubscriptionManagementControllerTest.subscribedAll.csv"
    )
    void getManagedNotificationsListNonManagedTheme() {
        //language=json
        String expected = "{\"notificationsMeta\":[]}";
        ResponseEntity<String> response = getResponse(NOT_MANAGED_THEME_ID);
        JsonTestUtil.assertEquals(response, expected);
    }

    private void doPostUnsubscribe(long notificationId) {
        FunctionalTestHelper.post(
                baseUrl + "/subscriptions/notifications/{typeId}/unsubscribe?transports=EMAIL&_user_id=100500",
                null, notificationId);
    }

    private void doPostUnsubscribeAllTransports(long notificationId) {
        FunctionalTestHelper.post(
                baseUrl + "/subscriptions/notifications/{typeId}/unsubscribe?transports=EMAIL,TELEGRAM_BOT" +
                        "&_user_id=12345&euid=100500", null, notificationId);
    }

    private void doPostSubscribe(long notificationId) {
        FunctionalTestHelper.post(
                baseUrl + "/subscriptions/notifications/{typeId}/subscribe?transports=EMAIL&_user_id=100500",
                null, notificationId);
    }

    private void doPostSubscribeAllTransports() {
        FunctionalTestHelper.post(
                baseUrl + "/subscriptions/notifications/{typeId}/subscribe?transports=EMAIL,TELEGRAM_BOT" +
                        "&_user_id=12345&euid=100500", null, NEW_ORDER_NOTIFICATION);
    }

    private ResponseEntity<String> getResponse(long themeId) {
        return FunctionalTestHelper.get(
                baseUrl + "/subscriptions/themes/{themeId}/notifications/managed?transport=EMAIL" +
                        "&_user_id=12345&euid=100500",
                themeId);
    }

    private void assertNoEmailSubscriptionMultipleCampaigns() {
        //language=json
        String expected = "{\n" +
                "    \"notificationsMeta\": [\n" +
                "        {\n" +
                "            \"id\": 1612872409,\n" +
                "            \"name\": \"о новых заказах\",\n" +
                "            \"isActive\": false\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": 1634797551,\n" +
                "            \"name\": \"об отмене заказов до отгрузки\",\n" +
                "            \"isActive\": false\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        ResponseEntity<String> response = getResponse(THEME_ID);
        JsonTestUtil.assertEquals(response, expected);
    }
}
