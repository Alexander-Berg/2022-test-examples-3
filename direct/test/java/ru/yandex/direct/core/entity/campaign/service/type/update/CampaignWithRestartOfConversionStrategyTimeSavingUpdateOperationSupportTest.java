package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithRestartOfConversionStrategyTimeSaving;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetCrrStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetRoiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;

@RunWith(MockitoJUnitRunner.class)
public class CampaignWithRestartOfConversionStrategyTimeSavingUpdateOperationSupportTest {

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private CampaignWithRestartOfConversionStrategyTimeSavingUpdateOperationSupport support;
    private RestrictedCampaignsUpdateOperationContainer updateParameters;
    private long campaignId;
    private CampaignWithRestartOfConversionStrategyTimeSaving campaign;
    private long goalId;

    @Before
    public void before() {
        int shard = 1;
        Long uid = 2L;
        ClientId clientId = ClientId.fromLong(3L);
        updateParameters = RestrictedCampaignsUpdateOperationContainer.create(
                shard,
                uid,
                clientId,
                uid,
                uid);

        campaign = new TextCampaign();
        goalId = 123;
        campaignId = RandomNumberUtils.nextPositiveLong();
        campaign.withId(campaignId)
                .withStrategy(defaultAverageCpaStrategy(goalId))
                .withAttributionModel(CampaignAttributionModel.FIRST_CLICK);
    }

    @Test
    public void changeGoalId_FeatureDisabled_RestartTimeNotUpdate() {
        doReturn(false)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        mc.process(defaultAverageCpaStrategy(goalId + 1), CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY);
        var ac = mc.applyTo(campaign);
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime()).isNull();
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();
    }

    @Test
    public void changeGoalId_RestartTimeUpdate() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        mc.process(defaultAverageCpaStrategy(goalId + 1), CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY);
        var ac = mc.applyTo(campaign);
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime()).isNotNull();
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();

    }

    @Test
    public void changeStrategyFromRoi_RestartTimeUpdate() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        mc.process(defaultAverageCpaStrategy(goalId), CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY);
        var ac = mc.applyTo(campaign.withStrategy(defaultAutobudgetRoiStrategy(goalId)));
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime()).isNotNull();
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();
    }

    @Test
    //не хотим проставлять LastBidderRestartTime при любом изменении кампании,
    //если до этого LastBidderRestartTime был не установлен
    public void changeAvgCpa_oldRestartTimeIsNull_RestartTimeNotUpdate() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        DbStrategy newStrategy = defaultAverageCpaStrategy(goalId);
        newStrategy.getStrategyData().setAvgCpa(newStrategy.getStrategyData().getAvgCpa().add(BigDecimal.TEN));
        mc.process(newStrategy, CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY);
        var ac = mc.applyTo(campaign);
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime()).isNull();
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();

    }

    @Test
    public void changeAvgCpa_oldRestartTimeIsNotNull_RestartTimeNotUpdate() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        LocalDateTime now = LocalDateTime.now();
        campaign.getStrategy().getStrategyData().setLastBidderRestartTime(now);
        DbStrategy newStrategy = defaultAverageCpaStrategy(goalId);
        newStrategy.getStrategyData().setAvgCpa(newStrategy.getStrategyData().getAvgCpa().add(BigDecimal.TEN));
        mc.process(newStrategy, CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY);
        var ac = mc.applyTo(campaign);
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime()).isEqualTo(now);
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();

    }

    @Test
    public void changeAttributionModel_RestartTimeUpdate() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        mc.process(CampaignAttributionModel.LAST_CLICK,
                CampaignWithRestartOfConversionStrategyTimeSaving.ATTRIBUTION_MODEL);
        var ac = mc.applyTo(campaign);
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime())
                .isNotNull();
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();

    }

    @Test
    public void changeStrategyFromRevenueToConversion_RestartTimeUpdate() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        mc.process(defaultAverageCpaStrategy(goalId), CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY);
        var ac = mc.applyTo(campaign.withStrategy(defaultAutobudgetCrrStrategy(goalId)));
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime()).isNotNull();
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();
    }

    @Test
    public void changeStrategyFromConversionToRevenue_RestartTimeUpdate() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        mc.process(defaultAutobudgetCrrStrategy(goalId), CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY);
        var ac = mc.applyTo(campaign.withStrategy(defaultAverageCpaStrategy(goalId)));
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime()).isNotNull();
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();
    }

    @Test
    public void changeStrategyFromConversionToConversion_NoRestartTimeUpdate() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        mc.process(defaultAutobudgetStrategy(goalId), CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY);
        var ac = mc.applyTo(campaign.withStrategy(defaultAverageCpaStrategy(goalId)));
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime()).isNull();
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();
    }

    @Test
    public void changeStrategyFromRevenue_NoRestartTimeSet() {
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(updateParameters.getClientId()),
                        eq(FeatureName.CONVERSION_STRATEGY_LEARNING_STATUS_ENABLED));

        var mc = new ModelChanges<>(campaignId, CampaignWithRestartOfConversionStrategyTimeSaving.class);
        mc.process(defaultAutobudgetRoiStrategy(goalId), CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY);
        var ac = mc.applyTo(campaign.withStrategy(defaultAutobudgetCrrStrategy(goalId)));
        support.onAppliedChangesValidated(updateParameters, List.of(ac));

        assertThat(ac.getModel().getStrategy().getStrategyData().getLastBidderRestartTime()).isNull();
        assertThat(ac.changed(CampaignWithRestartOfConversionStrategyTimeSaving.STRATEGY)).isTrue();
    }


}
