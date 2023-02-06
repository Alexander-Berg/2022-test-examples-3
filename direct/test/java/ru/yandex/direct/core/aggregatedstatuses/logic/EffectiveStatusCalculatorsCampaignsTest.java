package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusCampaign;
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusWallet;
import ru.yandex.direct.core.entity.campaign.aggrstatus.WalletStatus;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.TimeTargetStatus;
import ru.yandex.direct.core.entity.campaign.model.TimeTargetStatusInfo;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.aggregatedstatuses.logic.EffectiveStatusCalculators.campaignEffectiveStatus;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_PROCESSING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_PROCESSING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.allPause;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.allStop;

@RunWith(Parameterized.class)
public class EffectiveStatusCalculatorsCampaignsTest {
    // some day in past which code will treat as today
    private static final LocalDate VIRTUAL_TODAY = LocalDate.parse("2017-12-27");

    @Parameterized.Parameter
    public AggregatedStatusCampaign campaign;
    @Parameterized.Parameter(1)
    public Collection<AggregatedStatusCampaign> subCampaigns;
    @Parameterized.Parameter(2)
    public WalletStatus walletStatus;
    @Parameterized.Parameter(3)
    public TimeTargetStatusInfo timeTargetStatus;
    @Parameterized.Parameter(4)
    public GdSelfStatusEnum expectedStatus;
    @Parameterized.Parameter(5)
    public List<GdSelfStatusReason> expectedReasons;
    private SelfStatus effectiveStatus;

    @Parameterized.Parameters(name = "{4}: {5}")
    public static Object[][] allRunParameters() {
        List<Object[]> params = new ArrayList<>();

        params.add(params(campaign(RUN_OK, GdSelfStatusReason.ACTIVE), null, null, ttActive(),
                RUN_OK, singletonList(GdSelfStatusReason.ACTIVE)));

        params.add(params(campaign(RUN_WARN, GdSelfStatusReason.PROMO_EXTENSION_REJECTED), null, null, ttActive(),
                RUN_WARN, singletonList(GdSelfStatusReason.PROMO_EXTENSION_REJECTED)));

        params.add(params(campaign(RUN_OK, GdSelfStatusReason.ACTIVE), null, null, ttTomorrow(),
                PAUSE_OK, singletonList(GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING)));

        // all non run_* selfStatuses returned as is
        params.addAll(nonRunStatusParameters());

        // Campaigns has wallet_id, but not wallet provided => returning selfStatus as is
        params.add(params(campaign(1235L, RUN_OK, GdSelfStatusReason.ACTIVE), null, null, ttTomorrow(),
                RUN_OK, singletonList(GdSelfStatusReason.ACTIVE)));

        // startDate vs currentDate
        params.add(params(campaignWillStart(RUN_OK, GdSelfStatusReason.ACTIVE), null, null, ttActive(),
                PAUSE_OK, singletonList(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)));
        params.add(params(campaignStartedOneDayAgo(RUN_OK, GdSelfStatusReason.ACTIVE), null, null, ttActive(),
                RUN_OK, singletonList(GdSelfStatusReason.ACTIVE)));
        params.add(params(campaignStartedSameDay(RUN_OK, GdSelfStatusReason.ACTIVE), null, null, ttActive(),
                RUN_OK, singletonList(GdSelfStatusReason.ACTIVE)));

        // dayBudget
        params.add(params(campaignStoppedByDayBudget(RUN_OK, GdSelfStatusReason.ACTIVE), null, null, ttActive(),
                PAUSE_WARN, singletonList(GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_DAY_BUDGET)));
        params.add(params(campaign(1234L, RUN_OK, GdSelfStatusReason.ACTIVE), null, walletStatusStoppedByDayBudget(),
                ttActive(),
                PAUSE_WARN, singletonList(GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_WALLET_DAY_BUDGET)));

        params.add(params(campaign(RUN_OK, GdSelfStatusReason.ACTIVE), null, null, ttActive(),
                RUN_OK, singletonList(GdSelfStatusReason.ACTIVE)));
        params.add(params(campaign(ARCHIVED, GdSelfStatusReason.ARCHIVED), null, null, ttActive(),
                ARCHIVED, singletonList(GdSelfStatusReason.ARCHIVED)));

        LocalDate yesterday = VIRTUAL_TODAY.minusDays(1);
        params.add(params(campaign(yesterday, ARCHIVED, GdSelfStatusReason.ARCHIVED), null, null, ttActive(),
                ARCHIVED, singletonList(GdSelfStatusReason.ARCHIVED)));

        params.add(params(campaign(yesterday, STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY), null, null, ttActive(),
                STOP_OK, singletonList(GdSelfStatusReason.CAMPAIGN_IS_OVER)));

        params.add(params(campaignStrategyPeriodEnd(LocalDate.MIN, RUN_OK,
                        GdSelfStatusReason.CAMPAIGN_STRATEGY_PERIOD_HAS_ENDED), null, null, ttActive(),
                STOP_OK, singletonList(GdSelfStatusReason.CAMPAIGN_STRATEGY_PERIOD_HAS_ENDED)));
        params.add(params(campaignStrategyPeriodEnd(LocalDate.now(), RUN_OK,
                        GdSelfStatusReason.CAMPAIGN_STRATEGY_PERIOD_HAS_ENDED), null, null, ttActive(),
                PAUSE_OK, singletonList(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)));

        params.add(params(campaign(RUN_OK, GdSelfStatusReason.ACTIVE),
                List.of(campaign(ON_MODERATION, GdSelfStatusReason.CAMPAIGN_ON_MODERATION),
                        campaign(RUN_WARN, GdSelfStatusReason.CAMPAIGN_HAS_INACTIVE_BANNERS)),
                null, ttActive(),
                RUN_OK, singletonList(GdSelfStatusReason.ACTIVE)));

        params.add(params(campaign(RUN_OK, GdSelfStatusReason.ACTIVE),
                List.of(campaign(RUN_PROCESSING, GdSelfStatusReason.CAMPAIGN_HAS_PROCESSING_ADGROUPS),
                        campaign(RUN_OK, GdSelfStatusReason.ACTIVE)),
                null, ttActive(),
                RUN_OK, singletonList(GdSelfStatusReason.ACTIVE)));

        params.add(params(campaign(RUN_OK, GdSelfStatusReason.ACTIVE),
                List.of(campaign(STOP_PROCESSING, GdSelfStatusReason.CAMPAIGN_BL_PROCESSING),
                        campaign(RUN_OK, GdSelfStatusReason.ACTIVE)),
                null, ttActive(),
                RUN_PROCESSING, singletonList(GdSelfStatusReason.UC_CAMPAIGN_SUBJECTS_PROCESSING)));

        params.add(params(campaign(RUN_OK, GdSelfStatusReason.ACTIVE),
                List.of(campaign(RUN_OK, GdSelfStatusReason.ACTIVE),
                        campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING)),
                null, ttActive(),
                RUN_WARN, List.of(GdSelfStatusReason.UC_CAMPAIGN_SUBJECTS_REJECTED_ON_MODERATION,
                        GdSelfStatusReason.ACTIVE)));

