package ru.yandex.direct.core.entity.performancefilter.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFilterSelectionCriteria;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter;
import ru.yandex.direct.core.entity.performancefilter.model.NowOptimizingBy;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.PerformanceFiltersSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultSiteFilterCondition;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultUacFilterCondition;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.otherFilterConditions;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.sortConditions;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PerformanceFilterServiceGetTest {

    private static final String FILTER = "{\n" +
            "  \"available\": \"true\",\n" +
            "  \"name ilike\": [\"Платье\"],\n" +
            "  \"pickup\": \"true\",\n" +
            "  \"price <->\": [\"3000.00-100000.00\",\"111.25-222.75\"],\n" +
            "  \"price ==\": [555,666],\n" +
            "  \"url ilike\": [\"utm_campaign=DynRmkt_tver_av\"],\n" +
            "  \"url not ilike\": [\"utm_campaign=DynRmkt_tver_avocation\"],\n" +
            "  \"oldprice <->\": [\"-5000.00\"],\n" +
            "  \"categoryId <\": [5545343564],\n" +
            "  \"categoryId <->\": [\"567-890\"],\n" +
            "  \"categoryId ==\": [111,333],\n" +
            "  \"categoryId >\": [555],\n" +
            "  \"model ilike\": [\"ggg\",\"www\"],\n" +
            "  \"model not ilike\": [\"hhh\",\"jjj\"],\n" +
            "  \"vendor ilike\": [\"aaaa\",\"bbbb\"],\n" +
            "  \"vendor not ilike\": [\"cccc\"],\n" +
            "  \"manufacturer_warranty exists\": \"1\",\n" +
            "  \"store ==\": [\"1\"],\n" +
            "  \"typePrefix exists\": \"1\"\n" +
            "}";

    @Autowired
    private Steps steps;

    @Autowired
    private PerformanceFilterStorage performanceFilterStorage;

    @Autowired
    private PerformanceFilterService performanceFilterService;

    @Autowired
    private PerformanceFilterValidationService performanceFilterValidationService;

    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;

    @Test
    public void getPerformanceFilter_success() {
        FeedInfo feedInfo = steps.feedSteps().createFeed(new FeedInfo().withFeed(defaultFeed(null)
                .withFeedType(FeedType.YANDEX_MARKET)
                .withBusinessType(BusinessType.RETAIL)));
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(feedInfo.getFeedId());
        Long adGroupId = adGroupInfo.getAdGroupId();
        ClientId clientId = adGroupInfo.getClientId();
        PerformanceFilter expectedFilter = new PerformanceFilter()
                .withBusinessType(BusinessType.RETAIL)
                .withFeedType(FeedType.YANDEX_MARKET)
                .withPid(adGroupId)
                .withFeedId(feedInfo.getFeedId())
                .withIsDeleted(false)
                .withIsSuspended(false)
                .withName("test feed")
                .withNowOptimizingBy(NowOptimizingBy.CPC)
                .withPriceCpa(BigDecimal.TEN)
                .withPriceCpc(BigDecimal.TEN)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withTargetFunnel(TargetFunnel.NEW_AUDITORY)
                .withAutobudgetPriority(3)
                .withLastChange(LocalDateTime.now());
        expectedFilter.withConditions(
                PerformanceFilterConditionDBFormatParser.INSTANCE
                        .parse(performanceFilterStorage.getFilterSchema(expectedFilter), FILTER)
        );
        List<Long> ids = performanceFilterRepository
                .addPerformanceFilters(adGroupInfo.getShard(), singletonList(expectedFilter));
        expectedFilter.setId(ids.get(0));

        PerformanceFilter actual = performanceFilterService.getPerformanceFilters(clientId, singletonList(adGroupId))
                .get(adGroupId)
                .get(0);
        ValidationResult<List<PerformanceFilter>, Defect> vrs =
                performanceFilterValidationService.validate(clientId, adGroupInfo.getUid(), singletonList(actual));

        sortConditions(expectedFilter.getConditions());
        sortConditions(actual.getConditions());
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual).is(matchedBy(beanDiffer(expectedFilter)
                    .useCompareStrategy(onlyExpectedFields()
                            .forFields(newPath("priceCpa"), newPath("priceCpc")).useDiffer(new BigDecimalDiffer())
                            .forFields(newPath("lastChange")).useMatcher(Matchers.notNullValue()))));
            soft.assertThat(vrs.hasAnyErrors()).isFalse();
        });
    }

    @Test
    public void getPerformanceFilters_checkAccess_success() {
        PerformanceFilterInfo ownerFilterInfo =
                steps.performanceFilterSteps().createDefaultPerformanceFilter();
        ClientId clientId = ownerFilterInfo.getClientId();
        Long owner = ownerFilterInfo.getClientInfo().getUid();
        PerformanceFilterInfo someElseFilterInfo =
                steps.performanceFilterSteps().createDefaultPerformanceFilter();

        List<Long> campaignIds = List.of(ownerFilterInfo.getCampaignId(), someElseFilterInfo.getCampaignId());
        List<PerformanceFilter> performanceFilters =
                performanceFilterService.getPerfFiltersBySelectionCriteria(clientId, owner,
                        new PerformanceFilterSelectionCriteria()
                                .withCampaignIds(campaignIds));

        List<Long> returnedFilterIds = mapList(performanceFilters, PerformanceFilter::getPerfFilterId);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedFilterIds).as("result contains owner's filter")
                    .contains(ownerFilterInfo.getFilterId());
            soft.assertThat(returnedFilterIds).as("result doesn't contain some else filter")
                    .doesNotContain(someElseFilterInfo.getFilterId());
        });
    }

    @Test
    public void getPerformanceFilters_checkPerfFilterStates_success() {
        PerformanceFiltersSteps performanceFiltersSteps = steps.performanceFilterSteps();
        PerformanceFilterInfo firstFilterInfo = performanceFiltersSteps.createDefaultPerformanceFilter();
        performanceFiltersSteps.setPerformanceFilterProperty(firstFilterInfo, PerformanceFilter.IS_DELETED, true);
        Long firstFilterId = firstFilterInfo.getFilterId();
        Long adGroupId = firstFilterInfo.getAdGroupId();
        PerformanceFilter secondFilter = defaultPerformanceFilter(adGroupId, firstFilterInfo.getFeedId())
                .withConditions(otherFilterConditions())
                .withIsDeleted(true);
        Long secondFilterId = performanceFiltersSteps.addPerformanceFilter(firstFilterInfo.getShard(), secondFilter);

        PerformanceFilterSelectionCriteria selectionCriteria = new PerformanceFilterSelectionCriteria()
                .withPerfFilterIds(singletonList(firstFilterId))
                .withAdGroupIds(singletonList(adGroupId))
                .withoutDeleted();
        Long uid = firstFilterInfo.getClientInfo().getUid();
        ClientId clientId = firstFilterInfo.getClientId();
        List<PerformanceFilter> performanceFilters =
                performanceFilterService.getPerfFiltersBySelectionCriteria(clientId, uid, selectionCriteria);

        List<Long> returnedFilterIds = mapList(performanceFilters, PerformanceFilter::getPerfFilterId);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedFilterIds).as("result contains deleted filter by PerfFilterId")
                    .contains(firstFilterId);
            soft.assertThat(returnedFilterIds).as("result doesn't contain deleted filter by AdGroupId")
                    .doesNotContain(secondFilterId);
        });
    }

    @Test
    public void getPerformanceFilters_noDefaultConditionWhenSiteFeed_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withSource(Source.SITE)
                .withConditions(List.of(defaultSiteFilterCondition().withParsedValue(true)));

        PerformanceFilterInfo filterInfo = steps.performanceFilterSteps().addPerformanceFilter(
                new PerformanceFilterInfo().withAdGroupInfo(adGroupInfo).withFilter(filter));

        PerformanceFiltersQueryFilter filterQuery = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(List.of(filterInfo.getFilter().getPerfFilterId())).build();

        PerformanceFilter actualFilter = performanceFilterService.getPerformanceFilters(
                clientInfo.getClientId(), filterQuery).get(0);

        assertThat(actualFilter.getConditions(), beanDiffer(emptyList()));
    }

    @Test
    public void getPerformanceFilters_byAdGroupIds_noDefaultConditionWhenSiteFeed_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withSource(Source.SITE)
                .withConditions(List.of(defaultSiteFilterCondition().withParsedValue(true)));

        steps.performanceFilterSteps().addPerformanceFilter(
                new PerformanceFilterInfo().withAdGroupInfo(adGroupInfo).withFilter(filter));

        PerformanceFilter actualFilter = performanceFilterService.getPerformanceFilters(clientInfo.getClientId(),
                        singletonList(adGroupInfo.getAdGroupId()))
                .get(adGroupInfo.getAdGroupId())
                .get(0);

        assertThat(actualFilter.getConditions(), beanDiffer(emptyList()));
    }

    @Test
    public void getPerformanceFilters_noDefaultConditionNotEquals_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createPerformanceAdGroupWithSiteFeed(clientInfo);
        PerformanceFilter filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withConditions(List.of(defaultUacFilterCondition().withParsedValue(false)));

        PerformanceFilterInfo filterInfo = steps.performanceFilterSteps().addPerformanceFilter(
                new PerformanceFilterInfo().withAdGroupInfo(adGroupInfo).withFilter(filter));

        PerformanceFiltersQueryFilter filterQuery = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(List.of(filterInfo.getFilter().getPerfFilterId())).build();

        PerformanceFilter actualFilter = performanceFilterService.getPerformanceFilters(
                clientInfo.getClientId(), filterQuery).get(0);

        assertThat(actualFilter.getConditions(), beanDiffer(emptyList()));
    }
}
