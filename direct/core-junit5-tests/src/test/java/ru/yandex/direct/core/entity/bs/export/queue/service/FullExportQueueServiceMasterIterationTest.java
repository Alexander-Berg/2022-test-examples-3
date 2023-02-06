package ru.yandex.direct.core.entity.bs.export.queue.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.bs.export.BsExportParametersService;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportQueueRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class FullExportQueueServiceMasterIterationTest {
    private static final int TEST_SHARD = 1;

    @Mock
    private BsExportParametersService parametersService;

    @Mock
    private BsExportQueueRepository bsExportQueueRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private PpcProperty<Long> lastProcessedCampaignIdProperty;

    private FullExportQueueService.Master master;

    @BeforeEach
    void tuneMocks() {
        initMocks(this);

        when(parametersService.getFullExportLastProcessedCampaignIdProperty(TEST_SHARD))
                .thenReturn(lastProcessedCampaignIdProperty);

        when(campaignRepository.getCampaignIdsForFullExport(anyInt(), anyLong(), anyInt()))
                .thenAnswer(FullExportQueueServiceMasterIterationTest::getCampaignIdsForFullExportStub);

        when(bsExportQueueRepository.getCampaignsCountInFullExportQueue(anyInt()))
                .thenReturn(0);

        FullExportQueueService service =
                new FullExportQueueService(bsExportQueueRepository, parametersService, campaignRepository, null);
        master = service.getMaster(TEST_SHARD);
    }

    @Test
    void usingArgsFromPropertiesTest() {
        long lastProcessedCampaignId = RandomUtils.nextLong(1, Integer.MAX_VALUE);
        int maximumCampaignsInQueue = RandomUtils.nextInt(1, Integer.MAX_VALUE);
        int maximumChunkPerIteration = RandomUtils.nextInt(1, Integer.MAX_VALUE);

        when(parametersService.getFullExportChunkPerWorker())
                .thenReturn(0);
        when(parametersService.getFullExportMaximumCampaignsInQueue())
                .thenReturn(maximumCampaignsInQueue);
        when(parametersService.getFullExportMaximumChunkPerIteration())
                .thenReturn(maximumChunkPerIteration);
        when(lastProcessedCampaignIdProperty.getOrDefault(any()))
                .thenReturn(lastProcessedCampaignId);
        when(campaignRepository.getCampaignIdsForFullExport(anyInt(), anyLong(), anyInt()))
                .thenReturn(emptyList());
        master = spy(master);

        master.iteration();

        verify(master).iteration(maximumCampaignsInQueue, maximumChunkPerIteration, lastProcessedCampaignId);
    }

    @Test
    void skippedWorkIfMaximumCampaignsInQueueIsNotSetTest() {
        master.iteration(0, Integer.MAX_VALUE, Long.MAX_VALUE);

        verifyZeroInteractions(bsExportQueueRepository);
        verifyZeroInteractions(campaignRepository);
        verifyZeroInteractions(lastProcessedCampaignIdProperty);
    }

    @Test
    void skippedWorkIfMaximumChunkPerIterationIsNotSetTest() {
        master.iteration(Integer.MAX_VALUE, 0, Long.MAX_VALUE);

        verifyZeroInteractions(bsExportQueueRepository);
        verifyZeroInteractions(campaignRepository);
        verifyZeroInteractions(lastProcessedCampaignIdProperty);
    }

    @Test
    void skippedWorkIfReachedMaximumCampaignsInQueueTest() {
        int limit = RandomUtils.nextInt(200, Short.MAX_VALUE);

        when(bsExportQueueRepository.getCampaignsCountInFullExportQueue(anyInt()))
                .thenReturn(limit);

        master.iteration(limit, Integer.MAX_VALUE, Long.MAX_VALUE);

        verify(bsExportQueueRepository).getCampaignsCountInFullExportQueue(TEST_SHARD);
        verifyNoMoreInteractions(bsExportQueueRepository);
        verifyZeroInteractions(campaignRepository);
        verifyZeroInteractions(lastProcessedCampaignIdProperty);
    }

    @Test
    void skippedWorkIfExceededMaximumCampaignsInQueueLimitTest() {
        int limit = RandomUtils.nextInt(200, Short.MAX_VALUE);

        when(bsExportQueueRepository.getCampaignsCountInFullExportQueue(anyInt()))
                .thenReturn(limit + 1);

        master.iteration(limit, Integer.MAX_VALUE, Long.MAX_VALUE);

        verify(bsExportQueueRepository).getCampaignsCountInFullExportQueue(TEST_SHARD);
        verifyNoMoreInteractions(bsExportQueueRepository);
        verifyZeroInteractions(campaignRepository);
        verifyZeroInteractions(lastProcessedCampaignIdProperty);
    }

    @Test
    void skippedWorkIfLastProcessedCampaignIdIsNotSetAndRollingWorkIsNotAllowedTest() {
        when(parametersService.canFullExportRollingWork())
                .thenReturn(false);

        master.iteration(Integer.MAX_VALUE, Integer.MAX_VALUE, 0);

        verify(bsExportQueueRepository).getCampaignsCountInFullExportQueue(TEST_SHARD);
        verifyNoMoreInteractions(bsExportQueueRepository);
        verifyZeroInteractions(campaignRepository);
        verifyZeroInteractions(lastProcessedCampaignIdProperty);
    }

    @Test
    void skippedWorkIfLastProcessedCampaignIdIsNotSetAndThereIsNoCandidateValueTest() {
        when(parametersService.canFullExportRollingWork())
                .thenReturn(true);
        when(campaignRepository.getNewLastProcessedCampaignId(anyInt()))
                .thenReturn(0L);

        master.iteration(Integer.MAX_VALUE, Integer.MAX_VALUE, 0);

        verify(campaignRepository).getNewLastProcessedCampaignId(TEST_SHARD);
        verifyNoMoreInteractions(campaignRepository);
        verify(bsExportQueueRepository).getCampaignsCountInFullExportQueue(TEST_SHARD);
        verifyNoMoreInteractions(bsExportQueueRepository);
        verifyZeroInteractions(lastProcessedCampaignIdProperty);
    }

    @Test
    void getCampaignIdsForFullExportLimitedToRestOfMaximumQueueSizeTest() {
        int limit = RandomUtils.nextInt(1_000, 10_000);
        int queue = RandomUtils.nextInt(10, 900);

        when(bsExportQueueRepository.getCampaignsCountInFullExportQueue(anyInt()))
                .thenReturn(queue);

        master.iteration(limit, Integer.MAX_VALUE, Long.MAX_VALUE);

        int expected = limit - queue;
        verify(bsExportQueueRepository).getCampaignsCountInFullExportQueue(TEST_SHARD);
        verify(campaignRepository).getCampaignIdsForFullExport(eq(TEST_SHARD), anyLong(), eq(expected));
    }

    @Test
    void getCampaignIdsForFullExportLimitedToMaxChunkPerIterationTest() {
        int maxChunk = RandomUtils.nextInt(10, 4000);

        int limit = RandomUtils.nextInt(5_000, 10_000);
        int queue = RandomUtils.nextInt(10, 900);

        when(bsExportQueueRepository.getCampaignsCountInFullExportQueue(anyInt()))
                .thenReturn(queue);

        master.iteration(limit, maxChunk, Long.MAX_VALUE);

        verify(bsExportQueueRepository).getCampaignsCountInFullExportQueue(TEST_SHARD);
        verify(campaignRepository).getCampaignIdsForFullExport(eq(TEST_SHARD), anyLong(), eq(maxChunk));
    }

    @Test
    void getCampaignIdsForFullExportWhenLastProcessedCampaignIdWasDetectedTest() {
        int limit = RandomUtils.nextInt(50, 1000);
        long newLastProcessedCampaignId = RandomUtils.nextLong(Short.MAX_VALUE, Integer.MAX_VALUE);
        when(campaignRepository.getNewLastProcessedCampaignId(anyInt()))
                .thenReturn(newLastProcessedCampaignId);
        when(parametersService.canFullExportRollingWork())
                .thenReturn(true);

        master.iteration(limit, Integer.MAX_VALUE, -1);

        verify(campaignRepository).getCampaignIdsForFullExport(TEST_SHARD, newLastProcessedCampaignId, limit);
    }

    @Test
    void addCampaignsFullExportFlagTest() {
        int limit = RandomUtils.nextInt(50, 1000);
        long lastProcessedCampaignId = RandomUtils.nextLong(Short.MAX_VALUE, Integer.MAX_VALUE);
        List<Long> expected = getCampaignIdsForFullExportStub(lastProcessedCampaignId, limit);

        master.iteration(limit, Integer.MAX_VALUE, lastProcessedCampaignId);

        verify(bsExportQueueRepository).getCampaignsCountInFullExportQueue(TEST_SHARD);
        verify(campaignRepository).getCampaignIdsForFullExport(TEST_SHARD, lastProcessedCampaignId, limit);
        verify(bsExportQueueRepository).addCampaignsFullExportFlag(eq(TEST_SHARD), eq(expected));
    }

    @Test
    void updateNewLastProcessedCampaignIdFullChunkTest() {
        int limit = RandomUtils.nextInt(50, 1000);
        long lastProcessedCampaignId = RandomUtils.nextLong(Short.MAX_VALUE, Integer.MAX_VALUE);

        master.iteration(limit, Integer.MAX_VALUE, lastProcessedCampaignId);

        // в допущении, что для добавления нашелся сплошной кусок cid'ов
        long expected = lastProcessedCampaignId - limit;

        verify(lastProcessedCampaignIdProperty).set(expected);
    }

    @Test
    void updateNewLastProcessedCampaignIdChunkLessThanRequestedTest() {
        int limit = RandomUtils.nextInt(50, 1000);
        int chunk = RandomUtils.nextInt(0, limit);

        List<Long> ids = getCampaignIdsForFullExportStub(Integer.MAX_VALUE, chunk);
        when(campaignRepository.getCampaignIdsForFullExport(anyInt(), anyLong(), anyInt()))
                .thenReturn(ids);

        master.iteration(limit, Integer.MAX_VALUE, Long.MAX_VALUE);

        verify(lastProcessedCampaignIdProperty).set(0L);
    }

    @Test
    void updateNewLastProcessedCampaignIdShardEmptyTest() {
        when(campaignRepository.getCampaignIdsForFullExport(anyInt(), anyLong(), anyInt()))
                .thenReturn(emptyList());

        master.iteration(Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE);

        verify(lastProcessedCampaignIdProperty).set(0L);
    }

    private static List<Long> getCampaignIdsForFullExportStub(InvocationOnMock invocation) {
        int limit = invocation.getArgument(2);
        long start = invocation.getArgument(1);
        return getCampaignIdsForFullExportStub(start, limit);
    }

    private static List<Long> getCampaignIdsForFullExportStub(long start, long limit) {
        AtomicLong value = new AtomicLong(start);
        return LongStream.generate(value::decrementAndGet)
                .boxed()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
