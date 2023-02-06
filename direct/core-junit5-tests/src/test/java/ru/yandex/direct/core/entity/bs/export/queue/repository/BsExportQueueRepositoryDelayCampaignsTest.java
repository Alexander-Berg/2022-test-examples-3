package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.data.TemporalUnitOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;

class BsExportQueueRepositoryDelayCampaignsTest extends BsExportQueueRepositoryBase {
    private static final TemporalUnitOffset OFFSET = new TemporalUnitWithinOffset(10, ChronoUnit.MINUTES);


    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * Тест проверяет, что если тип очереди full_bs_export, то поле SEQ_TIME не сбросится, а сбросится
     * FULL_EXPORT_SEQ_TIME
     * Если предыдущее время FULL_EXPORT_SEQ_TIME + delay_time больше, чем сейчас, то после вызова метода
     * FULL_EXPORT_SEQ_TIME
     * установится в сейчас
     */
    @Test
    void fullExport_DelayToNowTest() {
        var bsExportQueueRepository = new BsExportQueueRepository(dslContextProvider, Duration.ofDays(2));
        var campaignId = createCampaign();
        var queueInfo = recordWithFullStat(campaignId);
        queueInfo
                .withFullExportSequenceTime(LocalDateTime.now().minusDays(1).truncatedTo(SECONDS));
        queueInfo
                .withSequenceTime(LocalDateTime.now().minusDays(4).truncatedTo(SECONDS));
        queueRepository
                .insertRecord(testShardContext, queueInfo);
        bsExportQueueRepository.delayCampaigns(TEST_SHARD, List.of(campaignId), true);

        var actualBsExportInfoList = queueRepository.getBsExportQueueInfo(TEST_SHARD, List.of(campaignId));
        assertThat(actualBsExportInfoList).hasSize(1);
        assertThat(actualBsExportInfoList.get(campaignId)).isEqualToIgnoringGivenFields(queueInfo,
                "fullExportSequenceTime");
        assertThat(actualBsExportInfoList.get(campaignId).getFullExportSequenceTime()).isCloseTo(LocalDateTime.now(),
                OFFSET);
    }

    /**
     * Тест проверяет, что если тип очереди full_bs_export, то поле SEQ_TIME не сбросится, а сбросится
     * FULL_EXPORT_SEQ_TIME
     * Если предыдущее время FULL_EXPORT_SEQ_TIME + delay_time меньше, чем сейчас, то после вызова метода
     * FULL_EXPORT_SEQ_TIME
     * установится в  FULL_EXPORT_SEQ_TIME + delay_time
     */
    @Test
    void fullExport_DelayOnDelayTimeTest() {
        var delayTime = Duration.ofDays(2);
        var bsExportQueueRepository = new BsExportQueueRepository(dslContextProvider, delayTime);
        var campaignId = createCampaign();
        var queueInfo = recordWithFullStat(campaignId);
        queueInfo
                .withFullExportSequenceTime(LocalDateTime.now().minusDays(3).truncatedTo(SECONDS));
        queueInfo
                .withSequenceTime(LocalDateTime.now().minusDays(4).truncatedTo(SECONDS));
        queueRepository
                .insertRecord(testShardContext, queueInfo);
        bsExportQueueRepository.delayCampaigns(TEST_SHARD, List.of(campaignId), true);

        var actualBsExportInfoList = queueRepository.getBsExportQueueInfo(TEST_SHARD, List.of(campaignId));
        assertThat(actualBsExportInfoList).hasSize(1);
        assertThat(actualBsExportInfoList.get(campaignId)).isEqualToIgnoringGivenFields(queueInfo,
                "fullExportSequenceTime");
        assertThat(actualBsExportInfoList.get(campaignId).getFullExportSequenceTime()).isEqualTo(queueInfo.getFullExportSequenceTime().plus(delayTime));
    }

    /**
     * Тест проверяет, что если тип очереди не full_bs_export, то поле FULL_EXPORT_SEQ_TIME не сбросится, а сбросится
     * SEQ_TIME
     * Если предыдущее время SEQ_TIME + delay_time больше, чем сейчас, то после вызова метода SEQ_TIME
     * установится в сейчас
     */
    @Test
    void notFullExport_DelayToNowTest() {
        var bsExportQueueRepository = new BsExportQueueRepository(dslContextProvider, Duration.ofDays(2));
        var campaignId = createCampaign();
        var queueInfo = recordWithFullStat(campaignId);
        queueInfo
                .withFullExportSequenceTime(LocalDateTime.now().minusDays(4).truncatedTo(SECONDS));
        queueInfo
                .withSequenceTime(LocalDateTime.now().minusDays(1).truncatedTo(SECONDS));
        queueRepository
                .insertRecord(testShardContext, queueInfo);
        bsExportQueueRepository.delayCampaigns(TEST_SHARD, List.of(campaignId), false);

        var actualBsExportInfoList = queueRepository.getBsExportQueueInfo(TEST_SHARD, List.of(campaignId));
        assertThat(actualBsExportInfoList).hasSize(1);
        assertThat(actualBsExportInfoList.get(campaignId)).isEqualToIgnoringGivenFields(queueInfo,
                "sequenceTime");
        assertThat(actualBsExportInfoList.get(campaignId).getSequenceTime()).isCloseTo(LocalDateTime.now(),
                OFFSET);
    }

    /**
     * Тест проверяет, что если тип очереди не full_bs_export, то поле FULL_EXPORT_SEQ_TIME не сбросится, а сбросится
     * SEQ_TIME
     * Если предыдущее время SEQ_TIME + delay_time меньше, чем сейчас, то после вызова метода
     * SEQ_TIME установится в  SEQ_TIME + delay_time
     */
    @Test
    void notFullExport_DelayOnDelayTimeTest() {
        var delayTime = Duration.ofDays(2);
        var bsExportQueueRepository = new BsExportQueueRepository(dslContextProvider, delayTime);
        var campaignId = createCampaign();
        var queueInfo = recordWithFullStat(campaignId);
        queueInfo
                .withFullExportSequenceTime(LocalDateTime.now().minusDays(4).truncatedTo(SECONDS));
        queueInfo
                .withSequenceTime(LocalDateTime.now().minusDays(3).truncatedTo(SECONDS));
        queueRepository
                .insertRecord(testShardContext, queueInfo);
        bsExportQueueRepository.delayCampaigns(TEST_SHARD, List.of(campaignId), false);

        var actualBsExportInfoList = queueRepository.getBsExportQueueInfo(TEST_SHARD, List.of(campaignId));
        assertThat(actualBsExportInfoList).hasSize(1);
        assertThat(actualBsExportInfoList.get(campaignId)).isEqualToIgnoringGivenFields(queueInfo,
                "sequenceTime");
        assertThat(actualBsExportInfoList.get(campaignId).getSequenceTime()).isEqualTo(queueInfo.getSequenceTime().plus(delayTime));
    }
}
