package ru.yandex.market.tms;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.tms.CleanMdsS3HistoryExecutor;
import ru.yandex.market.common.mds.s3.tms.SimpleCleanMdsS3HistoryExecutor;
import ru.yandex.market.mbi.tms.monitor.MonitorFriendlyExecutorUtils;
import ru.yandex.market.tms.quartz2.RunnableExecutor;
import ru.yandex.market.tms.quartz2.db.CopyTableJob;
import ru.yandex.market.tms.quartz2.dynamic.DynamicTasksSchedulerExecutor;
import ru.yandex.market.tms.quartz2.model.VerboseExecutor;
import ru.yandex.market.tms.quartz2.writers.WriteQueryResultToFileJob;

/**
 * Проверка того, что все наследники {@link ru.yandex.market.tms.quartz2.model.Executor} размечены.
 *
 * @author vbudnev
 */
public class ExecutorsAreMonitoringFriendlyTest {
    @Test
    public void test_friendly() {
        //игнорим экзекьюторы, которые приезжают из библиотек
        MonitorFriendlyExecutorUtils.assertExecutorsAreFriendly(
                "ru.yandex.market",
                ImmutableSet.of(
                        //common-mds-s3-tms
                        SimpleCleanMdsS3HistoryExecutor.class,
                        CleanMdsS3HistoryExecutor.class,

                        //tms-core-quartz-2
                        CopyTableJob.class,
                        DynamicTasksSchedulerExecutor.class,
                        WriteQueryResultToFileJob.class,
                        RunnableExecutor.class,
                        ru.yandex.market.tms.quartz2.model.RunnableExecutor.class,
                        VerboseExecutor.class
                )
        );
    }
}
