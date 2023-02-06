package ru.yandex.autotests.direct.cmd.conditions.ajaxeditperformancefilters;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка сохранения ретаргетинга в ДМО фильтрах контроллером ajaxEditPerformanceFilters")
@Stories(TestFeatures.Performance.AJAX_EDIT_PERFORMANCE_FILTERS)
@Features(TestFeatures.PERFORMANCE)
@Tag(CmdTag.AJAX_EDIT_PERFORMANCE_FILTERS)
@Tag(ObjectTag.PERFORMANCE_FILTER)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
public class AjaxEditPerformanceFiltersRetargetingTest extends AjaxEditPerformanceFiltersTestBase {

    @Before
    @Override
    public void before() {
        super.before();
    }

    @Override
    protected PerformanceFilter getExpectedPerformanceFilter() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
        Long retargetingid = cmdRule.apiSteps().retargetingSteps()
                .addRandomRetargetingCondition(CLIENT).longValue();
        return BeanLoadHelper.
                loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class)
                .withPerfFilterId(null)
                .withRetargeting(new RetargetingCondition().withRetCondId(retargetingid));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9636")
    public void checkSavePerformanceFilter() {
        super.checkSavePerformanceFilter();
    }
}
