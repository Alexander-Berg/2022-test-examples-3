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

public class ReferralNotificationAccrualPayloadProcessorTest {

    @Test
    public void mapIsPlus() throws JsonProcessingException {
        ReferralNotificationAccrualPayloadProcessor payloadProcessor =
                new ReferralNotificationAccrualPayloadProcessor();
        NotificationReferralPayloadResponse id =
                (NotificationReferralPayloadResponse) payloadProcessor.map(NotificationType.REFERRAL_ACCRUAL,
                        List.of(new Notification("id", DEFAULT_UID, NotificationType.REFERRAL_ACCRUAL, Instant.now(),
                                Instant.now(),
                                new ObjectMapper().writeValueAsString(
                                        new ReferralNotificationPayload(600, 1, null, 10)), false)), true);

        assertEquals("Вам 600 баллов за друга", id.getMessage().getTitle());
        assertEquals("Потратьте их на что-нибудь", id.getMessage().getText());
        assertTrue(id.getMessage().getLinkLabel().isBlank());
    }

    @Test
    public void mapIsNotPlus() throws JsonProcessingException {
        ReferralNotificationAccrualPayloadProcessor payloadProcessor =
                new ReferralNotificationAccrualPayloadProcessor();
        NotificationReferralPayloadResponse id =
                (NotificationReferralPayloadResponse) payloadProcessor.map(NotificationType.REFERRAL_ACCRUAL,
                List.of(new Notification("id", DEFAULT_UID, NotificationType.REFERRAL_ACCRUAL, Instant.now(),
                        Instant.now(), new ObjectMapper().writeValueAsString(new ReferralNotificationPayload(600, 1,
                        null, 11)), false)), false);
        assertEquals("Вам 600 баллов за друга", id.getMessage().getTitle());
        assertEquals("Подключите Плюс, чтобы их тратить", id.getMessage().getText());
        assertTrue(id.getMessage().getLinkLabel().isBlank());
    }
}
