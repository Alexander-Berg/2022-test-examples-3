package ru.yandex.market.loyalty.core.trigger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.trigger.event.LoginEvent;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.SqlMonitorService;
import ru.yandex.market.loyalty.core.service.coupon.CouponNotificationService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.trigger.actions.BrokenActionFactory;
import ru.yandex.market.loyalty.core.utils.BrokenLoginEvent;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createBrokenLoginEvent;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createLoginEvent;
import static ru.yandex.market.loyalty.core.utils.MonitorHelper.assertMonitor;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EMISSION_BUDGET;

public class ProcessEventTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private SqlMonitorService sqlMonitorService;
    @Autowired
    private PersNotifyClient persNotifyClient;
    @Autowired
    private CouponNotificationService couponNotificationService;
    @Autowired
    private TriggersFactory triggersFactory;

    private Promo promo;
    private LoginEvent blueLoginEvent = createLoginEvent(123L, CoreMarketPlatform.BLUE);
    private BrokenLoginEvent brokenBlueLoginEvent = createBrokenLoginEvent(123L, CoreMarketPlatform.BLUE);

    private LoginEvent greenLoginEvent = createLoginEvent(123L, CoreMarketPlatform.GREEN);

    @Override
    protected boolean shouldCheckConsistence() {
        return false;
    }

    @Before
    public void init() {
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(DEFAULT_EMISSION_BUDGET));
    }

    @After
    public void cleanUp() {
        BrokenActionFactory.cleanUp();
    }

    @Test
    public void actionWasCalled() throws PersNotifyClientException {
        triggersFactory.createLoginTrigger(promo);
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
    public void actionCalledOnlyOnce() {
        triggersFactory.createLoginTrigger(promo);
        triggerEventQueueService.addEventToQueue(blueLoginEvent);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void eventWasMarked() {
        triggersFactory.createLoginTrigger(promo);
        triggerEventQueueService.addEventToQueue(blueLoginEvent);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(triggerEventDao.getNotProcessed(Duration.ZERO), is(empty()));
        TriggerEvent triggerEvent = triggerEventDao.getAll().get(0);
        assertEquals(1, triggerEvent.getProcessTryCount());
        assertEquals(TriggerEventProcessedResult.SUCCESS, triggerEvent.getProcessedResult());
    }

    @Test
    public void defaultFieldsInEvent() {
        triggersFactory.createLoginTrigger(promo);
        triggerEventQueueService.addEventToQueue(blueLoginEvent);

        assertThat(triggerEventDao.getNotProcessed(Duration.ZERO), hasSize(1));
        TriggerEvent triggerEvent = triggerEventDao.getAll().get(0);
        assertEquals(0, triggerEvent.getProcessTryCount());
        assertEquals(TriggerEventProcessedResult.IN_QUEUE, triggerEvent.getProcessedResult());
    }

    @Test
    public void noTriggersForEvent() {
        triggersFactory.createLoginTrigger(promo);
        triggerEventQueueService.addEventToQueue(greenLoginEvent);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        TriggerEvent triggerEvent = triggerEventDao.getAll().get(0);
        assertEquals(TriggerEventProcessedResult.NO_TRIGGERS, triggerEvent.getProcessedResult());
    }

    @Test
    public void errorOnProcess() {
        triggersFactory.brokenLoginEventTrigger(promo);

        triggerEventQueueService.addEventToQueue(brokenBlueLoginEvent);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        TriggerEvent triggerEvent = triggerEventDao.getAll().get(0);
        assertEquals(1, triggerEvent.getProcessTryCount());
        assertEquals(TriggerEventProcessedResult.ERROR, triggerEvent.getProcessedResult());
    }

    @Test
    public void retryErrorsMaxRetryTimes() {
        triggersFactory.brokenLoginEventTrigger(promo);

        triggerEventQueueService.addEventToQueue(brokenBlueLoginEvent);

        for (int i = 0; i < TriggerEventDao.MAX_RETRY_COUNT * 2; i++) {
            triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
            clock.spendTime(1, ChronoUnit.HOURS);
        }

        TriggerEvent triggerEvent = triggerEventDao.getAll().get(0);
        assertEquals(TriggerEventDao.MAX_RETRY_COUNT, triggerEvent.getProcessTryCount());
        assertEquals(TriggerEventProcessedResult.ERROR, triggerEvent.getProcessedResult());
    }

    @Test
    public void successRetryError() {
        triggersFactory.brokenLoginEventTrigger(promo);

        triggerEventQueueService.addEventToQueue(brokenBlueLoginEvent);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        TriggerEvent triggerEvent = triggerEventDao.getAll().get(0);
        assertEquals(TriggerEventProcessedResult.ERROR, triggerEvent.getProcessedResult());

        BrokenActionFactory.notFailForPromo(promo.getId());
        clock.spendTime(1, ChronoUnit.HOURS);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        triggerEvent = triggerEventDao.getAll().get(0);
        assertEquals(2, triggerEvent.getProcessTryCount());
        assertEquals(TriggerEventProcessedResult.SUCCESS, triggerEvent.getProcessedResult());
    }

    @Test
    public void monitorForNotProcessed() {
        triggersFactory.createLoginTrigger(promo);
        clock.spendTime(-1, ChronoUnit.HOURS);
        triggerEventQueueService.addEventToQueue(blueLoginEvent);

        ComplicatedMonitoring.Result result = sqlMonitorService.checkDbState();
        assertMonitor(MonitoringStatus.CRITICAL, result);
        assertThat(result.getMessage(), endsWith("did not processed"));

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
        couponNotificationService.notifyUsersAboutCouponActivation();

        assertMonitor(MonitoringStatus.OK, sqlMonitorService.checkDbState());
    }

    @Test
    public void monitorForErrors() {
        triggersFactory.brokenLoginEventTrigger(promo);
        triggerEventQueueService.addEventToQueue(brokenBlueLoginEvent);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertMonitor(MonitoringStatus.OK, sqlMonitorService.checkDbState());


        for (int i = 0; i < TriggerEventDao.MAX_RETRY_COUNT; i++) {
            triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
            clock.spendTime(1, ChronoUnit.HOURS);
        }

        ComplicatedMonitoring.Result result = sqlMonitorService.checkDbState();
        assertMonitor(MonitoringStatus.CRITICAL, result);
        assertThat(result.getMessage(), startsWith("Error in processing of event"));
    }
}
