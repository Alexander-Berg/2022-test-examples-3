package ru.yandex.market.jmf.module.scheduling;

import java.time.OffsetDateTime;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.RequiredAttributesValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskDao;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskProcessor;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskTestUtils;
import ru.yandex.market.jmf.queue.retry.internal.SlowRetryTasksQueue;
import ru.yandex.market.jmf.tx.TxService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EntitySchedulerTest.TestConfiguration.class)
public class EntitySchedulerTest {

    private static final Fqn FQN_1 = Fqn.of("simple1");
    private static final String ENABLE_FAILED_SCHEDULER_STRATEGY_ATTR = "enableFailedSchedulerStrategy";
    private static final String ENABLE_SLOW_SCHEDULER_STRATEGY_ATTR = "enableSlowSchedulerStrategy";

    private static final String ACTIVE_STATUS = "active";
    private static final String EVERY_FIVE_MINUTES_CRON = "everyFiveMinutes";
    private static final String EVERY_THIRTY_MINUTES_CRON = "everyThirtyMinutes";

    @Inject
    DbService dbService;

    @Inject
    BcpService bcpService;

    @Inject
    TxService txService;

    @Inject
    SchedulerExecutor schedulerExecutor;

    @Inject
    RetryTaskProcessor retryTaskProcessor;

    @Inject
    RetryTaskTestUtils retryTaskTestUtils;

    @Inject
    SlowRetryTasksQueue retryTasksQueue;

    @Inject
    RetryTaskDao retryTaskDao;

    @BeforeEach
    public void setUp() {
        txService.runInNewTx(() -> {
            dbService.createQuery("delete from simple1").executeUpdate();
            retryTasksQueue.reset();
            txService.runInNewTx(() -> retryTaskDao.deleteTasks());
        });
    }

    @Test
    @Transactional
    public void createSchedulerOnCreateEntityWithScheduleLogic() {
        Entity entity = bcpService.create(FQN_1, Map.of());
        EntityScheduler scheduler = entity.getAttribute(ScheduledLogic.SCHEDULER);
        assertNotNull(scheduler);
        assertEquals(HasWorkflow.ARCHIVED, scheduler.getStatus());
        assertEquals(entity.getGid(), scheduler.getEntity());
        assertNull(scheduler.getTaskId());
        assertNull(scheduler.getLastExecutionFinishTime());
    }

    @Test
    @Transactional
    public void scheduleTaskOnActivateScheduler() {
        EntityScheduler scheduler = createActiveEntityScheduler();
        assertNotNull(scheduler.getTaskId());
        assertNull(scheduler.getLastExecutionFinishTime());
    }

    @Test
    @Transactional
    public void throwExceptionOnActivateSchedulerWithoutSchedulePlanning() {
        Entity entity = bcpService.create(FQN_1, Map.of());
        EntityScheduler scheduler = entity.getAttribute(ScheduledLogic.SCHEDULER);
        assertThrows(
                RequiredAttributesValidationException.class,
                () -> bcpService.edit(scheduler, Map.of(EntityScheduler.STATUS, ACTIVE_STATUS))
        );
    }

    @Test
    @Transactional
    public void unscheduleTaskOnArchiveScheduler() {
        EntityScheduler scheduler = createActiveEntityScheduler();
        bcpService.edit(scheduler, Map.of(EntityScheduler.STATUS, HasWorkflow.ARCHIVED));
        assertNull(scheduler.getTaskId());
    }

    @Test
    @Transactional
    public void rescheduleTaskOnChangeSchedulePlanning() {
        EntityScheduler scheduler = createActiveEntityScheduler();
        long unexpectedTaskId = retryTaskTestUtils.getSingleTaskId();
        bcpService.edit(scheduler, Map.of(EntityScheduler.SCHEDULE_PLANNING, EVERY_THIRTY_MINUTES_CRON));
        long taskId = retryTaskTestUtils.getSingleTaskId();
        assertNotEquals(unexpectedTaskId, taskId);
    }

    @Test
    public void setLastExecutionTimeOnExecuteTask() {
        EntityScheduler scheduler = txService.doInNewTx(this::createActiveEntityScheduler);
        schedulerExecutor.execute(scheduler.getGid());

        EntityScheduler actual = txService.doInNewTx(() -> dbService.get(scheduler.getGid()));
        assertNotNull(actual.getLastExecutionStartTime());
        assertNotNull(actual.getLastExecutionFinishTime());

        OffsetDateTime unexpectedLastExecutionStartTime = actual.getLastExecutionStartTime();
        OffsetDateTime unexpectedLastExecutionFinishTime = actual.getLastExecutionFinishTime();
        schedulerExecutor.execute(scheduler.getGid());

        actual = txService.doInNewTx(() -> dbService.get(scheduler.getGid()));
        assertNotEquals(unexpectedLastExecutionStartTime, actual.getLastExecutionStartTime());
        assertNotEquals(unexpectedLastExecutionFinishTime, actual.getLastExecutionFinishTime());
    }

    @Test
    public void dontSetLastExecutionFinishTimeOnFailedExecuteTask() {
        EntityScheduler scheduler = txService.doInNewTx(() -> {
            Entity entity = bcpService.create(FQN_1, Map.of(ENABLE_FAILED_SCHEDULER_STRATEGY_ATTR, true));
            EntityScheduler entityScheduler = entity.getAttribute(ScheduledLogic.SCHEDULER);
            return bcpService.edit(entityScheduler, Map.of(
                    EntityScheduler.STATUS, ACTIVE_STATUS,
                    EntityScheduler.SCHEDULE_PLANNING, EVERY_FIVE_MINUTES_CRON
            ));
        });
        schedulerExecutor.execute(scheduler.getGid());
        EntityScheduler actual = txService.doInNewTx(() -> dbService.get(scheduler.getGid()));
        assertNull(actual.getLastExecutionFinishTime());
        assertNotNull(actual.getLastExecutionStartTime());
    }

