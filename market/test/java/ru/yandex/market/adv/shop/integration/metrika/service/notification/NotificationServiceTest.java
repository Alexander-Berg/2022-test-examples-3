package ru.yandex.market.adv.shop.integration.metrika.service.notification;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationMockServerTest;
import ru.yandex.market.adv.shop.integration.metrika.exception.TemplateNotificationException;
import ru.yandex.market.adv.shop.integration.metrika.model.notification.NotificationResponse;


@DisplayName("Тесты на сервис NotificationServiceImpl.")
@MockServerSettings(ports = 12234)
public class NotificationServiceTest extends AbstractShopIntegrationMockServerTest {

    private static final long BUSINESS_ID = 1L;
    private static final String LOGIN = "login1";
    private static final String INVITATION_ID = "invitation1";

    @Autowired
    NotificationService notificationService;

    public NotificationServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Успешно отправлено уведомление о подтверждении привязки бизнеса к аккаунту в Директе.")
    @Test
    void sendNotification_success_NotificationResponse() {

        mockPath("json/request/sendNotification_approved.json", 200,
                "json/response/sendNotification_approved_response.json");

        Assertions.assertThat(notificationService
                        .sendNotification(BUSINESS_ID, LOGIN)
                )
                .isNotNull()
                .isEqualTo(new NotificationResponse(true, "OK", 1L));
    }

    @DisplayName("Успешно отправлен запрос на подтверждение привязки бизнеса к аккаунту в Директе.")
    @Test
    void sendInvitation_success_NotificationResponse() {

        mockPath("json/request/sendNotification_invite.json", 200,
                "json/response/sendNotification_invite_response.json");

        NotificationResponse notificationResponse = notificationService
                .sendNotification(BUSINESS_ID, LOGIN, INVITATION_ID);

        Assertions.assertThat(notificationService
                .sendNotification(BUSINESS_ID, LOGIN, INVITATION_ID)
        )
                .isNotNull()
                .isEqualTo(new NotificationResponse(true, "OK", 1L));
    }

    @DisplayName("Исключительная ситуация - некорректное тело ответа от MBI.")
    @Test
    void sendInvitation_incorrectResponseBody_exception() {

        mockPath("json/request/sendNotification_incorrectResponseBody.json", 200,
                "json/response/sendNotification_incorrectResponseBody_response.json");


        Assertions.assertThatThrownBy(() -> notificationService
                .sendNotification(BUSINESS_ID, LOGIN)
        ).isInstanceOf(TemplateNotificationException.class);
    }

    @DisplayName("Исключительная ситуация - статус ответа ERROR.")
    @Test
    void sendInvitation_error_exception() {

        mockPath("json/request/sendNotification_error.json", 200,
                "json/response/sendNotification_error_response.json");


        Assertions.assertThatThrownBy(() -> notificationService
                .sendNotification(BUSINESS_ID, LOGIN)
        )
                .isInstanceOf(TemplateNotificationException.class)
                .hasMessage("Error");
    }

    @DisplayName("Исключительная ситуация - http-статус 500 ручки /notification/business.")
    @Test
    void sendInvitation_httpError_exception() {

        mockPath("json/request/sendNotification_httpError.json", 500,
                "json/response/sendNotification_httpError_response.json");


        Assertions.assertThatThrownBy(() -> notificationService
                        .sendNotification(BUSINESS_ID, LOGIN)
                )
                .isInstanceOf(TemplateNotificationException.class)
                .hasMessage("Internal Server Error");
    }


    private void mockPath(String requestFile, int responseCode, String responseFile) {
        mockServerPath("POST",
                "/notification/business",
                requestFile,
                Map.of(),
                responseCode,
                responseFile
        );
    }
}
