package ru.yandex.autotests.direct.cmd.conditions.ajaxeditperformancefilters;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.conditions.common.AfterLastConditionTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.AjaxEditPerformanceFiltersResponse;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilterBannersMap;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilterMap;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверки изменения статусов модерации при удалении последнего фильтра ДМО через ajaxEditPerformanceFilters")
@Stories(TestFeatures.Conditions.AJAX_EDIT_PERFORMANCE_FILTERS)
@Features(TestFeatures.CONDITIONS)
@Tag("TESTIRT-8612")
public class AfterLastPerformanceFiltersLastFilterRemoveTest extends AfterLastConditionTestBase {


    protected static final String CLIENT = "at-direct-back-perf-filters";

    public AfterLastPerformanceFiltersLastFilterRemoveTest() {
        super(new PerformanceBannersRule());
    }

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Override
    protected void deleteCondition() {
        PerformanceFilterBannersMap expectedPerformanceFilterBannersMap = new PerformanceFilterBannersMap()
                .withPerformanceFilterBannerMap(String.valueOf(bannersRule.getGroupId()),
                        new PerformanceFilterMap().withDeleted(singletonList(getFilterId())));
        AjaxEditPerformanceFiltersResponse response = cmdRule.cmdSteps().ajaxEditPerformanceFiltersSteps()
                .postAjaxEditPerformanceFilters(campaignId, CLIENT, expectedPerformanceFilterBannersMap);

        assumeThat("действия над фильтром прошли успешно", response.getErrors(), nullValue());
        assumeThat("действия над фильтром прошли успешно", response.getError(), nullValue());

        Group actualGroup = bannersRule.getCurrentGroup();
        assumeThat("последний фильтр удален", actualGroup.getPerformanceFilters(), hasSize(0));
    }

    @Override
    protected void suspendCondition() {
        PerformanceFilterBannersMap expectedPerformanceFilterBannersMap = new PerformanceFilterBannersMap()
                .withPerformanceFilterBannerMap(String.valueOf(bannersRule.getGroupId()),
                        new PerformanceFilterMap().withEdited(getFilterId(),
                                new PerformanceFilter().withIsSuspended("1")));
        AjaxEditPerformanceFiltersResponse response = cmdRule.cmdSteps().ajaxEditPerformanceFiltersSteps()
                .postAjaxEditPerformanceFilters(campaignId, CLIENT, expectedPerformanceFilterBannersMap);

        assumeThat("действия над фильтром прошли успешно", response.getErrors(), nullValue());
        assumeThat("действия над фильтром прошли успешно", response.getError(), nullValue());
    }

    private String getFilterId() {
        return cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getPerformanceFilters().get(0).getPerfFilterId();
    }

    @Override
    protected Group getExpectedGroupStatuses() {
        return new Group().withStatus_moderate(expectedGroup.getStatus_moderate())
                .withStatus_post_moderate(expectedGroup.getStatus_post_moderate())
                .withStatus_bs_synced(StatusBsSynced.NO.toString())
                .withBanners(singletonList(new Banner()
                        .withStatus_moderate(expectedBanner.getStatus_moderate())
                        .withStatus_bs_synced(StatusBsSynced.NO.toString())
                        .withStatus_post_moderate(expectedBanner.getStatus_post_moderate())));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10868")
    public void deleteConditionTest() {
        super.deleteConditionTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10869")
    public void suspendConditionTest() {
        super.suspendConditionTest();
    }
}
