package ru.yandex.direct.core.entity.metrika.service;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaGoalsServiceGetByCampaignIdTest {
    @Autowired
    private MetrikaCampaignRepository metrikaCampaignRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    @Autowired
    private TestClientRepository testClientRepository;

    private ClientId clientId;
    private Long operatorUid;
    private Long campaignId;
    private CampaignType campaignType;

    private static Long metrikaGoalId = 1345L;

    @Before
    public void before() {
        UserInfo defaultUser = steps.userSteps().createDefaultUser();
        var textCampaign = steps.campaignSteps().createActiveTextCampaign(defaultUser.getClientInfo());
        campaignId = textCampaign.getCampaignId();
        campaignType = textCampaign.getCampaign().getType();
        var shard = defaultUser.getShard();
        clientId = defaultUser.getClientId();
        operatorUid = defaultUser.getUid();

        metrikaGoalId++;
        metrikaCampaignRepository.addGoalIds(shard, campaignId, Set.of(metrikaGoalId));
    }

    @Test
    public void getWithoutMobileGoalsFeature() {
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.MOBILE_APP_GOALS_FOR_TEXT_CAMPAIGN_STRATEGY_ENABLED, false);

        var goalIds = metrikaGoalsService.getGoalIdsByCampaignId(operatorUid, clientId,
                Map.of(campaignId, campaignType), true).get(campaignId);

        assertThat(goalIds).containsExactlyInAnyOrder(metrikaGoalId);
    }

    @Test
    public void getWithMobileGoalsEnabledFeature() {
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.MOBILE_APP_GOALS_FOR_TEXT_CAMPAIGN_STRATEGY_ENABLED, true);

        var goalIds = metrikaGoalsService.getGoalIdsByCampaignId(operatorUid, clientId,
                Map.of(campaignId, campaignType), true).get(campaignId);

        assertThat(goalIds).containsExactlyInAnyOrder(metrikaGoalId, 4L);
    }

    @Test
    public void getWithUserManagerRole() {
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.MOBILE_APP_GOALS_FOR_TEXT_CAMPAIGN_STRATEGY_ENABLED, true);
        var managerClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        operatorUid = managerClientInfo.getUid();
        testClientRepository.bindManagerToClient(managerClientInfo.getShard(), clientId, managerClientInfo.getUid());

        var goalIds = metrikaGoalsService.getGoalIdsByCampaignId(operatorUid, clientId,
                Map.of(campaignId, campaignType), true).get(campaignId);

        assertThat(goalIds).containsExactlyInAnyOrder(metrikaGoalId, 3L, 4L, 5L, 6L, 7L,
                38402972L, 38403008L, 38403053L, 38403071L, 38403080L, 38403095L, 38403104L,
                38403131L, 38403173L, 38403191L, 38403197L, 38403206L, 38403215L, 38403230L,
                38403338L, 38403494L, 38403530L, 38403545L, 38403581L);
    }

    @Test
    public void getWithUserSupportRole() {
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.MOBILE_APP_GOALS_FOR_TEXT_CAMPAIGN_STRATEGY_ENABLED, true);
        operatorUid = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPPORT).getUid();

        var goalIds = metrikaGoalsService.getGoalIdsByCampaignId(operatorUid, clientId,
                Map.of(campaignId, campaignType), true).get(campaignId);

        assertThat(goalIds).containsExactlyInAnyOrder(metrikaGoalId, 4L,
                38402972L, 38403008L, 38403053L, 38403071L, 38403080L, 38403095L, 38403104L,
                38403131L, 38403173L, 38403191L, 38403197L, 38403206L, 38403215L, 38403230L,
                38403338L, 38403494L, 38403530L, 38403545L, 38403581L);
    }
}
