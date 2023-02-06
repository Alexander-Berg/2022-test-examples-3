package ru.yandex.autotests.direct.cmd.adjustment.retargeting;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtGroupTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingMultiplier;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.RetargetingMultiplierErrors;
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
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class RetargetingShowsNegativeAtGroupSaveTest extends MultiplierShowsNegativeAtGroupTestBase {

    @Parameterized.Parameter(value = 0)
    public String retargetingId;
    @Parameterized.Parameter(value = 1)
    public String description;

    @Parameterized.Parameters(name = "Значение параметра retargeting->conditions {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {null, "Сохранение null значения retargetingId"},
                {"2342a", "Сохранение не существующего значения retargetingId"}
        });
    }

    @Override
    public String getClient() {
        return "at-direct-adjustment-ret8";
    }

    @Override
    protected String[] getErrorText() {
        return new String[]{RetargetingMultiplierErrors.ERROR_NULL.getErrorText()};
    }

    @Override
    protected String getSaveGroupErrorText() {
        return RetargetingMultiplierErrors.ERROR_RETARGETING_NULL.getErrorText();
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        HashMap<String, RetargetingCondition> retargetingConditionMap = new HashMap<>();
        retargetingConditionMap.put(retargetingId, new RetargetingCondition()
                .withMultiplierPct("151"));

        return new HierarchicalMultipliers()
                .withRetargetingMultiplier(new RetargetingMultiplier()
                        .withEnabled(1)
                        .withConditions(retargetingConditionMap));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8996")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        super.checkSaveGroupMobileMultiplierAtSaveTextAdGroups();
    }

}
