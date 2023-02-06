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
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierTestBase;
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

@Aqua.Test
@Description("Проверка корректировки ставок для посетивших сайт (параметр enabled)")
@Stories(TestFeatures.Campaigns.ADJUSTMENT_RETARGERING)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class RetargetingEnabledTest extends MultiplierTestBase {

    @Parameterized.Parameter(value = 0)
    public Integer enabled;
    @Parameterized.Parameter(value = 1)
    public Integer expectedEnabled;
    private String retargetingId;

    @Parameterized.Parameters(name = "Значение параметра retargeting->enabled   было: {0} - стало: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {null, 0},
                {-1, 0},
                {0, 0},
                {3, 0},
                {1, 1}
        });
    }

    @Override
    public String getClient() {
        return "at-direct-adjustment-ret3";
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
        retargetingConditionMap.put(retargetingId, new RetargetingCondition().
                withMultiplierPct(VALID_MULTIPLIER));
        return retargetingConditionMap;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8984")
    public void checkSaveMobileMultiplierAtSaveNewCamp() {
        super.checkSaveMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8985")
    public void checkSaveMobileMultiplierAtSaveCamp() {
        super.checkSaveMobileMultiplierAtSaveCamp();
    }
}
