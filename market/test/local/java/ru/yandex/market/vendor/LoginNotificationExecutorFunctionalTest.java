package ru.yandex.market.vendor;

import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.placement.tms.notification.LoginNotificationExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.vendor.notification.VendorNotificationParameterFormatter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/LoginNotificationExecutorFunctionalTest/before.csv",
        dataSource = "vendorDataSource"
)
class LoginNotificationExecutorFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private LoginNotificationExecutor loginNotificationExecutor;
    @Autowired
    private WireMockServer emailSenderMock;
    @Autowired
    private NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate;
    @Autowired
    private VendorNotificationParameterFormatter vendorNotificationParameterFormatter;

    @BeforeEach
    void beforeEachTest() {
        doReturn("2019").when(vendorNotificationParameterFormatter).year();
    }

    /**
     * Необработанные события успешно добавлены для центра уведомлений, и затем помечены как обработанные
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/LoginNotificationExecutorFunctionalTest/testTriggeredNotificationIsSavedToLoginNotifications/before.csv",
            after = "/ru/yandex/market/vendor/LoginNotificationExecutorFunctionalTest/testTriggeredNotificationIsSavedToLoginNotifications/after.csv",
            dataSource = "vendorDataSource"
    )
    void testTriggeredNotificationIsSavedToLoginNotifications() {
        emailSenderMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource(
                        "/testTriggeredNotificationIsSavedToLoginNotifications/campaignInfo.json"))));

        emailSenderMock.stubFor(post(anyUrl())
                .willReturn(aResponse().withBody(getStringResource(
                        "/testTriggeredNotificationIsSavedToLoginNotifications/renderedEmail.json"))));

        loginNotificationExecutor.doJob(null);
    }

    /**
     * Нет необработанных событий
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/LoginNotificationExecutorFunctionalTest/testNoNotNotifiedTriggeredNotifications/before.csv",
            after = "/ru/yandex/market/vendor/LoginNotificationExecutorFunctionalTest/testNoNotNotifiedTriggeredNotifications/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNoNotNotifiedTriggeredNotifications() {
        loginNotificationExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/LoginNotificationExecutorFunctionalTest/testNoNotificationsForUnsubscribedUsers/before.csv",
            after = "/ru/yandex/market/vendor/LoginNotificationExecutorFunctionalTest/testNoNotificationsForUnsubscribedUsers/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNoNotificationsForUnsubscribedUsers() {
        emailSenderMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testNoNotificationsForUnsubscribedUsers" +
                        "/campaignInfo.json"))));
        emailSenderMock.stubFor(post(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testNoNotificationsForUnsubscribedUsers" +
                        "/renderedEmail.json"))));

        loginNotificationExecutor.doJob(null);

        int subscribersDeletedCount = vendorNamedParameterJdbcTemplate.update(
                "DELETE FROM VENDORS.NOTIFICATION_SUBSCRIBERS WHERE PRODUCT_KEY = 'all' " +
                        "AND SUBSCRIBER_ID = '849589468' AND VENDOR_ID = 30",
                Collections.emptyMap()
        );
        assertThat(subscribersDeletedCount).isEqualTo(1);

        int updatedRowCount = vendorNamedParameterJdbcTemplate.update(
                "UPDATE VENDORS.TRIGGERED_NOTIFICATIONS SET IS_NOTIFIED = 0, OP_DATE = SYSTIMESTAMP WHERE EVENT_ID = 2",
                Collections.emptyMap()
        );
        assertThat(updatedRowCount).isEqualTo(1);

        loginNotificationExecutor.doJob(null);
    }

    @DisplayName("Отправляем в КВ только уведомления для КВ")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/LoginNotificationExecutorFunctionalTest/testNotifyOnlyLoginNotifications/before.csv",
            after = "/ru/yandex/market/vendor/LoginNotificationExecutorFunctionalTest/testNotifyOnlyLoginNotifications/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNotifyOnlyLoginNotifications() {

        emailSenderMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testNotifyOnlyLoginNotifications" +
                        "/campaignInfo.json"))));
        emailSenderMock.stubFor(post(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testNotifyOnlyLoginNotifications" +
                        "/renderedEmail.json"))));

        loginNotificationExecutor.doJob(null);
    }
}
