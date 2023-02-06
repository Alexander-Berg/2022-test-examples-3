package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportSpecials;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.bs.export.queue.model.QueueType.BUGGY;

@CoreTest
@ExtendWith(SpringExtension.class)
@ParametersAreNonnullByDefault
class BsExportSpecialsRepositoryMoveToBuggyTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BsExportSpecialsRepository bsExportSpecialsRepository;

    @ParameterizedTest
    @EnumSource(value = QueueType.class, names = {"NOSEND", "DEV1", "DEV2", "PREPROD"})
    void remainInCurrentQueueTest(QueueType queueType) {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        var campaignId = campaignInfo.getCampaignId();
        var shard = campaignInfo.getShard();
        insertIntoSpecialQueue(shard, campaignId, queueType);
        bsExportSpecialsRepository.moveToBuggy(shard, List.of(campaignId));
        var bsExportSpecialsRecord = bsExportSpecialsRepository.getByCampaignIds(shard, List.of(campaignId));

        assertThat(bsExportSpecialsRecord).hasSize(1);
        assertThat(bsExportSpecialsRecord.get(0).getType()).isEqualTo(queueType);
    }

    @ParameterizedTest
    @EnumSource(value = QueueType.class, names = {"NOSEND", "DEV1", "DEV2", "PREPROD"}, mode = EnumSource.Mode.EXCLUDE)
    void moveToBuggyQueueTest(QueueType queueType) {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        var campaignId = campaignInfo.getCampaignId();
        var shard = campaignInfo.getShard();
        insertIntoSpecialQueue(shard, campaignId, queueType);
        bsExportSpecialsRepository.moveToBuggy(shard, List.of(campaignId));
        var bsExportSpecialsRecord = bsExportSpecialsRepository.getByCampaignIds(shard, List.of(campaignId));

        assertThat(bsExportSpecialsRecord).hasSize(1);
        assertThat(bsExportSpecialsRecord.get(0).getType()).isEqualTo(BUGGY);
    }

    @Test
    void campaignWithoutQueueTest() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        var campaignId = campaignInfo.getCampaignId();
        var shard = campaignInfo.getShard();
        bsExportSpecialsRepository.moveToBuggy(shard, List.of(campaignId));
        var bsExportSpecialsRecord = bsExportSpecialsRepository.getByCampaignIds(shard, List.of(campaignId));

        assertThat(bsExportSpecialsRecord).hasSize(1);
        assertThat(bsExportSpecialsRecord.get(0).getType()).isEqualTo(BUGGY);
    }

    private void insertIntoSpecialQueue(int shard, long campaignId, QueueType queueType) {
        var bsExportSpecials = new BsExportSpecials()
                .withCampaignId(campaignId)
                .withType(queueType);
        bsExportSpecialsRepository.add(shard, List.of(bsExportSpecials));
    }
}
