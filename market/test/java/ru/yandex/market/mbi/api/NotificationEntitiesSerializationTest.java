package ru.yandex.market.mbi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.common.test.spring.MVCSerializationTest;
import ru.yandex.market.core.message.model.MessageRecipients;
import ru.yandex.market.mbi.api.client.entity.notification.MessageRecipientsView;
import ru.yandex.market.mbi.api.client.entity.notification.NotificationView;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationResponse;
import ru.yandex.market.mbi.api.client.entity.notification.SendRecipientsNotificationRequest;
import ru.yandex.market.mbi.web.converter.MbiHttpMessageConverter;
import ru.yandex.market.mbi.web.converter.XmlHttpMessageConverter;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

class NotificationEntitiesSerializationTest extends MVCSerializationTest {

    private static final String REQUEST_XML =
            "<send-recipients-notification-request>" +
                    "   <notification>" +
                    "       <notification-type-id>42</notification-type-id>" +
                    "       <template-parameters-xml>" +
                    "           <![CDATA[<template><param name='a'>qwe</param></template>]]>" +
                    "       </template-parameters-xml>" +
                    "   </notification>" +
                    "   <recipients>" +
                    "       <to>qwe@rty.ru</to>" +
                    "       <to>rty@qaz.ru</to>" +
                    "       <cc>cc@cc.com</cc>" +
                    "       <bcc>bcc@bcc.com</bcc>" +
                    "       <reply-to>reply@to.com</reply-to>" +
                    "       <from>mail@from.com</from>" +
                    "   </recipients>" +
                    "</send-recipients-notification-request>";

    private static final String RESPONSE_XML =
            "<send-notification-response>" +
                    "   <notification-group-id>" +
                    "       42" +
                    "   </notification-group-id>" +
                    "</send-notification-response>";

    private static final SendRecipientsNotificationRequest REQUEST = createTestNotificationRequest();
    private static final SendNotificationResponse RESPONSE = createTestNotificationResponse();

    private MbiHttpMessageConverter xml;
    private SerializationChecker requestChecker;
    private SerializationChecker responseChecker;

    private static SendRecipientsNotificationRequest createTestNotificationRequest() {
        SendRecipientsNotificationRequest request = new SendRecipientsNotificationRequest();
        request.setNotification(new NotificationView(42, "<template><param name='a'>qwe</param></template>"));
        MessageRecipients recipients = new MessageRecipients();
        recipients.setToAddressList(asList("qwe@rty.ru", "rty@qaz.ru"));
        recipients.setCcAddressList(singletonList("cc@cc.com"));
        recipients.setBccAddressList(singletonList("bcc@bcc.com"));
        recipients.setReplyTo("reply@to.com");
        recipients.setMailFrom("mail@from.com");
        request.setRecipients(new MessageRecipientsView(recipients));
        return request;
    }

    private static SendNotificationResponse createTestNotificationResponse() {
        return new SendNotificationResponse(42L);
    }

    @BeforeEach
    void setUp() {
        xml = new XmlHttpMessageConverter();
        requestChecker = new SerializationChecker(
                obj -> "{}",
                (s, type) -> REQUEST,
                obj -> out(xml, obj),
                (s, type) -> in(xml, s, type)
        );
        responseChecker = new SerializationChecker(
                obj -> "{}",
                (s, type) -> RESPONSE,
                obj -> out(xml, obj),
                (s, type) -> in(xml, s, type)
        );
    }

    @Test
    void test_notification_request_serialization() {
        requestChecker.testSerialization(REQUEST, "{}", REQUEST_XML);
    }

    @Test
    void test_notification_request_deserialization() {
        requestChecker.testDeserialization(REQUEST, "{}", REQUEST_XML);
    }

    @Test
    void test_notification_response_serialization() {
        responseChecker.testSerialization(RESPONSE, "{}", RESPONSE_XML);
    }

    @Test
    void test_notification_response_deserialization() {
        responseChecker.testDeserialization(RESPONSE, "{}", RESPONSE_XML);
    }
}
