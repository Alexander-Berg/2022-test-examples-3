package ru.yandex.market.common.mds.s3.tms;

import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;

import ru.yandex.market.common.mds.s3.spring.service.MdsS3ResourceConfigurationCleaner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit-тесты для {@link CleanMdsS3HistoryExecutor}.
 *
 * @author Vladislav Bauer
 */
public class CleanMdsS3HistoryExecutorTest {

    @Test
    public void testDoJob() {
        final JobExecutionContext mock = Mockito.mock(JobExecutionContext.class);
        final MdsS3ResourceConfigurationCleaner cleaner = Mockito.mock(MdsS3ResourceConfigurationCleaner.class);
        final CleanMdsS3HistoryExecutor executor = new CleanMdsS3HistoryExecutor(cleaner);

        executor.doJob(mock);

        verify(cleaner, atLeastOnce()).doClean();
        verifyNoMoreInteractions(cleaner);
    }

}
