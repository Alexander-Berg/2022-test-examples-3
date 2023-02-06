package ru.yandex.direct.core.entity.bs.export.queue.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;

class BsExportQueueRepositoryRemoveCampaignsFullExportFlagTest extends BsExportQueueRepositoryBase {

    private Long campaignId;

    @BeforeEach
    void createTestData() {
        campaignId = createCampaign();
    }

    @Test
    void nonExistenceInQueue_Success() {
        int updated = queueRepository.removeCampaignsFullExportFlag(TEST_SHARD, singleton(campaignId));
        assertEquals(0, updated, "removeCampaignsFullExportFlag ничего не обновил в базе");
    }

    @Test
    void withLockedBy_Success() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, queueRecord);

        int updated = queueRepository.removeCampaignsFullExportFlag(TEST_SHARD, singleton(campaignId));
        assertEquals(1, updated, "removeCampaignsFullExportFlag обновил 1 запись в базе");

        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isEqualToComparingFieldByField(queueRecord.withNeedFullExport(false));
    }

    @Test
    void withoutLockedBy_Success() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId).withLockedBy(null);
        queueRepository.insertRecord(testShardContext, queueRecord);

        int updated = queueRepository.removeCampaignsFullExportFlag(TEST_SHARD, singleton(queueRecord.getCampaignId()));
        assertEquals(1, updated, "removeCampaignsFullExportFlag обновил 1 запись в базе");

        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isEqualToComparingFieldByField(queueRecord.withNeedFullExport(false));
    }

    @Test
    void noFlag_Success() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId).withNeedFullExport(false);
        queueRepository.insertRecord(testShardContext, queueRecord);

        int updated = queueRepository.removeCampaignsFullExportFlag(TEST_SHARD, singleton(queueRecord.getCampaignId()));
        assertEquals(0, updated, "removeCampaignsFullExportFlag ничего не обновил в базе");

        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isEqualToComparingFieldByField(queueRecord);
    }
}

