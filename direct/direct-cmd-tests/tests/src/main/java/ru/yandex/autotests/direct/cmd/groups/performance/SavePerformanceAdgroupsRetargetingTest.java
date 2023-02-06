package ru.yandex.autotests.direct.cmd.groups.performance;

import org.junit.Before;
import org.junit.Ignore;
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

import java.util.Collections;

@Aqua.Test
@Description("Сохранение ДМО группы с ретаргетингом в фильтрах (cmd = savePerformanceAdGroups)")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.PERFORMANCE_FILTER)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SavePerformanceAdgroupsRetargetingTest extends SavePerformanceAdgroupsTestBase {

    @Before
    @Override
    public void before() {
        super.before();
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
    }

    private PerformanceFilter getExpectedPerformanceFilter() {
        Long retCondId = cmdRule.apiSteps().retargetingSteps()
                .addRandomRetargetingCondition(CLIENT).longValue();
        return BeanLoadHelper.
                loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class)
                .withRetargeting(new RetargetingCondition().withRetCondId(retCondId));
    }

    @Test
    @Description("Создание ДМО группы с ретаргетингом в фильтрах (cmd = savePerformanceAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9843")
    public void addPerformanceBannerWithRetargetingPositiveTest() {
        expectedGroup.withPerformanceFilters(Collections.singletonList(getExpectedPerformanceFilter()));
        saveGroup();
        adgroupId = getFirstAdgroupId();
        bids = getBid();

        check();
    }

    @Test
    @Description("Сохранение существующей ДМО группы с ретаргетингом в фильтрах (cmd = savePerformanceAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9844")
    public void editPerformanceBannerWithRetargetingPositiveTest() {
        saveGroup();
        adgroupId = getFirstAdgroupId();
        bids = getBid();

        expectedGroup.withPerformanceFilters(Collections.singletonList(getExpectedPerformanceFilter()));
        expectedGroup.setAdGroupID(getFirstAdgroupId());
        expectedGroup.getBanners().get(0).setBid(Long.valueOf(bids));

        saveGroup();
        check();
    }
}
