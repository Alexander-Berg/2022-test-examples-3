package ru.yandex.autotests.direct.cmd.adjustment.retargeting;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtGroupTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingMultiplier;
import ru.yandex.autotests.direct.cmd.data.groups.RetargetingMultiplierPctGroupErrors;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.adjustment.rates.RetargetingMultiplierPctErrors;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка ошибок при корректировки ставок для посетивших сайт (параметр multiplier_pct) " +
        "контроллера saveTextAdGroups")
@Stories(TestFeatures.Groups.ADJUSTMENT_RETARGERING)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class RetargetingPctShowsNegativeAtGroupSaveTest extends MultiplierShowsNegativeAtGroupTestBase {

    @Parameterized.Parameter(value = 0)
    public RetargetingMultiplierPctErrors retargetingMultiplierPctErrors;
    @Parameterized.Parameter(value = 1)
    public RetargetingMultiplierPctGroupErrors retargetingMultiplierPctGroupErrors;
    @Parameterized.Parameter(value = 2)
    public String description;
    private String retargetingId;

    @Parameterized.Parameters(name = "Значение параметра retargeting->multiplier_pct {0} ({2})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {RetargetingMultiplierPctErrors.TOO_SHORT, RetargetingMultiplierPctGroupErrors.TOO_SHORT,
                        "Сохранение значения меньше допустимого"},
                {RetargetingMultiplierPctErrors.TOO_LONG, RetargetingMultiplierPctGroupErrors.TOO_LONG,
                        "Сохранение значения больше допустимого"},
                {RetargetingMultiplierPctErrors.NOT_AN_INT, RetargetingMultiplierPctGroupErrors.NOT_AN_INT,
                        "Сохранение нецелого значения"},
                {RetargetingMultiplierPctErrors.NOT_AN_INT_TOO_SHORT, RetargetingMultiplierPctGroupErrors.NOT_AN_INT_TOO_SHORT,
                        "Сохранение нецелого значения меньше допустимого"},
                {RetargetingMultiplierPctErrors.NOT_AN_INT_TOO_LONG, RetargetingMultiplierPctGroupErrors.NOT_AN_INT_TOO_LONG,
                        "Сохранение нецелого значения больше допустимого"},
        });
    }

    @Override
    public String getClient() {
        return "at-direct-adjustment-ret5";
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
    protected String[] getErrorText() {
        return new String[]{retargetingMultiplierPctErrors.getErrorText()};
    }

    @Override
    protected String getSaveGroupErrorText() {
        return retargetingMultiplierPctGroupErrors.getErrorText();
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        HashMap<String, RetargetingCondition> retargetingConditionMap = new HashMap<>();
        retargetingConditionMap.put(String.valueOf(retargetingId), new RetargetingCondition()
                .withMultiplierPct(retargetingMultiplierPctErrors.getValue()));

        return new HierarchicalMultipliers()
                .withRetargetingMultiplier(new RetargetingMultiplier()
                        .withEnabled(1)
                        .withConditions(retargetingConditionMap));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8992")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        super.checkSaveGroupMobileMultiplierAtSaveTextAdGroups();
    }

}
