package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.result.MassResult;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetCrrStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualContextStrategy;
import static ru.yandex.direct.feature.FeatureName.CRR_STRATEGY_ALLOWED;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignStrategyServiceLastBidderRestartTimeUpdateTest {
    public static final int GOAL_ID_FIRST = 193;
    public static final int GOAL_ID_SECOND = 201;
    public static final int COUNTER_ID = 209;

    @Autowired
    public Steps steps;

    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public CampaignStrategyService campaignStrategyService;

    @Autowired
    public MetrikaClientStub metrikaClientStub;

    private UserInfo defaultUser;
    private TextCampaignInfo conversionTextCampaignInfo;
    private LocalDateTime now;

    @Before
    public void before() {
        now = now();
        defaultUser = steps.userSteps().createDefaultUser();
        metrikaClientStub.addUserCounter(defaultUser.getUid(), COUNTER_ID);
        metrikaClientStub.addCounterGoal(COUNTER_ID, GOAL_ID_FIRST);
        metrikaClientStub.addCounterGoal(COUNTER_ID, GOAL_ID_SECOND);
        DbStrategy strategy = defaultAutobudgetStrategy((long) GOAL_ID_FIRST);
        strategy.getStrategyData().setLastBidderRestartTime(now.minusHours(1));
        TextCampaign textCampaign =
                defaultTextCampaignWithSystemFields(defaultUser.getClientInfo())
                        .withMetrikaCounters(List.of((long) COUNTER_ID))
                        .withStrategy(strategy)
                        .withIsSimplifiedStrategyViewEnabled(true);

        conversionTextCampaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), textCampaign);

        steps.featureSteps().addClientFeature(defaultUser.getClientId(),
                FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED, true);
    }

    @Test
    public void updateTextCampaignStrategy_ChangeStrategyToConversion_TimeAdd() {
        DbStrategy strategy = manualContextStrategy();
        strategy.getStrategyData().setLastBidderRestartTime(now.minusHours(1));
        TextCampaign textCampaign =
                defaultTextCampaignWithSystemFields(defaultUser.getClientInfo())
                        .withMetrikaCounters(List.of((long) COUNTER_ID))
                        .withStrategy(strategy)
                        .withIsSimplifiedStrategyViewEnabled(true);

        var notConversionTextCampaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), textCampaign);
        MassResult<Long> result =
                campaignStrategyService.updateTextCampaignStrategy(notConversionTextCampaignInfo.getId(),
                        defaultAutobudgetStrategy((long) GOAL_ID_FIRST),
                        notConversionTextCampaignInfo.getUid(),
                        UidAndClientId.of(notConversionTextCampaignInfo.getUid(),
                                notConversionTextCampaignInfo.getClientId()), false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(notConversionTextCampaignInfo.getShard(),
                        List.of(notConversionTextCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getStrategy().getStrategyData().getLastBidderRestartTime())
                .isNotNull();
    }

    @Test
    public void updateTextCampaignStrategy_ChangeGoalId_TimeReset() {
        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(conversionTextCampaignInfo.getId(),
                defaultAutobudgetStrategy((long) GOAL_ID_SECOND),
                conversionTextCampaignInfo.getUid(),
                UidAndClientId.of(conversionTextCampaignInfo.getUid(), conversionTextCampaignInfo.getClientId()),
                false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(conversionTextCampaignInfo.getShard(),
                        List.of(conversionTextCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getStrategy().getStrategyData().getLastBidderRestartTime())
                .isAfter(now.minusSeconds(1));
    }

    @Test
    public void updateTextCampaignStrategy_ChangeStrategyModelFromConversionToRevenue_TimeReset() {
        steps.featureSteps().addClientFeature(defaultUser.getClientId(), CRR_STRATEGY_ALLOWED, true);
        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(conversionTextCampaignInfo.getId(),
                defaultAutobudgetCrrStrategy(GOAL_ID_FIRST),
                conversionTextCampaignInfo.getUid(),
                UidAndClientId.of(conversionTextCampaignInfo.getUid(), conversionTextCampaignInfo.getClientId()),
                false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(conversionTextCampaignInfo.getShard(),
                        List.of(conversionTextCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getStrategy().getStrategyData().getLastBidderRestartTime())
                .isAfter(now.minusSeconds(1));
    }

    @Test
    public void updateTextCampaignStrategy_AttributionModelChanged_TimeReset() {
        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(conversionTextCampaignInfo.getId(),
                defaultAutobudgetStrategy((long) GOAL_ID_FIRST),
                conversionTextCampaignInfo.getUid(),
                UidAndClientId.of(conversionTextCampaignInfo.getUid(), conversionTextCampaignInfo.getClientId()), true);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(conversionTextCampaignInfo.getShard(),
                        List.of(conversionTextCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getStrategy().getStrategyData().getLastBidderRestartTime())
                .isAfter(now.minusSeconds(1));
    }

    @Test
    public void updateTextCampaignStrategy_OtherFiledChanged_TimeNotReset() {
        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(conversionTextCampaignInfo.getId(),
                defaultAutobudgetStrategy((long) GOAL_ID_FIRST),
                conversionTextCampaignInfo.getUid(),
                UidAndClientId.of(conversionTextCampaignInfo.getUid(), conversionTextCampaignInfo.getClientId()),
                false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(conversionTextCampaignInfo.getShard(),
                        List.of(conversionTextCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getStrategy().getStrategyData().getLastBidderRestartTime())
                .isBefore(now);
    }

    @Test
    public void updateTextCampaign_ChangeStrategyToCrr_TimeAdd() {
        steps.featureSteps().addClientFeature(defaultUser.getClientId(), CRR_STRATEGY_ALLOWED, true);
        var strategy = manualContextStrategy();
        var textCampaign =
                defaultTextCampaignWithSystemFields(defaultUser.getClientInfo())
                        .withMetrikaCounters(List.of((long) COUNTER_ID))
                        .withStrategy(strategy)
                        .withIsSimplifiedStrategyViewEnabled(true);

        var campaign = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), textCampaign);

        MassResult<Long> result =
                campaignStrategyService.updateTextCampaignStrategy(campaign.getId(),
                        defaultAutobudgetCrrStrategy(GOAL_ID_FIRST),
                        campaign.getUid(),
                        UidAndClientId.of(campaign.getUid(), campaign.getClientId()), false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(campaign.getShard(),
                        List.of(campaign.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getStrategy().getStrategyData().getLastBidderRestartTime())
                .isNotNull();
    }

    @Test
    public void updateTextCampaignStrategy_UpdateCrrStrategy_TimeReset() {
        steps.featureSteps().addClientFeature(defaultUser.getClientId(), CRR_STRATEGY_ALLOWED, true);

        var strategy = defaultAutobudgetCrrStrategy(GOAL_ID_FIRST);
        strategy.getStrategyData().setLastBidderRestartTime(now.minusHours(1));
        var textCampaign =
                defaultTextCampaignWithSystemFields(defaultUser.getClientInfo())
                        .withMetrikaCounters(List.of((long) COUNTER_ID))
                        .withStrategy(strategy)
                        .withIsSimplifiedStrategyViewEnabled(true);

        var campaign = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), textCampaign);
        var lastUpdate = (campaign.getTypedCampaign()).getStrategy().getStrategyData().getLastBidderRestartTime();
        assertThat(lastUpdate).isNotNull();

        MassResult<Long> result =
                campaignStrategyService.updateTextCampaignStrategy(campaign.getId(),
                        defaultAutobudgetCrrStrategy(GOAL_ID_SECOND),
                        campaign.getUid(),
                        UidAndClientId.of(campaign.getUid(), campaign.getClientId()), false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(campaign.getShard(),
                        List.of(campaign.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getStrategy().getStrategyData().getLastBidderRestartTime())
                .isAfter(lastUpdate);
    }

}
