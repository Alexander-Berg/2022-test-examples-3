package ru.yandex.autotests.direct.cmd.adjustment.mobile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.GroupMultiplierTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.MobileMultiplier;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Проверка настройки цен на мобильных устройствах для группы объявлений (параметр group_mobile_multiplier_pct)")
@Stories(TestFeatures.Groups.ADJUSTMENT_MOBILE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class GroupMobileMultiplierTest extends GroupMultiplierTestBase {

    @Parameterized.Parameter(value = 0)
    public String multiplierPct;

    @Parameterized.Parameters(name = "Значение параметра multiplier_pct = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"151"},
                {"100"},
                {"50"}
        });
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers().
                withMobileMultiplier(new MobileMultiplier().
                        withMultiplierPct(multiplierPct));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8972")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        super.checkSaveGroupMobileMultiplierAtSaveTextAdGroups();
    }
}
