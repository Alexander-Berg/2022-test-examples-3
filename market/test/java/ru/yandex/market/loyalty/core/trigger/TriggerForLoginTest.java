package ru.yandex.market.loyalty.core.trigger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.trigger.event.LoginEvent;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coupon.CouponNotificationService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;

import java.math.BigDecimal;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createLoginEvent;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EMISSION_BUDGET;

public class TriggerForLoginTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private PersNotifyClient persNotifyClient;
    @Autowired
    private CouponNotificationService couponNotificationService;
    @Autowired
    private TriggersFactory triggersFactory;
    private Promo promo;
    private LoginEvent blueLoginEvent = createLoginEvent(123L, CoreMarketPlatform.BLUE);

    private LoginEvent greenLoginEvent = createLoginEvent(123L, CoreMarketPlatform.GREEN);

    @Before
    public void init() {
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        triggersFactory.createLoginTrigger(promo);
    }

    @Test
    public void shouldNotSendCouponForGreenLogin() {
        triggerEventQueueService.addEventToQueue(greenLoginEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET)
        );
    }

    @Test
    public void shouldSendCouponForBlueLogin() throws PersNotifyClientException {
        triggerEventQueueService.addEventToQueue(blueLoginEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );

        couponNotificationService.notifyUsersAboutCouponActivation();

        verify(persNotifyClient).createEvent(any());
    }

    @Test
    public void shouldNotSendCouponForBlueLoginTwice() throws PersNotifyClientException {
        triggerEventQueueService.addEventToQueue(blueLoginEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        couponNotificationService.notifyUsersAboutCouponActivation();

        verify(persNotifyClient).createEvent(any());
        triggerEventQueueService.addEventToQueue(blueLoginEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        couponNotificationService.notifyUsersAboutCouponActivation();

        verifyNoMoreInteractions(persNotifyClient);
    }


    @Test
    @Ignore("broken conditions should be clarified (what is the first login)")
    public void shouldSendCouponForBlueLoginTwiceIfPromoDisabledFirst() throws PersNotifyClientException {
        promoService.updateStatus(promo, PromoStatus.INACTIVE);
        triggerEventQueueService.addEventToQueue(blueLoginEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        couponNotificationService.notifyUsersAboutCouponActivation();

        verifyNoMoreInteractions(persNotifyClient);
        promoService.updateStatus(promo, PromoStatus.ACTIVE);
        triggerEventQueueService.addEventToQueue(blueLoginEvent);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        couponNotificationService.notifyUsersAboutCouponActivation();
        verify(persNotifyClient).createEvent(any());
    }
}
