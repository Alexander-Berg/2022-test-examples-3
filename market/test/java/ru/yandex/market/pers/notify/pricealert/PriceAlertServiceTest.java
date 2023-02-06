package ru.yandex.market.pers.notify.pricealert;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.pricealert.model.PriceAlertSubscription;
import ru.yandex.market.pers.notify.push.MobileAppInfoDAO;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 11.12.17
 */
public class PriceAlertServiceTest extends MarketMailerMockedDbTest {
    @Autowired
    private PriceAlertService priceAlertService;
    @Autowired
    private JdbcTemplate ytJdbcTemplate;
    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;
    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;

    @Test
    public void schedulePriceAlertNotifications() {
        long modelId1 = 876345L;
        long modelId2 = 348924L;
        String email = "my@email.com";
        String uuid = "uuid";
        addMobileAppInfo(uuid);
        addSubscription(email, priceAlertSubscription(email, modelId1));
        setActivePASubscriptions(Arrays.asList(
            paSubscriptionEmail(email, NotificationType.PA_ON_SALE, modelId1),
            paSubscriptionPush(uuid, modelId2, NotificationType.PA_ON_SALE)));

        priceAlertService.scheduleNotifications();

        lastEventContainsData(email, NotificationEventDataName.MODEL_ID, String.valueOf(modelId1));
    }

    @Test
    public void schedulePriceDropNotification() {
        long modelId = 876345L;
        double price = 100.0d;
        String email = "my@email.com";
        String uuid = "uuid";
        addMobileAppInfo(uuid);
        addSubscription(email, priceDropSubscription(email, modelId, price));
        setActivePASubscriptions(Collections.singletonList(
            paSubscriptionEmail(email, modelId, NotificationType.PRICE_DROP, 100.0d)));

        priceAlertService.scheduleNotifications();

        lastEventContainsData(email, NotificationEventDataName.MODEL_ID, String.valueOf(modelId));
        lastEventContainsData(email, NotificationEventDataName.PRICE, String.valueOf(price));
    }

    @Nested
    @SuppressFBWarnings("SS_SHOULD_BE_STATIC")
    class SendTimeChecks extends MarketMailerMockedDbTest {

        final String email = "user@example.com";
        final long modelId = 1L;

        //CHECKSTYLE:OFF
        ZonedDateTime DAY_SCHEDULE_TIME = ZonedDateTime.parse("2028-09-15T10:00:00+03:00");
        final Clock DAY_CLOCK = Clock.fixed(DAY_SCHEDULE_TIME.toInstant(), DAY_SCHEDULE_TIME.getZone());
        final Instant DAY_SEND_TIME = ZonedDateTime.parse("2028-09-15T11:30:01+03:00").toInstant();

        ZonedDateTime NIGHT_SCHEDULE_TIME = ZonedDateTime.parse("2028-09-15T23:00:00+03:00");
        final Clock NIGHT_CLOCK = Clock.fixed(NIGHT_SCHEDULE_TIME.toInstant(), NIGHT_SCHEDULE_TIME.getZone());
        final Instant MORNING_SEND_TIME = ZonedDateTime.parse("2028-09-16T08:00:00+03:00").toInstant();
        //CHECKSTYLE:ON

        Clock originalClock;

        @Test
        void priceAlertOnSaleDuringDayMustBeSentIn90Minutes() {
            checkPriceAlertOnSale(DAY_CLOCK, DAY_SEND_TIME);
        }

        @Test
        @Disabled("FIXME")
        void priceAlertOnSaleDuringNightMustBeSentNextMorning() {
            checkPriceAlertOnSale(NIGHT_CLOCK, MORNING_SEND_TIME);
        }

        @Test
        void priceDropDuringDayMustBeSentIn90Minutes() {
            checkPriceDrop(DAY_CLOCK, DAY_SEND_TIME);
        }

        @Test
        @Disabled("FIXME")
        void priceDropDuringNightMustBeSentNextMorning() {
            checkPriceDrop(NIGHT_CLOCK, MORNING_SEND_TIME);
        }

