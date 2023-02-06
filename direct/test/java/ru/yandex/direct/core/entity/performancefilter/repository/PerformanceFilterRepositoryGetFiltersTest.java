package ru.yandex.direct.core.entity.performancefilter.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.model.AutoBudgetPriority;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterBaseStatus;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestPerformanceFilters;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.BusinessIdAndShopId;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PerformanceFilterRepositoryGetFiltersTest {

    @Autowired
    private Steps steps;

    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;

    private static final int SHARD = 1;
    private static final Long INCORRECT_ID = 555555L;
    private static final Integer MEDIUM_PRIORITY = AutoBudgetPriority.MEDIUM.getTypedValue();
    private PerformanceFiltersQueryFilter.Builder correctBuilder;

    @Before
    public void setUp() {
        FeedInfo feedInfo = steps.feedSteps().createFeed(new FeedInfo()
            .withFeed(defaultFeed()
                    .withMarketBusinessId(1L)
                    .withMarketShopId(1L)));
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(feedInfo);
        PerformanceFilter performanceFilter =
                TestPerformanceFilters.defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                        .withName("name")
                        .withTargetFunnel(TargetFunnel.NEW_AUDITORY)
                        .withIsSuspended(false)
                        .withPriceCpa(BigDecimal.ONE)
                        .withPriceCpc(BigDecimal.ONE)
                        .withAutobudgetPriority(MEDIUM_PRIORITY);
        steps.performanceFilterSteps().addPerformanceFilter(SHARD, performanceFilter);

        correctBuilder = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(singletonList(performanceFilter.getPerfFilterId()))
                .withAdGroupIds(singletonList(adGroupInfo.getAdGroupId()))
                .withCampaignIds(singletonList(adGroupInfo.getCampaignId()))
                .withBusinessIdsAndShopIds(singleton(feedInfo.getBusinessIdAndShopId()))
                .withAutobudgetPriorities(singleton(MEDIUM_PRIORITY))
                .withNameContains("name")
                .withNameNotContains("qwerty")
                .withTargetFunnels(singleton(TargetFunnel.NEW_AUDITORY))
                .withBaseStatuses(singleton(PerformanceFilterBaseStatus.ACTIVE))
                .withMaxPriceCpa(BigDecimal.TEN)
                .withMaxPriceCpc(BigDecimal.TEN);
    }

    @Test
    public void getFilters_success() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder.build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(1);
    }

    @Test
    public void getFilters_incorrectPerfFilterId() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withPerfFilterIds(singletonList(INCORRECT_ID))
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectBusinessIdAndShopId() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withBusinessIdsAndShopIds(singletonList(BusinessIdAndShopId.of(INCORRECT_ID, INCORRECT_ID)))
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectAdGroupId() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withAdGroupIds(singletonList(INCORRECT_ID))
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectCampaignId() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withCampaignIds(singletonList(INCORRECT_ID))
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectAutobudgetPriorities() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withAutobudgetPriorities(singleton(AutoBudgetPriority.LOW.getTypedValue()))
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectTargetFunnel() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withTargetFunnels(singleton(TargetFunnel.PRODUCT_PAGE_VISIT))
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectPriceCpa() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withMinPriceCpa(BigDecimal.TEN)
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectNameContains() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withNameContains("qwerty")
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectNameNotContains() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withNameNotContains("name")
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectPriceCpc() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withMinPriceCpc(BigDecimal.TEN)
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFilters_incorrectBaseStatuses() {
        PerformanceFiltersQueryFilter queryFilter = correctBuilder
                .withBaseStatuses(singleton(PerformanceFilterBaseStatus.SUSPENDED))
                .build();

        List<PerformanceFilter> filters = performanceFilterRepository.getFilters(SHARD, queryFilter);
        assertThat(filters).hasSize(0);
    }

    @Test
    public void getFiltersCountByAdGroupId_success() {
        //Подготавливаем исходное состояние: 5 активных и 5 удалённых фильтров
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        int totalFiltersCount = 10;
        int enabledFiltersCount = 5;
        for (int i = 0; i < totalFiltersCount; i++) {
            boolean isDeleted = i < enabledFiltersCount;
            PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                    .withIsDeleted(isDeleted);
            PerformanceFilterInfo filterInfo = new PerformanceFilterInfo()
                    .withAdGroupInfo(adGroupInfo)
                    .withFilter(filter);
            steps.performanceFilterSteps().addPerformanceFilter(filterInfo);
        }

        //Выполняем метод и проверяем результат
        Map<Long, List<PerformanceFilter>> filtersCountByAdGroupId =
                performanceFilterRepository.getNotDeletedFiltersByAdGroupIds(adGroupInfo.getShard(),
                        singleton(adGroupInfo.getAdGroupId()));
        Integer actualFiltersCount = filtersCountByAdGroupId.get(adGroupInfo.getAdGroupId()).size();
        assertThat(actualFiltersCount).isEqualTo(enabledFiltersCount);
    }

}
