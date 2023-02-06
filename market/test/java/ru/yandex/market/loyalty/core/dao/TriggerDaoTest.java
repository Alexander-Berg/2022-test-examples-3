package ru.yandex.market.loyalty.core.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.trigger.TriggerDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.trigger.event.LoginEvent;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.Trigger;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createLoginEvent;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class TriggerDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private TriggerDao triggerDao;
    @Autowired
    TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private TriggerEventDao triggerEventDao;

    private final LoginEvent event = createLoginEvent(123L, CoreMarketPlatform.BLUE);
    private Trigger<LoginEvent> trigger;


    @Before
    public void init() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        trigger = triggersFactory.createLoginTrigger(promo);
    }

    @Test
    public void shouldAllowToDeleteTriggerWithProcessedEvents() {
        triggerEventQueueService.addEventToQueue(event);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        triggerDao.removeTrigger(trigger.getId());
    }

    @Test
    public void shouldReturnEventsAfterDelay() {
        triggerEventQueueService.addEventToQueue(event);

        assertThat(triggerEventDao.getNotProcessed(Duration.ofSeconds(2)), is(empty()));
        clock.spendTime(Duration.ofSeconds(2));
        assertThat(triggerEventDao.getNotProcessed(Duration.ofSeconds(2)), hasSize(1));
    }
}
