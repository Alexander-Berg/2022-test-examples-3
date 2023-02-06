package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;

class BsExportQueueRepositoryGetBsExportQueueInfoTest extends BsExportQueueRepositoryBase {
    private Long campaignId;

    @BeforeEach
    void createTestData() {
        campaignId = createCampaign();
    }

    @Test
    void mass_twoCampaignsInQueueTest() {
        BsExportQueueInfo record1 = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, record1);

        Long campaignId2 = createCampaign();
        BsExportQueueInfo record2 = recordWithFullStat(campaignId2).withNeedFullExport(true);
        queueRepository.insertRecord(testShardContext, record2);

        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, List.of(campaignId, campaignId2)))
                .containsEntry(campaignId, record1)
                .containsEntry(campaignId2, record2)
                .hasSize(2);
    }

    @Test
    void mass_oneCampaignInQueueTest() {
        BsExportQueueInfo record = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, record);

        Long campaignId2 = createCampaign();

        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, List.of(campaignId, campaignId2)))
                .containsEntry(campaignId, record)
                .doesNotContainKey(campaignId2)
                .hasSize(1);
    }

    @Test
    void mass_zeroCampaignsInQueueTest() {
        Long campaignId2 = createCampaign();

        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, List.of(campaignId, campaignId2)))
                .isEmpty();
    }

    @Test
    void oneCampaignInQueueTest() {
        BsExportQueueInfo record = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, record);

        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isEqualTo(record);
    }

    @Test
    void noCampaignInQueueTest() {
        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isNull();
    }
}

