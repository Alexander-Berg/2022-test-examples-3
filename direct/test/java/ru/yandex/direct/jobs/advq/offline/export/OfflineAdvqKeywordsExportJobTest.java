package ru.yandex.direct.jobs.advq.offline.export;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfflineAdvqKeywordsExportJobTest {
    private static final YtCluster DEFAULT_YT_CLUSTER = YtCluster.HAHN;

    private YtOperator ytOperator;
    private PpcPropertiesSupport ppcPropertiesSupport;
    private OfflineAdvqKeywordsExportJob job;

    @BeforeEach
    void before() {
        ytOperator = mock(YtOperator.class);

        YtProvider ytProvider = mock(YtProvider.class);
        when(ytProvider.getOperator(DEFAULT_YT_CLUSTER)).thenReturn(ytOperator);

        YtClusterConfig ytClusterConfig = mock(YtClusterConfig.class);
        when(ytClusterConfig.getHome()).thenReturn("//home/direct");

        when(ytProvider.getClusterConfig(DEFAULT_YT_CLUSTER)).thenReturn(ytClusterConfig);

        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        job = new OfflineAdvqKeywordsExportJob(ytProvider, DEFAULT_YT_CLUSTER, ppcPropertiesSupport,
                mock(StopWordService.class));
    }

    @Test
    void testExportTableNeedsUpdateTrue() {
        Map<YtTable, OfflineAdvqKeywordsExportJob.TableUploadTimes> testMap = new HashMap<>();
        testMap.put(new YtTable("//path/1"),
                new OfflineAdvqKeywordsExportJob.TableUploadTimes("2017-01-01T03:01:01", "2017-01-01T01:01:01"));
        testMap.put(new YtTable("//path/2"),
                new OfflineAdvqKeywordsExportJob.TableUploadTimes("2017-01-02T01:01:01", "2017-01-02T01:01:01"));
        assertTrue(job.exportTableNeedsUpdate(testMap), "Если хотя бы одно время не совпадает, возвращает true");
    }

    @Test
    void testExportTableNeedsUpdateTrueOnNull() {
        Map<YtTable, OfflineAdvqKeywordsExportJob.TableUploadTimes> testMap = new HashMap<>();
        testMap.put(new YtTable("//path/1"),
                new OfflineAdvqKeywordsExportJob.TableUploadTimes("2017-01-01T03:01:01", null));
        testMap.put(new YtTable("//path/2"),
                new OfflineAdvqKeywordsExportJob.TableUploadTimes("2017-01-02T01:01:01", "2017-01-02T01:01:01"));
        assertTrue(job.exportTableNeedsUpdate(testMap), "Если хотя бы одно время null, возвращает true");
    }

    @Test
    void testExportTableNeedsUpdateFalse() {
        Map<YtTable, OfflineAdvqKeywordsExportJob.TableUploadTimes> testMap = new HashMap<>();
        testMap.put(new YtTable("//path/1"),
                new OfflineAdvqKeywordsExportJob.TableUploadTimes("2017-01-01T01:01:01", "2017-01-01T01:01:01"));
        testMap.put(new YtTable("//path/2"),
                new OfflineAdvqKeywordsExportJob.TableUploadTimes("2017-01-02T01:01:01", "2017-01-02T01:01:01"));
        assertFalse(job.exportTableNeedsUpdate(testMap), "Если все совпадает, возвращает false");
    }

    @Test
    void testSetNewUploadTimesProperties() {
        Map<YtTable, OfflineAdvqKeywordsExportJob.TableUploadTimes> testMap = new HashMap<>();
        testMap.put(new YtTable("//path/1"),
                new OfflineAdvqKeywordsExportJob.TableUploadTimes("2017-01-04T01:01:01", "2017-01-03T01:01:01"));
        testMap.put(new YtTable("//path/2"),
                new OfflineAdvqKeywordsExportJob.TableUploadTimes("2017-01-02T01:01:01", "2017-01-02T01:01:01"));
        job.setNewUploadTimesProperties(testMap);

        verify(ppcPropertiesSupport)
                .set(eq("OfflineAdvqKeywordsExportJob_1_last_upload_time"), eq("2017-01-04T01:01:01"));
        verify(ppcPropertiesSupport)
                .set(eq("OfflineAdvqKeywordsExportJob_2_last_upload_time"), eq("2017-01-02T01:01:01"));
    }

    @Test
    void testGetUploadTimes() {
        when(ppcPropertiesSupport.get(eq("OfflineAdvqKeywordsExportJob_1_last_upload_time")))
                .thenReturn("2017-01-02T01:01:01");
        when(ytOperator.readTableUploadTime(any())).thenReturn("2017-01-03T01:01:01");

        YtTable table = new YtTable("//path/1");
        Map<YtTable, OfflineAdvqKeywordsExportJob.TableUploadTimes> testMap =
                job.getUploadTimes(ytOperator, Collections.singletonList(table));
        assertThat("Время полученное из базы корректно", testMap.get(table).uploadTimeOnLastUpdate,
                equalTo("2017-01-02T01:01:01"));
        assertThat("Время полученное из YT корректно", testMap.get(table).currentUploadTime,
                equalTo("2017-01-03T01:01:01"));
    }

    @Test
    void testJobExecute() {
        when(ppcPropertiesSupport.get(eq("OfflineAdvqKeywordsExportJob_campaigns_last_upload_time")))
                .thenReturn("2017-01-02T01:01:01");
        when(ppcPropertiesSupport.get(eq("OfflineAdvqKeywordsExportJob_phrases_last_upload_time"))).thenReturn(null);
        when(ppcPropertiesSupport.get(eq("OfflineAdvqKeywordsExportJob_bids_last_upload_time")))
                .thenReturn("2017-01-02T01:01:01");

        when(ytOperator.readTableStringAttribute(any(), eq("upload_time"))).thenReturn("2017-01-03T01:01:01");
        when(ytOperator.readTableStringAttribute(any(), eq("upload_time"))).thenReturn("2017-01-03T01:01:01");

        Assertions.assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }
}
