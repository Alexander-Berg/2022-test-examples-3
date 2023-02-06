package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.bs.export.model.WorkerSpec;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;

class BsExportQueueRepositoryGetCampaignIdsInQueueExceptMasterExportTest extends BsExportQueueRepositoryBase {

    private Long campaignId;

    @BeforeEach
    void createTestData() {
        campaignId = createCampaign();
    }

    @Test
    void notLocked_Empty() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId).withLockedBy(null);
        queueRepository.insertRecord(testShardContext, queueRecord);

        Set<Long> campaignIds = queueRepository.getCampaignIdsInQueueExceptMasterExport(TEST_SHARD, singleton(campaignId));
        assertThat(campaignIds).isEmpty();
    }

    @Test
    void lockedByStd1_ReturnCampaignId() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId)
                .withLockedBy((int) WorkerSpec.STD_1.getWorkerId());
        queueRepository.insertRecord(testShardContext, queueRecord);

        Set<Long> campaignIds = queueRepository.getCampaignIdsInQueueExceptMasterExport(TEST_SHARD, singleton(campaignId));
        assertThat(campaignIds).contains(campaignId);
    }

    @Test
    void lockedByMaster_Empty() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId)
                .withLockedBy((int) WorkerSpec.MASTER.getWorkerId());
        queueRepository.insertRecord(testShardContext, queueRecord);

        Set<Long> campaignIds = queueRepository.getCampaignIdsInQueueExceptMasterExport(TEST_SHARD, singleton(campaignId));
        assertThat(campaignIds).isEmpty();
    }

    @Test
    void twoLockedBy_ContainsTwoCampaignIds() {
        Long campaignId1 = campaignId;
        BsExportQueueInfo queueRecord1 = recordWithFullStat(campaignId1)
                .withLockedBy((int) WorkerSpec.STD_1.getWorkerId());
        queueRepository.insertRecord(testShardContext, queueRecord1);
        Long campaignId2 = createCampaign();
        BsExportQueueInfo queueRecord2 = recordWithFullStat(campaignId2)
                .withLockedBy((int) WorkerSpec.STD_2.getWorkerId());
        queueRepository.insertRecord(testShardContext, queueRecord2);

        Set<Long> campaignIds =
                queueRepository.getCampaignIdsInQueueExceptMasterExport(TEST_SHARD, asList(campaignId1, campaignId2));
        assertThat(campaignIds).contains(campaignId1, campaignId2);
    }

    @Test
    void differentLockedBy_ContainsCampaignId() {
        Long campaignId1 = campaignId;
        BsExportQueueInfo queueRecord1 = recordWithFullStat(campaignId1).withLockedBy(null);
        queueRepository.insertRecord(testShardContext, queueRecord1);
        Long campaignId2 = createCampaign();
        BsExportQueueInfo queueRecord2 = recordWithFullStat(campaignId2).withLockedBy(99);
        queueRepository.insertRecord(testShardContext, queueRecord2);
        Long campaignId3 = createCampaign();
        BsExportQueueInfo queueRecord3 = recordWithFullStat(campaignId3).withLockedBy(100);
        queueRepository.insertRecord(testShardContext, queueRecord3);

        Set<Long> campaignIds = queueRepository
                .getCampaignIdsInQueueExceptMasterExport(TEST_SHARD, asList(campaignId1, campaignId2, campaignId3));
        assertThat(campaignIds).contains(campaignId3);
    }
}

