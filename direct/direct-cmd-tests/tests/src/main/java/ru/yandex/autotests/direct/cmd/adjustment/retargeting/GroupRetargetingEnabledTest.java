package ru.yandex.autotests.direct.cmd.adjustment.retargeting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.GroupMultiplierTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingCondition;
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
import java.util.HashMap;

@Aqua.Test
@Description("Проверка корректировки ставок для посетивших сайт для группы объявлений  (параметр enabled)")
@Stories(TestFeatures.Groups.ADJUSTMENT_RETARGERING)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class GroupRetargetingEnabledTest extends GroupMultiplierTestBase {

    @Parameterized.Parameter(value = 0)
    public Integer enabled;
    @Parameterized.Parameter(value = 1)
    public Integer expectedEnabled;
    private String retargetingId;

    @Parameterized.Parameters(name = "Значение параметра demography->enabled   было: {0} - стало: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {null, 0},
                {-1, 1},
                {0, 0},
                {3, 1},
                {1, 1}
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
        return "at-direct-adjustment-ret1";
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers()
                .withRetargetingMultiplier(new RetargetingMultiplier()
                        .withEnabled(enabled)
                        .withConditions(getConditionHashMap()));
    }

    @Override
    protected HierarchicalMultipliers getExpectedHierarchicalMultipliers() {
        return new HierarchicalMultipliers()
                .withRetargetingMultiplier(new RetargetingMultiplier()
                        .withEnabled(expectedEnabled)
                        .withConditions(getConditionHashMap()));
    }

    private HashMap<String, RetargetingCondition> getConditionHashMap() {
        HashMap<String, RetargetingCondition> retargetingConditionMap = new HashMap<>();
        retargetingConditionMap.put(retargetingId, new RetargetingCondition().withMultiplierPct(VALID_MULTIPLIER));
        return retargetingConditionMap;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8980")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        super.checkSaveGroupMobileMultiplierAtSaveTextAdGroups();
    }
}
