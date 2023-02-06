package ru.yandex.direct.core.entity.autobudget.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBroadMatch;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestAutobudgetAlerts;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAlertServiceFreezeAlertsTest {

    @Autowired
    private AutobudgetAlertService autobudgetAlertService;

    @Autowired
    private AutobudgetHourlyAlertRepository alertRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    private CampaignInfo campaignInfo;
    private ClientId clientId;
    private Long campaignId;
    private int shard;

    private static final boolean DEFAULT_BROAD_MATCH_FLAG = true;
    private static final int DEFAULT_BROAD_MATCH_LIMIT = 10;

    @Before
    public void before() {
        campaignInfo = campaignSteps.createActiveTextCampaign();
        clientId = campaignInfo.getClientId();
        campaignId = campaignInfo.getCampaignId();
        shard = campaignInfo.getShard();
    }

    @Test
    public void freezeAlertsOnKeywordsChange_ProblemIsSuitableForFreezing_AlertIsFrozen() {
        HourlyAutobudgetAlert alert = TestAutobudgetAlerts.defaultActiveHourlyAlert(campaignId)
                .withProblems(Set.of(AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        alertRepository.addAlerts(shard, Set.of(alert));

        autobudgetAlertService.freezeAlertsOnKeywordsChange(clientId, Set.of(campaignId));

        Map<Long, HourlyAutobudgetAlert> actualAlerts = alertRepository.getAlerts(shard, Set.of(campaignId));
        assertThat("Предупреждение должно быть заморожено",
                actualAlerts.get(campaignId).getStatus(), is(AutobudgetCommonAlertStatus.FROZEN));
    }

    @Test
    public void freezeAlertsOnKeywordsChange_ProblemIsNotSuitableForFreezing_AlertIsNotFrozen() {
        HourlyAutobudgetAlert alert = TestAutobudgetAlerts.defaultActiveHourlyAlert(campaignId)
                .withProblems(Set.of(AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        alertRepository.addAlerts(shard, Set.of(alert));
        autobudgetAlertService.freezeAlertsOnKeywordsChange(clientId, Set.of(campaignId));

        Map<Long, HourlyAutobudgetAlert> actualAlerts = alertRepository.getAlerts(shard, Set.of(campaignId));
        assertThat("Предупреждение не должно быть заморожено",
                actualAlerts.get(campaignId).getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));
    }

    @Test
    public void freezeAlertsOnKeywordsChange_NoProblem_DontFail() {
        autobudgetAlertService.freezeAlertsOnKeywordsChange(clientId, Set.of(campaignId));

        Map<Long, HourlyAutobudgetAlert> alerts = alertRepository.getAlerts(shard, Set.of(campaignId));
        assertThat(alerts.get(campaignId), is(nullValue()));
    }

    @Test
    public void freezeAlertsOnKeywordsChange_OneProblemIsSuitableForFreezingAndSecondDoestExist_AlertIsFrozen() {
        Long campaignId2 = createActiveCampaign();

        HourlyAutobudgetAlert alert = TestAutobudgetAlerts.defaultActiveHourlyAlert(campaignId)
                .withProblems(Set.of(AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        alertRepository.addAlerts(shard, Set.of(alert));

        autobudgetAlertService.freezeAlertsOnKeywordsChange(clientId, Set.of(campaignId, campaignId2));

        Map<Long, HourlyAutobudgetAlert> actualAlerts = alertRepository.getAlerts(shard, Set.of(campaignId));
        assertThat("Предупреждение должно быть заморожено",
                actualAlerts.get(campaignId).getStatus(), is(AutobudgetCommonAlertStatus.FROZEN));
    }

    @Test
    public void freezeAlertsOnKeywordsChange_TwoSuitableForFreezingProblems_BothAlertsIsFrozen() {
        Long campaignId2 = createActiveCampaign();

        HourlyAutobudgetAlert alert1 = TestAutobudgetAlerts.defaultActiveHourlyAlert(campaignId)
                .withProblems(Set.of(AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);

        HourlyAutobudgetAlert alert2 = TestAutobudgetAlerts.defaultActiveHourlyAlert(campaignId2)
                .withProblems(Set.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);

        alertRepository.addAlerts(shard, Set.of(alert1, alert2));
        autobudgetAlertService.freezeAlertsOnKeywordsChange(clientId, Set.of(campaignId, campaignId2));

        Map<Long, HourlyAutobudgetAlert> actualAlerts =
                alertRepository.getAlerts(shard, Set.of(campaignId, campaignId2));
        assertThat("Предупреждение должно быть заморожено",
                actualAlerts.get(campaignId).getStatus(), is(AutobudgetCommonAlertStatus.FROZEN));
        assertThat("Предупреждение должно быть заморожено",
                actualAlerts.get(campaignId2).getStatus(), is(AutobudgetCommonAlertStatus.FROZEN));
    }

    @Test
    public void freezeAlertsOnKeywordsChange_OneProblemIsSuitableAndOneIsNotNotSuitableForFreezing_OneAlertIsFrozen() {
        Long campaignId2 = createActiveCampaign();

        HourlyAutobudgetAlert alert1 = TestAutobudgetAlerts.defaultActiveHourlyAlert(campaignId)
                .withProblems(Set.of(AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);

        HourlyAutobudgetAlert alert2 = TestAutobudgetAlerts.defaultActiveHourlyAlert(campaignId2)
                .withProblems(Set.of(AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);

        alertRepository.addAlerts(shard, Set.of(alert1, alert2));
        autobudgetAlertService.freezeAlertsOnKeywordsChange(clientId, Set.of(campaignId, campaignId2));

        Map<Long, HourlyAutobudgetAlert> actualAlerts =
                alertRepository.getAlerts(shard, Set.of(campaignId, campaignId2));
        assertThat("Предупреждение UPPER_POSITIONS_REACHED должно быть заморожено",
                actualAlerts.get(campaignId).getStatus(), is(AutobudgetCommonAlertStatus.FROZEN));
        assertThat("Предупреждение WALLET_DAILY_BUDGET_REACHED не должно быть заморожено",
                actualAlerts.get(campaignId2).getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));
    }

    @Test
    public void freezeAlertsOnBroadMatchChange_ChangesAreNotSuitableForFreezing_OneAlertIsNotFrozen() {
        TextCampaign campaign = createActiveCampaignBroadMatch();
        Long cid = campaign.getId();

        HourlyAutobudgetAlert alert = TestAutobudgetAlerts.defaultActiveHourlyAlert(cid)
                .withProblems(Set.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        assertThat("Предупреждение не заморожено", alert.getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));
        alertRepository.addAlerts(shard, Set.of(alert));

        autobudgetAlertService.freezeAlertsOnBroadMatchChange(campaignInfo.getClientId(),
                getAppliedChangesWithBroadMatchChangeSet(cid, getBroadMatch(DEFAULT_BROAD_MATCH_FLAG, 5)));

        var actualAlert = alertRepository.getAlerts(shard, Set.of(cid)).get(cid);
        assertThat("Предупреждение не заморожено", actualAlert.getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));
    }

    @Test
    public void freezeAlertsOnBroadMatchChange_ChangesAreSuitableForFreezing_OneAlertIsFrozen() {
        TextCampaign campaign = createActiveCampaignBroadMatch();
        Long cid = campaign.getId();

        HourlyAutobudgetAlert alert = TestAutobudgetAlerts.defaultActiveHourlyAlert(cid)
                .withProblems(Set.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        assertThat("Предупреждение не заморожено", alert.getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));
        alertRepository.addAlerts(shard, Set.of(alert));

        autobudgetAlertService.freezeAlertsOnBroadMatchChange(campaignInfo.getClientId(),
                getAppliedChangesWithBroadMatchChangeSet(cid, getBroadMatch(false, DEFAULT_BROAD_MATCH_LIMIT)));

        var actualAlert = alertRepository.getAlerts(shard, Set.of(cid)).get(cid);
        assertThat("Предупреждение должно быть заморожено", actualAlert.getStatus(),
                is(AutobudgetCommonAlertStatus.FROZEN));
    }

    @Test
    public void freezeAlertsOnBroadMatchChange_ChangesAreSuitableForFreezing_BothAlertsAreFrozen() {
        TextCampaign campaign1 = createActiveCampaignBroadMatch();
        Long cid1 = campaign1.getId();

        HourlyAutobudgetAlert alert1 = TestAutobudgetAlerts.defaultActiveHourlyAlert(cid1)
                .withProblems(Set.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        assertThat("Предупреждение MAX_BID_REACHED не заморожено", alert1.getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));

        var campaignChanges1 = getAppliedChangesWithBroadMatchChange(cid1,
                getBroadMatch(DEFAULT_BROAD_MATCH_FLAG, -1));


        TextCampaign campaign2 = createActiveCampaignBroadMatch();
        Long cid2 = campaign2.getId();

        HourlyAutobudgetAlert alert2 = TestAutobudgetAlerts.defaultActiveHourlyAlert(cid2)
                .withProblems(Set.of(AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        assertThat("Предупреждение UPPER_POSITIONS_REACHED не заморожено", alert2.getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));

        var campaignChanges2 = getAppliedChangesWithBroadMatchChange(cid2,
                getBroadMatch(DEFAULT_BROAD_MATCH_FLAG, DEFAULT_BROAD_MATCH_LIMIT + 10)); // больше, чем было


        alertRepository.addAlerts(shard, Set.of(alert1, alert2));
        autobudgetAlertService.freezeAlertsOnBroadMatchChange(campaignInfo.getClientId(),
                Set.of(campaignChanges1, campaignChanges2));

        var actualAlerts = alertRepository.getAlerts(shard, Set.of(cid1, cid2));
        assertThat("Предупреждение MAX_BID_REACHED должно быть заморожено", actualAlerts.get(cid1).getStatus(),
                is(AutobudgetCommonAlertStatus.FROZEN));
        assertThat("Предупреждение UPPER_POSITIONS_REACHED должно быть заморожено", actualAlerts.get(cid2).getStatus(),
                is(AutobudgetCommonAlertStatus.FROZEN));
    }

    @Test
    public void freezeAlertsOnBroadMatchChange_ChangesAreSuitableForFreezing_OnlyOneFrozen() {
        TextCampaign campaign1 = createActiveCampaignBroadMatch();
        Long cid1 = campaign1.getId();

        HourlyAutobudgetAlert alert1 = TestAutobudgetAlerts.defaultActiveHourlyAlert(cid1)
                .withProblems(Set.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        assertThat("Предупреждение MAX_BID_REACHED не заморожено", alert1.getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));

        var campaignChanges1 = getAppliedChangesWithBroadMatchChange(cid1,
                getBroadMatch(DEFAULT_BROAD_MATCH_FLAG, -1));

        TextCampaign campaign2 = createActiveCampaignBroadMatch();
        Long cid2 = campaign2.getId();

        HourlyAutobudgetAlert alert2 = TestAutobudgetAlerts.defaultActiveHourlyAlert(cid2)
                .withProblems(Set.of(AutobudgetHourlyProblem.ENGINE_MIN_COST_LIMITED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        assertThat("Предупреждение ENGINE_MIN_COST_LIMITED не заморожено", alert2.getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));

        var campaignChanges2 = getAppliedChangesWithBroadMatchChange(cid2,
                getBroadMatch(DEFAULT_BROAD_MATCH_FLAG, DEFAULT_BROAD_MATCH_LIMIT + 10)); // больше, чем было


        alertRepository.addAlerts(shard, Set.of(alert1, alert2));
        autobudgetAlertService.freezeAlertsOnBroadMatchChange(campaignInfo.getClientId(),
                Set.of(campaignChanges1, campaignChanges2));

        var actualAlerts = alertRepository.getAlerts(shard, Set.of(cid1, cid2));
        assertThat("Предупреждение MAX_BID_REACHED должно быть заморожено", actualAlerts.get(cid1).getStatus(),
                is(AutobudgetCommonAlertStatus.FROZEN));
        assertThat("Предупреждение ENGINE_MIN_COST_LIMITED не должно быть заморожено", actualAlerts.get(cid2).getStatus(),
                is(AutobudgetCommonAlertStatus.ACTIVE));
    }

    private TextCampaign createActiveCampaignBroadMatch() {
        var info = campaignSteps.createActiveTextCampaignAutoStrategy(campaignInfo.getClientInfo());
        var cid = info.getCampaignId();
        var uid = info.getUid();
        var clientId = info.getClientId();
        var shard = info.getShard();

        var defaultBroadMatch = getBroadMatch(DEFAULT_BROAD_MATCH_FLAG, DEFAULT_BROAD_MATCH_LIMIT);
        RestrictedCampaignsUpdateOperationContainer updateParameters = RestrictedCampaignsUpdateOperationContainer.create(
                shard, uid, clientId, uid, uid);
        campaignModifyRepository.updateCampaigns(updateParameters,
                getAppliedChangesWithBroadMatchChangeSet(cid, defaultBroadMatch));

        return getCampaign(cid);
    }

    private Long createActiveCampaign() {
        return campaignSteps.createActiveCampaign(campaignInfo.getClientInfo()).getCampaignId();
    }

    private BroadMatch getBroadMatch(boolean flag, int limit) {
        return new BroadMatch()
                .withBroadMatchFlag(flag)
                .withBroadMatchLimit(limit);
    }

    private TextCampaign getCampaign(Long campaignId) {
        List<? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaigns(shard,
                Set.of(campaignId));
        List<TextCampaign> textCampaigns = mapList(typedCampaigns, TextCampaign.class::cast);
        return textCampaigns.get(0);
    }

    private Set<AppliedChanges<CampaignWithBroadMatch>> getAppliedChangesWithBroadMatchChangeSet(
            Long campaignId, BroadMatch broadMatch) {
        return Set.of(getAppliedChangesWithBroadMatchChange(campaignId, broadMatch));
    }

    private AppliedChanges<CampaignWithBroadMatch> getAppliedChangesWithBroadMatchChange(
            Long campaignId, BroadMatch broadMatch) {
        TextCampaign campaign = getCampaign(campaignId);

        ModelChanges<CampaignWithBroadMatch> mc = new ModelChanges<>(campaignId, CampaignWithBroadMatch.class);
        mc.process(broadMatch, CampaignWithBroadMatch.BROAD_MATCH);
        return mc.applyTo(campaign);
    }
}
