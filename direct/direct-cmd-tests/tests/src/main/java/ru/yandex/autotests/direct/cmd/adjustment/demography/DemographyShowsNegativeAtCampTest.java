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
import ru.yandex.autotests.direct.httpclient.data.campaigns.DemographyMultiplierErrors;
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
@Description("Проверка ошибок при корректировки ставок по полу и возрасту " +
        "(кол-во корректировщиков, пересечения корректировок, значения age и gender) контроллерами saveNewCamp, saveCamp")
@Stories(TestFeatures.Campaigns.ADJUSTMENT_DEMOGRAPHY)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class DemographyShowsNegativeAtCampTest extends MultiplierShowsNegativeAtCampTestBase {

    private final static String MULTIPLIER_PCT = "151";
    private final static String INCORRECT_GENDER = "middle";
    private final static String INCORRECT_AGE = "0-171";

    @Parameterized.Parameter(0)
    public DemographyMultiplierErrors demographyMultiplierErrors;
    @Parameterized.Parameter(1)
    public HierarchicalMultipliers hierarchicalMultipliers;
    @Parameterized.Parameter(2)
    public String description;

    @Parameterized.Parameters(name = "Неверные значения корректировки по полу и возрасту: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {DemographyMultiplierErrors.ERROR_INCORRECT_FIELD_VALUE, getWrongConditionCount(),
                        "Сохранение некорректного (больше максимального) кол-во demography корректировщиков"},
                {DemographyMultiplierErrors.ERROR_INCORRECT_FIELD_VALUE, getInvalidIntersectConditions(),
                        "Сохранение пересекающихся корректировок"},
                {DemographyMultiplierErrors.ERROR_INCORRECT_FIELD_VALUE, getInvalidConditionParameters(),
                        "Сохранение неправильных значений"},
                {DemographyMultiplierErrors.ERROR_INCORRECT_FIELD_VALUE, getNullConditionParameters(),
                        "Сохранение null значений"}
        });
    }

    private static HierarchicalMultipliers getHierarchicalMultipliersWithDemography(
            List<DemographyCondition> demographyConditions) {
        return new HierarchicalMultipliers()
                .withDemographyMultiplier(new DemographyMultiplier()
                        .withEnabled(1)
                        .withConditions(demographyConditions));
    }

    private static HierarchicalMultipliers getWrongConditionCount() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        for (DemographyGenderEnum genderEnum : DemographyGenderEnum.values()) {
            for (DemographyAgeEnum ageEnum : DemographyAgeEnum.values()) {
                demographyConditionList.add(new DemographyCondition()
                        .withGender(genderEnum.getKey())
                        .withAge(ageEnum.getKey())
                        .withMultiplierPct(MULTIPLIER_PCT));
            }
        }
        return getHierarchicalMultipliersWithDemography(demographyConditionList);
    }

    private static HierarchicalMultipliers getInvalidIntersectConditions() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition()
                .withGender(DemographyGenderEnum.FEMALE.getKey())
                .withAge(DemographyAgeEnum.BETWEEN_0_AND_17.getKey())
                .withMultiplierPct(MULTIPLIER_PCT));
        demographyConditionList.add(new DemographyCondition()
                .withGender(DemographyGenderEnum.ALL.getKey())
                .withAge(DemographyAgeEnum.BETWEEN_0_AND_17.getKey())
                .withMultiplierPct(MULTIPLIER_PCT));
        return getHierarchicalMultipliersWithDemography(demographyConditionList);
    }

    private static HierarchicalMultipliers getInvalidConditionParameters() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition()
                .withGender(INCORRECT_GENDER)
                .withAge(INCORRECT_AGE)
                .withMultiplierPct(MULTIPLIER_PCT));
        return getHierarchicalMultipliersWithDemography(demographyConditionList);
    }

    private static HierarchicalMultipliers getNullConditionParameters() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition()
                .withGender(null)
                .withAge(null)
                .withMultiplierPct(null));
        return getHierarchicalMultipliersWithDemography(demographyConditionList);
    }

    @Override
    protected String getErrorText() {
        return demographyMultiplierErrors.getErrorText();
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8963")
    public void checkSaveInvalidMobileMultiplierAtSaveNewCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8964")
    public void checkSaveInvalidMobileMultiplierAtSaveCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveCamp();
    }
}
