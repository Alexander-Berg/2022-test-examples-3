package ru.yandex.direct.jobs.campaign;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesService;
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.aggregatedstatuses.repository.model.RecalculationDepthEnum;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.RECALCULATE_CAMPAIGNS_STATUS_JOB_DEPTH;
import static ru.yandex.direct.common.db.PpcPropertyNames.RECALCULATE_CAMPAIGNS_STATUS_JOB_ITERATION_LIMIT;
import static ru.yandex.direct.common.db.PpcPropertyNames.RECALCULATE_CAMPAIGNS_STATUS_JOB_SLEEP_COEFFICIENT;
import static ru.yandex.direct.common.db.PpcPropertyNames.recalculateCampaignsStatusJobEnabled;

@JobsTest
@ExtendWith(SpringExtension.class)
class RecalculateCampaignsStatusJobTest {
    private static final SelfStatus DRAFT_SELF_STATUS =
            new SelfStatus(GdSelfStatusEnum.DRAFT, GdSelfStatusReason.DRAFT);
    private static final SelfStatus PAID_CAMPAIGN_SELF_STATUS =
            new SelfStatus(GdSelfStatusEnum.DRAFT, GdSelfStatusReason.CAMPAIGN_NO_ACTIVE_BANNERS);
    private static final SelfStatus RANDOM_CAMPAIGN_SELF_STATUS =
            new SelfStatus(GdSelfStatusEnum.RUN_OK, GdSelfStatusReason.ARCHIVED);

    private ClientInfo clientInfo;
    private int shard;
    private RecalculateCampaignsStatusJob job;

    @Autowired
    private Steps steps;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private AggregatedStatusesService aggregatedStatusesService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    @BeforeEach
    void beforeEach() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        ppcPropertiesSupport.set(recalculateCampaignsStatusJobEnabled(shard).getName(), String.valueOf(true));
        ppcPropertiesSupport.set(RECALCULATE_CAMPAIGNS_STATUS_JOB_SLEEP_COEFFICIENT.getName(), String.valueOf(0.0));
        ppcPropertiesSupport.set(RECALCULATE_CAMPAIGNS_STATUS_JOB_ITERATION_LIMIT.getName(), String.valueOf(10));
        ppcPropertiesSupport.set(RECALCULATE_CAMPAIGNS_STATUS_JOB_DEPTH.getName(), RecalculationDepthEnum.ALL.value());

