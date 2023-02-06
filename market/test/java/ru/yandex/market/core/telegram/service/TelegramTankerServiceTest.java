package ru.yandex.market.core.telegram.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.notification.service.NotificationTypeTransportTemplateService;
import ru.yandex.market.core.tanker.TankerService;
import ru.yandex.market.core.tanker.dao.TankerDao;
import ru.yandex.market.core.tanker.model.TankerCode;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TelegramTankerServiceTest {

    private TankerService tankerService;
    private TelegramTankerService telegramTankerService;

    private static final Long NOTIFICATION_ID_WITH_SEVERAL_TRANSPORTS = 23L;
    private static final Long NOTIFICATION_ID_WITH_TELEGRAM_ONLY = 1600249726L;

    @BeforeEach
    public void init() {
        tankerService = mock(TankerService.class);
        TankerDao tankerDao = mock(TankerDao.class);

        NotificationTypeTransportTemplateService notificationTypeTransportTemplateService =
                mock(NotificationTypeTransportTemplateService.class);

        Mockito
                .when(notificationTypeTransportTemplateService.getAvailableTransports(NOTIFICATION_ID_WITH_TELEGRAM_ONLY))
                .thenReturn(Collections.singletonList(NotificationTransport.TELEGRAM_BOT));

        Mockito
                .when(notificationTypeTransportTemplateService.getAvailableTransports(NOTIFICATION_ID_WITH_SEVERAL_TRANSPORTS))
                .thenReturn(Arrays.asList(NotificationTransport.TELEGRAM_BOT, NotificationTransport.EMAIL,
                        NotificationTransport.MBI_WEB_UI));

        telegramTankerService = new TelegramTankerService(
                tankerService, tankerDao,
                notificationTypeTransportTemplateService);
    }

    @Test
    @DisplayName("Сообщение об переподписке для пользователя без email")
    void getConfirmationMessageShouldRequestResubscribeMsg() {
        telegramTankerService.getConfirmationMessage(Map.of(), NOTIFICATION_ID_WITH_SEVERAL_TRANSPORTS, false);

        var codeCaptor = ArgumentCaptor.forClass(TankerCode.class);
        verify(tankerService).translateCode(any(), codeCaptor.capture());

        assertEquals(
                TelegramTankerService.TANKER_KEY_RESUBSCRIBED,
                codeCaptor.getValue().getCode()
        );
    }


    @Test
    @DisplayName("Сообщение об отписке для пользователя с заполненным email")
    void getConfirmationMessageShouldRequestEmailTemplate() {
        telegramTankerService.getConfirmationMessage(Map.of("email", "a@ya.ru"),
                NOTIFICATION_ID_WITH_SEVERAL_TRANSPORTS, true);

        var codeCaptor = ArgumentCaptor.forClass(TankerCode.class);
        verify(tankerService).translateCode(any(), codeCaptor.capture());

        assertEquals(
                TelegramTankerService.TANKER_KEY_UNSUBSCRIBED_WITH_EMAIL,
                codeCaptor.getValue().getCode()
        );
    }

    @Test
    @DisplayName("Сообщение об отписке для пользователя без email")
    void getConfirmationMessageShouldRequestTemplateWithoutEmailWhenEmailIsAbsent() {
        telegramTankerService.getConfirmationMessage(Map.of(), NOTIFICATION_ID_WITH_SEVERAL_TRANSPORTS, true);

        var codeCaptor = ArgumentCaptor.forClass(TankerCode.class);
        verify(tankerService).translateCode(any(), codeCaptor.capture());

        assertEquals(
                TelegramTankerService.TANKER_KEY_UNSUBSCRIBED,
                codeCaptor.getValue().getCode()
        );
    }

    @Test
    @DisplayName("Сообщение об отписке для рассылок, которые не идут в EMAIL и MBI_WEB_UI")
    void getConfirmationMessageShouldRequestTemplateWithShortText() {
        telegramTankerService.getConfirmationMessage(Map.of(), NOTIFICATION_ID_WITH_TELEGRAM_ONLY, true);

        var codeCaptor = ArgumentCaptor.forClass(TankerCode.class);
        verify(tankerService).translateCode(any(), codeCaptor.capture());

        assertEquals(
                TelegramTankerService.TANKER_KEY_UNSUBSCRIBED_SHORT,
                codeCaptor.getValue().getCode()
        );
    }
}
