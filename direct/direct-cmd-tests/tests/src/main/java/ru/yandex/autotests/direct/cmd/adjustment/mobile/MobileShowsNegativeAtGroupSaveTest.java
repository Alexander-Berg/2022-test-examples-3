package ru.yandex.autotests.direct.cmd.adjustment.mobile;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtGroupTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.MobileMultiplier;
import ru.yandex.autotests.direct.cmd.data.groups.MobileMultiplierPctGroupErrors;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.MobileMultiplierPctErrors;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка ошибок при настройке цен на мобильных устройствах (параметр mobile_multiplier_pct) " +
        "контроллера saveTextAdGroups")
@Stories(TestFeatures.Groups.ADJUSTMENT_MOBILE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class MobileShowsNegativeAtGroupSaveTest extends MultiplierShowsNegativeAtGroupTestBase {

    @Parameterized.Parameter(value = 0)
    public MobileMultiplierPctErrors mobileMultiplierPctErrors;
    @Parameterized.Parameter(value = 1)
    public MobileMultiplierPctGroupErrors mobileMultiplierPctGroupErrors;
    @Parameterized.Parameter(value = 2)
    public String description;

    @Parameterized.Parameters(name = "Значение параметра mobile_multiplier_pct {0} ({2})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {MobileMultiplierPctErrors.TOO_SHORT, MobileMultiplierPctGroupErrors.TOO_SHORT,
                        "Сохранение значения меньше допустимого"},
                {MobileMultiplierPctErrors.TOO_LONG, MobileMultiplierPctGroupErrors.TOO_LONG,
                        "Сохранение значения больше допустимого"},
                {MobileMultiplierPctErrors.NOT_AN_INT, MobileMultiplierPctGroupErrors.NOT_AN_INT,
                        "Сохранение нецелого значения"},
                {MobileMultiplierPctErrors.NOT_AN_INT_TOO_SHORT, MobileMultiplierPctGroupErrors.NOT_AN_INT_TOO_SHORT,
                        "Сохранение нецелого значения меньше допустимого"},
                {MobileMultiplierPctErrors.NOT_AN_INT_TOO_LONG, MobileMultiplierPctGroupErrors.NOT_AN_INT_TOO_LONG,
                        "Сохранение нецелого значения больше допустимого"},
        });
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return
                new HierarchicalMultipliers().
                        withMobileMultiplier(new MobileMultiplier().
                                withMultiplierPct(mobileMultiplierPctErrors.getValue()));
    }

    @Override
    protected String[] getErrorText() {
        return new String[]{mobileMultiplierPctErrors.getErrorText()};
    }

    @Override
    protected String getSaveGroupErrorText() {
        return mobileMultiplierPctGroupErrors.getErrorText();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8978")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        super.checkSaveGroupMobileMultiplierAtSaveTextAdGroups();
    }
}
