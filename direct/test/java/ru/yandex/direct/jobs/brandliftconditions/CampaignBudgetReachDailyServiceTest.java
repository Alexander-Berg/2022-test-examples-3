package ru.yandex.direct.jobs.brandliftconditions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.brandlift.service.targetestimation.TargetEstimation;
import ru.yandex.direct.core.entity.campaign.model.BrandSurveyStopReason;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignBudgetReachDaily;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.jobs.brandliftconditions.budgetestimation.BudgetEstimation;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.jobs.brandliftconditions.CampaignBudgetReachDailyService.createCampaignBudgetReachDaily;

@ParametersAreNonnullByDefault
class CampaignBudgetReachDailyServiceTest {
    private final static long campaignId = 1;
    private final static long campaignId2 = 2;

    private final static Integer workDuration = 2;
    private final static Integer workDuration2 = 3;
    private final static int workDurationWithoutStop = 5;

    private final static LocalDate startDate = LocalDate.now().minusWeeks(1);
    private final static LocalDate startDate2 = LocalDate.now().minusDays(3);
    private final static LocalDate finishDate = LocalDate.now().plusWeeks(3);
    private final static LocalDate finishDate2 = LocalDate.now().plusDays(7);

    private final static long targetThreshold = 2_000_000L;
    private final static long targetForecastMoreThenThreshold = 3_000_000L;
    private final static int defaultTrafficLight = 2;

    private final static double budgetTotalSpentMoreThenThreshold = 500_000d;
    private final static double spentMoreThenDailyThreshold = 32_000d;
    private final static double budgetEstimatedMoreThenDailyThreshold = 32_000d;

