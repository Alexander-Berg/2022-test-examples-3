package ru.yandex.direct.core.entity.autobudget.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualContextStrategy;

@CoreTest
@RunWith(SpringRunner.class)
public class AutobudgetAlertServiceFreezeAlertsOnStrategyChangeTest {

    @Autowired
    private AutobudgetAlertService autobudgetAlertService;

    @Autowired
    private AutobudgetHourlyAlertRepository autobudgetHourlyAlertRepository;

    @Autowired
    private AutobudgetCpaAlertRepository autobudgetCpaAlertRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    private CampaignInfo campaignInfo;
    private ClientId clientId;
    private Long campaignId;
    private int shard;

    @Before
    public void before() {
        campaignInfo = campaignSteps.createActiveTextCampaign();
        clientId = campaignInfo.getClientId();
        campaignId = campaignInfo.getCampaignId();
        shard = campaignInfo.getShard();
    }

    private TextCampaignWithCustomStrategy getCampaignWithCustomStrategy(int shard, Long cid) {
        return (TextCampaignWithCustomStrategy) campaignTypedRepository
                .getTypedCampaigns(shard, List.of(cid))
                .get(0);
    }

    @Test
    public void freezeAlertsOnStrategyChange_NoHintsNoFreezeTest() {
        List campaignIdAsList = Collections.singletonList(campaignId);

        TextCampaignWithCustomStrategy campaign = getCampaignWithCustomStrategy(shard, campaignId);

        ModelChanges<TextCampaignWithCustomStrategy> campaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        autobudgetAlertService.freezeAlertsOnStrategyChange(clientId,
                Collections.singletonList(campaignAppliedChanges));

        // ??????????????????, ?????? ?????????????? ?? ?????????? ?????????????????? ??????
        assertTrue(autobudgetCpaAlertRepository.getCidOfExistingAlerts(shard, campaignIdAsList).isEmpty());
        assertTrue(autobudgetHourlyAlertRepository.getCidOfExistingAlerts(shard, campaignIdAsList).isEmpty());
    }

    @Test
    public void freezeAlertsOnStrategyChange_NoAutobudgetNoFreezeTest() {
        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.NO);
        TextCampaignWithCustomStrategy campaign =
                getCampaignWithCustomStrategy(shard, campaignId).withStrategy(strategy);

        // ?????????????????? ?????????? ???????????????? ?????????? ?? ???????????????? autobudget_alerts
        autobudgetHourlyAlertRepository.addAlerts(shard,
                Collections.singletonList(new HourlyAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withProblems(Set.of(AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED))
                        .withOverdraft(1L)));

        ModelChanges<TextCampaignWithCustomStrategy> campaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        autobudgetAlertService.freezeAlertsOnStrategyChange(clientId,
                Collections.singletonList(campaignAppliedChanges));