        private void checkPriceAlertOnSale(Clock clock, Instant expectedSendTime) {
            addSubscription(email, priceAlertSubscription(email, modelId));
            setActivePASubscriptions(Collections.singletonList(
                paSubscriptionEmail(email, NotificationType.PA_ON_SALE, modelId)));
            runAndCheck(clock, expectedSendTime);
        }

        private void checkPriceDrop(Clock clock, Instant expectedSendTime) {
            final double price = 1000.0;
            addSubscription(email, priceDropSubscription(email, modelId, price));
            setActivePASubscriptions(Collections.singletonList(
                paSubscriptionEmail(email, modelId, NotificationType.PRICE_DROP, price)));
            runAndCheck(clock, expectedSendTime);
        }

        void runAndCheck(Clock clock, Instant expectedSendTime) {
            originalClock = priceAlertService.getClock();
            try {
                priceAlertService.setClock(clock);

                priceAlertService.scheduleNotifications();

                NotificationEvent event = mailerNotificationEventService.getLastEventByEmail(email);
                assertEquals(expectedSendTime, event.getSendTime().toInstant());
            } finally {
                priceAlertService.setClock(originalClock);
            }
        }

    }

    private void lastEventContainsData(String address, String key, String expectedValue) {
        NotificationEvent event = mailerNotificationEventService.getLastEventByEmail(address);
        assertEquals(expectedValue, event.getData().get(key));
    }

    private PriceAlertSubscription paSubscriptionEmail(String email, long modelId, NotificationType type, double price) {
        PriceAlertSubscription result = paSubscriptionEmail(email, type, modelId);
        result.setPrice(price);
        return result;
    }

    private PriceAlertSubscription paSubscriptionEmail(String email, NotificationType type, long modelId) {
        PriceAlertSubscription result = paSubscription(modelId, type);
        result.setEmail(email);
        return result;
    }

    private PriceAlertSubscription paSubscriptionPush(String uuid, long modelId, NotificationType type) {
        PriceAlertSubscription result = paSubscription(modelId, type);
        result.setUuid(uuid);
        return result;
    }

    private PriceAlertSubscription paSubscription(long modelId, NotificationType type) {
        return new PriceAlertSubscription(
            null, null, null, null,
            modelId, 0.0d, Currency.AMD, null, null, type
        );
    }

    private EmailSubscription priceAlertSubscription(String email, long modelId) {
        EmailSubscription result = new EmailSubscription(
            email, NotificationType.PA_ON_SALE, EmailSubscriptionStatus.CONFIRMED);
        result.setParameters(Collections.singletonMap(EmailSubscriptionParam.PARAM_MODEL_ID, String.valueOf(modelId)));
        return result;
    }

    private EmailSubscription priceDropSubscription(String email, long modelId, double price) {
        EmailSubscription result = new EmailSubscription(
            email, NotificationType.PRICE_DROP, EmailSubscriptionStatus.CONFIRMED);
        result.setParameters(new HashMap<>());
        result.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, String.valueOf(modelId));
        result.addParameter(EmailSubscriptionParam.PARAM_PRICE, String.valueOf(price));
        return result;
    }

    private void addMobileAppInfo(String uuid) {
        mobileAppInfoDAO.add(new MobileAppInfo(
            null, uuid, "app", "push_token", MobilePlatform.ANDROID, false));
    }

    private void addSubscription(String email, EmailSubscription subscription) {
        subscriptionAndIdentityService.createSubscriptions(email,
            Collections.singletonList(subscription), new Uid(1L), true);
        subscriptionAndIdentityService.reloadNotificationPool();
    }

    @SuppressWarnings("unchecked")
    private void setActivePASubscriptions(List<PriceAlertSubscription> subscriptions) {
        when(ytJdbcTemplate.query(anyString(), any(RowMapper.class), anyVararg())).thenReturn(subscriptions);
    }
}
