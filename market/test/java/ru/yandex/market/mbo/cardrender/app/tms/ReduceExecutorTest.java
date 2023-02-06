package ru.yandex.market.mbo.cardrender.app.tms;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.mbo.cardrender.app.service.ModelReduceService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author apluhin
 * @created 6/8/21
 */
public class ReduceExecutorTest {

    ReduceExecutor reduceExecutor;
    ModelReduceService hahnSkuReducer;
    ModelReduceService hahnModelReducer;
    ModelReduceService arnoldSkuReducer;
    ModelReduceService arnoldModelReducer;
    StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() throws Exception {
        hahnSkuReducer = Mockito.mock(ModelReduceService.class);
        hahnModelReducer = Mockito.mock(ModelReduceService.class);
        arnoldSkuReducer = Mockito.mock(ModelReduceService.class);
        arnoldModelReducer = Mockito.mock(ModelReduceService.class);
        storageKeyValueService = Mockito.mock(StorageKeyValueService.class);
        reduceExecutor = new ReduceExecutor(
                hahnSkuReducer, hahnModelReducer,
                arnoldSkuReducer, arnoldModelReducer,
                storageKeyValueService
        );
        Mockito.when(storageKeyValueService.getBool(Mockito.eq("is_enable_reduce"), Mockito.any()))
                .thenReturn(true);
    }