        // ??????????????????, ?????? ?????????? ?????????????? ????????????????
        assertEquals(AutobudgetCommonAlertStatus.ACTIVE, autobudgetHourlyAlertRepository.getAlerts(shard,
                Collections.singletonList(campaignId)).get(campaignId).getStatus());
        // ??????????????????, ?????? ?????????????? ?? autobudget_cpa_alerts ???? ??????????????????
        assertTrue(autobudgetCpaAlertRepository.getCidOfExistingAlerts(shard, Collections.singletonList(campaignId)).isEmpty());
    }

    @Test
    public void freezeAlertsOnStrategyChange_NoStrategyChangeNoFreezeTest() {
        // ???????????????? ?? ?????????????????????????? ????????????????????
        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setStrategyData(new StrategyData());
        TextCampaignWithCustomStrategy campaign =
                getCampaignWithCustomStrategy(shard, campaignId).withStrategy(strategy);

        // ?????????????????? ?????????? ???????????????? ?????????? ?? ???????????????? autobudget_alerts
        autobudgetHourlyAlertRepository.addAlerts(shard,
                Collections.singletonList(new HourlyAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withProblems(Set.of(AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED))
                        .withOverdraft(1L)));

        ModelChanges<TextCampaignWithCustomStrategy> campaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        autobudgetAlertService.freezeAlertsOnStrategyChange(clientId,
                Collections.singletonList(campaignAppliedChanges));

        // ??????????????????, ?????? ?????????? ?????????????? ????????????????
        assertEquals(AutobudgetCommonAlertStatus.ACTIVE, autobudgetHourlyAlertRepository.getAlerts(shard,
                Collections.singletonList(campaignId)).get(campaignId).getStatus());
        // ??????????????????, ?????? ?????????????? ?? autobudget_cpa_alerts ???? ??????????????????
        assertTrue(autobudgetCpaAlertRepository.getCidOfExistingAlerts(shard, Collections.singletonList(campaignId)).isEmpty());
    }

    @Test
    public void freezeAlertsOnStrategyChange_StrategyChangeFreezeTest() {
        TextCampaignWithCustomStrategy campaign = getCampaignWithCustomStrategy(shard, campaignId);

        // ?????????????????? ?????????? ???????????????? ?????????? ?? ???????????????? autobudget_alerts
        autobudgetHourlyAlertRepository.addAlerts(shard,
                Collections.singletonList(new HourlyAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withProblems(Set.of(AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED))
                        .withOverdraft(1L)));

        // ???????????? ?????????????????? ???? ??????????????????????????
        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        ModelChanges<TextCampaignWithCustomStrategy> campaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        campaignModelChanges.process(strategy, TextCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        autobudgetAlertService.freezeAlertsOnStrategyChange(clientId,
                Collections.singletonList(campaignAppliedChanges));

        // ??????????????????, ?????? ?????????? ??????????????????????, ?????? ?????? ?????????????????? ??????????????????
        assertEquals(AutobudgetCommonAlertStatus.FROZEN, autobudgetHourlyAlertRepository.getAlerts(shard,
                Collections.singletonList(campaignId)).get(campaignId).getStatus());
        // ??????????????????, ?????? ?????????????? ?? autobudget_cpa_alerts ???? ??????????????????
        assertTrue(autobudgetCpaAlertRepository.getCidOfExistingAlerts(shard, Collections.singletonList(campaignId)).isEmpty());
    }

    @Test
    public void freezeAlertsOnStrategyChange_BidOrAvdBidChangeFreezeTest() {
        // ???????????????? ?? ?????????????????????????? ????????????????????
        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        strategy.setStrategyData(new StrategyData().withBid(BigDecimal.valueOf(1.0)));// ?????????????????? ????????????????, ??????????
        // ???????? ?????????????????? ???????? bid
        TextCampaignWithCustomStrategy campaign =
                getCampaignWithCustomStrategy(shard, campaignId).withStrategy(strategy);

        autobudgetHourlyAlertRepository.addAlerts(shard,
                Collections.singletonList(new HourlyAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withProblems(Set.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                        .withOverdraft(1L)));

        DbStrategy newStrategy = new DbStrategy();
        newStrategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        newStrategy.setAutobudget(CampaignsAutobudget.YES);
        newStrategy.setStrategyData(new StrategyData().withBid(BigDecimal.valueOf(2.0))); // ?????????????????? ????????????????,
        // ?????????? ???????? ??????????????????
        // ???????? bid
        ModelChanges<TextCampaignWithCustomStrategy> campaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        campaignModelChanges.process(newStrategy, TextCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        autobudgetAlertService.freezeAlertsOnStrategyChange(clientId,
                Collections.singletonList(campaignAppliedChanges));

        assertEquals(AutobudgetCommonAlertStatus.FROZEN, autobudgetHourlyAlertRepository.getAlerts(shard,
                Collections.singletonList(campaignId)).get(campaignId).getStatus());
        assertTrue(autobudgetCpaAlertRepository.getCidOfExistingAlerts(shard, Collections.singletonList(campaignId)).isEmpty());
    }

    @Test
    public void freezeAlertsOnStrategyChange_MiddleConventionTest() {
        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setStrategyName(StrategyName.MIN_PRICE);
        strategy.setStrategyData(new StrategyData().withAvgCpa(BigDecimal.valueOf(1.0))); // ?????????????????? ????????????????,
        // ?????????? ???????? ??????????????????
        // ???????? avgCpa
        TextCampaignWithCustomStrategy campaign =
                getCampaignWithCustomStrategy(shard, campaignId).withStrategy(strategy);

        autobudgetCpaAlertRepository.addAlerts(shard,
                Collections.singletonList(new CpaAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withApcDeviation(0L)
                        .withCpaDeviation(5555L)));

        autobudgetHourlyAlertRepository.addAlerts(shard,
                Collections.singletonList(new HourlyAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withProblems(Set.of(AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED))
                        .withOverdraft(1L)));

        DbStrategy newStrategy = new DbStrategy();
        newStrategy.setStrategyName(StrategyName.MIN_PRICE);
        newStrategy.setAutobudget(CampaignsAutobudget.YES);
        newStrategy.setStrategyData(new StrategyData().withAvgCpa(BigDecimal.valueOf(2.0)));// ?????????????????? ????????????????,
        // ?????????? ???????? ??????????????????
        // ???????? avgCpa
        ModelChanges<TextCampaignWithCustomStrategy> campaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        campaignModelChanges.process(newStrategy, TextCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        autobudgetAlertService.freezeAlertsOnStrategyChange(clientId,
                Collections.singletonList(campaignAppliedChanges));

        assertEquals(AutobudgetCommonAlertStatus.ACTIVE, autobudgetHourlyAlertRepository.getAlerts(shard,
                Collections.singletonList(campaignId)).get(campaignId).getStatus());
        assertEquals(AutobudgetCommonAlertStatus.FROZEN, autobudgetCpaAlertRepository.getAlerts(shard,
                Collections.singletonList(campaignId)).get(campaignId).getStatus());
    }

    @Test
    public void freezeAlertsOnStrategyChange_AFewCampaignsTest() {
        CampaignInfo secondCampaignInfo = campaignSteps.createActiveTextCampaign();
        TextCampaignWithCustomStrategy secondCampaign = getCampaignWithCustomStrategy(shard,
                secondCampaignInfo.getCampaignId());
        secondCampaign.setClientId(clientId.asLong());
        TextCampaignWithCustomStrategy firstCampaign = getCampaignWithCustomStrategy(shard, campaignId);

        // alert ?????? firstCampaign
        autobudgetHourlyAlertRepository.addAlerts(shard,
                Collections.singletonList(new HourlyAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withProblems(Set.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                        .withOverdraft(1L)));
        // ?????? secondCampaign ?????????????? ???? ??????????

        ModelChanges<TextCampaignWithCustomStrategy> firstCampaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        ModelChanges<TextCampaignWithCustomStrategy> secondCampaignModelChanges =
                new ModelChanges<>(secondCampaign.getId(),
                TextCampaignWithCustomStrategy.class);
        AppliedChanges<TextCampaignWithCustomStrategy> firstCampaignAppliedChanges =
                firstCampaignModelChanges.applyTo(firstCampaign);
        AppliedChanges<TextCampaignWithCustomStrategy> secondCampaignAppliedChanges =
                secondCampaignModelChanges.applyTo(secondCampaign);

        autobudgetAlertService.freezeAlertsOnStrategyChange(clientId, List.of(firstCampaignAppliedChanges,
                secondCampaignAppliedChanges));

        assertTrue(autobudgetCpaAlertRepository.getCidOfExistingAlerts(secondCampaignInfo.getShard(),
                Collections.singletonList(secondCampaign.getId())).isEmpty());
        assertTrue(autobudgetHourlyAlertRepository.getCidOfExistingAlerts(secondCampaignInfo.getShard(),
                Collections.singletonList(secondCampaign.getId())).isEmpty());

        assertTrue(autobudgetCpaAlertRepository.getCidOfExistingAlerts(shard, Collections.singletonList(campaignId)).isEmpty());
        assertEquals(AutobudgetCommonAlertStatus.ACTIVE, autobudgetHourlyAlertRepository.getAlerts(shard,
                Collections.singletonList(campaignId)).get(campaignId).getStatus());
    }

    @Test
    public void freezeAlertsOnStrategyChange_isChangedFieldTest() {
        assertFalse(AutobudgetAlertService.isChanged(defAvgClick(), defAvgClick(), StrategyData.BID));
        assertFalse(AutobudgetAlertService.isChanged(defAvgClick(), defAvgClick(), StrategyData.AVG_BID));
    }

    private static DbStrategy defAvgClick() {
        DbStrategy strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setStrategyData(new StrategyData()
                .withAvgBid(new BigDecimal("33"))
                .withSum(new BigDecimal("345")));
        return strategy;
    }

    @Test
    public void freezeAlertsOnStrategyChangeToManual_StrategyChangeFreezeTest() {
        TextCampaignWithCustomStrategy campaign = getCampaignWithCustomStrategy(shard, campaignId);
        campaign.setStrategy(weekBundle(null));//?????????????????????? ???????????? ???? ???????????? ????????????

        // ?????????????????? ?????????? ???????????????? ?????????? ?? ???????????????? autobudget_alerts
        autobudgetHourlyAlertRepository.addAlerts(shard,
                Collections.singletonList(new HourlyAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withProblems(Set.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                        .withOverdraft(-7L)));

        // ???????????? ?????????????????? ???? ??????????????????????????
        ModelChanges<TextCampaignWithCustomStrategy> modelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        modelChanges.process(manualContextStrategy(), TextCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = modelChanges.applyTo(campaign);

        autobudgetAlertService.freezeAlertsOnStrategyChange(clientId, List.of(campaignAppliedChanges));

        // ??????????????????, ?????? ?????????? ??????????????????????, ?????? ?????? ?????????????????? ??????????????????
        assertEquals(AutobudgetCommonAlertStatus.FROZEN, autobudgetHourlyAlertRepository.getAlerts(shard,
                List.of(campaignId)).get(campaignId).getStatus());
    }

    private DbStrategy weekBundle(StrategyData strategyData) {
        var strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setPlatform(CampaignsPlatform.CONTEXT);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        strategy.setStrategyName(StrategyName.AUTOBUDGET_WEEK_BUNDLE);
        strategy.setStrategyData(new StrategyData().withLimitClicks(222L));
        if (strategyData != null) {
            strategy.setStrategyData(strategyData);
        }
        return strategy;
    }

    @Test
    public void freezeAlertsOnStrategyWeekBundlel_StrategyChangeFreezeTest() {
        TextCampaignWithCustomStrategy campaign = getCampaignWithCustomStrategy(shard, campaignId);
        campaign.setStrategy(weekBundle(new StrategyData().withAvgBid(new BigDecimal("345"))));//?????????????????????? ????????????
        // ???? ???????????? ????????????

        // ?????????????????? ?????????? ???????????????? ?????????? ?? ???????????????? autobudget_alerts
        autobudgetHourlyAlertRepository.addAlerts(shard,
                Collections.singletonList(new HourlyAutobudgetAlert()
                        .withCid(campaignId)
                        .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                        .withLastUpdate(LocalDateTime.now())
                        .withProblems(Set.of(AutobudgetHourlyProblem.MAX_BID_REACHED))
                        .withOverdraft(-7L)));

        // ???????????? ?????????????????? ???? ??????????????????????????
        ModelChanges<TextCampaignWithCustomStrategy> modelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        modelChanges.process(weekBundle(new StrategyData().withBid(new BigDecimal("345"))),
                TextCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = modelChanges.applyTo(campaign);

        autobudgetAlertService.freezeAlertsOnStrategyChange(clientId, List.of(campaignAppliedChanges));

        // ??????????????????, ?????? ?????????? ??????????????????????, ?????? ?????? ?????????????????? ??????????????????
        assertEquals(AutobudgetCommonAlertStatus.ACTIVE, autobudgetHourlyAlertRepository.getAlerts(shard,
                List.of(campaignId)).get(campaignId).getStatus());
    }
}
