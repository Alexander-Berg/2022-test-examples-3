package ru.yandex.autotests.direct.cmd.adjustment.demography;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtCampTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyCondition;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.adjustment.rates.DemographyMultiplierPctErrors;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyAgeEnum;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyGenderEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Aqua.Test
@Description("Проверка ошибок при корректировки ставок по полу и возрасту (параметр multiplier_pct)" +
        " контроллерами saveNewCamp, saveCamp")
@Stories(TestFeatures.Campaigns.ADJUSTMENT_DEMOGRAPHY)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class DemographyPctShowsNegativeAtCampTest extends MultiplierShowsNegativeAtCampTestBase {

    @Parameterized.Parameter(value = 0)
    public DemographyMultiplierPctErrors demographyMultiplierPctErrors;
    @Parameterized.Parameter(value = 1)
    public String description;

    @Parameterized.Parameters(name = "Значение параметра demography_multiplier_pct {0} ({1})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {DemographyMultiplierPctErrors.TOO_SHORT, "Сохранение значения меньше допустимого"},
                {DemographyMultiplierPctErrors.TOO_LONG, "Сохранение значения больше допустимого"},
                {DemographyMultiplierPctErrors.NOT_AN_INT, "Сохранение нецелого значения"},
                {DemographyMultiplierPctErrors.NOT_AN_INT_TOO_SHORT, "Сохранение нецелого значения меньше допустимого"},
                {DemographyMultiplierPctErrors.NOT_AN_INT_TOO_LONG, "Сохранение нецелого значения больше допустимого"},
        });
    }

    @Override
    protected String getErrorText() {
        return demographyMultiplierPctErrors.getErrorText();
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition()
                .withGender(DemographyGenderEnum.FEMALE.getKey())
                .withAge(DemographyAgeEnum.BETWEEN_0_AND_17.getKey())
                .withMultiplierPct(String.valueOf(demographyMultiplierPctErrors.getValue())));
        return new HierarchicalMultipliers()
                .withDemographyMultiplier(new DemographyMultiplier()
                        .withEnabled(1)
                        .withConditions(demographyConditionList));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8959")
    public void checkSaveInvalidMobileMultiplierAtSaveNewCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8960")
    public void checkSaveInvalidMobileMultiplierAtSaveCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveCamp();
    }
}
