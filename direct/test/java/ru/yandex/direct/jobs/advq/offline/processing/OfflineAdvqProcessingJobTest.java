package ru.yandex.direct.jobs.advq.offline.processing;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OfflineAdvqProcessingJobTest {
    private static final String TEST_UPLOAD_TIME = "2017-10-10 00:00:01";
    private static final String TEST_UPLOAD_TIME_OTHER = "2017-10-12 00:00:01";
    private static final String TEST_FORECAST_DATES_PERIOD = "20171010-20171109";
    private static final String TEST_FORECAST_DATE = "2017-11-10T00:00";
    private static final YtCluster DEFAULT_YT_CLUSTER = YtCluster.HAHN;

    @Mock
    private YtOperator ytOperator;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private YtProvider ytProvider;
    @Mock
    private ShardHelper shardHelper;
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    private OfflineAdvqProcessingJob job;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(TEST_FORECAST_DATES_PERIOD)
                .when(ytOperator)
                .readTableStringAttribute(any(), eq(OfflineAdvqProcessingJob.FORECAST_TIME_TABLE_ATTR));
        doReturn(TEST_UPLOAD_TIME_OTHER)
                .when(ytOperator).readTableUploadTime(any());
        doReturn(ytOperator)
                .when(ytProvider).getOperator(DEFAULT_YT_CLUSTER);
        YtClusterConfig ytClusterConfig = mock(YtClusterConfig.class);
        doReturn("//home/direct")
                .when(ytClusterConfig).getHome();
        doReturn(ytClusterConfig)
                .when(ytProvider).getClusterConfig(DEFAULT_YT_CLUSTER);
        doReturn(TEST_UPLOAD_TIME)
                .when(ppcPropertiesSupport).get(eq(OfflineAdvqProcessingJob.UPLOAD_TIME_PROP_NAME));
        doReturn(Arrays.asList(1, 2, 3))
                .when(shardHelper).dbShards();

        job = new OfflineAdvqProcessingJob(ytProvider, DEFAULT_YT_CLUSTER, shardHelper, ppcPropertiesSupport);
    }

    @Test
    void testCanRunFalse() {
        assertThat(job.canRun(TEST_UPLOAD_TIME))
                .isFalse();
    }

    @Test
    void testCanRunTrueOnValue() {
        assertThat(job.canRun(TEST_UPLOAD_TIME_OTHER))
                .isTrue();
    }

    @Test
    void testCanRunTrueOnNull() {
        doReturn(null)
                .when(ppcPropertiesSupport).get(eq(OfflineAdvqProcessingJob.UPLOAD_TIME_PROP_NAME));

        assertThat(job.canRun(TEST_UPLOAD_TIME))
                .isTrue();
    }

    @Test
    void testGetForecastTime() {
        assertThat(job.getForecastTime(TEST_FORECAST_DATES_PERIOD))
                .isEqualTo(TEST_FORECAST_DATE);
    }

    @Test
    void testExecuteCorrect() {
        job.execute();

        verify(ytOperator, times(1))
                .yqlExecute(any());

        verify(ytOperator, times(1))
                .runOperation(any());

        verify(ppcPropertiesSupport)
                .set(eq(OfflineAdvqProcessingJob.UPLOAD_TIME_PROP_NAME), eq(TEST_UPLOAD_TIME_OTHER));
        verify(ppcPropertiesSupport)
                .set(eq(OfflineAdvqProcessingJob.FORECAST_TIME_PROP_NAME), eq(TEST_FORECAST_DATE));
    }

    @Test
    void testExecuteError() {
        doThrow(new RuntimeException())
                .when(ytOperator).runOperation(any());

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(RuntimeException.class);

        verify(ppcPropertiesSupport, never())
                .set(anyString(), anyString());
    }
}
