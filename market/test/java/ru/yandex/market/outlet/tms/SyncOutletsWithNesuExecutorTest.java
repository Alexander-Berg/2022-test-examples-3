package ru.yandex.market.outlet.tms;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.nesu.client.NesuClient;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Тесты для {@link SyncOutletsWithNesuExecutor}.
 * @author Vladislav Bauer
 */
@DbUnitDataSet(before = "SyncOutletsWithNesuTest.common.before.csv")
public class SyncOutletsWithNesuExecutorTest extends AbstractNesuSyncTest {

    @Autowired
    private SyncOutletsWithNesuExecutor executor;

    @Autowired
    private NesuClient nesuClient;

    @AfterEach
    void onAfter() {
        checkFollowingSyncSkip();
    }

    @Test
    @DbUnitDataSet(
            before = "SyncOutletsWithNesuTest.testNoChanges.csv",
            after = "SyncOutletsWithNesuTest.testNoChanges.csv"
    )
    void testNoChanges() {
        runProcess();
        verifyNoInteractions(nesuClient);
    }

    @Override
    protected void runProcess() {
        executor.doJob(Mockito.mock(JobExecutionContext.class));
    }

    /**
     * Проверить что после предыдущей синхронизации нечего актуализировать.
     */
    private void checkFollowingSyncSkip() {
        reset(nesuClient);
        runProcess();
        verifyNoInteractions(nesuClient);
    }

}
