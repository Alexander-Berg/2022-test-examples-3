package ru.yandex.market.common.mds.s3.tms;

import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.service.api.PureHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link SimpleCleanMdsS3HistoryExecutor}.
 *
 * @author Vladislav Bauer
 */
public class SimpleCleanMdsS3HistoryExecutorTest {

    @Test
    public void testDoJob() {
        final ResourceConfigurationProvider configurationProvider = createResourceConfigurationProvider();
        final PureHistoryMdsS3Client historyMdsS3Client = createHistoryMdsS3Client();

        final JobExecutionContext jobExecutionContext = Mockito.mock(JobExecutionContext.class);
        final SimpleCleanMdsS3HistoryExecutor executor = new SimpleCleanMdsS3HistoryExecutor(
            configurationProvider, historyMdsS3Client
        );

        executor.doJob(jobExecutionContext);

        verify(configurationProvider, times(1)).getConfigurations();
        verify(historyMdsS3Client, times(1)).deleteOld(any());

        verifyNoMoreInteractions(configurationProvider);
        verifyNoMoreInteractions(historyMdsS3Client);
    }


    private ResourceConfigurationProvider createResourceConfigurationProvider() {
        final ResourceConfigurationProvider configurationProvider = Mockito.mock(ResourceConfigurationProvider.class);

        when(configurationProvider.getConfigurations())
            .thenReturn(Collections.singleton(Mockito.mock(ResourceConfiguration.class)));

        return configurationProvider;
    }

    private PureHistoryMdsS3Client createHistoryMdsS3Client() {
        return Mockito.mock(PureHistoryMdsS3Client.class);
    }

}
