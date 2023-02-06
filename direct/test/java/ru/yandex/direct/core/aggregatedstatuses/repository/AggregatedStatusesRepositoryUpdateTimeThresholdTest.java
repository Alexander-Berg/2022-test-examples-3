package ru.yandex.direct.core.aggregatedstatuses.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum.MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum.PAYED;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatedStatusesRepositoryUpdateTimeThresholdTest {
    private static final SelfStatus PAID_CAMPAIGN_SELF_STATUS = new SelfStatus(
            GdSelfStatusEnum.DRAFT, GdSelfStatusReason.CAMPAIGN_NO_ACTIVE_BANNERS);
    private static final SelfStatus RANDOM_CAMPAIGN_SELF_STATUS = new SelfStatus(
            GdSelfStatusEnum.RUN_WARN, GdSelfStatusReason.ARCHIVED);

    private static final AggregatedStatusCampaignData PAID_CAMPAIGN_DATA = new AggregatedStatusCampaignData(
            List.of(PAYED),
            new CampaignCounters(),
            PAID_CAMPAIGN_SELF_STATUS);
    private static final AggregatedStatusCampaignData RANDOM_CAMPAIGN_DATA = new AggregatedStatusCampaignData(
            List.of(MODERATION),
            new CampaignCounters(),
            RANDOM_CAMPAIGN_SELF_STATUS);

    private int shard;

    private Long firstCampaignId;
    private Long secondCampaignId;

    @Autowired
    private Steps steps;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    @Before
    public void before() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        firstCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        secondCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
    }

    @Test
    public void initializeCampaignStatuses() {
        var firstCampaignStatuses = Map.of(firstCampaignId, PAID_CAMPAIGN_DATA);
        var secondCampaignStatuses = Map.of(secondCampaignId, PAID_CAMPAIGN_DATA);

        // Both campaigns should have initialized statuses regardless of updateBefore value
        // which only comes into play in case of onDuplicateKeyUpdate
        aggregatedStatusesRepository.updateCampaigns(shard, LocalDateTime.now().plusSeconds(10), firstCampaignStatuses);
        aggregatedStatusesRepository.updateCampaigns(shard, null, secondCampaignStatuses);

        checkCampaignSelfStatuses(Map.of(
                firstCampaignId, PAID_CAMPAIGN_SELF_STATUS,
                secondCampaignId, PAID_CAMPAIGN_SELF_STATUS));
    }

    @Test
    public void updateCampaignStatusesWithUpdateBefore() {
        LocalDateTime updateBefore = setDefaultCampaignStatuses();

        // Only the first campaign should update its statuses because of updateBefore condition
        var newCampaignsStatuses = Map.of(
                firstCampaignId, PAID_CAMPAIGN_DATA,
                secondCampaignId, RANDOM_CAMPAIGN_DATA);
        aggregatedStatusesRepository.updateCampaigns(shard, updateBefore, newCampaignsStatuses);

        checkCampaignSelfStatuses(Map.of(
                firstCampaignId, PAID_CAMPAIGN_SELF_STATUS,
                secondCampaignId, PAID_CAMPAIGN_SELF_STATUS));
    }

    @Test
    public void updateCampaignStatusesWithoutUpdateBefore() {
        setDefaultCampaignStatuses();

        // Both campaigns should update their statuses because updateBefore is not set
        var newCampaignsStatuses = Map.of(
                firstCampaignId, PAID_CAMPAIGN_DATA,
                secondCampaignId, RANDOM_CAMPAIGN_DATA);
        aggregatedStatusesRepository.updateCampaigns(shard, null, newCampaignsStatuses);

        checkCampaignSelfStatuses(Map.of(
                firstCampaignId, PAID_CAMPAIGN_SELF_STATUS,
                secondCampaignId, RANDOM_CAMPAIGN_SELF_STATUS));
    }

    private LocalDateTime setDefaultCampaignStatuses() {
        var campaignStatuses = Map.of(
                firstCampaignId, RANDOM_CAMPAIGN_DATA,
                secondCampaignId, PAID_CAMPAIGN_DATA);

        // Initialization of campaigns' statuses
        aggregatedStatusesRepository.updateCampaigns(shard, null, campaignStatuses);

        // Устанавливаем значение updateBefore после обновления статуса первой и до обновления статуса второй кампании
        var updateBefore = LocalDateTime.now();
        aggregatedStatusesRepository.setCampaignStatusUpdateTime(shard, firstCampaignId, updateBefore.minusSeconds(1));
        aggregatedStatusesRepository.setCampaignStatusUpdateTime(shard, secondCampaignId, updateBefore.plusSeconds(1));

        // Checking the initialization
        checkCampaignSelfStatuses(Map.of(
                firstCampaignId, RANDOM_CAMPAIGN_SELF_STATUS,
                secondCampaignId, PAID_CAMPAIGN_SELF_STATUS));

        return updateBefore;
    }

    private void checkCampaignSelfStatuses(Map<Long, SelfStatus> expectedCampaignSelfStatusesByIds) {
        var campaignStatusesByIds = aggregatedStatusesRepository.getCampaignStatusesByIds(shard,
                expectedCampaignSelfStatusesByIds.keySet());
        expectedCampaignSelfStatusesByIds.forEach((campaignId, selfStatus) ->
                assertEquals(selfStatus.getStatus(), campaignStatusesByIds.get(campaignId).getStatus().orElse(null)));
    }
}
