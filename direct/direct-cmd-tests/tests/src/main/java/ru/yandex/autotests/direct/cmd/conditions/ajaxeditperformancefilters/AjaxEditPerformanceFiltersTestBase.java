package ru.yandex.autotests.direct.cmd.conditions.ajaxeditperformancefilters;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.AjaxEditPerformanceFiltersResponse;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilterBannersMap;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.steps.performancefilters.PerformanceFiltersHelper.convertPerformanceFilterMapToPerformanceFilter;
import static ru.yandex.autotests.direct.cmd.steps.performancefilters.PerformanceFiltersHelper.sortConditions;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

public abstract class AjaxEditPerformanceFiltersTestBase {

    protected static final String SUPER = Logins.SUPER;
    protected static final String CLIENT = "at-direct-back-perf-filters";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected String campaignId;
    protected Long adgroupId;
    protected String filterId;
    protected PerformanceFilter expectedFilter;
    protected PerformanceFilterBannersMap expectedPerformanceFilterBannersMap;
    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    protected abstract PerformanceFilter getExpectedPerformanceFilter();

    @Before
    public void before() {

        expectedFilter = getExpectedPerformanceFilter();
        campaignId = bannersRule.getCampaignId().toString();
        adgroupId = bannersRule.getGroupId();

        filterId = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getPerformanceFilters().get(0).getPerfFilterId();

        expectedPerformanceFilterBannersMap = PerformanceFilterBannersMap
                .forPerformanceFilter(String.valueOf(adgroupId), filterId, expectedFilter);
    }

    @Description("Проверяем редактирование фильтров ДМО")
    public void checkSavePerformanceFilter() {
        AjaxEditPerformanceFiltersResponse response = cmdRule.cmdSteps().ajaxEditPerformanceFiltersSteps()
                .postAjaxEditPerformanceFilters(campaignId, CLIENT, expectedPerformanceFilterBannersMap);
        assumeThat("Фильтр ДМО успешно сохранился", response.getResult(), equalTo(CommonResponse.RESULT_OK));

        List<PerformanceFilter> actualFilters = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getPerformanceFilters();
        assertThat("ДМО-фильтр был правильно отредактирован", sortConditions(actualFilters), beanDiffer(
                sortConditions(convertPerformanceFilterMapToPerformanceFilter(
                        expectedPerformanceFilterBannersMap
                                .getPerformanceFilterBannerMap().get(String.valueOf(adgroupId)))))
                .useCompareStrategy(onlyExpectedFields()));
    }
}
