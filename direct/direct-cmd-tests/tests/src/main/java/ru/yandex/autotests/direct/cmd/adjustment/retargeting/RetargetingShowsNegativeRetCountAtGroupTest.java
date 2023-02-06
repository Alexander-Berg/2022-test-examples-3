package ru.yandex.autotests.direct.cmd.adjustment.retargeting;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtGroupTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingMultiplier;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.RetargetingMultiplierErrors;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка ошибок при корректировки ставок для посетивших сайт контроллером saveTextAdGroups")
@Stories(TestFeatures.Groups.ADJUSTMENT_RETARGERING)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class RetargetingShowsNegativeRetCountAtGroupTest extends MultiplierShowsNegativeAtGroupTestBase {

    private int[] retargetingIds;

    @Override
    protected String getClient() {
        return "at-direct-adjustment-ret11";
    }

    @Before
    @Override
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(getClient()).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(getClient()).getClientID()));
        cmdRule.apiSteps().retargetingSteps().addConditionsForUser(getClient(), 101);
        retargetingIds = cmdRule.apiSteps().retargetingSteps().getRetargetingConditions(getClient());
        super.before();
    }


    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        HashMap<String, RetargetingCondition> retargetingConditionMap = new HashMap<>();
        for (int retargetingId : retargetingIds) {
            retargetingConditionMap.put(String.valueOf(retargetingId), new RetargetingCondition()
                    .withMultiplierPct(VALID_MULTIPLIER));
        }

        return new HierarchicalMultipliers()
                .withRetargetingMultiplier(new RetargetingMultiplier()
                        .withEnabled(1)
                        .withConditions(retargetingConditionMap));
    }

    @Override
    protected String[] getErrorText() {
        return new String[]{RetargetingMultiplierErrors.ERROR_COUNT.getErrorText()};
    }

    @Override
    protected String getSaveGroupErrorText() {
        return RetargetingMultiplierErrors.ERROR_RETARGETING_COUNT.getErrorText();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9000")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        super.checkSaveGroupMobileMultiplierAtSaveTextAdGroups();
    }
}