        job = new RecalculateCampaignsStatusJob(shard, ppcPropertiesSupport, aggregatedStatusesService,
                campaignRepository);
    }

    @Test
    void executeWithNumberOfCampaignsLessThanIterationLimit() {
        var adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        var adGroupId = adGroupInfo.getAdGroupId();
        var noMoneyCampaignId = adGroupInfo.getCampaignId();
        var paidCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();

        job.execute();

        checkCampaignsSelfStatus(Map.of(
                noMoneyCampaignId, DRAFT_SELF_STATUS,
                paidCampaignId, PAID_CAMPAIGN_SELF_STATUS));
        checkAdGroupsSelfStatus(Map.of(adGroupId, DRAFT_SELF_STATUS));
    }

    @Test
    void executeWithNumberOfCampaignsMultipleOfIterationLimit() {
        ppcPropertiesSupport.set(RECALCULATE_CAMPAIGNS_STATUS_JOB_ITERATION_LIMIT.getName(), String.valueOf(1));

        var paidCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();

        job.execute();

        checkCampaignsSelfStatus(Map.of(paidCampaignId, PAID_CAMPAIGN_SELF_STATUS));
    }

    @Test
    void executeWithUpdateBeforeCondition() {
        // Создаем две одинаковые кампании
        var firstCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        var secondCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();

        // Запускаем job, кампании получают одинаковые статусы
        job.execute();

        // Меняем обеим кампаниям статус на неправильный
        var aggregatedStatusCampaignData = aggregatedStatusesRepository.getCampaignStatusesByIds(shard,
                List.of(firstCampaignId, secondCampaignId));
        updateCampaignSelfStatus(aggregatedStatusCampaignData, firstCampaignId, RANDOM_CAMPAIGN_SELF_STATUS);
        updateCampaignSelfStatus(aggregatedStatusCampaignData, secondCampaignId, RANDOM_CAMPAIGN_SELF_STATUS);

        // Устанавливаем значение updateBefore после обновления статуса первой и до обновления статуса второй кампании
        var updateBefore = LocalDateTime.now();
        aggregatedStatusesRepository.setCampaignStatusUpdateTime(shard, firstCampaignId, updateBefore.minusSeconds(1));
        aggregatedStatusesRepository.setCampaignStatusUpdateTime(shard, secondCampaignId, updateBefore.plusSeconds(1));

        // Перезапускаем job, заново пересчитаться должен статус только первой кампании
        ppcPropertiesSupport.set(recalculateCampaignsStatusJobEnabled(shard).getName(), String.valueOf(true));
        job.execute(updateBefore);

        checkCampaignsSelfStatus(Map.of(
                firstCampaignId, PAID_CAMPAIGN_SELF_STATUS,
                secondCampaignId, RANDOM_CAMPAIGN_SELF_STATUS));
    }

    @Test
    void executeWithCampaignsRecalculationDepth() {
        ppcPropertiesSupport.set(RECALCULATE_CAMPAIGNS_STATUS_JOB_DEPTH.getName(),
                RecalculationDepthEnum.CAMPAIGNS.value());
        var adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        var adGroupId = adGroupInfo.getAdGroupId();
        var noMoneyCampaignId = adGroupInfo.getCampaignId();

        job.execute();

        checkCampaignsSelfStatus(Map.of(noMoneyCampaignId, DRAFT_SELF_STATUS));
        var adGroupStatusesByIds = aggregatedStatusesRepository.getAdGroupStatusesByIds(shard, List.of(adGroupId));
        assertThat(adGroupStatusesByIds).isEmpty();
    }

    @Test
    void executeWithAdGroupsRecalculationDepth() {
        ppcPropertiesSupport.set(RECALCULATE_CAMPAIGNS_STATUS_JOB_DEPTH.getName(),
                RecalculationDepthEnum.ADGROUPS.value());
        var adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        var adGroupId = adGroupInfo.getAdGroupId();
        var noMoneyCampaignId = adGroupInfo.getCampaignId();

        job.execute();

        var campaignStatusesByIds = aggregatedStatusesRepository.getCampaignStatusesByIds(shard,
                List.of(noMoneyCampaignId));
        assertThat(campaignStatusesByIds).isEmpty();
        checkAdGroupsSelfStatus(Map.of(adGroupId, DRAFT_SELF_STATUS));
    }

    private void updateCampaignSelfStatus(Map<Long, AggregatedStatusCampaignData> aggregatedStatusCampaignData,
                                          Long campaignId, SelfStatus selfStatus) {
        var campaignData = aggregatedStatusCampaignData.get(campaignId);
        campaignData.updateSelfStatus(selfStatus);
        aggregatedStatusesRepository.updateCampaigns(shard, null, Map.of(campaignId, campaignData));
    }

    private void checkCampaignsSelfStatus(Map<Long, SelfStatus> expectedCampaignSelfStatusesByIds) {
        var campaignStatusesByIds = aggregatedStatusesRepository.getCampaignStatusesByIds(shard,
                expectedCampaignSelfStatusesByIds.keySet());

        expectedCampaignSelfStatusesByIds.forEach((campaignId, selfStatus) ->
                assertThat(selfStatus.getStatus())
                        .describedAs("status for campaign %s", campaignId)
                        .isEqualTo(campaignStatusesByIds.get(campaignId).getStatus().orElse(null)));
    }

    private void checkAdGroupsSelfStatus(Map<Long, SelfStatus> expectedAdGroupSelfStatusesByIds) {
        var adGroupStatusesByIds = aggregatedStatusesRepository.getAdGroupStatusesByIds(shard,
                expectedAdGroupSelfStatusesByIds.keySet());
        expectedAdGroupSelfStatusesByIds.forEach((adGroupId, selfStatus) ->
                assertThat(selfStatus.getStatus())
                .describedAs("status for adgroup %s", adGroupId)
                .isEqualTo(adGroupStatusesByIds.get(adGroupId).getStatus().orElse(null)));
    }
}
