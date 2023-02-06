package ru.yandex.market.loyalty.core.trigger;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.UserAccountCouponInfoDto;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResult;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.Trigger;
import ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType;
import ru.yandex.market.loyalty.core.model.trigger.event.LoginEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.SubscriptionEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.exception.UserNotFoundException;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerFastEvaluatorService;
import ru.yandex.market.loyalty.core.service.trigger.coordinator.OrderStatusUpdatedEventProcessingCoordinator;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.trigger.actions.ProcessResultUtils;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggerUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.pers.notify.model.NotificationType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class TriggerFastEvaluatorServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final int SILENCE_GAP_HOURS = 24;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerFastEvaluatorService triggerService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private CouponService couponService;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private ClockForTests clock;
    @Autowired
    private ProcessResultUtils processResultUtils;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private TriggerEventService triggerEventService;

    @Test
    public void shouldNotFetchNotActivePromo() {
        Promo notActiveByStatus = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStatus(PromoStatus.INACTIVE)
        );

        Promo notActiveByDate = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setEndEmissionDate(Date.from(clock.instant().minus(1, ChronoUnit.DAYS)))
        );

        triggersFactory.createLoginTrigger(notActiveByStatus);
        triggersFactory.createLoginTrigger(notActiveByDate);

        assertThat(
                triggerEventService.getTriggersForProcessingEventType(TriggerEventTypes.LOGIN,
                        discountUtils.getRulesPayload()
                ),
                is(empty())
        );
    }

    @Test
    public void emissionPeriodIsCloseToEndAndTriggerDisabledBySilenceGapRestriction() {
        Date now = Date.from(clock.instant());
        Date end = DateUtils.addMonths(now, 1);
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStartEmissionDate(now)
                .setEndEmissionDate(end)
        );

        Trigger<SubscriptionEvent> subscriptionTrigger = triggersFactory.createSubscriptionTrigger(promo);
        triggersFactory.createSilenceGapRestriction(subscriptionTrigger, SILENCE_GAP_HOURS);

        clock.setDate(DateUtils.addHours(end, -SILENCE_GAP_HOURS * 2));

        assertTrue(
                triggerService.evaluate(SubscriptionEvent.createNew(NotificationType.ADVERTISING, "test@test.ru",
                        CoreMarketPlatform.BLUE, 0L, 213L, null, "request_id"))
        );

        clock.setDate(DateUtils.addHours(end, -SILENCE_GAP_HOURS / 2));

        assertFalse(
                triggerService.evaluate(SubscriptionEvent.createNew(NotificationType.ADVERTISING, "test@test.ru",
                        CoreMarketPlatform.BLUE, 0L, 213L, null, "request_id"))
        );
    }


    @Test
    public void shouldExecuteOnlyOneRandomTrigger() {
        Promo promo1 = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createLoginTrigger(promo1, null, TriggerGroupType.RANDOM_TRIGGERS);

        Promo promo2 = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createLoginTrigger(promo2, null, TriggerGroupType.RANDOM_TRIGGERS);

        Promo promo3 = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createLoginTrigger(promo3, null, TriggerGroupType.RANDOM_TRIGGERS);

        assertThat(
                triggerEventService.getTriggersForProcessingEventType(TriggerEventTypes.LOGIN,
                        discountUtils.getRulesPayload()
                ),
                hasSize(3)
        );

        String someUid = "0";

        assertThat(getCouponsByIdentity(someUid), empty());

        triggerEventQueueService.addEventToQueue(LoginEvent.createNew(Long.parseLong(someUid), null,
                CoreMarketPlatform.BLUE, 213L, "user", "request_id"));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(getCouponsByIdentity(someUid), hasSize(1));
    }

    @Test
    public void shouldProcessEventEvenAlreadyInserted() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createLoginTrigger(promo);

        LoginEvent event = EventFactory.createLoginEvent(DEFAULT_UID, CoreMarketPlatform.BLUE);

        triggerEventQueueService.addEventToQueue(event);

        List<TriggerActionResult> processResults = triggerEventQueueService.insertAndProcessEvent(event,
                discountUtils.getRulesPayload(), BudgetMode.SYNC
        );

        assertThat(
                processResultUtils.request(processResults, Coupon.class),
                hasSize(1)
        );
    }


    @Test
    public void shouldExecuteAllMandatoryTriggers() {
        Promo promo1 = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createLoginTrigger(promo1, null, TriggerGroupType.MANDATORY_TRIGGERS);

        Promo promo2 = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createLoginTrigger(promo2, null, TriggerGroupType.MANDATORY_TRIGGERS);

        Promo promo3 = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createLoginTrigger(promo3, null, TriggerGroupType.MANDATORY_TRIGGERS);

        assertThat(
                triggerEventService.getTriggersForProcessingEventType(TriggerEventTypes.LOGIN,
                        discountUtils.getRulesPayload()
                ),
                hasSize(3)
        );

        String someUid = "0";

        assertThat(getCouponsByIdentity(someUid), empty());

        triggerEventQueueService.addEventToQueue(LoginEvent.createNew(Long.parseLong(someUid), null,
                CoreMarketPlatform.BLUE, 213L, "user", "request_id"));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(getCouponsByIdentity(someUid), hasSize(3));
    }

    public List<UserAccountCouponInfoDto> getCouponsByIdentity(String userId) {
        try {
            return couponService.getCouponsByIdentity(Identity.Type.UID, userId, discountUtils.getRulesPayload());
        } catch (UserNotFoundException e) {
            return Collections.emptyList();
        }
    }

}
