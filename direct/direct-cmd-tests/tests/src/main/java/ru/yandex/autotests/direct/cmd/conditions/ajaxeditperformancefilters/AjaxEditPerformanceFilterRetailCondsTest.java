package ru.yandex.autotests.direct.cmd.conditions.ajaxeditperformancefilters;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedBusinessType;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.steps.performancefilters.PerformanceFiltersHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

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
@Ignore("https://st.yandex-team.ru/TESTIRT-9293") //заигнорено, т.к. решили проверять в юнит тестах
public class AjaxEditPerformanceFilterRetailCondsTest extends AjaxEditPerformanceFiltersTestBase {

    @Parameterized.Parameter(0)
    public PerformanceFilter filter;

    @Parameterized.Parameters(name = "Фильтр типа Retail № {index}")
    public static Collection testData() {
        return PerformanceFiltersHelper.getFilters(FeedBusinessType.RETAIL);
    }

    @Override
    protected PerformanceFilter getExpectedPerformanceFilter() {
        return filter.withPerfFilterId(null);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9622")
    public void checkSavePerformanceFilter() {
        super.checkSavePerformanceFilter();
    }

}
