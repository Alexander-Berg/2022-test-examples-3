package ru.yandex.autotests.direct.cmd.conditions.ajaxeditperformancefilters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.TargetFunnelEnum;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Проверка сохранения ДМО фильтра контроллером ajaxEditPerformanceFilters")
@Stories(TestFeatures.Conditions.AJAX_EDIT_PERFORMANCE_FILTERS)
@Features(TestFeatures.CONDITIONS)
@Tag(CmdTag.AJAX_EDIT_PERFORMANCE_FILTERS)
@Tag(ObjectTag.PERFORMANCE_FILTER)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AjaxEditPerformanceFiltersTest extends AjaxEditPerformanceFiltersTestBase {

    @Parameterized.Parameter(0)
    public PerformanceFilter filter;

    @Parameterized.Parameter(1)
    public String desc;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {getDefaultFilter().withPerfFilterId(null),
                        "Дефолтный фильтр"},
                {getDefaultFilter().withPerfFilterId(null).withTargetFunnel(TargetFunnelEnum.NEW_AUDITORY.getValue()),
                        "Фильтр с target_funnel = new_auditory"},
                {getDefaultFilter().withPerfFilterId(null).withFromTab("all-products"),
                        "Фильтр с from_tab = all-products"},
        });
    }

    private static PerformanceFilter getDefaultFilter() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class);
    }

    @Override
    protected PerformanceFilter getExpectedPerformanceFilter() {
        return filter;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9637")
    public void checkSavePerformanceFilter() {
        super.checkSavePerformanceFilter();
    }
}
