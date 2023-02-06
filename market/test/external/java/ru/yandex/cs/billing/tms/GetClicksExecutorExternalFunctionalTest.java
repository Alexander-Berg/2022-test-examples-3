package ru.yandex.cs.billing.tms;

import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.cs.billing.msapi.ClicksProcessingJobQueueRepository;
import ru.yandex.cs.billing.tms.multi.MultiServiceExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.cs.billing.tms.GetClicksExecutor.PROCESSING_STATUSES;

public class GetClicksExecutorExternalFunctionalTest extends AbstractCsBillingTmsExternalFunctionalTest {
    private final MultiServiceExecutor getClicksExecutor;
    private final ClicksProcessingJobQueueRepository jobsRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public GetClicksExecutorExternalFunctionalTest(MultiServiceExecutor getClicksExecutor,
                                                   ClicksProcessingJobQueueRepository clicksProcessingQueueRepository,
                                                   TransactionTemplate mbiStatsTransactionTemplate) {
        this.getClicksExecutor = getClicksExecutor;
        this.jobsRepository = clicksProcessingQueueRepository;
        this.transactionTemplate = mbiStatsTransactionTemplate;
    }

    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testMarketplaceModelbidsClicksAndRollbacksGet/before.csv",
            after = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testMarketplaceModelbidsClicksAndRollbacksGet/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMarketplaceModelbidsClicksAndRollbacksGet() {
        getClicksExecutor.doJob(mockContext());
    }

    @DisplayName("Запоздавшие к обработке клике в текущем дне переносятся на trunc(SYSDATE) в таблице CLICKS_TRT")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testModelbidsMergeLateClicksToClicksTrtAtSysdate/before.csv",
            after = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testModelbidsMergeLateClicksToClicksTrtAtSysdate/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testModelbidsMergeLateClicksToClicksTrtAtSysdate() {
        getClicksExecutor.doJob(mockContext());
    }

    @DisplayName("Агрегируем и привязываем сырые клики с одним датасорцом из нескольких кликлогов")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testGetModelbidsClicksFromMultipleClicklogs/before.csv",
            after = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testGetModelbidsClicksFromMultipleClicklogs/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetModelbidsClicksFromMultipleClicklogs() {
        getClicksExecutor.doJob(mockContext());
    }

    @DisplayName("Агрегируем и привязываем сырые клики с одним датасорцом из нескольких кликлогов только один раз")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testGetModelbidsClicksFromMultipleClicklogsOnlyOnce/before.csv",
            after = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testGetModelbidsClicksFromMultipleClicklogsOnlyOnce/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetModelbidsClicksFromMultipleClicklogsOnlyOnce() {
        JobExecutionContext context = mockContext();
        getClicksExecutor.doJob(context);
        getClicksExecutor.doJob(context);
    }

    @DisplayName("Агрегируем и привязываем сырые клики с одним датасорцом из нескольких кликлогов только один раз")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testGetModelbidsClicksFromMultipleClicklogsWithLockedJob/before.csv",
            after = "/ru/yandex/cs/billing/tms/GetClicksExecutorExternalFunctionalTest/testGetModelbidsClicksFromMultipleClicklogsWithLockedJob/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetModelbidsClicksFromMultipleClicklogsWithLockedJob() {
        SleepingBlock innerSleepingBlock = new SleepingBlock();

        new Thread(() -> transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobsRepository.lock(5L, PROCESSING_STATUSES);

                innerSleepingBlock.sleep();
            }
        })).start();

        SleepingBlock currentSleepingBlock = new SleepingBlock();
        currentSleepingBlock.sleep(() -> !innerSleepingBlock.isSleeping());

        getClicksExecutor.doJob(mockContext());
        innerSleepingBlock.stop();
    }

    private JobExecutionContext mockContext() {
        final JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        final JobDetail mockJobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        Mockito.when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }

    private static class SleepingBlock {
        private static final long SLEEP_IN_MILLIS = 100L;

        private volatile boolean sleeping;

        public void sleep() {
            sleep(() -> true);
        }

        public void sleep(Supplier<Boolean> status) {
            start();
            while (sleeping && status.get()) {
                try {
                    Thread.sleep(SLEEP_IN_MILLIS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        void start() {
            this.sleeping = true;
        }

        void stop() {
            this.sleeping = false;
        }

        boolean isSleeping() {
            return this.sleeping;
        }
    }
}
