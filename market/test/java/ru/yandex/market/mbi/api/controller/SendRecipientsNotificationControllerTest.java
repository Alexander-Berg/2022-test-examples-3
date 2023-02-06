package ru.yandex.market.mbi.api.controller;

import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.jdom.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.core.message.model.MessageRecipients;
import ru.yandex.market.core.notification.context.NotificationContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.mbi.api.client.entity.notification.MessageRecipientsView;
import ru.yandex.market.mbi.api.client.entity.notification.NotificationView;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationResponse;
import ru.yandex.market.mbi.api.client.entity.notification.SendRecipientsNotificationRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class SendRecipientsNotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private SendRecipientsNotificationController controller;

    @BeforeEach
    public void setUp() {
        controller = new SendRecipientsNotificationController(notificationService);
    }

    /**
     * Вызываем {@link SendRecipientsNotificationController#sendNotificationToRecipients(SendRecipientsNotificationRequest)}
     * с тестовым запросом и проверяем, что он вызывает `notificationService` с правильными параметрами
     * и возвращает правильный результат.
     */
    @Test
    public void testSendNotificationToRecipients() {
        SendRecipientsNotificationRequest request = createRequest();
        Long expectedNotificationGroupId = configureExpectedNotificationGroupId();
        SendNotificationResponse response = controller.sendNotificationToRecipients(request);
        assertEquals(expectedNotificationGroupId, response.getNotificationGroupId());
        ArgumentCaptor<List> templateParamsArgumentCaptor = ArgumentCaptor.forClass(List.class);
        Integer expectedTemplateId = request.getNotification().getNotificationTypeId();
        MessageRecipients expectedRecipients = request.getRecipients().getActualRecipients();
        Mockito.verify(notificationService).send(eq(expectedTemplateId), templateParamsArgumentCaptor.capture(), eq(expectedRecipients), any());
        assertTemplateParametersXml(templateParamsArgumentCaptor);
    }

    private SendRecipientsNotificationRequest createRequest() {
        NotificationView notificationView = new NotificationView();
        notificationView.setNotificationTypeId(12);
        notificationView.setTemplateParametersXml("<xml><some tag='42'/></xml>");
        MessageRecipientsView recipientsView = new MessageRecipientsView();
        recipientsView.setTo(asList("12@13.14", "a@b.c", "pop@bob.top"));
        recipientsView.setBcc(singletonList("bcc@bcc.bcc"));
        recipientsView.setReplyTo("reply@to.me");
        SendRecipientsNotificationRequest request = new SendRecipientsNotificationRequest();
        request.setNotification(notificationView);
        request.setRecipients(recipientsView);
        return request;
    }

    private Long configureExpectedNotificationGroupId() {
        Long expectedNotificationGroupId = 42L;
        Mockito.when(
                notificationService.send(anyInt(), anyList(),
                        any(MessageRecipients.class), any(NotificationContext.class))
        ).thenReturn(expectedNotificationGroupId);
        return expectedNotificationGroupId;
    }

    private void assertTemplateParametersXml(ArgumentCaptor<List> templateParamsArgumentCaptor) {
        List templateParams = templateParamsArgumentCaptor.getValue();
        assertEquals(1, templateParams.size());
        assertTrue(templateParams.get(0) instanceof Element);
        Element xml = (Element) templateParams.get(0);
        assertEquals("xml", xml.getName());
        List children = xml.getChildren();
        assertEquals(1, children.size());
        Element child = (Element) children.get(0);
        assertEquals("some", child.getName());
        assertEquals("42", child.getAttributeValue("tag"));
        assertEquals(0, child.getChildren().size());
    }
}
