package ru.yandex.autotests.direct.cmd.groups.performance;

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
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;
import java.util.Collections;

@Aqua.Test
@Description("Проверка сохранения ДМО группы контроллером savePerformanceAdGroups")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(CmdTag.EDIT_AD_GROUPS_PERFORMANCE)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
@Ignore("проверяется в юнит тестах, оставлен для истории")
public class SavePerformanceAdgroupsFilterRetailCondsTest extends SavePerformanceAdgroupsTestBase {

    @Parameterized.Parameter(0)
    public PerformanceFilter filter;

    @Parameterized.Parameters(name = "Фильтр типа Retail № {index}")
    public static Collection testData() {
        return PerformanceFiltersHelper.getFilters(FeedBusinessType.RETAIL);
    }

    @Test
    @Description("Проверка создания ДМО группы с различными фильтрами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9839")
    public void addPerformanceGroupRetailFilterTest() {
        expectedGroup.setPerformanceFilters(Collections.singletonList(filter));
        saveGroup();
        adgroupId = getFirstAdgroupId();
        bids = getBid();

        check();
    }

    @Test
    @Description("Проверка сохранения ДМО группы с различными фильтрами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9840")
    public void changePerformanceGroupRetailFilterTest() {
        saveGroup();
        adgroupId = getFirstAdgroupId();
        bids = getBid();

        expectedGroup.setPerformanceFilters(Collections.singletonList(filter));
        expectedGroup.setAdGroupID(getFirstAdgroupId());
        expectedGroup.getBanners().get(0).setBid(Long.valueOf(bids));

        saveGroup();
        check();
    }

}
