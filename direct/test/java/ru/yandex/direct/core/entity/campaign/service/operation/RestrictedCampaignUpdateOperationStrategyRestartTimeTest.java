package ru.yandex.direct.core.entity.campaign.service.operation;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignUpdateOperationStrategyRestartTimeTest {
    public static final int GOAL_ID_FIRST = 124;
    public static final int GOAL_ID_SECOND = 125;
    public static final int COUNTER_ID = 123;

    @Autowired
    CampaignOperationService campaignOperationService;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    MetrikaClientStub metrikaClientStub;

    @Autowired
    private Steps steps;

    private UserInfo defaultUser;
    private TextCampaignInfo textCampaignInfo;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
        metrikaClientStub.addUserCounter(defaultUser.getUid(), COUNTER_ID);
        metrikaClientStub.addCounterGoal(COUNTER_ID, GOAL_ID_FIRST);
        metrikaClientStub.addCounterGoal(COUNTER_ID, GOAL_ID_SECOND);

        TextCampaign textCampaign =
                defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        textCampaign
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withMetrikaCounters(List.of((long) COUNTER_ID))
                .withStrategy((DbStrategy) defaultAverageCpaStrategy((long) GOAL_ID_FIRST).withAutobudget(CampaignsAutobudget.YES));

        textCampaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(),
                textCampaign);
    }

    @Test
    public void updateGoalIdInConversionStrategy_AddRestartTime() {
        steps.featureSteps().addClientFeature(textCampaignInfo.getClientId(),
                FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED, true);
        ModelChanges<TextCampaign> mc = new ModelChanges<>(textCampaignInfo.getId(), TextCampaign.class);
        mc.process((DbStrategy) defaultAverageCpaStrategy((long) GOAL_ID_SECOND)
                        .withAutobudget(CampaignsAutobudget.YES),
                TextCampaign.STRATEGY);

        LocalDateTime now = now().minusSeconds(1);
        var options = new CampaignOptions();
        var updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc),
                        textCampaignInfo.getUid(),
                        UidAndClientId.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId()),
                        options);

        updateOperation.apply();
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaignsMap(textCampaignInfo.getShard(),
                        List.of(textCampaignInfo.getId())).get(textCampaignInfo.getId());
        assertThat(actualCampaign.getStrategy().getStrategyData().getLastBidderRestartTime()).isAfterOrEqualTo(now);
    }

    @Test
    public void updateGoalIdInConversionStrategy_FeatureDisabled_RestartTimeNotAdded() {
        steps.featureSteps().addClientFeature(textCampaignInfo.getClientId(),
                FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED, false);

        ModelChanges<TextCampaign> mc = new ModelChanges<>(textCampaignInfo.getId(), TextCampaign.class);
        mc.process((DbStrategy) defaultAverageCpaStrategy((long) GOAL_ID_SECOND)
                        .withAutobudget(CampaignsAutobudget.YES),
                TextCampaign.STRATEGY);

        var options = new CampaignOptions();
        var updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc),
                        textCampaignInfo.getUid(),
                        UidAndClientId.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId()),
                        options);

        updateOperation.apply();
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaignsMap(textCampaignInfo.getShard(),
                        List.of(textCampaignInfo.getId())).get(textCampaignInfo.getId());
        assertThat(actualCampaign.getStrategy().getStrategyData().getLastBidderRestartTime()).isNull();
    }
}
