package ru.yandex.direct.core.entity.bs.export.queue.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.yesterdayRecordWithoutStat;

class BsExportQueueRepositoryResetQueueTimeTest extends BsExportQueueRepositoryBase {

    private Long campaignId1;

    @BeforeEach
    void createTestData() {
        campaignId1 = createCampaign();
    }


    @Test
    void emptyCollection_queueTimeNotReset() {
        BsExportQueueInfo queueRecord1 = yesterdayRecordWithoutStat(campaignId1);
        queueRepository.insertRecord(testShardContext, queueRecord1);

        queueRepository.resetQueueTime(dslContextProvider.ppc(TEST_SHARD), emptySet());
        BsExportQueueInfo actual = queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId1);
        assertThat(actual.getQueueTime()).isEqualTo(queueRecord1.getQueueTime());
    }

    @Test
    void oneCampaign_oneCampaignIsReset() {
        BsExportQueueInfo queueRecord1 = yesterdayRecordWithoutStat(campaignId1);
        queueRepository.insertRecord(testShardContext, queueRecord1);
        Long campaignId2 = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        BsExportQueueInfo queueRecord2 = yesterdayRecordWithoutStat(campaignId2);
        queueRepository.insertRecord(testShardContext, queueRecord2);

        queueRepository.resetQueueTime(dslContextProvider.ppc(TEST_SHARD), singletonList(campaignId2));

        BsExportQueueInfo actualRecord1 = queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId1);
        assertThat(actualRecord1.getQueueTime()).isEqualTo(queueRecord1.getQueueTime());
        BsExportQueueInfo actualRecord2 = queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId2);
        assertThat(actualRecord2.getQueueTime()).isNotEqualTo(queueRecord2.getQueueTime());
    }
}

