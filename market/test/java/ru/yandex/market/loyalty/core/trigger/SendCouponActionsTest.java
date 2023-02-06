package ru.yandex.market.loyalty.core.trigger;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coupon.CouponNotificationService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createLoginEvent;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createSubscriptionEvent;

public class SendCouponActionsTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long UID = 123L;
    private static final String EMAIL = "email@yandex-team.ru";
    private static final String USER_NAME = "userName";
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private PersNotifyClient persNotifyClient;
    @Autowired
    private CouponNotificationService couponNotificationService;
    @Autowired
    private Clock clock;
    @Autowired
    private TriggersFactory triggersFactory;

    private Promo promo;
    private Promo promo_percent;

    @Before
    public void init() {
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promo_percent = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUseWithPercent());
    }

    @Test
    public void sendByIdentity() throws Exception {
        triggersFactory.createLoginTrigger(promo);

        triggerEventQueueService.addEventToQueue(createLoginEvent(UID, CoreMarketPlatform.BLUE, USER_NAME));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
        couponNotificationService.notifyUsersAboutCouponActivation();

        verifySendByIdentity();
    }

    private void verifySendByIdentity() throws PersNotifyClientException {
        LocalDateTime expectedExpirationDate = LocalDateTime.now(clock).plusDays(PromoUtils.DEFAULT_EXPIRATION_DAYS);
        verify(persNotifyClient).createEvent(argThat(
                allOf(
                        isA(NotificationEventSource.class),
                        hasProperty("notificationSubtype", equalTo(NotificationSubtype.PROMO_MISC)),
                        hasProperty("uid", equalTo(UID)),
                        hasProperty("email", nullValue()),
                        hasProperty("data", allOf(
                                hasEntry(CouponNotificationService.COUPON_VALUE,
                                        PromoUtils.DEFAULT_COUPON_VALUE.toString()),
                                hasEntry(CouponNotificationService.COUPON_VALUE_TYPE,
                                        CoreCouponValueType.FIXED.toString()),
                                hasEntry(CouponNotificationService.COUPON_EXPIRE_DATE,
                                        expectedExpirationDate.format(CouponNotificationService.PRETTY_DATE_TIME_FORMATTER)),
                                hasEntry(NotificationEventDataName.SENDER_TEMPLATE_ID,
                                        TriggersFactory.EMAIL_TEMPLATE_ID),
                                hasEntry(NotificationEventDataName.USER_NAME, USER_NAME)
                        ))
                )
        ));
    }

    @Test
    public void sendByEmail() throws Exception {
        triggersFactory.createSubscriptionTrigger(promo);

        triggerEventQueueService.addEventToQueue(createSubscriptionEvent(NotificationType.ADVERTISING, EMAIL));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
        couponNotificationService.notifyUsersAboutCouponActivation();

        verifySendByEmail();
    }

    private void verifySendByEmail() throws PersNotifyClientException {
        LocalDateTime expectedExpirationDate = LocalDateTime.now(clock).plusDays(PromoUtils.DEFAULT_EXPIRATION_DAYS);
        verify(persNotifyClient).createEvent(argThat(
                allOf(
                        isA(NotificationEventSource.class),
                        hasProperty("notificationSubtype", equalTo(NotificationSubtype.PROMO_MISC)),
                        hasProperty("uid", nullValue()),
                        hasProperty("email", equalTo(EMAIL)),
                        hasProperty("data", allOf(
                                hasEntry(CouponNotificationService.COUPON_VALUE,
                                        PromoUtils.DEFAULT_COUPON_VALUE.toString()),
                                hasEntry(CouponNotificationService.COUPON_VALUE_TYPE,
                                        CoreCouponValueType.FIXED.toString()),
                                hasEntry(CouponNotificationService.COUPON_EXPIRE_DATE,
                                        expectedExpirationDate.format(CouponNotificationService.PRETTY_DATE_TIME_FORMATTER)),
                                hasEntry(NotificationEventDataName.SENDER_TEMPLATE_ID,
                                        TriggersFactory.EMAIL_TEMPLATE_ID)
                        ))
                )
        ));
    }

    @Test
    public void sendByPercent() throws Exception {
        triggersFactory.createSubscriptionTrigger(promo_percent);

        triggerEventQueueService.addEventToQueue(createSubscriptionEvent(NotificationType.ADVERTISING, EMAIL));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
        couponNotificationService.notifyUsersAboutCouponActivation();

        verifySendByPercent();
    }

    private void verifySendByPercent() throws PersNotifyClientException {
        LocalDateTime expectedExpirationDate = LocalDateTime.now(clock).plusDays(PromoUtils.DEFAULT_EXPIRATION_DAYS);
        verify(persNotifyClient).createEvent(argThat(
                allOf(
                        isA(NotificationEventSource.class),
                        hasProperty("notificationSubtype", equalTo(NotificationSubtype.PROMO_MISC)),
                        hasProperty("uid", nullValue()),
                        hasProperty("email", equalTo(EMAIL)),
                        hasProperty("data", allOf(
                                hasEntry(CouponNotificationService.COUPON_VALUE,
                                        PromoUtils.DEFAULT_COUPON_VALUE_PERCENT.toString()),
                                hasEntry(CouponNotificationService.COUPON_VALUE_TYPE,
                                        CoreCouponValueType.PERCENT.toString()),
                                hasEntry(CouponNotificationService.COUPON_EXPIRE_DATE,
                                        expectedExpirationDate.format(CouponNotificationService.PRETTY_DATE_TIME_FORMATTER)),
                                hasEntry(NotificationEventDataName.SENDER_TEMPLATE_ID, TriggersFactory.EMAIL_TEMPLATE_ID),
                                hasEntry(CouponNotificationService.MAX_PURCHASE_VALUE, PromoUtils.DEFAULT_MAX_PURCHASE_VALUE.toString())
                        ))
                )
        ));
    }

}
