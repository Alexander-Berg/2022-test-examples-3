package ru.yandex.direct.core.entity.bs.export.queue.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.TemporalUnitOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportQueueRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.TIME_FIELDS;

@CoreTest
@ExtendWith(SpringExtension.class)
@ParametersAreNonnullByDefault
class FullExportQueueServiceMasterIterationSmokeTest {
    private static final TemporalUnitOffset OFFSET = new TemporalUnitWithinOffset(3, ChronoUnit.MINUTES);

    @Autowired
    private FullExportQueueService service;

    @Autowired
    private BsExportQueueRepository queueRepository;

    @Autowired
    private Steps steps;

    private final SoftAssertions softly = new SoftAssertions();

    private Long campaignId;
    private Integer shard;

    @BeforeEach
    void createTestData() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        campaignId = campaignInfo.getCampaignId();
        shard = campaignInfo.getShard();
    }

    @Test
    void campaignNotPresentedInQueue_Added() {
        BsExportQueueInfo expected = new BsExportQueueInfo()
                .withSynchronizeValue(0L)
                .withCampaignId(campaignId)
                .withCampaignsCount(0L)
                .withContextsCount(0L)
                .withBannersCount(0L)
                .withKeywordsCount(0L)
                .withPricesCount(0L)
                .withNeedFullExport(true);

        service.getMaster(shard).iteration(Integer.MAX_VALUE, 1, campaignId + 1);
        BsExportQueueInfo queueInfo = queueRepository.getBsExportQueueInfo(shard, campaignId);

        softly.assertThat(queueInfo).isNotNull();
        softly.assertThat(queueInfo).isEqualToIgnoringGivenFields(expected, TIME_FIELDS);
        softly.assertThat(queueInfo.getFullExportSequenceTime()).isCloseTo(LocalDateTime.now(), OFFSET);
        softly.assertAll();
    }

    @Test
    void campaignAlreadyProcessedByStandardExport_Updated() {
        LocalDateTime time = LocalDateTime.now().minusMinutes(34);
        BsExportQueueInfo initial = new BsExportQueueInfo()
                .withSynchronizeValue(1L)
                .withCampaignId(campaignId)
                .withCampaignsCount(1L)
                .withContextsCount(intRandom())
                .withBannersCount(intRandom())
                .withKeywordsCount(intRandom())
                .withPricesCount(intRandom())
                .withSequenceTime(time)
                .withQueueTime(time)
                .withFullExportSequenceTime(time)
                .withNeedFullExport(false);
        queueRepository.insertRecord(shard, initial);

        BsExportQueueInfo expected = queueRepository.getBsExportQueueInfo(shard, campaignId);
        expected.setNeedFullExport(true);
        expected.setSynchronizeValue(initial.getSynchronizeValue() + 1);

        service.getMaster(shard).iteration(Integer.MAX_VALUE, 1, campaignId + 1);
        BsExportQueueInfo queueInfo = queueRepository.getBsExportQueueInfo(shard, campaignId);

        softly.assertThat(queueInfo).isNotNull();
        softly.assertThat(queueInfo).isEqualToIgnoringGivenFields(expected, TIME_FIELDS);
        softly.assertThat(queueInfo.getFullExportSequenceTime()).isCloseTo(LocalDateTime.now(), OFFSET);
        softly.assertThat(queueInfo.getQueueTime()).isEqualTo(expected.getQueueTime());
        softly.assertThat(queueInfo.getSequenceTime()).isEqualTo(expected.getSequenceTime());
        softly.assertAll();

    }

    private static Long intRandom() {
        return RandomUtils.nextLong(2, 1_000);
    }
}