        params.add(params(campaign(RUN_OK, GdSelfStatusReason.ACTIVE),
                List.of(campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING),
                        campaign(STOP_PROCESSING, GdSelfStatusReason.CAMPAIGN_BL_PROCESSING)),
                null, ttActive(),
                RUN_WARN, List.of(GdSelfStatusReason.UC_CAMPAIGN_SUBJECTS_REJECTED_ON_MODERATION,
                        GdSelfStatusReason.UC_CAMPAIGN_SUBJECTS_PROCESSING,
                        GdSelfStatusReason.ACTIVE)));

        params.add(params(campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING),
                List.of(campaign(RUN_OK, GdSelfStatusReason.ACTIVE),
                        campaign(STOP_PROCESSING, GdSelfStatusReason.CAMPAIGN_BL_PROCESSING)),
                null, ttActive(),
                RUN_WARN, List.of(GdSelfStatusReason.CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING,
                        GdSelfStatusReason.UC_CAMPAIGN_SUBJECTS_PROCESSING)));

        params.add(params(campaign(RUN_OK, GdSelfStatusReason.ACTIVE),
                List.of(campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_BL_NOTHING_GENERATED),
                        campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_BL_NOTHING_GENERATED)),
                null, ttActive(),
                RUN_WARN, List.of(GdSelfStatusReason.UC_CAMPAIGN_SUBJECTS_BL_NOTHING_GENERATED,
                        GdSelfStatusReason.ACTIVE)));

        return params.toArray(Object[][]::new);
    }

    private static List<Object[]> nonRunStatusParameters() {
        List<Object[]> params = new ArrayList<>();

        ArrayList<GdSelfStatusEnum> nonRunStatuses = new ArrayList<>(allStop());
        nonRunStatuses.addAll(allPause());
        nonRunStatuses.add(ARCHIVED);
        nonRunStatuses.add(DRAFT);
        //nonRunStatuses.add(RUN_OK); // -- negative check, to test the test
        for (GdSelfStatusEnum nonRunStatus : nonRunStatuses) {
            params.add(params(campaign(nonRunStatus, null), null, null, ttTomorrow(), nonRunStatus, null));
        }

        return params;
    }

    @Before
    public void getEffectiveStatus() {
        AggregatedStatusWallet wallet = walletStatus != null
                ? new AggregatedStatusWallet().withStatus(walletStatus) : null;
        effectiveStatus = campaignEffectiveStatus(VIRTUAL_TODAY, campaign, subCampaigns, wallet, timeTargetStatus);
    }

    @Test
    public void reasonsMatch() {
        assertThat(effectiveStatus.getReasons())
                .as("reason match")
                .containsExactlyInAnyOrderElementsOf(expectedReasons == null ? List.of() : expectedReasons);
    }

    @Test
    public void statusMatch() {
        assertThat(expectedStatus)
                .as("status match")
                .usingRecursiveComparison()
                .isEqualTo(effectiveStatus.getStatus());
    }


    private static Object[] params(AggregatedStatusCampaign campaign,
                                   Collection<AggregatedStatusCampaign> subCampaigns, WalletStatus walletStatus,
                                   TimeTargetStatusInfo timeTargetStatusInfo, GdSelfStatusEnum status,
                                   List<GdSelfStatusReason> reasons) {
        return new Object[]{campaign, subCampaigns, walletStatus, timeTargetStatusInfo, status, reasons};
    }

    private static TimeTargetStatusInfo ttTomorrow() {
        return tt(TimeTargetStatus.TOMORROW);
    }

    private static TimeTargetStatusInfo ttActive() {
        return tt(TimeTargetStatus.ACTIVE);
    }

    private static TimeTargetStatusInfo tt(TimeTargetStatus timeTargetStatus) {
        return new TimeTargetStatusInfo().withStatus(timeTargetStatus);
    }

    private static AggregatedStatusCampaign campaignWillStart(GdSelfStatusEnum status, GdSelfStatusReason reason) {
        AggregatedStatusCampaign campaign = campaign(status, reason);
        campaign.withStartDate(LocalDate.now());
        return campaign;
    }

    private static AggregatedStatusCampaign campaignStrategyPeriodEnd(LocalDate date, GdSelfStatusEnum status,
                                                                      GdSelfStatusReason reason) {
        AggregatedStatusCampaign campaign = campaign(status, reason);
        campaign
                .withStartDate(LocalDate.now())
                .withStrategy(
                        (DbStrategy) new DbStrategy()
                                .withStrategyData(
                                        new StrategyData().withFinish(date)));
        return campaign;
    }

    private static AggregatedStatusCampaign campaignStartedSameDay(GdSelfStatusEnum status, GdSelfStatusReason reason) {
        AggregatedStatusCampaign campaign = campaign(status, reason);
        campaign.withStartDate(VIRTUAL_TODAY);
        return campaign;
    }

    private static AggregatedStatusCampaign campaignStartedOneDayAgo(GdSelfStatusEnum status,
                                                                     GdSelfStatusReason reason) {
        AggregatedStatusCampaign campaign = campaign(status, reason);
        campaign.withStartDate(VIRTUAL_TODAY.minusDays(1));
        return campaign;
    }

    private static AggregatedStatusCampaign campaignStoppedByDayBudget(GdSelfStatusEnum status,
                                                                       GdSelfStatusReason reason) {
        AggregatedStatusCampaign campaign = campaign(status, reason);
        campaign.withDayBudgetStopTime(VIRTUAL_TODAY.atTime(1, 0));
        return campaign;
    }

    private static WalletStatus walletStatusStoppedByDayBudget() {
        return new WalletStatus().withBudgetLimitationStopTime(VIRTUAL_TODAY.atTime(1, 0));
    }

    private static AggregatedStatusCampaign campaign(GdSelfStatusEnum status, @Nullable GdSelfStatusReason reason) {
        final var aggrStatus = reason != null
                ? new AggregatedStatusCampaignData(status, reason)
                : new AggregatedStatusCampaignData(status);

        return new AggregatedStatusCampaign()
                .withAggregatedStatus(aggrStatus)
                .withDayBudget(new BigDecimal("600.00"))
                .withDayBudgetStopTime((VIRTUAL_TODAY.minusDays(1).atTime(1, 0)));
    }

    private static AggregatedStatusCampaign campaign(Long walletId,
                                                     GdSelfStatusEnum status,
                                                     @Nullable GdSelfStatusReason reason) {
        return campaign(status, reason).withWalletId(walletId);
    }

    private static AggregatedStatusCampaign campaign(LocalDate finishDate,
                                                     GdSelfStatusEnum status,
                                                     @Nullable GdSelfStatusReason reason) {
        return campaign(status, reason).withFinishDate(finishDate);
    }
}
