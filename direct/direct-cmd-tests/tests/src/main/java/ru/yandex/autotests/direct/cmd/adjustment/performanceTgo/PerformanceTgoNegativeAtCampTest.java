package ru.yandex.autotests.direct.cmd.adjustment.performanceTgo;

import java.util.Arrays;
import java.util.Collection;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtCampTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.PerformanceTgoMultiplier;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.PerformanceTgoMultiplierPctErrors;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка ошибок при настройке корректировок Смарт-ТГО у кампании (параметр performance_tgo_multiplier)")
@Stories(TestFeatures.Campaigns.ADJUSTMENT_PERFORMANCE_TGO)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.PERFORMANCE)
@RunWith(Parameterized.class)
public class PerformanceTgoNegativeAtCampTest extends MultiplierShowsNegativeAtCampTestBase {

    @Parameterized.Parameter(value = 0)
    public PerformanceTgoMultiplierPctErrors performanceTgoMultiplierPctErrors;
    @Parameterized.Parameter(value = 1)
    public String description;

    @Parameterized.Parameters(name = "Значение параметра performance_tgo_multiplier.multiplier_pct {0} ({1})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {PerformanceTgoMultiplierPctErrors.TOO_SHORT, "Сохранение значения меньше допустимого"},
                {PerformanceTgoMultiplierPctErrors.TOO_LONG, "Сохранение значения больше допустимого"},
                {PerformanceTgoMultiplierPctErrors.NOT_AN_INT, "Сохранение нецелого значения"},
                {PerformanceTgoMultiplierPctErrors.NOT_AN_INT_TOO_SHORT,
                        "Сохранение нецелого значения меньше допустимого"},
                {PerformanceTgoMultiplierPctErrors.NOT_AN_INT_TOO_LONG,
                        "Сохранение нецелого значения больше допустимого"},
        });
    }

    @Override
    protected String getErrorText() {
        return performanceTgoMultiplierPctErrors.getErrorText();
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers().
                withPerformanceTgoMultiplier(new PerformanceTgoMultiplier().
                        withMultiplierPct(performanceTgoMultiplierPctErrors.getValue()));
    }

    @Override
    protected CampaignTypeEnum getCampaignType() {
        return CampaignTypeEnum.DMO;
    }

    @Test
    @TestCaseId("11027")
    public void checkSaveInvalidPerformanceTgoMultiplierAtSaveNewCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @TestCaseId("11028")
    public void checkSaveInvalidPerformanceTgoMultiplierAtSaveCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveCamp();
    }
}
