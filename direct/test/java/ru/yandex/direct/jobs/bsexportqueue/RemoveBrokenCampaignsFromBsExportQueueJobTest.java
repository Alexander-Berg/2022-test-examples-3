package ru.yandex.direct.jobs.bsexportqueue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportQueueRepository;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportSpecialsRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты на джобу {@link ru.yandex.direct.jobs.bsexportqueue.RemoveBrokenCampaignsFromBsExportQueueJob}
 */
class RemoveBrokenCampaignsFromBsExportQueueJobTest {

    private static final int TEST_SHARD = 2;
    private static final Long CAMPAIGN_ID = 1L;

    private RemoveBrokenCampaignsFromBsExportQueueJob removeBrokenCampaignsFromBsExportQueueJob;

    private BsExportQueueRepository queueRepository;
    private BsExportSpecialsRepository specialsRepository;

    @BeforeEach
    void initMocks() {
        queueRepository = mock(BsExportQueueRepository.class);
        specialsRepository = mock(BsExportSpecialsRepository.class);

        removeBrokenCampaignsFromBsExportQueueJob = new RemoveBrokenCampaignsFromBsExportQueueJob(TEST_SHARD,
                queueRepository);
    }

    /**
     * Тестируем, что проблемная незалоченная кампания удаляется
     */
    @Test
    void execute_notLocked_isDeleted() {
        when(queueRepository.getExpiredAndBrokenCampaignIds(eq(TEST_SHARD), any(LocalDateTime.class)))
                .thenReturn(Map.of(CAMPAIGN_ID, false));

        removeBrokenCampaignsFromBsExportQueueJob.execute();

        verify(queueRepository).delete(anyInt(), eq(List.of(CAMPAIGN_ID)));
        verify(specialsRepository, never()).remove(anyInt(), anyCollection());
    }

    /**
     * Тестируем, что залоченная кампания не удаляется
     */
    @Test
    void execute_locked_isNotDeleted() {
        when(queueRepository.getExpiredAndBrokenCampaignIds(eq(TEST_SHARD), any(LocalDateTime.class)))
                .thenReturn(Map.of(CAMPAIGN_ID, true));

        removeBrokenCampaignsFromBsExportQueueJob.execute();

        verify(queueRepository, never()).delete(anyInt(), anyCollection());
    }

    /**
     * Тестируем, что не проблемная кампания не удаляется
     */
    @Test
    void execute_notSelected_isNotDeleted() {
        when(queueRepository.getExpiredAndBrokenCampaignIds(eq(TEST_SHARD), any(LocalDateTime.class)))
                .thenReturn(Map.of());

        removeBrokenCampaignsFromBsExportQueueJob.execute();

        verify(queueRepository, never()).delete(anyInt(), anyCollection());
    }
}
