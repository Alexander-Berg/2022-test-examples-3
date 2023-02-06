package ru.yandex.direct.core.entity.performancefilter.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;

@CoreTest
@RunWith(Parameterized.class)
public class PerformanceFilterRepositoryReadTest {
    @Parameterized.Parameter()
    public PerformanceFilterTab parameterTab;

    @Parameterized.Parameter(1)
    public PerformanceFilterTab expectedTab;

    @Autowired
    private Steps steps;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;

    private Integer shard;
    private Long adGroupId;
    private Long feedId;
    private PerformanceAdGroupInfo adGroupInfo;

    @Parameterized.Parameters(name = "{0}, expected tab {1}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {PerformanceFilterTab.TREE, PerformanceFilterTab.TREE},
                {PerformanceFilterTab.ALL_PRODUCTS, PerformanceFilterTab.ALL_PRODUCTS},
                {PerformanceFilterTab.CONDITION, PerformanceFilterTab.CONDITION},
                {null, PerformanceFilterTab.CONDITION}
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() throws Exception {
        // Manual Spring integration (because we're using Parametrized runner)
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        shard = adGroupInfo.getShard();
        adGroupId = adGroupInfo.getAdGroupId();
        feedId = adGroupInfo.getFeedId();
    }

    @Test
    public void getFilters_success_getTabs() {
        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, feedId)
                .withTab(parameterTab);
        PerformanceFilterInfo filterInfo = new PerformanceFilterInfo()
                .withAdGroupInfo(adGroupInfo)
                .withFilter(filter);
        steps.performanceFilterSteps().addPerformanceFilter(filterInfo);
        List<PerformanceFilter> filters = performanceFilterRepository.getFiltersById(shard, singleton(filter.getId()));
        PerformanceFilter actualFilter = filters.get(0);
        assertThat(actualFilter.getTab()).describedAs("getTab").isEqualTo(expectedTab);
    }
}
