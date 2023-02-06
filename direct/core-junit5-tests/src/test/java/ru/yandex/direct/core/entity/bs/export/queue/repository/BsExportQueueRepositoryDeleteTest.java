package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.bs.export.model.CampaignIdWithSyncValue;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.yesterdayRecordWithoutStat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

class BsExportQueueRepositoryDeleteTest extends BsExportQueueRepositoryBase {

    /**
     * Тестируем, что метод удаляет запись по переданному ключу, если у нее она есть
     */
    @Test
    void recordExists_recordDeleted() {
        Long campaignId = createCampaign();
        BsExportQueueInfo queueRecord = yesterdayRecordWithoutStat(campaignId);
        queueRepository.insertRecord(TEST_SHARD, queueRecord);
        assumeThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId), notNullValue());

        queueRepository.delete(TEST_SHARD, List.of(campaignId));

        var actual = queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId);

        assertThat(actual).isNull();
    }

    /**
     * Тестируем, что метод удаляет запись по переданному ключу, только если у него не изменился syncVal
     */
    @Test
    void recordDeletedIfSyncValueNotChanged() {
        Long campaignIdSyncValNotChanged = createCampaign();
        BsExportQueueInfo queueRecordSyncValNotChanged = recordWithFullStat(campaignIdSyncValNotChanged);
        queueRecordSyncValNotChanged.withSynchronizeValue(0L);
        queueRepository.insertRecord(TEST_SHARD, queueRecordSyncValNotChanged);

        List<CampaignIdWithSyncValue> cidsWithSyncVal = List.of(
                new CampaignIdWithSyncValueImpl(campaignIdSyncValNotChanged, 0L)
        );
        queueRepository.deleteIfSyncValueNotChanged(TEST_SHARD, cidsWithSyncVal);

        var actualRecordSyncValNotChanged = queueRepository.getBsExportQueueInfo(TEST_SHARD,
                campaignIdSyncValNotChanged);
        assertThat(actualRecordSyncValNotChanged).isNull();
    }

    /**
     * Тестируем, что метод удаляет запись по переданному ключу, только если у него не изменился syncVal
     */
    @Test
    void recordNotDeletedIfSyncValueChanged() {
        Long campaignIdSyncValChanged = createCampaign();
        BsExportQueueInfo queueRecordSyncValChanged = recordWithFullStat(campaignIdSyncValChanged);
        queueRecordSyncValChanged.withSynchronizeValue(2L);
        queueRepository.insertRecord(TEST_SHARD, queueRecordSyncValChanged);

        List<CampaignIdWithSyncValue> cidsWithSyncVal = List.of(
                new CampaignIdWithSyncValueImpl(campaignIdSyncValChanged, 1L)
        );
        queueRepository.deleteIfSyncValueNotChanged(TEST_SHARD, cidsWithSyncVal);

        var actualRecordSyncValChanged = queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignIdSyncValChanged);
        assertThat(actualRecordSyncValChanged)
                .isNotNull()
                .isEqualTo(queueRecordSyncValChanged);
    }
}
