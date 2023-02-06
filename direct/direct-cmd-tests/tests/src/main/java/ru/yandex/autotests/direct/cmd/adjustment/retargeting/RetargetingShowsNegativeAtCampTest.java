package ru.yandex.autotests.direct.cmd.adjustment.retargeting;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtCampTestBase;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

@Aqua.Test
@Description("Проверка ошибок при корректировки ставок для посетивших сайт контроллерами saveNewCamp, saveCamp")
@Stories(TestFeatures.Campaigns.ADJUSTMENT_RETARGERING)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class RetargetingShowsNegativeAtCampTest extends MultiplierShowsNegativeAtCampTestBase {

    private static final String ERROR_NULL = RetargetingMultiplierErrors.ERROR_NULL.getErrorText();

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
    protected String getClient() {
        return "at-direct-adjustment-ret9";
    }

    @Override
    protected String getErrorText() {
        return ERROR_NULL;
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
    @ru.yandex.qatools.allure.annotations.TestCaseId("8994")
    public void checkSaveInvalidMobileMultiplierAtSaveNewCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8995")
    public void checkSaveInvalidMobileMultiplierAtSaveCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveCamp();
    }
}
