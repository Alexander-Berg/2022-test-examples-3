package ru.yandex.autotests.direct.cmd.groups.performance;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.TargetFunnelEnum;
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

@Aqua.Test
@Description("Проверка сохранения ДМО группы контроллером savePerformanceAdGroups")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(CmdTag.EDIT_AD_GROUPS_PERFORMANCE)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SavePerformanceAdgroupsPositiveTest extends SavePerformanceAdgroupsTestBase {

    @Test
    @Description("Проверка сохранения ДМО группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9841")
    public void addPerformanceBannerPositiveTest() {
        saveGroup();
        adgroupId = getFirstAdgroupId();
        bids = getBid();

        check();
    }

    @Test
    @Description("Проверка создания ДМО группы с target_funnel = new_auditory")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9842")
    public void addPerformanceBannerNewAuditoryPositiveTest() {
        expectedGroup.getPerformanceFilters().get(0).withTargetFunnel(TargetFunnelEnum.NEW_AUDITORY.getValue());
        saveGroup();
        adgroupId = getFirstAdgroupId();
        bids = getBid();

        check();
    }
}
