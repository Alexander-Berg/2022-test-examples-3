package ru.yandex.direct.core.entity.autobudget.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.CpaAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetCpaAlertRepository;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithNetworkSettings;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.model0.BroadmatchFlag;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.context.ContextLimitType;
import ru.yandex.direct.core.testing.steps.campaign.model0.context.ContextSettings;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAlertServiceGetIdsToFreezeTest {
    private static final Boolean DEFAULT_BROAD_MATCH_FLAG = true;
    private static final Integer DEFAULT_BROAD_MATCH_LIMIT = 10;

    @Autowired
    private AutobudgetAlertService autobudgetAlertService;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private AutobudgetHourlyAlertRepository autobudgetHourlyAlertRepository;

    @Autowired
    private AutobudgetCpaAlertRepository autobudgetCpaAlertRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    private ClientId clientId;
    private Long campaignId;
    private int shard;

    @Before
    public void before() {
        ContextSettings cs = new ContextSettings()
                .withLimitType(ContextLimitType.MANUAL)
                .withLimit(10)
                .withPriceCoeff(50);
        Campaign campaign = TestCampaigns.activeTextCampaign(null, null)
                .withBroadmatchFlag(BroadmatchFlag.YES)
                .withBroadMatchLimit(DEFAULT_BROAD_MATCH_LIMIT)
                .withContextSettings(cs);
        CampaignInfo campaignInfo = campaignSteps.createCampaign(campaign);
        clientId = campaignInfo.getClientId();
        campaignId = campaignInfo.getCampaignId();
        shard = campaignInfo.getShard();
    }

    @Test
    public void getCampaignsToFreezeAlertsOnStrategyChange_success() {
        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);

        addSimpleAlert();

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnStrategyChange(clientId,
                Set.of(getAppliedChangesWithStrategyChange(strategy)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), hasSize(1));
        assertThat(info.getCampaignsToFreezeHourlyAlerts(), contains(campaignId));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnStrategyChange_NoHints() {
        DbStrategy newStrategy = new DbStrategy();
        newStrategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP);

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnStrategyChange(clientId,
                Set.of(getAppliedChangesWithStrategyChange(newStrategy)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), is(empty()));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnStrategyChange_NoAutobudget() {
        DbStrategy newStrategy = new DbStrategy();
        newStrategy.setAutobudget(CampaignsAutobudget.NO);

        addSimpleAlert();

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnStrategyChange(clientId,
                Set.of(getAppliedChangesWithStrategyChange(newStrategy)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), is(empty()));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnStrategyChange_NoChanges() {
        TextCampaign campaign = getCampaign();

        addSimpleAlert();

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnStrategyChange(clientId,
                Set.of(getAppliedChangesWithStrategyChange(campaign.getStrategy())));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), is(empty()));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnStrategyChange_BidChange() {
        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        strategy.setStrategyData(new StrategyData().withBid(BigDecimal.valueOf(1.0)));

        addSimpleAlert();

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnStrategyChange(clientId,
                Set.of(getAppliedChangesWithStrategyChange(strategy)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), hasSize(1));
        assertThat(info.getCampaignsToFreezeHourlyAlerts(), contains(campaignId));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnStrategyChange_CpaChange() {
        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setStrategyName(StrategyName.MIN_PRICE);
        strategy.setStrategyData(new StrategyData().withAvgCpa(BigDecimal.valueOf(1.0)));

        autobudgetCpaAlertRepository.addAlerts(shard,
                Set.of(new CpaAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withApcDeviation(1L)
                        .withCpaDeviation(1L)));
        addSimpleAlert();

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnStrategyChange(clientId,
                Set.of(getAppliedChangesWithStrategyChange(strategy)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), hasSize(1));
        assertThat(info.getCampaignsToFreezeHourlyAlerts(), contains(campaignId));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), hasSize(1));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), contains(campaignId));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnBroadMatchChange_success() {
        addSimpleAlert(AutobudgetHourlyProblem.MAX_BID_REACHED);

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnBroadMatchChange(clientId,
                Set.of(getAppliedChangesWithBroadMatchChange()));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), hasSize(1));
        assertThat(info.getCampaignsToFreezeHourlyAlerts(), contains(campaignId));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnBroadMatchChange_NoHints() {
        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnBroadMatchChange(clientId,
                Set.of(getAppliedChangesWithBroadMatchChange()));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), is(empty()));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnBroadMatchChange_NoChanges() {
        addSimpleAlert(AutobudgetHourlyProblem.MAX_BID_REACHED);

        TextCampaign campaign = getCampaign();
        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnBroadMatchChange(clientId,
                Set.of(getAppliedChangesWithBroadMatchChange(campaign.getBroadMatch())));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), is(empty()));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnBroadMatchChange_newLimitIsLowerThanOld() {
        addSimpleAlert(AutobudgetHourlyProblem.MAX_BID_REACHED);

        BroadMatch broadMatch = new BroadMatch()
                .withBroadMatchLimit(DEFAULT_BROAD_MATCH_LIMIT - 1)
                .withBroadMatchFlag(DEFAULT_BROAD_MATCH_FLAG);
        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnBroadMatchChange(clientId,
                Set.of(getAppliedChangesWithBroadMatchChange(broadMatch)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), is(empty()));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignToFreezeAlertsOnBroadMatchChange_newLimitMinusOne() {
        addSimpleAlert(AutobudgetHourlyProblem.MAX_BID_REACHED);

        BroadMatch broadMatch = new BroadMatch()
                .withBroadMatchLimit(-1)
                .withBroadMatchFlag(DEFAULT_BROAD_MATCH_FLAG);
        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnBroadMatchChange(clientId,
                Set.of(getAppliedChangesWithBroadMatchChange(broadMatch)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), hasSize(1));
        assertThat(info.getCampaignsToFreezeHourlyAlerts(), contains(campaignId));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignToFreezeAlertsOnBroadMatchChange_NoBroadMatch() {
        addSimpleAlert(AutobudgetHourlyProblem.MAX_BID_REACHED);

        BroadMatch broadMatch = new BroadMatch()
                .withBroadMatchLimit(DEFAULT_BROAD_MATCH_LIMIT + 10)
                .withBroadMatchFlag(false);
        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnBroadMatchChange(clientId,
                Set.of(getAppliedChangesWithBroadMatchChange(broadMatch)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), hasSize(1));
        assertThat(info.getCampaignsToFreezeHourlyAlerts(), contains(campaignId));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignToFreezeAlertsOnBroadMatchChange_NoTypedHints() {
        addSimpleAlert(AutobudgetHourlyProblem.ENGINE_MIN_COST_LIMITED);

        BroadMatch broadMatch = new BroadMatch()
                .withBroadMatchLimit(DEFAULT_BROAD_MATCH_LIMIT + 10)
                .withBroadMatchFlag(false);
        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnBroadMatchChange(clientId,
                Set.of(getAppliedChangesWithBroadMatchChange(broadMatch)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), is(empty()));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnContextLimitChange_success() {
        addSimpleAlert(AutobudgetHourlyProblem.MAX_BID_REACHED);

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnContextLimitChange(clientId,
                Set.of(getAppliedChangesWithContextLimitChange()));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), hasSize(1));
        assertThat(info.getCampaignsToFreezeHourlyAlerts(), contains(campaignId));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnContextLimitChange_NewLimitIsLower() {
        addSimpleAlert(AutobudgetHourlyProblem.MAX_BID_REACHED);

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnContextLimitChange(clientId,
                Set.of(getAppliedChangesWithContextLimitChange(5)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), is(empty()));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    @Test
    public void getCampaignsToFreezeAlertsOnContextLimitChange_DisableNetwork() {
        addSimpleAlert(AutobudgetHourlyProblem.MAX_BID_REACHED);

        AlertsFreezeInfo info = autobudgetAlertService.getCampaignsToFreezeAlertsOnContextLimitChange(clientId,
                Set.of(getAppliedChangesWithContextLimitChange(254)));

        assertThat(info.getCampaignsToFreezeHourlyAlerts(), is(empty()));
        assertThat(info.getCampaignsToFreezeCpaAlerts(), is(empty()));
    }

    private TextCampaign getCampaign() {
        List<? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaigns(shard,
                Set.of(campaignId));
        List<TextCampaign> textCampaigns = mapList(typedCampaigns, TextCampaign.class::cast);
        return textCampaigns.get(0);
    }

    private void addSimpleAlert(AutobudgetHourlyProblem problem) {
        autobudgetHourlyAlertRepository.addAlerts(shard,
                Set.of(new HourlyAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withProblems(Set.of(problem))
                        .withOverdraft(1L)));
    }

    private void addSimpleAlert() {
        addSimpleAlert(AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED);
    }

    private AppliedChanges<TextCampaignWithCustomStrategy> getAppliedChangesWithStrategyChange(DbStrategy strategy) {
        TextCampaign campaign = getCampaign();

        ModelChanges<TextCampaignWithCustomStrategy> mc = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        mc.process(strategy, TextCampaignWithCustomStrategy.STRATEGY);
        return mc.applyTo(campaign);
    }

    private AppliedChanges<CampaignWithBroadMatch> getAppliedChangesWithBroadMatchChange() {
        BroadMatch broadMatch = new BroadMatch()
                .withBroadMatchFlag(DEFAULT_BROAD_MATCH_FLAG)
                .withBroadMatchLimit(DEFAULT_BROAD_MATCH_LIMIT + 10);
        return getAppliedChangesWithBroadMatchChange(broadMatch);
    }

    private AppliedChanges<CampaignWithBroadMatch> getAppliedChangesWithBroadMatchChange(BroadMatch broadMatch) {
        TextCampaign campaign = getCampaign();
        ModelChanges<CampaignWithBroadMatch> mc = new ModelChanges<>(campaignId, CampaignWithBroadMatch.class)
                .process(broadMatch, CampaignWithBroadMatch.BROAD_MATCH);
        return mc.applyTo(campaign);
    }

    private AppliedChanges<CampaignWithNetworkSettings> getAppliedChangesWithContextLimitChange() {
        return getAppliedChangesWithContextLimitChange(20);
    }

    private AppliedChanges<CampaignWithNetworkSettings> getAppliedChangesWithContextLimitChange(int contextLimit) {
        TextCampaign campaign = getCampaign();

        ModelChanges<CampaignWithNetworkSettings> mc = new ModelChanges<>(campaignId,
                CampaignWithNetworkSettings.class);
        mc.process(contextLimit, CampaignWithNetworkSettings.CONTEXT_LIMIT);
        return mc.applyTo(campaign);
    }
}
