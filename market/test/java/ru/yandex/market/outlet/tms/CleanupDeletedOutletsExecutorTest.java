package ru.yandex.market.outlet.tms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тесты для {@link CleanupDeletedOutletsExecutor}.
 */
@DbUnitDataSet(before = "CleanupDeletedOutletsExecutor.common.before.csv")
class CleanupDeletedOutletsExecutorTest extends FunctionalTest {

    @Autowired
    private CleanupDeletedOutletsExecutor executor;


    @Test
    @DisplayName("Удаление не происходит если не было отсечки по времени")
    @DbUnitDataSet(
            before = "CleanupDeletedOutletsExecutor.testDeletionThreshold.csv",
            after = "CleanupDeletedOutletsExecutor.testDeletionThreshold.csv"
    )
    void testDeletionThreshold() {
        runJob();
    }

    @Test
    @DisplayName("Физическое удаление не происходит если не выставлен флаг что точка удалена")
    @DbUnitDataSet(
            before = "CleanupDeletedOutletsExecutor.testDeletionFlag.csv",
            after = "CleanupDeletedOutletsExecutor.testDeletionFlag.csv"
    )
    void testDeletionFlag() {
        runJob();
    }

    @Test
    @DisplayName("Успешно удалили уже удаленные в Nesu точки")
    @DbUnitDataSet(
            before = "CleanupDeletedOutletsExecutor.testCorrectDeletion.before.csv",
            after = "CleanupDeletedOutletsExecutor.testCorrectDeletion.after.csv"
    )
    void testCorrectDeletion() {
        runJob();
    }

    private void runJob() {
        executor.doJob(Mockito.mock(JobExecutionContext.class));
    }

}
