package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.TemporalUnitOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;

import static java.util.Collections.singleton;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.FULL_EXPORT_SEQ_TIME_FIELD;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.TIME_FIELDS;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithoutStat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.yesterdayRecordWithoutStat;

class BsExportQueueRepositoryAddCampaignsFullExportFlagTest extends BsExportQueueRepositoryBase {
    private static final TemporalUnitOffset OFFSET = new TemporalUnitWithinOffset(10, ChronoUnit.MINUTES);

    private Long campaignId;

    @BeforeEach
    void createTestData() {
        campaignId = createCampaign();
    }

    @Test
    void nonExistenceInQueue_Added() {
        SoftAssertions soft = new SoftAssertions();

        int updated = queueRepository.addCampaignsFullExportFlag(TEST_SHARD, singleton(campaignId));
        soft.assertThat(updated).isEqualTo(1);

        BsExportQueueInfo actual = queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId);
        BsExportQueueInfo expected = recordWithoutStat(campaignId).withNeedFullExport(true);
        soft.assertThat(actual).isEqualToIgnoringGivenFields(expected, TIME_FIELDS);
        soft.assertThat(actual.getFullExportSequenceTime()).isCloseTo(LocalDateTime.now(), OFFSET);

        soft.assertAll();
    }

    @Test
    void withoutFlag_Updated() {
        BsExportQueueInfo record = yesterdayRecordWithoutStat(campaignId);
        queueRepository.insertRecord(testShardContext, record);

        SoftAssertions soft = new SoftAssertions();

        int updated = queueRepository.addCampaignsFullExportFlag(TEST_SHARD, singleton(campaignId));
        soft.assertThat(updated).isEqualTo(2);

        BsExportQueueInfo actual = queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId);
        BsExportQueueInfo expected = record.withNeedFullExport(true).withSynchronizeValue(1L);
        soft.assertThat(actual).isEqualToIgnoringGivenFields(expected, FULL_EXPORT_SEQ_TIME_FIELD);
        soft.assertThat(actual.getFullExportSequenceTime()).isCloseTo(LocalDateTime.now(), OFFSET);

        soft.assertAll();
    }

    @Test
    void withFlag_IncreaseSyncValOnly() {
        BsExportQueueInfo record = yesterdayRecordWithoutStat(campaignId).withNeedFullExport(true);
        queueRepository.insertRecord(testShardContext, record);

        SoftAssertions soft = new SoftAssertions();

        int updated = queueRepository.addCampaignsFullExportFlag(TEST_SHARD, singleton(campaignId));
        soft.assertThat(updated).isEqualTo(2);

        BsExportQueueInfo actual = queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId);
        BsExportQueueInfo expected = record.withSynchronizeValue(1L);
        soft.assertThat(actual).isEqualToComparingFieldByField(expected);

        soft.assertAll();
    }

    @Test
    void withBigSyncVal_ResetQueueTime() {
        BsExportQueueInfo record = yesterdayRecordWithoutStat(campaignId).withSynchronizeValue(199L);
        queueRepository.insertRecord(testShardContext, record);

        SoftAssertions soft = new SoftAssertions();

        int updated = queueRepository.addCampaignsFullExportFlag(TEST_SHARD, singleton(campaignId));
        soft.assertThat(updated).isEqualTo(2);

        BsExportQueueInfo actual = queueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId);
        BsExportQueueInfo expected = record.withSynchronizeValue(200L).withNeedFullExport(true);
        soft.assertThat(actual).isEqualToIgnoringGivenFields(expected, TIME_FIELDS);
        soft.assertThat(actual.getSequenceTime()).isEqualTo(expected.getSequenceTime());
        soft.assertThat(actual.getFullExportSequenceTime()).isCloseTo(LocalDateTime.now(), OFFSET);
        soft.assertThat(actual.getQueueTime()).isCloseTo(LocalDateTime.now(), OFFSET);

        soft.assertAll();
    }
}

