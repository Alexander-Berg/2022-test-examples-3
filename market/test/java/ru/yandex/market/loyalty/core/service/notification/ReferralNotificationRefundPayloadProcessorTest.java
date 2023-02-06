package ru.yandex.market.loyalty.core.service.notification;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.market.loyalty.api.model.notifications.NotificationReferralPayloadResponse;
import ru.yandex.market.loyalty.api.model.notifications.NotificationType;
import ru.yandex.market.loyalty.api.model.notifications.ReferralNotificationPayload;
import ru.yandex.market.loyalty.core.model.notification.Notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class ReferralNotificationRefundPayloadProcessorTest {

    @Test
    public void mapIsPlus() throws JsonProcessingException {
        ReferralNotificationRefundPayloadProcessor payloadProcessor =
                new ReferralNotificationRefundPayloadProcessor();
        NotificationReferralPayloadResponse id =
                (NotificationReferralPayloadResponse) payloadProcessor.map(NotificationType.REFERRAL_REFUND,
                        List.of(new Notification("id", DEFAULT_UID, NotificationType.REFERRAL_REFUND, Instant.now(),
                                Instant.now(),
                                new ObjectMapper().writeValueAsString(new ReferralNotificationPayload(600, 1,
                                        null, 10)), false)), true);

        assertEquals("Увы, вынуждены забрать 600 баллов", id.getMessage().getTitle());
        assertEquals("Ваш друг вернул заказ- пригласите другого", id.getMessage().getText());
        assertTrue(id.getMessage().getLinkLabel().isBlank());
    }

    @Test
    public void mapIsNotPlus() throws JsonProcessingException {
        ReferralNotificationRefundPayloadProcessor payloadProcessor =
                new ReferralNotificationRefundPayloadProcessor();
        NotificationReferralPayloadResponse id =
                (NotificationReferralPayloadResponse) payloadProcessor.map(NotificationType.REFERRAL_REFUND,
                List.of(new Notification("id", DEFAULT_UID, NotificationType.REFERRAL_REFUND, Instant.now(),
                        Instant.now(), new ObjectMapper().writeValueAsString(new ReferralNotificationPayload(600, 1,
                        null, 11)), false)), false);

        assertEquals("Увы, вынуждены забрать 600 баллов", id.getMessage().getTitle());
        assertEquals("Ваш друг вернул заказ- пригласите другого", id.getMessage().getText());
        assertTrue(id.getMessage().getLinkLabel().isBlank());
    }
}
