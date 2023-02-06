package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.LOCKED_BY_FIELD;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;

class BsExportQueueRepositoryUnlockCampaignsTest extends BsExportQueueRepositoryBase {

    private Long campaignId;

    @BeforeEach
    void createTestData() {
        campaignId = createCampaign();
    }

    @Test
    void unlockedCampaign_nothingChanged() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId).withLockedBy(null);
        queueRepository.insertRecord(testShardContext, queueRecord);

        int workerId = RandomUtils.nextInt(1, 254);
        queueRepository.unlockCampaigns(TEST_SHARD, singleton(campaignId), workerId);

        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isEqualToComparingFieldByField(queueRecord);
    }

    @Test
    void lockedByOther_stillLocked() {
        int lockedBy = RandomUtils.nextInt(50, 254);
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId).withLockedBy(lockedBy);
        queueRepository.insertRecord(testShardContext, queueRecord);

        int workerId = RandomUtils.nextInt(1, 49);
        queueRepository.unlockCampaigns(TEST_SHARD, singleton(campaignId), workerId);

        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isEqualToComparingOnlyGivenFields(queueRecord, LOCKED_BY_FIELD);
    }

    @Test
    void lockedCampaign_unlocked() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, queueRecord);

        queueRepository.unlockCampaigns(TEST_SHARD, singleton(campaignId), queueRecord.getLockedBy());

        BsExportQueueInfo expected = new BsExportQueueInfo();
        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isEqualToComparingOnlyGivenFields(expected, LOCKED_BY_FIELD);
    }

    @Test
    void twoLockedCampaignOneSpecified_oneUnlocked() {
        int workerId = RandomUtils.nextInt(1, 254);
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId).withLockedBy(workerId);
        queueRepository.insertRecord(testShardContext, queueRecord);

        Long campaignId2 = createCampaign();
        BsExportQueueInfo record2 = recordWithFullStat(campaignId2).withLockedBy(workerId);
        queueRepository.insertRecord(testShardContext, record2);

        queueRepository.unlockCampaigns(TEST_SHARD, singleton(campaignId), queueRecord.getLockedBy());

        BsExportQueueInfo expected = new BsExportQueueInfo();
        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isEqualToComparingOnlyGivenFields(expected, LOCKED_BY_FIELD);
        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId2))
                .isEqualToComparingOnlyGivenFields(record2, LOCKED_BY_FIELD);
    }

    @Test
    void twoLockedCampaignTwoSpecified_bothUnlocked() {
        int workerId = RandomUtils.nextInt(1, 254);
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId).withLockedBy(workerId);
        queueRepository.insertRecord(testShardContext, queueRecord);

        Long campaignId2 = createCampaign();
        BsExportQueueInfo record2 = recordWithFullStat(campaignId2).withLockedBy(workerId);
        queueRepository.insertRecord(testShardContext, record2);

        queueRepository.unlockCampaigns(TEST_SHARD, List.of(campaignId, campaignId2), workerId);

        BsExportQueueInfo expected = new BsExportQueueInfo();
        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId))
                .isEqualToComparingOnlyGivenFields(expected, LOCKED_BY_FIELD);
        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId2))
                .isEqualToComparingOnlyGivenFields(expected, LOCKED_BY_FIELD);
    }
}

