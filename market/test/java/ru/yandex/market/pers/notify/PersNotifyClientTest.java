package ru.yandex.market.pers.notify;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pers.notify.model.EmailSubscriptionReadRequest;
import ru.yandex.market.pers.notify.model.EmailSubscriptionWriteRequest;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author artemmz
 *         created on 18.12.15.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-bean.xml")
@Rollback
@Transactional
@Disabled
public class PersNotifyClientTest {
    @Autowired
    PersNotifyClient persNotifyClient;

    @Test
    public void testGetMobileAppsInfo() throws Exception {
        Long UID = 23427062L;
        String UUID = "d1b33358d51611e38e5f4ad91f040a9a";
        List<MobileAppInfo> appsInfo = persNotifyClient.getMobileAppsInfo(UID, UUID);
        assertEquals(1, appsInfo.size());
    }

    @Test
    public void testAddInvalidEmailToSubscriptions() throws Exception {
        assertThrows(PersNotifyClientException.class, () -> {
            Long UID = 4001841831L;
            String REGION_ID = "213";
            String USER_NAME = "Vasya";
            String INVALID_EMAIL = "abc@.org";
            EmailSubscriptionWriteRequest request = EmailSubscriptionWriteRequest
                .builder()
                .addEmail(INVALID_EMAIL)
                .addUserAgent("test_agent")
                .addUserIp("127.0.0.1")
                .build();

            EmailSubscription subscription = new EmailSubscription();
            subscription.setSubscriptionType(NotificationType.ADVERTISING);
            subscription.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, REGION_ID);
            subscription.addParameter(EmailSubscriptionParam.PARAM_USER_NAME, USER_NAME);
            request.setEmailSubscriptions(Collections.singletonList(subscription));

            List<EmailSubscription> addedSubscriptions = persNotifyClient.createSubscriptions(request);
            assertFalse(addedSubscriptions.isEmpty());

            List<EmailSubscription> emailSubscriptions = persNotifyClient.readSubscriptions(EmailSubscriptionReadRequest
                .builder()
                .addId(addedSubscriptions.get(0).getId())
                .addUid(UID)
                .build());
            assertFalse(emailSubscriptions.isEmpty());
        });
    }

    @Test
    public void testAddEvent() throws Exception {
        NotificationEventSource source = NotificationEventSource
            .fromEmail("kate.konysheva", NotificationSubtype.ORDER_PROCESSING).build();

        assertThrows(IllegalArgumentException.class, () -> {
            persNotifyClient.createEvent(source);
        });
    }
}
