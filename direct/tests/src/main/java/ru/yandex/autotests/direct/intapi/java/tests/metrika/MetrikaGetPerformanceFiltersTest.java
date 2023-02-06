package ru.yandex.autotests.direct.intapi.java.tests.metrika;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.MetrikaPerformanceFiltersParam;
import ru.yandex.autotests.direct.intapi.models.MetrikaPerformanceFiltersResult;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("metrika/performance-filters")
@Stories(TestFeatures.Metrika.PERFORMANCE_FILTERS)
@Features(TestFeatures.METRIKA)
@Tag(Tags.METRIKA)
@Tag(TagDictionary.TRUNK)
@Tag("DIRECT-64729")
@Issue("DIRECT-64729")
public class MetrikaGetPerformanceFiltersTest {
    public static final String ULOGIN = "at-direct-intapi-metrika2";
    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private MetrikaPerformanceFiltersResult expectedMetrikaPerformanceFiltersResult;
    private PerformanceBannersRule bannersRule = new PerformanceBannersRule()
            .withUlogin(ULOGIN);

    private DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(bannersRule);
    @Rule
    public DirectRule directRule = DirectRule.defaultRule()
            .withRules(cmdRule);

    private List<MetrikaPerformanceFiltersResult> results;

    @Before
    public void setUp() {
        Long orderId = RandomUtils.nextLong(0L, Integer.MAX_VALUE);
        Long clientId = directRule.dbSteps().shardingSteps().getClientIdByLogin(ULOGIN);
        directRule.dbSteps().shardingSteps().createOrderIdMapping(orderId, clientId);
        directRule.dbSteps()
                .useShardForLogin(ULOGIN)
                .campaignsSteps().setOrderId(bannersRule.getCampaignId(), orderId);

        List<PerformanceFilter> filters = bannersRule.getCurrentGroup().getPerformanceFilters();
        assumeThat("список фильтров не пустой", filters, Matchers.hasSize(greaterThan(0)));

        PerformanceFilter performanceFilter = filters.get(0);

        MetrikaPerformanceFiltersParam param = new MetrikaPerformanceFiltersParam()
                .withOrderId(orderId)
                .withPerfFilterId(Long.valueOf(performanceFilter.getPerfFilterId()));

        results = directRule.intapiSteps().metrikaControllerSteps()
                .getPerformanceFilters(Collections.singletonList(param));

        expectedMetrikaPerformanceFiltersResult = new MetrikaPerformanceFiltersResult()
                .withFilterName(performanceFilter.getFilterName())
                .withOrderId(orderId)
                .withPerfFilterId(param.getPerfFilterId());
    }

    @Test
    public void getFilters() {
        assumeThat("Получили не пустой список фильтров", results, Matchers.hasSize(greaterThan(0)));
        assertThat("Полученный фильтр соответствует ожиданиям", results.get(0),
                equalTo(expectedMetrikaPerformanceFiltersResult));
    }
}
