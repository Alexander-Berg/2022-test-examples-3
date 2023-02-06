package ru.yandex.market.loyalty.core.trigger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResult;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.ForceEmmitCouponEvent;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.TRIGGER_NOT_FIT_CONDITION;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createForceEmmitCouponEvent;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EMISSION_BUDGET;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

public class TriggerForceEmmitCouponEventTest extends MarketLoyaltyCoreMockedDbTestBase {
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
    public void shouldEmmitCoupon() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        triggerFastEvaluatorService.processEvent(promo.getId(), createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY),
                TriggerEventTypes.FORCE_EMMIT_COUPON,
                discountUtils.getRulesPayload()
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldNotSendCoupon() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        triggerFastEvaluatorService.processEvent(promo.getId(), createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY),
                TriggerEventTypes.FORCE_EMMIT_COUPON,
                discountUtils.getRulesPayload()
        );

        couponNotificationService.notifyUsersAboutCouponActivation();

        verifyZeroInteractions(persNotifyClient);
    }

    @Test
    public void shouldEmmitOnlyOneCouponOnRetry() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        ForceEmmitCouponEvent event = createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY);
        triggerFastEvaluatorService.processEvent(promo.getId(), event, TriggerEventTypes.FORCE_EMMIT_COUPON,
                discountUtils.getRulesPayload()
        );
        triggerFastEvaluatorService.processEvent(promo.getId(), event, TriggerEventTypes.FORCE_EMMIT_COUPON,
                discountUtils.getRulesPayload()
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldEmmitTwoCouponsIfClientUniqueKeyIsChanged() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        triggerFastEvaluatorService.processEvent(promo.getId(), createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY),
                TriggerEventTypes.FORCE_EMMIT_COUPON,
                discountUtils.getRulesPayload()
        );
        triggerFastEvaluatorService.processEvent(promo.getId(),
                createForceEmmitCouponEvent(ANOTHER_CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_EMMIT_COUPON,
                discountUtils.getRulesPayload()
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.valueOf(2)))
        );
    }

    @Repeat(5)
    @Test
    public void shouldEmmitOnlyOneCouponOnConcurrentForceCreate() throws InterruptedException {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        ForceEmmitCouponEvent event = createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY);
        List<Coupon> coupons = new ArrayList<>();
        testConcurrency(() -> () -> {
            try {
                TriggerActionResult result = triggerFastEvaluatorService.processEvent(promo.getId(), event,
                        TriggerEventTypes.FORCE_EMMIT_COUPON,
                        discountUtils.getRulesPayload()
                );
                Coupon coupon = processResultUtils.requestSingleton(result, Coupon.class,
                        MarketLoyaltyErrorCode.TRIGGER_NOT_FIT_CONDITION
                );
                coupons.add(coupon);
            } catch (ConflictException ignore) {
            }
        });
        assertThat(coupons.stream().map(Coupon::getCode).collect(Collectors.toSet()), hasSize(1));
        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldProduceCouponToConsumer() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        ForceEmmitCouponEvent event = createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY);
        TriggerActionResult result = triggerFastEvaluatorService.processEvent(promo.getId(), event,
                TriggerEventTypes.FORCE_EMMIT_COUPON,
                discountUtils.getRulesPayload()
        );
        Coupon coupon = processResultUtils.requestSingleton(result, Coupon.class,
                MarketLoyaltyErrorCode.TRIGGER_NOT_FIT_CONDITION
        );

        assertNotNull(coupon);
        assertEquals(promo.getId(), coupon.getPromoId());
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());

        TriggerActionResult result2 = triggerFastEvaluatorService.processEvent(promo.getId(), event,
                TriggerEventTypes.FORCE_EMMIT_COUPON,
                discountUtils.getRulesPayload()
        );
        assertEquals(
                coupon.getCode(),
                processResultUtils.requestSingleton(result2, Coupon.class,
                        MarketLoyaltyErrorCode.TRIGGER_NOT_FIT_CONDITION).getCode()
        );
    }

    @Test
    public void shouldSaveTriggerEventResult() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createForceEmmitCouponTrigger(promo);

        triggerFastEvaluatorService.processEvent(promo.getId(), createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY),
                TriggerEventTypes.FORCE_EMMIT_COUPON,
                discountUtils.getRulesPayload()
        );

        List<TriggerEvent> events = triggerEventDao.getAll();
        assertThat(events, hasSize(1));

        ForceEmmitCouponEvent event = (ForceEmmitCouponEvent) events.get(0);
        assertEquals(CLIENT_UNIQUE_KEY, event.getUniqueKey());
        assertEquals(TriggerEventProcessedResult.SUCCESS, event.getProcessedResult());
    }

    @Test
    public void shouldFailIfPromoNotActive() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStartEmissionDate(DateUtils.addDays(new Date(), -1))
                .setStatus(PromoStatus.INACTIVE)
        );
        triggersFactory.createForceEmmitCouponTrigger(promo);

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(),
                        createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_EMMIT_COUPON,
                        discountUtils.getRulesPayload()
                )
        );
        assertEquals(MarketLoyaltyErrorCode.PROMO_NOT_ACTIVE, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldFailIfEmissionPeriodEnded() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStartEmissionDate(DateUtils.addDays(new Date(), 10))
        );
        triggersFactory.createForceEmmitCouponTrigger(promo);

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(),
                        createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_EMMIT_COUPON,
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
        triggersFactory.createForceEmmitCouponTrigger(promo);

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(),
                        createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_EMMIT_COUPON,
                        discountUtils.getRulesPayload()
                )
        );
        assertEquals(TRIGGER_NOT_FIT_CONDITION, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldFailIfNoTriggerFound() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        assertThrows(TriggerNotFoundException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(),
                        createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_EMMIT_COUPON,
                        discountUtils.getRulesPayload()
                )
        );
    }

    @Test
    public void shouldFailIfTriggersDuplicated() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        triggersFactory.createForceEmmitCouponTrigger(promo);
        triggersFactory.createForceEmmitCouponTrigger(promo);
        assertThrows(IllegalStateException.class,
                () -> triggerFastEvaluatorService.processEvent(promo.getId(), createForceEmmitCouponEvent(CLIENT_UNIQUE_KEY), TriggerEventTypes.FORCE_EMMIT_COUPON,
                        discountUtils.getRulesPayload()
                )
        );
    }

}
