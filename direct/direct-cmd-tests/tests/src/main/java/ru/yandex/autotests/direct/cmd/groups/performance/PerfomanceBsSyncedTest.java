package ru.yandex.autotests.direct.cmd.groups.performance;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.MobileMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.groups.CreateGroupBsSyncedBaseTest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;

//Task: TESTIRT-9418.
@Aqua.Test
@Description("Проверка статуса синхронизации смарт-группы")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
public class PerfomanceBsSyncedTest extends CreateGroupBsSyncedBaseTest {
    public PerfomanceBsSyncedTest() {
        bannersRule = new PerformanceBannersRule().withUlogin(getClient());
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Override
    protected String getClient() {
        return Logins.DEFAULT_CLIENT;
    }

    @Test
    @Description("Изменение мобильных коэффициентов на группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9833")
    public void changeMobileKOnGroup() {
        Group group = bannersRule.getCurrentGroup()
                .withAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.setHierarchicalMultipliers(new HierarchicalMultipliers()
                .withMobileMultiplier(new MobileMultiplier().withMultiplierPct("100")));
        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9828")
    public void changeGeoTargetingCheckGroupStatus() {
        super.changeGeoTargetingCheckGroupStatus();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9829")
    public void changeNothingCheckBannerStatus() {
        super.changeNothingCheckBannerStatus();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9830")
    public void changeMinusWordsCheckGroup() {
        super.changeMinusWordsCheckGroup();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9831")
    public void changeDemographKOnGroup() {
        super.changeDemographKOnGroup();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9832")
    public void changeRetargetingKOnGroup() {
        super.changeRetargetingKOnGroup();
    }
}
