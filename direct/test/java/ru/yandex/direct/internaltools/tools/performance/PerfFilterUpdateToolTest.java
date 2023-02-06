package ru.yandex.direct.internaltools.tools.performance;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterConditionDBFormatParser;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterStorage;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterValidationService;
import ru.yandex.direct.core.testing.data.TestPerformanceFilters;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PerfFilterUpdateToolTest {

    private static final CompareStrategy STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath(PerformanceFilter.PRICE_CPC.name()),
                    newPath(PerformanceFilter.PRICE_CPA.name()))
            .useDiffer(new BigDecimalDiffer());

    @Autowired
    PerformanceFilterRepository performanceFilterRepository;

    @Autowired
    private PerformanceFilterStorage performanceFilterStorage;

    @Autowired
    private Steps steps;

    @Autowired
    ShardHelper shardHelper;

    @Mock
    private FeatureService featureService;

    @Mock
    private PerformanceFilterValidationService performanceFilterValidationService;

    @Autowired
    private PerformanceFilterService performanceFilterService;

    private PerfFilterUpdateTool perfFilterUpdateTool;

    private FilterSchema filterSchema;
    private AdGroupInfo adGroupInfo;
    private Long feedId;
    private int shard = 1;

    /**
     * Сценарии, которые проверяются тестом:
     * 1. Фильтр по всему фиду сузить до нескольких категорий.
     * 2. Фильтр по одной категории расширить до всего фида.
     * 3. Поменять одну категорию в фиде на другую.
     */
    @Before
    public void setUp() {
        initMocks(this);
        perfFilterUpdateTool = new PerfFilterUpdateTool(shardHelper, performanceFilterService,
                performanceFilterValidationService, featureService);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedId);
        filterSchema = performanceFilterStorage.getFilterSchema(BusinessType.AUTO, FeedType.YANDEX_MARKET);
        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(eq(clientInfo.getClientId()), eq(FeatureName.CHANGE_FILTER_CONDITIONS_ALLOWED));
        doReturn(ValidationResult.success(emptyList()))
                .when(performanceFilterValidationService)
                .validate(eq(clientInfo.getClientId()), anyLong(), anyList());
    }

    @Test
    public void testScenario1() {
        List<PerformanceFilterCondition> conditions = PerformanceFilterConditionDBFormatParser.INSTANCE
                .parse(filterSchema, "{}");
        PerformanceFilter filter = TestPerformanceFilters.defaultPerformanceFilter(adGroupInfo.getAdGroupId(), feedId);
        filter.setConditions(conditions);
        steps.performanceFilterSteps().addPerformanceFilter(shard, filter);

        String updatedConditions = "{\"categoryId ==\":[29,57,66,69,73]}";
        updateFilterAndCheck(filter, updatedConditions, PerformanceFilterTab.TREE);
    }

    @Test
    public void testScenario2() {
        List<PerformanceFilterCondition> conditions = PerformanceFilterConditionDBFormatParser.INSTANCE
                .parse(filterSchema, "{\"categoryId ==\":[30,58,59]}");
        PerformanceFilter filter = TestPerformanceFilters.defaultPerformanceFilter(adGroupInfo.getAdGroupId(), feedId);
        filter.setConditions(conditions);
        steps.performanceFilterSteps().addPerformanceFilter(shard, filter);

        String updatedConditions = "{}";
        updateFilterAndCheck(filter, updatedConditions, PerformanceFilterTab.ALL_PRODUCTS);
    }

    @Test
    public void testScenario3() {
        List<PerformanceFilterCondition> conditions = PerformanceFilterConditionDBFormatParser.INSTANCE
                .parse(filterSchema, "{\"categoryId ==\":[29,57,66,69,73]}");
        PerformanceFilter filter = TestPerformanceFilters.defaultPerformanceFilter(adGroupInfo.getAdGroupId(), feedId);
        filter.setConditions(conditions);
        steps.performanceFilterSteps().addPerformanceFilter(shard, filter);

        String updatedConditions = "{\"categoryId ==\":[30,58,59]}";
        updateFilterAndCheck(filter, updatedConditions, PerformanceFilterTab.TREE);
    }

    private void updateFilterAndCheck(PerformanceFilter filter, String updatedConditions,
                                      PerformanceFilterTab expectedTab) {
        PerfFilterParameters perfFilterParameters = new PerfFilterParameters();
        perfFilterParameters.setAdGroupId(adGroupInfo.getAdGroupId());
        perfFilterParameters.setLogin(shardHelper.getLoginByUid(adGroupInfo.getUid()));
        perfFilterParameters.setPerfFilterId(filter.getId());
        perfFilterParameters.setConditionJson(updatedConditions);

        perfFilterUpdateTool.process(perfFilterParameters);

        List<PerformanceFilter> existedFilters = performanceFilterRepository.getFiltersById(shard,
                singletonList(filter.getId()));

        List<PerformanceFilterCondition> updateConditions = PerformanceFilterConditionDBFormatParser.INSTANCE
                .parse(filterSchema, updatedConditions);
        assertThat(existedFilters.get(0)).is(matchedBy(beanDiffer(filter
                .withTab(expectedTab)
                .withConditions(updateConditions)).useCompareStrategy(STRATEGY)));
    }
}