    @Test
    void createCampaignBudgetReachDaily_Ok_ReturnNoStopReasons() {
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                10,
                Map.of(campaignId, createGoodBudgetEstimation()),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign()),
                true,
                null,
                "commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily -> assertThat(reachDaily.getBrandSurveyStopReasons()).isEmpty());
    }

    @Test
    void createCampaignBudgetReachDailies_Ok_ReturnNoStopReasons() {
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(
                        campaignId, createGoodBudgetEstimation(),
                        campaignId2, createGoodBudgetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, createGoodTargetEstimation(),
                        campaignId2, createGoodTargetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, workDuration,
                        campaignId2, workDuration2
                ),
                List.of(
                        defaultCampaign(),
                        defaultCampaign2()
                ),
                true,
                null,
                "commonBrandSurveyId");

        assertReachDaily(
                campaignBudgetReaches,
                spentMoreThenDailyThreshold * 2,
                budgetEstimatedMoreThenDailyThreshold * 2,
                targetForecastMoreThenThreshold * 2
        );
    }

    @Test
    void createCampaignBudgetReachDailies_FirstLowBudgetSecondOk_ReturnNoStopReasons() {
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetSpent(0d);
        budgetEstimation.setBudgetTotalSpent(0d);
        budgetEstimation.setBudgetEstimated(0d);

        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(
                        campaignId, budgetEstimation,
                        campaignId2, createGoodBudgetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, createGoodTargetEstimation(),
                        campaignId2, createGoodTargetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, workDuration,
                        campaignId2, workDuration2
                ),
                List.of(
                        defaultCampaign(),
                        defaultCampaign2()
                ),
                true,
                null,
                "commonBrandSurveyId");

        assertReachDaily(
                campaignBudgetReaches,
                spentMoreThenDailyThreshold,
                budgetEstimatedMoreThenDailyThreshold,
                targetForecastMoreThenThreshold * 2
        );
    }

    @Test
    void createCampaignBudgetReachDailies_LowButTogetherOk_ReturnNoStopReasons() {
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetSpent(spentMoreThenDailyThreshold / 2);
        budgetEstimation.setBudgetTotalSpent(budgetTotalSpentMoreThenThreshold / 2);
        budgetEstimation.setBudgetEstimated(0d);

        var budgetEstimation2 = createGoodBudgetEstimation(campaignId2);
        budgetEstimation2.setBudgetSpent(spentMoreThenDailyThreshold / 2);
        budgetEstimation2.setBudgetTotalSpent(budgetTotalSpentMoreThenThreshold / 2);
        budgetEstimation2.setBudgetEstimated(0d);

        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(
                        campaignId, budgetEstimation,
                        campaignId2, budgetEstimation2
                ),
                Map.of(
                        campaignId,
                        createGoodTargetEstimation().withTargetForecast(targetForecastMoreThenThreshold / 2),
                        campaignId2,
                        createGoodTargetEstimation(campaignId2).withTargetForecast(targetForecastMoreThenThreshold / 2)
                ),
                Map.of(
                        campaignId, workDuration,
                        campaignId2, workDuration2
                ),
                List.of(
                        defaultCampaign(),
                        defaultCampaign2()
                ),
                true,
                null,
                "commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .hasSize(2)
                .allSatisfy(reachDaily -> assertThat(reachDaily.getBrandSurveyStopReasons()).isEmpty());
    }

    @Test
    void createCampaignBudgetReachDailies_FirstLowReachSecondOk_ReturnNoStopReasons() {

        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(
                        campaignId, createGoodBudgetEstimation(),
                        campaignId2, createGoodBudgetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, createGoodTargetEstimation().withTargetForecast(0L),
                        campaignId2, createGoodTargetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, workDuration,
                        campaignId2, workDuration2
                ),
                List.of(
                        defaultCampaign(),
                        defaultCampaign2()
                ),
                true,
                null,
                "commonBrandSurveyId");

        assertReachDaily(
                campaignBudgetReaches,
                spentMoreThenDailyThreshold * 2,
                budgetEstimatedMoreThenDailyThreshold * 2,
                targetForecastMoreThenThreshold
        );
    }

    @Test
    void createCampaignBudgetReachDailies_LowTargetForecastGoodWorkDuration_ReturnNoStopReasons() {
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(
                        campaignId, createGoodBudgetEstimation(),
                        campaignId2, createGoodBudgetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, createGoodTargetEstimation().withTargetForecast(0L),
                        campaignId2, createGoodTargetEstimation(campaignId2).withTargetForecast(0L)
                ),
                Map.of(
                        campaignId, workDuration,
                        campaignId2, workDuration2 + workDurationWithoutStop
                ),
                List.of(
                        defaultCampaign(),
                        defaultCampaign2()
                ),
                true,
                null,
                "commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .hasSize(2)
                .allSatisfy(reachDaily -> assertThat(reachDaily.getBrandSurveyStopReasons()).isEmpty());

    }

    @Test
    void createCampaignBudgetReachDailies_ReturnLowTotalBudget() {
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetTotalSpent(0d);
        budgetEstimation.setBudgetEstimated(0d);
        var budgetEstimation2 = createGoodBudgetEstimation(campaignId2);
        budgetEstimation2.setBudgetTotalSpent(0d);
        budgetEstimation2.setBudgetEstimated(0d);
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(
                        campaignId, budgetEstimation,
                        campaignId2, budgetEstimation2
                ),
                Map.of(
                        campaignId, createGoodTargetEstimation(),
                        campaignId2, createGoodTargetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, workDuration,
                        campaignId2, workDuration2
                ),
                List.of(
                        defaultCampaign(),
                        defaultCampaign2()
                ),
                true,
                null,
                "commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .hasSize(2)
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_TOTAL_BUDGET));
    }

    @Test
    void createCampaignBudgetReachDailies_ReturnLowDailyBudget() {
        var budgetEstimation = createGoodBudgetEstimation();
        var budgetEstimation2 = createGoodBudgetEstimation(campaignId2);

        budgetEstimation.setBudgetSpent(0d);
        budgetEstimation2.setBudgetSpent(0d);
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(
                        campaignId, budgetEstimation,
                        campaignId2, budgetEstimation2
                ),
                Map.of(
                        campaignId, createGoodTargetEstimation(),
                        campaignId2, createGoodTargetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, workDuration,
                        campaignId2, workDuration2
                ),
                List.of(
                        defaultCampaign(),
                        defaultCampaign2()
                ),
                true,
                null,
                "commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .hasSize(2)
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_DAILY_BUDGET));
    }

    @Test
    void createCampaignBudgetReachDailies_ReturnLowReach() {
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(
                        campaignId, createGoodBudgetEstimation(),
                        campaignId2, createGoodBudgetEstimation(campaignId2)
                ),
                Map.of(
                        campaignId, createGoodTargetEstimation().withTargetForecast(0L),
                        campaignId2, createGoodTargetEstimation(campaignId2).withTargetForecast(0L)
                ),
                Map.of(
                        campaignId, workDuration,
                        campaignId2, workDuration2
                ),
                List.of(
                        defaultCampaign(),
                        defaultCampaign2()
                ),
                true,
                null,
                "commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .hasSize(2)
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_REACH));
    }

    @Test
    void createCampaignBudgetReach_TrafficLightZero_ReturnNoStopReasons() {
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, createGoodBudgetEstimation()),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign()),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily -> assertThat(reachDaily.getBrandSurveyStopReasons()).isEmpty());
    }

    @Test
    void createCampaignBudgetReach_LowBudget_ReturnLowDailyBudget() {
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetSpent(0d);
        budgetEstimation.setBudgetEstimated(0d);
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, budgetEstimation),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign()),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_DAILY_BUDGET));
    }

    @Test
    void totalBudgetWarnDisabled_ReturnLowBudget() {
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetSpent(0d);
        budgetEstimation.setBudgetEstimated(0d);
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, budgetEstimation),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign()),
                false, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_BUDGET));
    }

    @Test
    void createCampaignBudgetReach_LowReach_ReturnLowReach() {
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, createGoodBudgetEstimation()),
                Map.of(campaignId,
                        createGoodTargetEstimation().withTargetForecast(targetForecastMoreThenThreshold / 2)),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign()),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_REACH));
    }

    @Test
    void createCampaignBudgetReachDaily_NullWorkDuration_ReturnLowReach() {
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, createGoodBudgetEstimation()),
                Map.of(campaignId,
                        createGoodTargetEstimation().withTargetForecast(targetForecastMoreThenThreshold / 2)),
                Map.of(),
                List.of(defaultCampaign()),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_REACH));
    }

    @Test
    void createCampaignBudgetReach_LowReachAndBudget_ReturnLowReachAndBudget() {
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetSpent(0d);
        budgetEstimation.setBudgetEstimated(0d);
        budgetEstimation.setBudgetTotalSpent(0d);
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, budgetEstimation),
                Map.of(campaignId,
                        createGoodTargetEstimation().withTargetForecast(targetForecastMoreThenThreshold / 2)),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign()),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons())
                                .containsExactlyInAnyOrder(BrandSurveyStopReason.LOW_DAILY_BUDGET,
                                        BrandSurveyStopReason.LOW_TOTAL_BUDGET, BrandSurveyStopReason.LOW_REACH));
    }

    @Test
    void createCampaignBudgetReach_CampaignWithoutFinishTime_ReturnLowTotalBudget() {
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, createGoodBudgetEstimation()),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign().withFinishTime(null)),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_TOTAL_BUDGET));
    }

    @Test
    void createCampaignBudgetReach_InactiveCampaignLowTotalBudget_ReturnLowDailyBudget() {
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetEstimated(0d);
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, budgetEstimation),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign().withStatusShow(false)),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_DAILY_BUDGET));
    }

    @Test
    void createCampaignBudgetReach_CampaignStartedYesterday_Ok_ReturnNoStopReasons() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate finishDate = LocalDate.now().plusDays(20);
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, createGoodBudgetEstimation()),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign().withStartTime(yesterday).withFinishTime(finishDate)),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily -> assertThat(reachDaily.getBrandSurveyStopReasons()).isEmpty());
    }

    @Test
    void createCampaignBudgetReach_CampaignStrategyStartedYesterday_Ok_ReturnNoStopReasons() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate strategyFinish = LocalDate.now().plusDays(50);
        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withStrategyData(new StrategyData().withStart(yesterday).withFinish(strategyFinish));

        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, createGoodBudgetEstimation()),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(new Campaign().withId(campaignId).withStatusShow(true).withStartTime(startDate).withStrategy(strategy)),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily -> assertThat(reachDaily.getBrandSurveyStopReasons()).isEmpty());
    }

    @Test
    void createCampaignBudgetReach_LowTotalBudgetWithStrategy_ReturnLowBudget() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate strategyFinish = LocalDate.now().plusDays(3);
        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withStrategyData(new StrategyData().withStart(yesterday).withFinish(strategyFinish));

        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetTotalSpent(0d);

        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, budgetEstimation),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign().withStrategy(strategy)),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_TOTAL_BUDGET));
    }

    @Test
    void createCampaignBudgetReach_OkWithStrategy_ReturnNoStopReasons() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate strategyFinish = LocalDate.now().plusDays(5);
        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withStrategyData(new StrategyData().withStart(yesterday).withFinish(strategyFinish));

        var daysCount = LocalDate.now().datesUntil(strategyFinish).count();
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetTotalSpent(0d);
        budgetEstimation.setBudgetEstimated(budgetTotalSpentMoreThenThreshold / daysCount);

        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, budgetEstimation),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign().withStrategy(strategy)),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily -> assertThat(reachDaily.getBrandSurveyStopReasons()).isEmpty());
    }

    @Test
    void createCampaignBudgetReach_LowTotalBudgetWithNotStartedStrategy_ReturnLowBudget() {
        LocalDate strategyStart = LocalDate.now().plusDays(2);
        LocalDate strategyFinish = LocalDate.now().plusDays(5);
        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withStrategyData(new StrategyData().withStart(strategyStart).withFinish(strategyFinish));

        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetTotalSpent(0d);

        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, budgetEstimation),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign().withStrategy(strategy)),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily ->
                        assertThat(reachDaily.getBrandSurveyStopReasons()).containsExactly(BrandSurveyStopReason.LOW_TOTAL_BUDGET));
    }

    @Test
    void createCampaignBudgetReach_OkWithAutoProlongatedStrategy_ReturnNoStopReasons() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate finishDate = LocalDate.now().plusDays(14);
        LocalDate strategyFinish = LocalDate.now().plusDays(6);
        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withStrategyData(new StrategyData()
                        .withStart(yesterday)
                        .withFinish(strategyFinish)
                        .withAutoProlongation(1L));

        var daysCount = LocalDate.now().datesUntil(finishDate).count();
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetTotalSpent(0d);
        budgetEstimation.setBudgetEstimated(budgetTotalSpentMoreThenThreshold / daysCount);

        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, budgetEstimation),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign().withStrategy(strategy).withFinishTime(finishDate)),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily -> assertThat(reachDaily.getBrandSurveyStopReasons()).isEmpty());
    }

    @Test
    void createCampaignBudgetReach_LowTotalBudgetWithAutoProlongatedStrategy_ReturnLowBudget() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate strategyFinish = LocalDate.now().plusDays(6);
        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withStrategyData(new StrategyData()
                        .withStart(yesterday)
                        .withFinish(strategyFinish)
                        .withAutoProlongation(1L));

        var daysCount = LocalDate.now().datesUntil(strategyFinish).count();
        var budgetEstimation = createGoodBudgetEstimation();
        budgetEstimation.setBudgetTotalSpent(0d);
        budgetEstimation.setBudgetEstimated(budgetTotalSpentMoreThenThreshold / (daysCount + 10));
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                targetThreshold,
                Map.of(campaignId, budgetEstimation),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(new Campaign().withId(campaignId).withStatusShow(true).withStartTime(startDate).withStrategy(strategy)),
                true, null,"commonBrandSurveyId");

        assertThat(campaignBudgetReaches.get(0).getBrandSurveyStopReasons()).contains(BrandSurveyStopReason.LOW_TOTAL_BUDGET);
    }

    @Test
    void createCampaignBudgetReachDaily_NewAge_ReturnLowBudget() {
        //бюджет 32 тысчи рублей, но пропертя включена. Дневных откруток не хватит тогда
        var campaignBudgetReaches = createCampaignBudgetReachDaily(
                10,
                Map.of(campaignId, createGoodBudgetEstimation()),
                Map.of(campaignId, createGoodTargetEstimation()),
                Map.of(campaignId, workDuration),
                List.of(defaultCampaign()),
                true,
                LocalDate.now().minusMonths(1),
                "commonBrandSurveyId");

        assertThat(campaignBudgetReaches.get(0).getBrandSurveyStopReasons()).contains(BrandSurveyStopReason.LOW_DAILY_BUDGET);
    }


    private static BudgetEstimation createGoodBudgetEstimation(long otherCampaignId) {

        return new BudgetEstimation(
                otherCampaignId,
                spentMoreThenDailyThreshold,
                budgetEstimatedMoreThenDailyThreshold,
                budgetTotalSpentMoreThenThreshold,
                1d,
                "RUB"
        );
    }

    private static BudgetEstimation createGoodBudgetEstimation() {
        return createGoodBudgetEstimation(campaignId);
    }

    private static TargetEstimation createGoodTargetEstimation(long otherCampaignId) {
        return new TargetEstimation(
                otherCampaignId,
                targetForecastMoreThenThreshold,
                defaultTrafficLight
        );
    }

    private static TargetEstimation createGoodTargetEstimation() {
        return createGoodTargetEstimation(campaignId);
    }

    private static Campaign defaultCampaign() {
        return new Campaign().withId(campaignId)
                .withStatusShow(true)
                .withStartTime(startDate)
                .withFinishTime(finishDate)
                .withCreateTime(LocalDateTime.now());
    }

    private static Campaign defaultCampaign2() {
        return new Campaign().withId(campaignId2).withStatusShow(true).withStartTime(startDate2).withFinishTime(finishDate2);
    }

    private static void assertReachDaily(List<CampaignBudgetReachDaily> campaignBudgetReaches, double spent,
                                         double estimated, long targetForecast) {
        assertThat(campaignBudgetReaches)
                .isNotEmpty()
                .allSatisfy(reachDaily -> {
                    assertThat(reachDaily.getBrandSurveyStopReasons()).isEmpty();

                    assertThat(reachDaily.getBudgetSpent())
                            .as("Потрачено за день")
                            .isEqualTo(BigDecimal.valueOf(spent));

                    assertThat(reachDaily.getBudgetEstimated())
                            .as("Прогноз бюджета в день")
                            .isEqualTo(BigDecimal.valueOf(estimated));

                    assertThat(reachDaily.getTargetForecast())
                            .as("Охват")
                            .isEqualTo(targetForecast);

                    assertThat(reachDaily.getBrandSurveyId())
                            .as("проставился BS")
                            .isEqualTo("commonBrandSurveyId");
                });
    }
}
