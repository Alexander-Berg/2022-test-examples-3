package ru.yandex.direct.core.entity.campaign.service.pricerecalculation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaignWithPriceRecalculation;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.PerformanceFiltersSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BidsPerformanceStatusbssynced;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_PERFORMANCE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(JUnitParamsRunner.class)
@Description("Проверка изменений при смене стратегии для смарт/performance кампаний")
public class SmartCampaignPriceRecalculationServiceTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public Steps steps;
    @Autowired
    public PerformanceFilterRepository performanceFilterRepository;
    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private PerformanceFiltersSteps performanceFiltersSteps;

    private SmartCampaignPriceRecalculationService smartCampaignPriceRecalculationService;
    private CampaignInfo campaignInfo;
    private Long campaignId;
    private int shard;
    private UidClientIdShard uidClientIdShard;
    private Long bidPerformanceId;

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        shard = campaignInfo.getShard();
        campaignId = campaignInfo.getCampaignId();
        uidClientIdShard = UidClientIdShard.of(campaignInfo.getUid(), campaignInfo.getClientId(), shard);

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedId);
        bidPerformanceId = performanceFiltersSteps.addDefaultBidsPerformance(adGroupInfo).getPerfFilterId();

        smartCampaignPriceRecalculationService =
                new SmartCampaignPriceRecalculationService(performanceFilterRepository);
    }

    /**
     * Если AUTOBUDGET_ROI новая стратегия и фильтры кампании (bidsPerformance) без приоритета и со статусом
     * синхронизации 'Yes' -> сбрасывается приоритет фразы (Autobudgetpriority) у всех фильтров кампании и статус
     * синхронизации с БК (BidsPerformanceStatusbssynced) в No
     */
    @Test
    public void afterCampaignsStrategyChanged_WithAutobudgetRoiNewStrategy() {
        changeBidsPerformancePriorityAndBsStatus(shard, bidPerformanceId, null);

        steps.campaignSteps().setStrategy(campaignInfo, StrategyName.AUTOBUDGET_ROI);

        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_ROI);

        var changes = new ModelChanges<>(campaignId, SmartCampaignWithPriceRecalculation.class)
                .process(strategy, SmartCampaignWithPriceRecalculation.STRATEGY)
                .applyTo(new SmartCampaign().withId(campaignId));

        smartCampaignPriceRecalculationService.afterCampaignsStrategyChanged(singletonList(changes), uidClientIdShard);

        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(bidPerformanceId))
                .build();
        List<PerformanceFilter> actualPerformanceFilters = performanceFilterRepository.getFilters(shard, queryFilter);

        PerformanceFilter expectedPerformanceFilter = new PerformanceFilter()
                .withAutobudgetPriority(3)
                .withStatusBsSynced(StatusBsSynced.NO);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualPerformanceFilters).as("количество фильтров")
                    .hasSize(1);
            soft.assertThat(actualPerformanceFilters.get(0)).as("фильтр")
                    .is(matchedBy(beanDiffer(expectedPerformanceFilter)
                            .useCompareStrategy(onlyExpectedFields())));
        });
    }

    @SuppressWarnings("unused")
    @Parameterized.Parameters(name = "{0}")
    private static Object[] strategyParameters() {
        return Arrays.stream(StrategyName.values())
                .filter(strategyName -> strategyName != StrategyName.AUTOBUDGET_ROI)
                .toArray();
    }

    /**
     * Если новая стратегия не AUTOBUDGET_ROI -> приоритеты фраз bidsPerformance кампании не меняются статус
     * синхронизации с БК (BidsPerformanceStatusbssynced) в No
     */
    @Test
    @Parameters(method = "strategyParameters")
    public void afterCampaignsStrategyChanged_WithNotAutobudgetRoiAsNewStartegy(StrategyName strategyName) {
        changeBidsPerformancePriorityAndBsStatus(shard, bidPerformanceId, null);

        steps.campaignSteps().setStrategy(campaignInfo, StrategyName.AUTOBUDGET_ROI);

        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyName(strategyName);

        var changes = new ModelChanges<>(campaignId, SmartCampaignWithPriceRecalculation.class)
                .process(strategy, SmartCampaignWithPriceRecalculation.STRATEGY)
                .applyTo(new SmartCampaign().withId(campaignId));

        smartCampaignPriceRecalculationService.afterCampaignsStrategyChanged(singletonList(changes), uidClientIdShard);

        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(bidPerformanceId))
                .build();
        List<PerformanceFilter> actualPerformanceFilters = performanceFilterRepository.getFilters(shard, queryFilter);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualPerformanceFilters).as("количество фильтров")
                    .hasSize(1);
            soft.assertThat(actualPerformanceFilters.get(0).getAutobudgetPriority())
                    .as("приоритет фразы для автобюджета (autobudgetPriority)")
                    .isNull();
            soft.assertThat(actualPerformanceFilters.get(0).getStatusBsSynced())
                    .as("статус синхронизации ставки или приоритета автобюджета с БК (statusBsSynced)")
                    .isEqualTo(StatusBsSynced.NO);

        });
    }

    /**
     * При любом изменении стратегии у всех фильтров кампании сбрасывается статус
     * синхронизации с БК (BidsPerformanceStatusbssynced) в No
     */
    @Test
    @Parameters(method = "strategyParameters")
    public void afterCampaignsStrategyChanged_ChangeBidsPerformanceBsStatus(StrategyName strategyName) {
        Long autobudgetPriority = 1L;
        changeBidsPerformancePriorityAndBsStatus(shard, bidPerformanceId, autobudgetPriority);

        steps.campaignSteps().setStrategy(campaignInfo, StrategyName.AUTOBUDGET_ROI);

        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyName(strategyName);

        var changes = new ModelChanges<>(campaignId, SmartCampaignWithPriceRecalculation.class)
                .process(strategy, SmartCampaignWithPriceRecalculation.STRATEGY)
                .applyTo(new SmartCampaign().withId(campaignId));

        smartCampaignPriceRecalculationService.afterCampaignsStrategyChanged(singletonList(changes), uidClientIdShard);

        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(bidPerformanceId))
                .build();
        List<PerformanceFilter> actualPerformanceFilters = performanceFilterRepository.getFilters(shard, queryFilter);

        PerformanceFilter expectedPerformanceFilter = new PerformanceFilter()
                .withAutobudgetPriority(autobudgetPriority.intValue())
                .withStatusBsSynced(StatusBsSynced.NO);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualPerformanceFilters).as("количество фильтров")
                    .hasSize(1);
            soft.assertThat(actualPerformanceFilters.get(0)).as("фильтр")
                    .is(matchedBy(beanDiffer(expectedPerformanceFilter)
                            .useCompareStrategy(onlyExpectedFields())));
        });
    }

    private void changeBidsPerformancePriorityAndBsStatus(int shard, Long perfFilterId, Long priority) {
        dslContextProvider.ppc(shard)
                .update(BIDS_PERFORMANCE)
                .set(BIDS_PERFORMANCE.AUTOBUDGET_PRIORITY, priority)
                .set(BIDS_PERFORMANCE.STATUS_BS_SYNCED, BidsPerformanceStatusbssynced.Yes)
                .where(BIDS_PERFORMANCE.PERF_FILTER_ID.eq(perfFilterId))
                .execute();
    }
}
