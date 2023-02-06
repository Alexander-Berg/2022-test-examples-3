package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportSpecials;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.yesterdayRecordWithoutStat;

class BsExportQueueRepositoryGetExpiredAndBrokenCampaignIdsTest extends BsExportQueueRepositoryBase {

    private static final long NON_EXISTENT_CAMPAIGN_ID = 1_406_890L;
    private Long campaignId;

    @BeforeEach
    void createTestData() {
        campaignId = createCampaign();
    }

    /**
     * Тестируем, что неустаревшая запись не возвращается
     */
    @Test
    void recordIsNotExpired_recordIsNotReturned() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, queueRecord);

        var actual = queueRepository.getExpiredAndBrokenCampaignIds(TEST_SHARD,
                LocalDateTime.now().minusHours(1));

        assertThat(actual).doesNotContainKey(campaignId);
    }

    /**
     * Тестируем, что неустаревшая запись в NOSEND не возвращается
     */
    @Test
    void recordIsNotExpired_NoSend_recordIsNotReturned() {
        var queueRecord = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, queueRecord);
        specialsRepository.add(testShardContext,
                List.of(new BsExportSpecials().withCampaignId(campaignId).withType(QueueType.NOSEND)));

        var actual = queueRepository.getExpiredAndBrokenCampaignIds(TEST_SHARD,
                LocalDateTime.now().minusHours(1));

        assertThat(actual).doesNotContainKey(campaignId);
    }

    /**
     * Тестируем, что устаревшая запись не возвращается, если кампания не в NOSEND
     */
    @Test
    void recordIsExpired_recordIsReturned() {
        var queueRecord = yesterdayRecordWithoutStat(campaignId);
        queueRepository.insertRecord(testShardContext, queueRecord);

        var actual = queueRepository.getExpiredAndBrokenCampaignIds(TEST_SHARD, LocalDateTime.now());

        assertThat(actual).doesNotContainKey(campaignId);
    }

    /**
     * Тестируем, что устаревшая запись возвращается, если кампания в NOSEND
     */
    @Test
    void recordIsExpired_NoSend_recordIsReturned() {
        var queueRecord = yesterdayRecordWithoutStat(campaignId);
        queueRepository.insertRecord(testShardContext, queueRecord);
        specialsRepository.add(testShardContext,
                List.of(new BsExportSpecials().withCampaignId(campaignId).withType(QueueType.NOSEND)));

        var actual = queueRepository.getExpiredAndBrokenCampaignIds(TEST_SHARD, LocalDateTime.now());

        assertThat(actual).containsKey(campaignId);
    }

    /**
     * Тестируем, что устаревшая запись возвращается, если кампании нет в Директе
     */
    @Test
    void recordIsExpired_NoCampaign_recordIsReturned() {
        var queueRecord = yesterdayRecordWithoutStat(NON_EXISTENT_CAMPAIGN_ID);
        queueRepository.insertRecord(testShardContext, queueRecord);

        var actual = queueRepository.getExpiredAndBrokenCampaignIds(TEST_SHARD, LocalDateTime.now());

        assertThat(actual).containsKey(NON_EXISTENT_CAMPAIGN_ID);
    }

    /**
     * Тестируем, что конкретно возвращается, если кампании в NOSEND
     */
    @Test
    void recordIsExpired_NoSend_checkLocked() {
        var campaignId2 = createCampaign();
        var queueRecord = yesterdayRecordWithoutStat(campaignId);
        var queueRecord2 = yesterdayRecordWithoutStat(campaignId2).withLockedBy(1);
        queueRepository.insertRecord(testShardContext, queueRecord);
        queueRepository.insertRecord(testShardContext, queueRecord2);
        specialsRepository.add(testShardContext,
                List.of(
                        new BsExportSpecials().withCampaignId(campaignId).withType(QueueType.NOSEND),
                        new BsExportSpecials().withCampaignId(campaignId2).withType(QueueType.NOSEND)
                ));

        var actual = queueRepository.getExpiredAndBrokenCampaignIds(TEST_SHARD, LocalDateTime.now());

        assertThat(actual).containsAllEntriesOf(Map.of(campaignId, false, campaignId2, true));
    }
}

