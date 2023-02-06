package ru.yandex.market.loyalty.core.trigger;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.trigger.event.SubscriptionEvent;
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
import ru.yandex.market.pers.notify.model.NotificationType;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createSubscriptionEvent;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EMISSION_BUDGET;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

public class TriggerSubscriptionTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd")
    );

    private static final String EMAIL = "email@yandex-team.ru";
    private static final Date EMISSION_DATE_FROM = makeExceptionsUnchecked(() -> DATE_FORMAT.get().parse("2018-01-01"));
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
    @Autowired
    private TriggerEventDao triggerEventDao;

    private long promoId;

    @Before
    public void init() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setStartEmissionDate(EMISSION_DATE_FROM)
        );

        triggersFactory.createSubscriptionTrigger(promo);
        promoId = promo.getId();
    }

    @Test
    public void commonPath() throws PersNotifyClientException {
        triggerEventQueueService.addEventToQueue(createSubscriptionEvent(NotificationType.ADVERTISING, EMAIL));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promoId).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );

        couponNotificationService.notifyUsersAboutCouponActivation();

        verify(persNotifyClient).createEvent(any());
    }

    @Test
    public void withUnsubscribeDateInPast() {
        triggerEventQueueService.addEventToQueue(createSubscriptionEvent(NotificationType.ADVERTISING, EMAIL,
                DateUtils.addMonths(EMISSION_DATE_FROM, -1), null));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promoId).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void withUnsubscribeDateInEmissionPeriod() {
        triggerEventQueueService.addEventToQueue(createSubscriptionEvent(NotificationType.ADVERTISING, EMAIL,
                DateUtils.addDays(EMISSION_DATE_FROM, 10), null));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promoId).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET)
        );
    }

    @Test
    public void shouldSaveUidIfPresent() {
        Long uid = 100L;
        triggerEventQueueService.addEventToQueue(createSubscriptionEvent(NotificationType.ADVERTISING, EMAIL,
                DateUtils.addDays(EMISSION_DATE_FROM, 10), uid));

        Identity<?> identity =
                ((SubscriptionEvent) triggerEventDao.getAll().get(0)).getIdentity().orElseThrow(AssertionError::new);

        assertEquals(new Uid(uid), identity);
    }
}
