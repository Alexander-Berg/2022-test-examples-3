package ru.yandex.market.loyalty.core.dao;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.core.dao.trigger.InsertResult;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.trigger.event.BaseTriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.LoginEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.TRIGGER_EVENT_LONG_RETRY_INTERVAL;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createLoginEvent;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 14.06.17
 */
public class TriggerEventDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void add() {
        assertThat(triggerEventDao.getAll(), is(empty()));
        BaseTriggerEvent event = createLoginEvent(123L, CoreMarketPlatform.BLUE);
        triggerEventQueueService.addEventToQueue(event);
        triggerEventQueueService.addEventToQueue(event);

        assertThat(triggerEventDao.getAll(), hasSize(1));
    }

    @Test
    public void params() {
        LoginEvent event = createLoginEvent(123L, CoreMarketPlatform.BLUE);
        triggerEventQueueService.addEventToQueue(event);

        LoginEvent fetched = (LoginEvent) triggerEventDao.getAll().get(0);
        assertEquals(123L, fetched.getIdentity().getValue());
        assertEquals(CoreMarketPlatform.BLUE, fetched.getPlatform());
    }

    @Test
    public void process() {
        triggerEventQueueService.addEventToQueue(createLoginEvent(123L, CoreMarketPlatform.BLUE));

        assertThat(triggerEventDao.getNotProcessed(Duration.ZERO), hasSize(1));

        triggerEventDao.setProcessResult(triggerEventDao.getAll().get(0), TriggerEventProcessedResult.SUCCESS, null);

        assertThat(triggerEventDao.getNotProcessed(Duration.ZERO), is(empty()));
    }

    @Test
    public void markError() {
        triggerEventQueueService.addEventToQueue(createLoginEvent(123L, CoreMarketPlatform.BLUE));

        assertThat(triggerEventDao.getNotProcessed(Duration.ZERO), hasSize(1));

        triggerEventDao.setProcessResult(triggerEventDao.getAll().get(0), TriggerEventProcessedResult.ERROR, null);

        clock.spendTime(1, ChronoUnit.HOURS);

        List<TriggerEvent> events = triggerEventDao.getNotProcessed(Duration.ZERO);
        assertThat(events, hasSize(1));
        TriggerEvent failedEvent = events.get(0);
        assertEquals(TriggerEventProcessedResult.ERROR, failedEvent.getProcessedResult());
    }

    @Test
    public void markErrorWithLongRetryInterval() {
        configurationService.set(TRIGGER_EVENT_LONG_RETRY_INTERVAL, 60); // 4, 8, 16 hours

        triggerEventQueueService.addEventToQueue(createLoginEvent(123L, CoreMarketPlatform.BLUE));
        assertThat(triggerEventDao.getNotProcessed(Duration.ZERO), hasSize(1));

        triggerEventDao.setProcessResultWithLongInterval(triggerEventDao.getAll().get(0), TriggerEventProcessedResult.ERROR, null);

        clock.spendTime(3, ChronoUnit.HOURS);
        List<TriggerEvent> events = triggerEventDao.getNotProcessed(Duration.ZERO);
        assertThat("Время обработки события еще не наступило", events, hasSize(0));

        clock.spendTime(1, ChronoUnit.HOURS);
        events = triggerEventDao.getNotProcessed(Duration.ZERO);
        assertThat("Время обработки события наступило", events, hasSize(1));
    }

    @Test
    public void shouldNotAllowToChangeSuccessToError() {
        triggerEventQueueService.addEventToQueue(createLoginEvent(123L, CoreMarketPlatform.BLUE));

        triggerEventDao.setProcessResult(triggerEventDao.getAll().get(0), TriggerEventProcessedResult.SUCCESS, null);

        triggerEventDao.setProcessResult(triggerEventDao.getAll().get(0), TriggerEventProcessedResult.ERROR, null);

        assertEquals(TriggerEventProcessedResult.SUCCESS, triggerEventDao.getAll().get(0).getProcessedResult());
    }

    @Test
    public void shouldIgnoreUnknownEventParams() {
        InsertResult<LoginEvent> event = triggerEventQueueService.addEventToQueue(
                LoginEvent.createNew(123, "13", CoreMarketPlatform.BLUE, 213L, "test", "request_id")
        );
        jdbcTemplate.update(TriggerEventDao.INSERT_EVENT_PARAM,
                event.getData().getId(),
                "UNKNOWN_CODE",
                "UNKNOWN_VALUE"
        );
        triggerEventDao.getNotProcessed(Duration.ZERO);
    }
}
