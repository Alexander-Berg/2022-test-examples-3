package ru.yandex.direct.jobs.bannersystem.export.job;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.bannersystem.BannerSystemClient;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.jobs.bannersystem.export.container.MobileContentExportIndicators;
import ru.yandex.direct.jobs.bannersystem.export.service.BsExportMobileContentService;
import ru.yandex.direct.jobs.bannersystem.export.service.BsMobileContentExporter;
import ru.yandex.direct.juggler.JugglerStatus;
import ru.yandex.direct.libs.curator.CuratorFrameworkProvider;
import ru.yandex.direct.solomon.SolomonPushClient;
import ru.yandex.monlib.metrics.registry.MetricRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.jobs.bannersystem.export.job.BsExportMobileContentJob.LOCK_TIMEOUT;
import static ru.yandex.direct.jobs.bannersystem.export.job.BsExportMobileContentJob.PROD_LOCK_NAME;

class BsExportMobileContentJobTest {
    private static final int TEST_SHARD = 1;
    private static final List<Long> TEST_IDS_LIST = Collections.singletonList(1L);

    @Mock
    private BsExportMobileContentService bsExportMobileContentService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CuratorFrameworkProvider curatorFrameworkProvider;
    @Mock
    private BannerSystemClient bsClient;
    @Mock
    private MobileContentService mobileContentService;
    @Mock
    private SolomonPushClient solomonPushClient;
    @Captor
    private ArgumentCaptor<MetricRegistry> captor;

    private BsExportMobileContentJob job;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        job = spy(new BsExportMobileContentJob(TEST_SHARD, bsExportMobileContentService,
                solomonPushClient, bsClient, mobileContentService, curatorFrameworkProvider));
    }

    @Test
    void testRunIterationInLock() {
        doReturn(TEST_IDS_LIST)
                .when(bsExportMobileContentService).getMobileContentIdsForBsExport(eq(TEST_SHARD));

        job.runIterationsInLock(TEST_SHARD, Integer.MAX_VALUE, null, false);

        verify(curatorFrameworkProvider).getLock(eq(PROD_LOCK_NAME + "_shard_" + TEST_SHARD), eq(LOCK_TIMEOUT), any());
        verify(job).getExporter(eq(TEST_SHARD), eq(Integer.MAX_VALUE), eq(TEST_IDS_LIST));
    }

    @Test
    void testRunIterationInLockNoActionWhenLockIsLost() {
        doReturn(TEST_IDS_LIST)
                .when(bsExportMobileContentService).getMobileContentIdsForBsExport(eq(TEST_SHARD));

        job.lockIsLost();
        job.runIterationsInLock(TEST_SHARD, Integer.MAX_VALUE, null, false);

        verify(curatorFrameworkProvider).getLock(eq(PROD_LOCK_NAME + "_shard_" + TEST_SHARD), eq(LOCK_TIMEOUT), any());
        verify(job, never()).getExporter(eq(TEST_SHARD), eq(Integer.MAX_VALUE), eq(TEST_IDS_LIST));
    }

    @Test
    void testRunIteration() {
        BsMobileContentExporter exporter = mock(BsMobileContentExporter.class);
        doReturn(false)
                .when(exporter).isLimitExceeded();
        //noinspection ResultOfMethodCallIgnored
        doReturn(new MobileContentExportIndicators())
                .when(exporter).getIndicators();
        doReturn(exporter)
                .when(job).getExporter(eq(TEST_SHARD), eq(Integer.MAX_VALUE), eq(TEST_IDS_LIST));

        job.runIterations(TEST_SHARD, Integer.MAX_VALUE, TEST_IDS_LIST, false);

        verify(exporter).sendOneChunkOfMobileContent();
    }

    @Test
    void testRunIterationWithLimitExceeded() {
        BsMobileContentExporter exporter = mock(BsMobileContentExporter.class);
        doReturn(true, false)
                .when(exporter).isLimitExceeded();
        //noinspection ResultOfMethodCallIgnored
        doReturn(new MobileContentExportIndicators())
                .when(exporter).getIndicators();
        doReturn(exporter)
                .when(job).getExporter(eq(TEST_SHARD), eq(Integer.MAX_VALUE), eq(TEST_IDS_LIST));

        job.runIterations(TEST_SHARD, Integer.MAX_VALUE, TEST_IDS_LIST, false);

        verify(exporter, times(2)).sendOneChunkOfMobileContent();
    }

    @Test
    void testRunIterationWithLimitExceededButLockLostRunsOnlyOnce() {
        BsMobileContentExporter exporter = mock(BsMobileContentExporter.class);
        doReturn(true, false)
                .when(exporter).isLimitExceeded();
        //noinspection ResultOfMethodCallIgnored
        doReturn(new MobileContentExportIndicators())
                .when(exporter).getIndicators();

        doAnswer(c -> {
            job.lockIsLost();
            return exporter;
        })
                .when(job).getExporter(eq(TEST_SHARD), eq(Integer.MAX_VALUE), eq(TEST_IDS_LIST));

        job.runIterations(TEST_SHARD, Integer.MAX_VALUE, TEST_IDS_LIST, false);

        verify(exporter).sendOneChunkOfMobileContent();
    }

    @Test
    void testRunIterationWithLimitExceededAndOneIteration() {
        BsMobileContentExporter exporter = mock(BsMobileContentExporter.class);
        doReturn(true)
                .when(exporter).isLimitExceeded();
        //noinspection ResultOfMethodCallIgnored
        doReturn(new MobileContentExportIndicators())
                .when(exporter).getIndicators();
        doReturn(exporter)
                .when(job).getExporter(eq(TEST_SHARD), eq(Integer.MAX_VALUE), eq(TEST_IDS_LIST));

        job.runIterations(TEST_SHARD, Integer.MAX_VALUE, TEST_IDS_LIST, true);

        verify(exporter).sendOneChunkOfMobileContent();
    }

    @Test
    void testRunIterationSendsIndicators() {
        MobileContentExportIndicators indicators = new MobileContentExportIndicators();
        indicators.addItemsSent(110);
        indicators.addItemsReceived(105);
        indicators.addItemsError(100);
        indicators.addItemsSynced(5);

        BsMobileContentExporter exporter = mock(BsMobileContentExporter.class);
        doReturn(false)
                .when(exporter).isLimitExceeded();
        //noinspection ResultOfMethodCallIgnored
        doReturn(indicators)
                .when(exporter).getIndicators();
        doReturn(exporter)
                .when(job).getExporter(eq(TEST_SHARD), eq(Integer.MAX_VALUE), eq(TEST_IDS_LIST));

        job.runIterations(TEST_SHARD, Integer.MAX_VALUE, TEST_IDS_LIST, false);

        verify(exporter).sendOneChunkOfMobileContent();

        verify(solomonPushClient).sendMetrics(captor.capture());

        assertThat(captor.getValue().estimateCount())
                .as("Отправили метрики")
                .isEqualTo(4);
    }

    @Test
    void testJobRaisesWhenFailed() {
        doThrow(RuntimeException.class)
                .when(job).getExporter(anyInt(), anyInt(), any());

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(RuntimeException.class);

        assertThat(job.getJugglerStatus())
                .as("Ошибка итерации приводит к падению джоба, и не меняет статус выполнения")
                .isEqualTo(JugglerStatus.OK);
    }
}