    @Test
    public void testFindCorrectExportKey() throws Exception {
        Mockito.when(hahnSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(hahnModelReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldModelReducer.isServiceReady()).thenReturn(true);
        String exportKey = "20210608_1057";
        Mockito.when(hahnSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(hahnModelReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldModelReducer.findLastKey()).thenReturn(exportKey);

        reduceExecutor.doRealJob(null);

        expectedDisableCount(1);
        expectedEnableCount(2);
        Mockito.verify(storageKeyValueService, Mockito.times(1)).putValue(
                Mockito.eq("last_export_table"), Mockito.eq(exportKey)
        );
        Mockito.verify(hahnSkuReducer, Mockito.times(1)).startReduce(
                Mockito.eq(exportKey)
        );
        Mockito.verify(hahnModelReducer, Mockito.times(1)).startReduce(
                Mockito.eq(exportKey)
        );
        Mockito.verify(arnoldSkuReducer, Mockito.times(1)).startReduce(
                Mockito.eq(exportKey)
        );
        Mockito.verify(arnoldModelReducer, Mockito.times(1)).startReduce(
                Mockito.eq(exportKey)
        );
    }

    @Test
    public void testIgnoreClusterDuringReduce() throws Exception {
        Mockito.when(hahnSkuReducer.isServiceReady()).thenReturn(false);
        Mockito.when(hahnModelReducer.isServiceReady()).thenReturn(false);
        Mockito.when(arnoldSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldModelReducer.isServiceReady()).thenReturn(true);
        String exportKey = "20210608_1057";
        Mockito.when(hahnSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(hahnModelReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldModelReducer.findLastKey()).thenReturn(exportKey);

        reduceExecutor.doRealJob(null);

        expectedDisableCount(1);
        expectedEnableCount(2);
        Mockito.verify(storageKeyValueService, Mockito.times(1)).putValue(
                Mockito.eq("last_export_table"), Mockito.eq(exportKey)
        );
        Mockito.verify(hahnSkuReducer, Mockito.times(0)).startReduce(
                Mockito.eq(exportKey)
        );
        Mockito.verify(hahnModelReducer, Mockito.times(0)).startReduce(
                Mockito.eq(exportKey)
        );
        Mockito.verify(arnoldSkuReducer, Mockito.times(1)).startReduce(
                Mockito.eq(exportKey)
        );
        Mockito.verify(arnoldModelReducer, Mockito.times(1)).startReduce(
                Mockito.eq(exportKey)
        );
    }

    @Test
    public void testIgnoreSameExportKey() throws Exception {
        Mockito.when(hahnSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(hahnModelReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldModelReducer.isServiceReady()).thenReturn(true);
        String exportKey = "20210608_1057";
        Mockito.when(hahnSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(hahnModelReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldModelReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(storageKeyValueService.getString(Mockito.eq("last_export_table"),
                Mockito.eq(null))).thenReturn(exportKey);

        reduceExecutor.doRealJob(null);

        expectedDisableCount(0);
        expectedEnableCount(1);
        Mockito.verify(storageKeyValueService, Mockito.times(0)).putValue(
                Mockito.eq("last_export_table"), Mockito.eq(exportKey)
        );
        Mockito.verify(hahnSkuReducer, Mockito.times(0)).startReduce(
                Mockito.eq(exportKey)
        );
        Mockito.verify(hahnModelReducer, Mockito.times(0)).startReduce(
                Mockito.eq(exportKey)
        );
        Mockito.verify(arnoldSkuReducer, Mockito.times(0)).startReduce(
                Mockito.eq(exportKey)
        );
        Mockito.verify(arnoldModelReducer, Mockito.times(0)).startReduce(
                Mockito.eq(exportKey)
        );
    }

    @Test(expected = RuntimeException.class)
    public void testFailedOnReduceWithNonCompleteExportInClusters() throws Exception {
        Mockito.when(hahnSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(hahnModelReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldModelReducer.isServiceReady()).thenReturn(true);
        String exportKey1 = "20210608_1057";
        String exportKey2 = "20210607_1057";
        Mockito.when(hahnSkuReducer.findLastKey()).thenReturn(exportKey1);
        Mockito.when(hahnModelReducer.findLastKey()).thenReturn(exportKey1);
        Mockito.when(arnoldSkuReducer.findLastKey()).thenReturn(exportKey2);
        Mockito.when(arnoldModelReducer.findLastKey()).thenReturn(exportKey2);
        Mockito.when(storageKeyValueService.getString(Mockito.eq("last_export_table"), Mockito.eq(null)))
                .thenReturn("20210607_1057");

        reduceExecutor.doRealJob(null);

        expectedDisableCount(0);
        expectedEnableCount(0);
        Mockito.verify(storageKeyValueService, Mockito.times(0)).putValue(
                Mockito.eq("last_export_table"), Mockito.any()
        );
        Mockito.verify(hahnSkuReducer, Mockito.times(0)).startReduce(
                Mockito.anyString()
        );
        Mockito.verify(hahnModelReducer, Mockito.times(0)).startReduce(
                Mockito.anyString()
        );
        Mockito.verify(arnoldSkuReducer, Mockito.times(0)).startReduce(
                Mockito.anyString()
        );
        Mockito.verify(arnoldModelReducer, Mockito.times(0)).startReduce(
                Mockito.anyString()
        );
    }

    @Test(timeout = 3000)
    public void testCheckParallelUpdateCluster() throws Exception {
        Mockito.when(hahnSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(hahnModelReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldModelReducer.isServiceReady()).thenReturn(true);
        String exportKey = "20210608_1057";
        Mockito.when(hahnSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(hahnModelReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldModelReducer.findLastKey()).thenReturn(exportKey);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            Thread.sleep(2_000);
            return null;
        }).when(hahnSkuReducer).startReduce(Mockito.any());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Thread.sleep(2_000);
            return null;
        }).when(hahnModelReducer).startReduce(Mockito.any());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Thread.sleep(2_000);
            return null;
        }).when(arnoldSkuReducer).startReduce(Mockito.any());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Thread.sleep(2_000);
            return null;
        }).when(arnoldModelReducer).startReduce(Mockito.any());

        reduceExecutor.doRealJob(null);

        Mockito.verify(storageKeyValueService, Mockito.times(1)).putValue(
                Mockito.eq("last_export_table"), Mockito.eq(exportKey)
        );
    }

    @Test
    public void testFailedParallelUpdateCluster() throws Exception {
        Mockito.when(hahnSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(hahnModelReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldSkuReducer.isServiceReady()).thenReturn(true);
        Mockito.when(arnoldModelReducer.isServiceReady()).thenReturn(true);
        String exportKey = "20210608_1057";
        Mockito.when(hahnSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(hahnModelReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldSkuReducer.findLastKey()).thenReturn(exportKey);
        Mockito.when(arnoldModelReducer.findLastKey()).thenReturn(exportKey);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            throw new RuntimeException("err");
        }).when(hahnSkuReducer).startReduce(Mockito.any());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            throw new RuntimeException("err");
        }).when(hahnModelReducer).startReduce(Mockito.any());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Thread.sleep(2_000);
            return null;
        }).when(arnoldSkuReducer).startReduce(Mockito.any());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Thread.sleep(2_000);
            return null;
        }).when(arnoldModelReducer).startReduce(Mockito.any());

        try {
            reduceExecutor.doRealJob(null);
        } catch (Exception e) {
            Mockito.verify(storageKeyValueService, Mockito.times(0)).putValue(
                    Mockito.eq("last_export_table"), Mockito.any()
            );
            return;
        }
        //broken test without catch block execution
        Assert.assertTrue(false);
    }

    private void expectedDisableCount(int count) {
        Mockito.verify(storageKeyValueService, Mockito.times(count))
                .putValue(Mockito.eq("render_model_to_yt_enabled"), Mockito.eq(false));
    }

    private void expectedEnableCount(int count) {
        Mockito.verify(storageKeyValueService, Mockito.times(count))
                .putValue(Mockito.eq("render_model_to_yt_enabled"), Mockito.eq(true));
    }
}

