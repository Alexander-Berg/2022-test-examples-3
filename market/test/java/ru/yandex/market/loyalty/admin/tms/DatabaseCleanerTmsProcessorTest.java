package ru.yandex.market.loyalty.admin.tms;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.test.TestFor;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

@TestFor(DatabaseCleanerTmsProcessor.class)
public class DatabaseCleanerTmsProcessorTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    private static final Duration MINUTE = Duration.ofMinutes(1);
    private static final Duration DAY = Duration.ofDays(1);
    private static final Duration TWO_MONTHS = Duration.ofDays(60);
    private static final Duration TWO_MONTHS_PLUS = Duration.ofDays(61);
    private static final Duration THREE_MONTHS = Duration.ofDays(90);
    @Autowired
    private DatabaseCleanerTmsProcessor databaseCleanerTmsProcessor;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggerEventDao triggerEventDao;

    @Test
    public void shouldNotCleanNewEvents() {
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated());
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        clock.spendTime(DAY);

        databaseCleanerTmsProcessor.cleanOldTriggerEvents(
                clock.instant().minus(TWO_MONTHS),
                clock.instant().minus(THREE_MONTHS),
                MINUTE
        );

        assertThat(triggerEventDao.getAll(), not(empty()));
    }

    @Test
    public void shouldCleanOldEvents() {
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated());
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        clock.spendTime(TWO_MONTHS_PLUS);

        databaseCleanerTmsProcessor.cleanOldTriggerEvents(
                clock.instant().minus(TWO_MONTHS),
                clock.instant().minus(THREE_MONTHS),
                MINUTE
        );

        assertThat(triggerEventDao.getAll(), empty());
    }

    @Test
    public void shouldCleanOnlyProcessedEvents() {
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated());

        clock.spendTime(TWO_MONTHS_PLUS);

        databaseCleanerTmsProcessor.cleanOldTriggerEvents(
                clock.instant().minus(TWO_MONTHS),
                clock.instant().minus(THREE_MONTHS),
                MINUTE
        );

        assertThat(triggerEventDao.getAll(), not(empty()));

        clock.spendTime(THREE_MONTHS);

        databaseCleanerTmsProcessor.cleanOldTriggerEvents(
                clock.instant().minus(TWO_MONTHS),
                clock.instant().minus(THREE_MONTHS),
                MINUTE
        );
        assertThat(triggerEventDao.getAll(), empty());
    }
}
