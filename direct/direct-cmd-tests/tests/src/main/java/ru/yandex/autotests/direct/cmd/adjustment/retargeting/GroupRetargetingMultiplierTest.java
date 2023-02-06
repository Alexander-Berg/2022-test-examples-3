package ru.yandex.autotests.direct.cmd.adjustment.retargeting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.GroupMultiplierTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingMultiplier;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Проверка настройки цен для посетивших сайт (корректировка ставок retargeting_multiplier)")
@Stories(TestFeatures.Groups.ADJUSTMENT_RETARGERING)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class GroupRetargetingMultiplierTest extends GroupMultiplierTestBase {

    @Parameterized.Parameter(value = 0)
    public String multiplierPct;
    private String retargetingId;

    @Parameterized.Parameters(name = "Значение параметра multiplier_pct = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"151"},
                {"100"},
                {"50"}
        });
    }

    @Before
    @Override
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(getClient()).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(getClient()).getClientID()));
        cmdRule.apiSteps().retargetingSteps().addConditionsForUser(getClient(), 1);
        retargetingId = String.valueOf(cmdRule.apiSteps().
                retargetingSteps().getRetargetingConditions(getClient())[0]);
        super.before();
    }

    @Override
    protected String getClient() {
        return "at-direct-adjustment-ret2";
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers()
                .withRetargetingMultiplier(
                        RetargetingMultiplier.getDefaultRetargetingMultiplier(retargetingId, multiplierPct));
    }

    @Override
    protected HierarchicalMultipliers getExpectedHierarchicalMultipliers() {
        return getHierarchicalMultipliers();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8982")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        super.checkSaveGroupMobileMultiplierAtSaveTextAdGroups();
    }
}
