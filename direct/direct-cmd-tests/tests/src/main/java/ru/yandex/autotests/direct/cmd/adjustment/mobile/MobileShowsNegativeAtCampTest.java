package ru.yandex.autotests.direct.cmd.adjustment.mobile;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtCampTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.MobileMultiplier;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.MobileMultiplierPctErrors;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Проверка ошибок при настройке цен на мобильных устройствах кампании (параметр mobile_multiplier_pct)")
@Stories(TestFeatures.Campaigns.ADJUSTMENT_MOBILE)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class MobileShowsNegativeAtCampTest extends MultiplierShowsNegativeAtCampTestBase {

    @Parameterized.Parameter(value = 0)
    public MobileMultiplierPctErrors mobileMultiplierPctErrors;
    @Parameterized.Parameter(value = 1)
    public String description;

    @Parameterized.Parameters(name = "Значение параметра mobile_multiplier_pct {0} ({1})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {MobileMultiplierPctErrors.TOO_SHORT, "Сохранение значения меньше допустимого"},
                {MobileMultiplierPctErrors.TOO_LONG, "Сохранение значения больше допустимого"},
                {MobileMultiplierPctErrors.NOT_AN_INT, "Сохранение нецелого значения"},
                {MobileMultiplierPctErrors.NOT_AN_INT_TOO_SHORT, "Сохранение нецелого значения меньше допустимого"},
                {MobileMultiplierPctErrors.NOT_AN_INT_TOO_LONG, "Сохранение нецелого значения больше допустимого"},
        });
    }

    @Override
    protected String getErrorText() {
        return mobileMultiplierPctErrors.getErrorText();
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers().
                withMobileMultiplier(new MobileMultiplier().
                        withMultiplierPct(mobileMultiplierPctErrors.getValue()));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8976")
    public void checkSaveInvalidMobileMultiplierAtSaveNewCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8977")
    public void checkSaveInvalidMobileMultiplierAtSaveCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveCamp();
    }
}
