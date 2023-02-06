package ru.yandex.market.loyalty.core.trigger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponParamName;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResult;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.trigger.event.ForceCreateCouponEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coupon.CouponNotificationService;
import ru.yandex.market.loyalty.core.service.exception.ConflictException;
import ru.yandex.market.loyalty.core.service.trigger.TriggerFastEvaluatorService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerNotFoundException;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.trigger.actions.ProcessResultUtils;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.PROMO_NOT_ACTIVE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.TRIGGER_NOT_FIT_CONDITION;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.coupon.CouponNotificationService.MIN_PURCHASE_VALUE;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createForceCreateCouponEvent;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EMISSION_BUDGET;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

public class TriggerForceCreateCouponEventTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final String EMAIL = "email@yandex-team.ru";
    private static final long UID = 123L;
    private static final String CLIENT_UNIQUE_KEY = "someKey";
    private static final String ANOTHER_CLIENT_UNIQUE_KEY = "anotherKey";
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerFastEvaluatorService triggerFastEvaluatorService;
    @Autowired
    private PersNotifyClient persNotifyClient;
    @Autowired
    private CouponNotificationService couponNotificationService;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private ProcessResultUtils processResultUtils;
    @Autowired
    private DiscountUtils discountUtils;

    @Test
    public void shouldSendCoupon() throws PersNotifyClientException {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(EMAIL,
                CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );

        couponNotificationService.notifyUsersAboutCouponActivation();

        verify(persNotifyClient).createEvent(argThat(hasProperty("uid", nullValue())));
    }

    @Test
    public void shouldSendOnlyOneCouponOnRetry() throws PersNotifyClientException {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        ForceCreateCouponEvent event = createForceCreateCouponEvent(EMAIL, CLIENT_UNIQUE_KEY);
        triggerFastEvaluatorService.processEvent(promo.getId(), event, TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );
        triggerFastEvaluatorService.processEvent(promo.getId(), event, TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );

        couponNotificationService.notifyUsersAboutCouponActivation();

        verify(persNotifyClient).createEvent(any());
    }

    @Test
    public void shouldSendTwoCouponsIfClientUniqueKeyIsChanged() throws PersNotifyClientException {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(EMAIL,
                CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );
        triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(EMAIL,
                ANOTHER_CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.valueOf(2)))
        );

        couponNotificationService.notifyUsersAboutCouponActivation();

        verify(persNotifyClient, times(2)).createEvent(any());
    }

    @Repeat(5)
    @Test
    public void shouldSendOnlyOneCouponOnConcurrentForceCreate() throws PersNotifyClientException,
            InterruptedException {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        ForceCreateCouponEvent event = createForceCreateCouponEvent(EMAIL, CLIENT_UNIQUE_KEY);
        List<Coupon> coupons = new ArrayList<>();
        testConcurrency(() -> () -> {
            try {
                TriggerActionResult result = triggerFastEvaluatorService.processEvent(promo.getId(), event,
                        TriggerEventTypes.FORCE_CREATE_COUPON,
                        discountUtils.getRulesPayload()
                );
                Coupon coupon = processResultUtils.requestSingleton(result, Coupon.class,
                        MarketLoyaltyErrorCode.TRIGGER_NOT_FIT_CONDITION
                );
                assertNotNull(coupon);
                coupons.add(coupon);
            } catch (ConflictException ignore) {
            }
        });

        assertThat(coupons.stream().map(Coupon::getCode).collect(Collectors.toSet()), hasSize(1));
        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );

        couponNotificationService.notifyUsersAboutCouponActivation();

        verify(persNotifyClient).createEvent(any());
    }

    @Test
    public void shouldProduceCouponToConsumer() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        ForceCreateCouponEvent event = createForceCreateCouponEvent(EMAIL, CLIENT_UNIQUE_KEY);
        TriggerActionResult result = triggerFastEvaluatorService.processEvent(promo.getId(), event,
                TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );
        Coupon coupon = processResultUtils.requestSingleton(result, Coupon.class,
                MarketLoyaltyErrorCode.TRIGGER_NOT_FIT_CONDITION
        );

        assertNotNull(coupon);
        assertEquals(promo.getId(), coupon.getPromoId());
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
        assertEquals(EMAIL, coupon.getParams().get(CouponParamName.USER_EMAIL));

        TriggerActionResult result2 = triggerFastEvaluatorService.processEvent(promo.getId(), event,
                TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );
        assertEquals(
                coupon.getCode(),
                processResultUtils.requestSingleton(result2, Coupon.class,
                        MarketLoyaltyErrorCode.TRIGGER_NOT_FIT_CONDITION).getCode()
        );
    }

    @Test
    public void shouldSendCouponWithUidIfGiven() throws PersNotifyClientException {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(UID, EMAIL,
                CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );

        couponNotificationService.notifyUsersAboutCouponActivation();

        verify(persNotifyClient).createEvent(argThat(hasProperty("uid", equalTo(UID))));
    }

    @Test
    public void shouldContainsMinOrderTotalIfGiven() throws PersNotifyClientException {
        BigDecimal minOrderTotal = BigDecimal.valueOf(100);

        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .addPromoRule(MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                        Collections.singleton(minOrderTotal))
        );

        triggersFactory.createForceCreateCouponTrigger(promo);

        triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(UID, EMAIL,
                CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );

        couponNotificationService.notifyUsersAboutCouponActivation();

        verify(persNotifyClient).createEvent(argThat(hasProperty("data", hasEntry(MIN_PURCHASE_VALUE,
                minOrderTotal.toString()))));
    }

    @Test
    public void shouldSaveTriggerEventResult() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceCreateCouponTrigger(promo);

        triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(EMAIL,
                CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                discountUtils.getRulesPayload()
        );

        List<TriggerEvent> events = triggerEventDao.getAll();
        assertThat(events, hasSize(1));

        ForceCreateCouponEvent event = (ForceCreateCouponEvent) events.get(0);
        assertEquals(EMAIL, event.getEmail());
        assertEquals(TriggerEventProcessedResult.SUCCESS, event.getProcessedResult());
    }

    @Test
    public void shouldFailIfPromoNotActive() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStartEmissionDate(DateUtils.addDays(new Date(), -1))
                .setStatus(PromoStatus.INACTIVE)
        );
        triggersFactory.createForceCreateCouponTrigger(promo);

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(EMAIL,
                        CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                        discountUtils.getRulesPayload()
                )
        );
        assertEquals(PROMO_NOT_ACTIVE, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldFailIfEmissionPeriodEnded() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStartEmissionDate(DateUtils.addDays(new Date(), 10))
        );
        triggersFactory.createForceCreateCouponTrigger(promo);

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(EMAIL,
                        CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                        discountUtils.getRulesPayload()
                )
        );
        assertEquals(TRIGGER_NOT_FIT_CONDITION, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldFailIfEmissionBudgetEnded() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setEmissionBudget(BigDecimal.ZERO)
        );
        triggersFactory.createForceCreateCouponTrigger(promo);

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(EMAIL,
                        CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                        discountUtils.getRulesPayload()
                )
        );
        assertEquals(TRIGGER_NOT_FIT_CONDITION, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldFailIfNoTriggerFound() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        assertThrows(TriggerNotFoundException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(EMAIL,
                        CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                        discountUtils.getRulesPayload()
                )
        );
    }

    @Test
    public void shouldFailIfTriggersDuplicated() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        triggersFactory.createForceCreateCouponTrigger(promo);
        triggersFactory.createForceCreateCouponTrigger(promo);
        assertThrows(IllegalStateException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(), createForceCreateCouponEvent(EMAIL, CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_CREATE_COUPON,
                        discountUtils.getRulesPayload()
                )
        );
    }

}
