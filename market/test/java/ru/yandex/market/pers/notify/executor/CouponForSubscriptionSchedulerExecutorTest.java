package ru.yandex.market.pers.notify.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.api.service.subscription.EmailSubscriptionDAO;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 09.10.17
 */
public class CouponForSubscriptionSchedulerExecutorTest extends MarketMailerMockedDbTest {
    @Autowired
    private CouponForSubscriptionSchedulerExecutor executor;
    @Autowired
    private EmailSubscriptionDAO emailSubscriptionDAO;
    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    private NotificationEventService notificationEventServiceReal;

    private NotificationEventService notificationEventServiceMocked;

    @BeforeEach
    public void init() throws Exception {
        notificationEventServiceMocked = spy(notificationEventServiceReal);
        subscriptionAndIdentityService.setNotificationEventService(notificationEventServiceMocked);
    }

    @AfterEach
    public void clear() {
        subscriptionAndIdentityService.setNotificationEventService(notificationEventServiceReal);
    }

    @Test
    public void doJob() throws Exception {
        String email = "fdsflsd@sdlfkjds.ru";

        doThrow(new RuntimeException()).when(notificationEventServiceMocked).addEvent(any());
        subscriptionAndIdentityService.createSubscriptions(email,
            Collections.singletonList(childPromoSubscription(email)), new YandexUid("empty"), true);
        subscriptionAndIdentityService.reloadNotificationPool();

        List<EmailSubscription> subscriptionsWithoutEmails = emailSubscriptionDAO
            .getSubscriptionsForChildPromoWithoutEmail(
                Date.from(LocalDateTime.now().minusMinutes(120).atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(LocalDateTime.now().plusMinutes(120).atZone(ZoneId.systemDefault()).toInstant()));
        assertEquals(1, subscriptionsWithoutEmails.size());

        doCallRealMethod().when(notificationEventServiceMocked).addEvent(any());
        executor.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofMinutes(31)));
        executor.doRealJob(null);

        verify(notificationEventServiceMocked, times(1)).addEvent(argThat(new ArgumentMatcher<NotificationEventSource>() {
            @Override
            public boolean matches(Object argument) {
                NotificationEventSource source = (NotificationEventSource) argument;
                return email.equals(source.getEmail());
            }
        }));
    }

    private EmailSubscription childPromoSubscription(String email) {
        EmailSubscription result = new EmailSubscription(
            email, NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED
        );
        result.addParameter(EmailSubscriptionParam.PARAM_REGION_ID, "213");
        result.addParameter(EmailSubscriptionParam.PARAM_ADS_LOCATION, "mall");
        return result;
    }
}
