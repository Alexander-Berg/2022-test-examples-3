package ru.yandex.direct.core.entity.metrika.service;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class MetrikaGoalsGetGoalsWithCountersTest {

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    @Autowired
    private MetrikaCampaignRepository metrikaCampaignRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private MetrikaClientStub metrikaClient;

    private int shard;
    private ClientId clientId;
    private Long operatorUid;
    private Long campaignId;
    private CampaignType campaignType;

    private static Long metrikaCounterId = 1234L;
    private static Long metrikaGoalId = 1234L;

    public static final CounterGoal.Type COUNTER_GOAL_TYPE = CounterGoal.Type.NUMBER;

    @Before
    public void before() {
        UserInfo defaultUser = steps.userSteps().createDefaultUser();
        var textCampaign = steps.campaignSteps().createActiveTextCampaign(defaultUser.getClientInfo());
        campaignId = textCampaign.getCampaignId();
        campaignType = textCampaign.getCampaign().getType();
        shard = defaultUser.getShard();
        clientId = defaultUser.getClientId();
        operatorUid = defaultUser.getUid();

        metrikaCounterId++;
        metrikaGoalId++;
        metrikaClient.addUserCounter(defaultUser.getUid(), metrikaCounterId.intValue());
        metrikaClient.addCounterGoal(metrikaCounterId.intValue(), new CounterGoal()
                .withId(metrikaGoalId.intValue())
                .withType(COUNTER_GOAL_TYPE));
    }

    @Test
    public void get_HasMobileGoalsWithStatistic_Success() {
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.MOBILE_APP_GOALS_FOR_TEXT_CAMPAIGN_STRATEGY_ENABLED, true);

        long mobileGoalId = 4L;
        metrikaCampaignRepository.addGoalIds(shard, campaignId, Set.of(mobileGoalId));

        Set<Goal> goals = Set.of((Goal) new Goal()
                .withId(mobileGoalId)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.NUMBER));

        steps.retargetingGoalsSteps().createMetrikaGoalsInPpcDict(goals);
        List<Goal> actualGoals = metrikaGoalsService.getGoalsWithCounters(operatorUid, clientId,
                Set.of(campaignId));

        assertThat(actualGoals).hasSize(1);
    }

}