    @Test
    @Transactional
    public void unscheduleTaskOnDeleteScheduler() {
        EntityScheduler scheduler = createActiveEntityScheduler();
        bcpService.delete(scheduler);
        retryTaskTestUtils.assertTasksIsEmpty();
    }

    @Test
    public void cronSchedulePlanningCountedFromLastExecutionStartTime() {
        EntityScheduler scheduler = txService.doInNewTx(() -> {
            SchedulePlanning schedulePlanning = bcpService.create(CronSchedulePlanning.FQN, Map.of(
                    CronSchedulePlanning.CODE, Randoms.string(),
                    CronSchedulePlanning.TITLE, Randoms.string(),
                    CronSchedulePlanning.EXPRESSION, "0/2 * * * * ?",
                    CronSchedulePlanning.COUNT_FROM_LAST_EXECUTION_START_TIME, true
            ));
            return createActiveEntityScheduler(schedulePlanning, Map.of(
                    ENABLE_SLOW_SCHEDULER_STRATEGY_ATTR, true
            ));
        });

        Exceptions.sneakyRethrow(() -> Thread.sleep(2000));

        txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        EntityScheduler current = txService.doInNewTx(() -> dbService.get(scheduler.getGid()));

        OffsetDateTime lastExecutionStartTime = current.getLastExecutionStartTime();
        assertNotNull(lastExecutionStartTime);

        txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        current = txService.doInNewTx(() -> dbService.get(scheduler.getGid()));
        // т.к. countFromLastExecutionStartTime=true и последний запуск длился больше 2 сек.,
        // следущий запуск должен начаться сразу же, как предыдщий завершится
        assertNotEquals(lastExecutionStartTime, current.getLastExecutionStartTime());
    }

    @Test
    public void cronSchedulePlanningCountedFromLastExecutionFinishTime() {
        EntityScheduler scheduler = txService.doInNewTx(() -> {
            SchedulePlanning schedulePlanning = bcpService.create(CronSchedulePlanning.FQN, Map.of(
                    CronSchedulePlanning.CODE, Randoms.string(),
                    CronSchedulePlanning.TITLE, Randoms.string(),
                    CronSchedulePlanning.EXPRESSION, "0/2 * * * * ?",
                    CronSchedulePlanning.COUNT_FROM_LAST_EXECUTION_START_TIME, false
            ));
            return createActiveEntityScheduler(schedulePlanning, Map.of(
                    ENABLE_SLOW_SCHEDULER_STRATEGY_ATTR, true
            ));
        });

        Exceptions.sneakyRethrow(() -> Thread.sleep(2100));

        txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        EntityScheduler current = txService.doInNewTx(() -> dbService.get(scheduler.getGid()));

        OffsetDateTime lastExecutionStartTime = current.getLastExecutionStartTime();
        assertNotNull(lastExecutionStartTime);

        txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue));
        current = txService.doInNewTx(() -> dbService.get(scheduler.getGid()));
        // хотя последний запуск длился больше 2 сек., следующий запуск не должен произойти сразу же, т.к.
        // countFromLastExecutionStartTime=false
        assertEquals(lastExecutionStartTime, current.getLastExecutionStartTime());
    }

    private EntityScheduler createActiveEntityScheduler() {
        return createActiveEntityScheduler(EVERY_FIVE_MINUTES_CRON, Map.of());
    }

    private EntityScheduler createActiveEntityScheduler(Object schedulePlanning,
                                                        Map<String, Object> additionalProps) {
        Entity entity = bcpService.create(FQN_1, additionalProps);
        EntityScheduler scheduler = entity.getAttribute(ScheduledLogic.SCHEDULER);
        bcpService.edit(scheduler, Map.of(
                EntityScheduler.STATUS, ACTIVE_STATUS,
                EntityScheduler.SCHEDULE_PLANNING, schedulePlanning
        ));
        return scheduler;
    }

    @Configuration
    @Import(InternalModuleSchedulingTestConfiguration.class)
    static class TestConfiguration {

        @Bean
        public SchedulerStrategy failedSchedulerStrategy(DbService dbService) {
            return new SchedulerStrategy() {
                @Override
                @Transactional
                public boolean isApplicable(Scheduler scheduler) {
                    if (!(scheduler instanceof EntityScheduler)) {
                        return false;
                    }
                    String entityGid = ((EntityScheduler) scheduler).getEntity();
                    if (null == entityGid) {
                        return false;
                    }
                    return dbService.get(entityGid).getAttribute(ENABLE_FAILED_SCHEDULER_STRATEGY_ATTR);
                }

                @Override
                public boolean apply(String schedulerGid, SchedulerExecutionContext context) {
                    return false;
                }
            };
        }

        @Bean
        public SchedulerStrategy slowSchedulerStrategy(DbService dbService) {
            return new SchedulerStrategy() {
                @Override
                @Transactional
                public boolean isApplicable(Scheduler scheduler) {
                    if (!(scheduler instanceof EntityScheduler)) {
                        return false;
                    }
                    String entityGid = ((EntityScheduler) scheduler).getEntity();
                    if (null == entityGid) {
                        return false;
                    }
                    return dbService.get(entityGid).getAttribute(ENABLE_SLOW_SCHEDULER_STRATEGY_ATTR);
                }

                @Override
                public boolean apply(String schedulerGid, SchedulerExecutionContext context) {
                    Exceptions.sneakyRethrow(() -> Thread.sleep(2100));
                    return true;
                }
            };
        }
    }
}
