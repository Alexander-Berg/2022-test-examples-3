package ru.yandex.direct.jobs.advq.offline.dataimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsShowsForecastService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.advq.offline.dataimport.OfflineAdvqKeywordsImportJob.IMPORT_TIME_PROP_NAME;
import static ru.yandex.direct.jobs.advq.offline.processing.OfflineAdvqProcessingJob.FORECAST_TIME_PROP_NAME;
import static ru.yandex.direct.jobs.advq.offline.processing.OfflineAdvqProcessingJob.UPLOAD_TIME_PROP_NAME;

class OfflineAdvqKeywordsImportJobTest {
    private static final String DATE_BEFORE = "2016-01-01T01:01:00.1234";
    private static final String DATE_AFTER = "2016-01-01T01:01:01.1234";
    private static final String DATE_AFTER_TWO = "2016-02-01T01:01:01.1234";
    private static final int SHARD = 2;
    private static final YtCluster DEFAULT_YT_CLUSTER = YtCluster.HAHN;

    private OfflineAdvqKeywordsImportJob job;

    @Mock
    private YtProvider ytProvider;

    @Mock
    private YtOperator ytOperator;

    @Mock
    private YtClusterConfig ytClusterConfig;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private AdGroupsShowsForecastService adGroupsShowsForecastService;

    @Mock
    private ShardHelper shardHelper;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        when(ytProvider.getOperator(DEFAULT_YT_CLUSTER)).thenReturn(ytOperator);
        when(ytProvider.getClusterConfig(DEFAULT_YT_CLUSTER)).thenReturn(ytClusterConfig);

        when(ytClusterConfig.getHome()).thenReturn("//tmp");

        job = new OfflineAdvqKeywordsImportJob(SHARD, shardHelper, ytProvider, DEFAULT_YT_CLUSTER, ppcPropertiesSupport,
                adGroupsShowsForecastService);
    }

    @Test
    void testForecastNeedsUpdateNull() {
        when(ppcPropertiesSupport.get(IMPORT_TIME_PROP_NAME + "_s" + SHARD)).thenReturn(null);

        assertTrue(job.forecastNeedsUpdate(DATE_BEFORE), "Считаем что нужно обновить данные");
    }

    @Test
    void testForecastNeedsUpdate() {
        when(ppcPropertiesSupport.get(IMPORT_TIME_PROP_NAME + "_s" + SHARD)).thenReturn(DATE_BEFORE);

        assertTrue(job.forecastNeedsUpdate(DATE_AFTER), "Считаем что нужно обновить данные");
    }

    @Test
    void testForecastNotNeedsUpdate() {
        when(ppcPropertiesSupport.get(IMPORT_TIME_PROP_NAME + "_s" + SHARD)).thenReturn(DATE_BEFORE);

        assertFalse(job.forecastNeedsUpdate(DATE_BEFORE), "Считаем что не нужно обновить данные");
    }

    @Test
    void testSetLastUploadedProp() {
        job.setLastUploadedProp(DATE_BEFORE);

        // Проставляем дату, полученную из YT
        verify(ppcPropertiesSupport).set(eq(IMPORT_TIME_PROP_NAME + "_s" + SHARD), eq(DATE_BEFORE));
    }

    @Test
    void testExecuteNoRead() {
        when(ppcPropertiesSupport.get(FORECAST_TIME_PROP_NAME)).thenReturn(DATE_BEFORE);
        when(ppcPropertiesSupport.get(UPLOAD_TIME_PROP_NAME)).thenReturn(DATE_AFTER);
        when(ppcPropertiesSupport.get(IMPORT_TIME_PROP_NAME + "_s" + SHARD)).thenReturn(DATE_AFTER);

        job.execute();

        // Не тправили запрос на чтение таблицы и не записали дату синхронизации
        verify(ytOperator, never()).readTableByKeyRange(any(), any(), any(), anyLong(), anyLong());
        verify(ppcPropertiesSupport, never()).set(anyString(), any());
    }

    @Test
    void testExecute() {
        when(ppcPropertiesSupport.get(FORECAST_TIME_PROP_NAME)).thenReturn(DATE_BEFORE);
        when(ppcPropertiesSupport.get(UPLOAD_TIME_PROP_NAME)).thenReturn(DATE_AFTER_TWO);
        when(ppcPropertiesSupport.get(IMPORT_TIME_PROP_NAME + "_s" + SHARD)).thenReturn(DATE_AFTER);

        job.execute();

        verify(ytOperator).readTableByKeyRange(any(), any(), any(), anyLong(), anyLong());
        verify(ppcPropertiesSupport).set(eq(IMPORT_TIME_PROP_NAME + "_s" + SHARD), eq(DATE_AFTER_TWO));
    }
}
