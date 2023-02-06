package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.campaign.model.BrandSurveyStopReason.LOW_DAILY_BUDGET;
import static ru.yandex.direct.core.entity.campaign.model.BrandSurveyStopReason.LOW_TOTAL_BUDGET;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;

@ParametersAreNonnullByDefault
@RunWith(MockitoJUnitRunner.class)
public class BrandSurveyConditionsServiceTest {

    @Mock
    private CurrencyRateService currencyRateService;

    private BrandSurveyConditionsService brandSurveyConditionsService;

    @Before
    public void before() {
        brandSurveyConditionsService = new BrandSurveyConditionsService(currencyRateService);
    }

    @Test
    public void getBrandSurveyStatus_cpmDefaultLowBudget_returnStopReason() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.CPM_DEFAULT)
                        .withStrategyData(new StrategyData());

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                BigDecimal.valueOf(40_000),
                CurrencyCode.RUB,
                true, null, false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).containsExactly(LOW_DAILY_BUDGET);
        assertThat(result.getSumSpentByTotalPeriod()).isEqualByComparingTo(
                BigDecimal.valueOf(40_000).multiply(BigDecimal.valueOf(31)));
    }

    @Test
    public void getBrandSurveyStatus_cpmDefaultHighBudget_noStopReasons() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.CPM_DEFAULT)
                        .withStrategyData(new StrategyData());

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                BigDecimal.valueOf(700000),
                CurrencyCode.RUB,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).isEmpty();
    }

    @Test
    public void getBrandSurveyStatus_cpmDefaultLowBudgetForCustomCurrency_returnStopReason() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.CPM_DEFAULT)
                        .withStrategyData(new StrategyData());

        doReturn(Money.valueOf(10_000, CurrencyCode.RUB))
                .when(currencyRateService).convertMoney(any(), eq(CurrencyCode.RUB));

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                BigDecimal.valueOf(1_000),
                CurrencyCode.UAH,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).containsExactlyInAnyOrder(LOW_DAILY_BUDGET, LOW_TOTAL_BUDGET);
    }

    @Test
    public void getBrandSurveyStatus_maxReachLowBudget_returnStopReason() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH)
                        .withStrategyData(
                                new StrategyData()
                                        .withSum(BigDecimal.valueOf(100))
                                        .withAutoProlongation(0L));

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                BigDecimal.ZERO,
                CurrencyCode.EUR,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).containsExactlyInAnyOrder(LOW_DAILY_BUDGET, LOW_TOTAL_BUDGET);
    }

    @Test
    public void getBrandSurveyStatus_maxReachHighBudget_noStopReasons() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH)
                        .withStrategyData(
                                new StrategyData()
                                        .withSum(BigDecimal.valueOf(700000))
                                        .withAutoProlongation(0L));

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                BigDecimal.ZERO,
                CurrencyCode.EUR,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).containsExactly();
    }

    @Test
    public void getBrandSurveyStatus_maxImpressionsByWeek_noStopReasons() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS)
                        .withStrategyData(
                                new StrategyData()
                                        .withSum(BigDecimal.valueOf(1_000_000))
                                        .withAvgCpm(BigDecimal.valueOf(100))
                        );

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                null,
                BigDecimal.ZERO,
                CurrencyCode.RUB,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).isEmpty();
    }


    @Test
    public void getBrandSurveyStatus_maxReachCustomPeriodLowBudget_returnStopReason() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                        .withStrategyData(
                                new StrategyData()
                                        .withStart(LocalDate.now().plusDays(1))
                                        .withFinish(LocalDate.now().plusDays(19))
                                        .withBudget(BigDecimal.valueOf(100))
                                        .withAutoProlongation(0L));

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                BigDecimal.ZERO,
                CurrencyCode.EUR,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).containsExactlyInAnyOrder(LOW_DAILY_BUDGET, LOW_TOTAL_BUDGET);
    }

    @Test
    public void getBrandSurveyStatus_maxReachCustomPeriodHighBudget_noStopReasons() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                        .withStrategyData(
                                new StrategyData()
                                        .withStart(LocalDate.now().plusDays(1))
                                        .withFinish(LocalDate.now().plusDays(20))
                                        .withBudget(BigDecimal.valueOf(490 * 20))
                                        .withAutoProlongation(0L));

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                BigDecimal.ZERO,
                CurrencyCode.EUR,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).isEmpty();
    }

    @Test
    public void getBrandSurveyStatus_shortPeriodCampaign_returnStopReason() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.CPM_DEFAULT)
                        .withStrategyData(new StrategyData());

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(12),
                BigDecimal.valueOf(100_000),
                CurrencyCode.RUB,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).isEmpty();
    }

    @Test
    public void getBrandSurveyStatus_longPeriodCampaign_noStopReasons() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.CPM_DEFAULT)
                        .withStrategyData(new StrategyData());

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(13),
                BigDecimal.valueOf(100_000),
                CurrencyCode.RUB,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).isEmpty();
    }

    @Test
    public void getBrandSurveyStatus_campaignWithoutFinish_noStopReasons() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.CPM_DEFAULT)
                        .withStrategyData(new StrategyData());

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                null,
                BigDecimal.valueOf(1_000_000),
                CurrencyCode.RUB,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).isEmpty();
    }

    @Test
    public void getBrandSurveyStatus_shortPeriodStrategy_returnStopReason() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                        .withStrategyData(
                                new StrategyData()
                                        .withStart(LocalDate.now().plusDays(1))
                                        .withFinish(LocalDate.now().plusDays(13))
                                        .withBudget(BigDecimal.valueOf(3_000))
                                        .withAutoProlongation(0L));

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                null,
                BigDecimal.ZERO,
                CurrencyCode.EUR,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).containsExactlyInAnyOrder(LOW_DAILY_BUDGET, LOW_TOTAL_BUDGET);
    }

    @Test
    public void getBrandSurveyStatus_longPeriodStrategy_noStopReasons() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                        .withStrategyData(
                                new StrategyData()
                                        .withStart(LocalDate.now().plusDays(1))
                                        .withFinish(LocalDate.now().plusDays(14))
                                        .withBudget(BigDecimal.valueOf(8_000))
                                        .withAutoProlongation(0L));

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                null,
                BigDecimal.ZERO,
                CurrencyCode.EUR,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).isEmpty();
    }

    @Test
    public void getBrandSurveyStatus_shortPeriodStrategyWithAutoprolongation_noStopReasons() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH)
                        .withStrategyData(
                                new StrategyData()
                                        .withSum(BigDecimal.valueOf(8_000))
                                        .withAutoProlongation(1L));

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                null,
                BigDecimal.ZERO,
                CurrencyCode.EUR,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).isEmpty();
    }

    @Test
    public void getBrandSurveyStatus_shortPeriodCampaignWithValidStrategy_returnStopReason() {
        var campaignStrategy =
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH)
                        .withStrategyData(
                                new StrategyData()
                                        .withSum(BigDecimal.valueOf(200))
                                        .withAutoProlongation(1L));

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                LocalDate.now().plusDays(12),
                BigDecimal.ZERO,
                CurrencyCode.EUR,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).containsExactlyInAnyOrder(LOW_DAILY_BUDGET, LOW_TOTAL_BUDGET);
    }

    @Test
    public void getBrandSurveyStatus_AutobudgetMaxImpressionsCustomPeriod() {
        var campaignStrategy = getDefaultStrategyAutobudgetMaxImpressionsCustomPeriod(201000);

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                null,
                BigDecimal.ZERO,
                CurrencyCode.RUB,
                true, LocalDate.now().minusDays(2),false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).containsExactlyInAnyOrder(LOW_DAILY_BUDGET, LOW_TOTAL_BUDGET);
    }

    @Test
    public void getBrandSurveyStatus_prevCampsForBrandlift() {
        //Если есть уже РК для этого брендлифта, то учитывать их в валидации
        var campaignStrategy = getDefaultStrategyAutobudgetMaxImpressionsCustomPeriod(201000);
        Campaign prevCamp = new Campaign()
                .withStrategy(getDefaultStrategyAutobudgetMaxImpressionsCustomPeriod(2_000_000))
                .withStartTime(LocalDate.now());

        var result = brandSurveyConditionsService.getBrandSurveyStatus(
                campaignStrategy,
                LocalDate.now(),
                null,
                BigDecimal.ZERO,
                CurrencyCode.RUB,
                true, LocalDate.now().minusDays(2),false, List.of(prevCamp));

        assertThat(result.getBrandSurveyStopReasonsDaily()).doesNotContain(LOW_DAILY_BUDGET, LOW_TOTAL_BUDGET);
    }

    @Test
    public void getCpmPriceBrandSurveyStatus_million() {
        //Из-за проблемы с округлением миллион ровно не проходит валидацию
        var result = brandSurveyConditionsService.getCpmPriceBrandSurveyStatus(
                LocalDate.of(2022, 06, 06),
                LocalDate.of(2022, 06, 12),
                defaultPricePackage(),
                BigDecimal.valueOf(1_000_000),
                false,
                true, null,false, emptyList());

        assertThat(result.getBrandSurveyStopReasonsDaily()).isEmpty();
    }

    private DbStrategy getDefaultStrategyAutobudgetMaxImpressionsCustomPeriod(int budget) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                .withPlatform(CampaignsPlatform.CONTEXT)
                .withStrategyData(
                        new StrategyData()
                                .withBudget(BigDecimal.valueOf(budget))
                                .withAvgCpm(BigDecimal.valueOf(100))
                                .withStart(LocalDate.now())
                                .withFinish(LocalDate.now().plusDays(30))
                                .withAutoProlongation(1L));
    }
}
