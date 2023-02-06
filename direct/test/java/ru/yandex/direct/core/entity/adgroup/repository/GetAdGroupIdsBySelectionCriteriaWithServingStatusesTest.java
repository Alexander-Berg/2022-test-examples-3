package ru.yandex.direct.core.entity.adgroup.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.ServingStatus;
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetAdGroupIdsBySelectionCriteriaWithServingStatusesTest {

    private int shard;
    private Set<Long> campaignIds = new HashSet<>();
    private Long rarelyServedAdGroupId;
    private Long eligibleAdGroupId1;
    private Long eligibleAdGroupId2;
    private Long eligibleAdGroupId3;
    private Long eligibleAdGroupId4;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupRepository repository;

    @Before
    public void setUp() {

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        shard = clientInfo.getShard();

        CampaignSteps campaignSteps = steps.campaignSteps();
        AdGroupSteps adGroupSteps = steps.adGroupSteps();

        // NB - https://st.yandex-team.ru/DIRECT-62527

        // adgroup is rarely loaded
        CampaignInfo activeCampaign = campaignSteps.createActiveCampaign(clientInfo);
        campaignIds.add(activeCampaign.getCampaignId());

        rarelyServedAdGroupId = adGroupSteps
                .createAdGroup(activeTextAdGroup(activeCampaign.getCampaignId()).withBsRarelyLoaded(true),
                        activeCampaign).getAdGroupId();
        steps.adGroupSteps().setBsRarelyLoaded(shard, rarelyServedAdGroupId, true);

        // active text adgroup
        CampaignInfo activeCampaign1 = campaignSteps
                .createCampaign(activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid()).withArchived(false));
        campaignIds.add(activeCampaign1.getCampaignId());

        eligibleAdGroupId1 =
                adGroupSteps.createAdGroup(activeTextAdGroup(activeCampaign1.getCampaignId()), activeCampaign1)
                        .getAdGroupId();

        // archived campaign
        CampaignInfo archivedCampaign = campaignSteps
                .createCampaign(activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid()).withArchived(true));
        campaignIds.add(archivedCampaign.getCampaignId());

        eligibleAdGroupId2 =
                adGroupSteps.createAdGroup(activeTextAdGroup(archivedCampaign.getCampaignId()), archivedCampaign)
                        .getAdGroupId();

        // dynamic adgroup
        CampaignInfo dynamicCampaign = campaignSteps.createActiveDynamicCampaign(clientInfo);
        campaignIds.add(dynamicCampaign.getCampaignId());

        eligibleAdGroupId3 =
                adGroupSteps.createAdGroup(activeDynamicTextAdGroup(dynamicCampaign.getCampaignId()), dynamicCampaign)
                        .getAdGroupId();

        // performance adgroup
        CampaignInfo performanceCampaign = campaignSteps.createActivePerformanceCampaign(clientInfo);
        campaignIds.add(performanceCampaign.getCampaignId());

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        eligibleAdGroupId4 = adGroupSteps
                .createAdGroup(
                        activePerformanceAdGroup(performanceCampaign.getCampaignId(), feedId),
                        performanceCampaign)
                .getAdGroupId();
    }

    @Test
    public void getRarelyServedAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupServingStatuses(ServingStatus.RARELY_SERVED), maxLimited());

        assertThat("вернулись id ожидаемых групп со статусом 'мало показов'", adGroupIds,
                contains(rarelyServedAdGroupId));
    }

    @Test
    public void getEligibleAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupServingStatuses(ServingStatus.ELIGIBLE), maxLimited());

        assertThat("вернулись id ожидаемых групп со статусом 'показы возможны'", adGroupIds,
                contains(eligibleAdGroupId1, eligibleAdGroupId2, eligibleAdGroupId3, eligibleAdGroupId4));
    }

    @Test
    public void getAdGroupsWithAllServingStatuses() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupServingStatuses(ServingStatus.RARELY_SERVED, ServingStatus.ELIGIBLE), maxLimited());

        assertThat("вернулись id ожидаемых групп - группы с любыми статусами возможности показов", adGroupIds,
                contains(rarelyServedAdGroupId, eligibleAdGroupId1, eligibleAdGroupId2, eligibleAdGroupId3,
                        eligibleAdGroupId4));
    }

    @Test
    public void getAdGroupsWithNullAsServingStatus() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupServingStatuses((Set<ServingStatus>) null), maxLimited());

        assertThat("вернулись id ожидаемых групп - группы с любыми статусами возможности показов", adGroupIds,
                contains(rarelyServedAdGroupId, eligibleAdGroupId1, eligibleAdGroupId2, eligibleAdGroupId3,
                        eligibleAdGroupId4));
    }
}
