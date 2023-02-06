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
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.result.MassResult;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignAddOperationStrategyRestartTimeTest {

    public static final int GOAL_ID_FIRST = 124;
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

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
        metrikaClientStub.addUserCounter(defaultUser.getUid(), COUNTER_ID);
        metrikaClientStub.addCounterGoal(COUNTER_ID, GOAL_ID_FIRST);
    }

    @Test
    public void addCampaignWithConversionStrategy_AddRestartTime() {
        steps.featureSteps().addClientFeature(defaultUser.getClientId(),
                FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED, true);

        TextCampaign textCampaign = defaultTextCampaign()
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withMetrikaCounters(List.of((long) COUNTER_ID))
                .withStrategy((DbStrategy) defaultAverageCpaStrategy((long) GOAL_ID_FIRST)
                        .withAutobudget(CampaignsAutobudget.YES));

        LocalDateTime now = now().minusSeconds(1);
        RestrictedCampaignsAddOperation addOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(List.of(textCampaign),
                        defaultUser.getUid(), UidAndClientId.of(defaultUser.getUid(), defaultUser.getClientId()),
                        new CampaignOptions());

        MassResult<Long> result = addOperation.prepareAndApply();
        Long id = result.get(0).getResult();
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaignsMap(defaultUser.getShard(),
                        List.of(id)).get(id);
        assertThat(actualCampaign.getStrategy().getStrategyData().getLastBidderRestartTime()).isAfterOrEqualTo(now);
    }

    @Test
    public void addCampaignWithConversionStrategy_FeatureDisabled_RestartTimeNotAdded() {
        TextCampaign textCampaign = defaultTextCampaign()
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withMetrikaCounters(List.of((long) COUNTER_ID))
                .withStrategy((DbStrategy) defaultAverageCpaStrategy((long) GOAL_ID_FIRST)
                        .withAutobudget(CampaignsAutobudget.YES));

        LocalDateTime now = now().minusSeconds(1);
        RestrictedCampaignsAddOperation addOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(List.of(textCampaign),
                        defaultUser.getUid(), UidAndClientId.of(defaultUser.getUid(), defaultUser.getClientId()),
                        new CampaignOptions());

        MassResult<Long> result = addOperation.prepareAndApply();
        Long id = result.get(0).getResult();
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaignsMap(defaultUser.getShard(),
                        List.of(id)).get(id);
        assertThat(actualCampaign.getStrategy().getStrategyData().getLastBidderRestartTime()).isNull();
    }

}
