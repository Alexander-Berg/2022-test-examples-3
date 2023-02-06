package ru.yandex.market.mbi.api.controller.notification;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationResponse;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.notification.exception.InvalidTypeException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendNotificationToEmailControllerTest extends FunctionalTest {
    private static final String EMAIL = "email@yandex-team.ru";
    private static final int PROMO_ERROR_NOTIFICATION_TYPE = 1606180000;
    private static final String PROMO_ERROR_XML = "<errors>ErrorName</errors>";

    @Autowired
    private NotificationService notificationService;

    @Test
    void testWrongNotificationType() {
        when(partnerNotificationClient.sendNotification(any()))
                .thenThrow(new InvalidTypeException("boom"));

        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.sendNotificationToEmail(
                        EMAIL,
                        -1,
                        PROMO_ERROR_XML
                )
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
    }

    @Test
    void testWrongXmlData() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.sendNotificationToEmail(
                        EMAIL,
                        PROMO_ERROR_NOTIFICATION_TYPE,
                        "<errors Error </errors>"
                )
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
    }

    @Test
    void testNotificationSent() {
        SendNotificationResponse response = mbiApiClient.sendNotificationToEmail(
                EMAIL,
                PROMO_ERROR_NOTIFICATION_TYPE,
                PROMO_ERROR_XML
        );
        assertEquals(1, response.getNotificationGroupId());

        final ArgumentCaptor<NotificationSendContext> argument =
                ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService, times(1)).send(argument.capture());

        NotificationSendContext sendContext = argument.getValue();
        assertEquals(EMAIL, sendContext.getRecipientEmail());
        assertEquals(PROMO_ERROR_NOTIFICATION_TYPE, sendContext.getTypeId());
        assertEquals(1, sendContext.getData().size());
        XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setLineSeparator("\n"));
        assertEquals(PROMO_ERROR_XML, outputter.outputString((Element) sendContext.getData().get(0)));
    }
}
