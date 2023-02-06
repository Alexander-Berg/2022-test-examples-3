package ru.yandex.autotests.direct.cmd.adjustment.demography;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtGroupTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyCondition;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.groups.DemographyMultiplierGroupErrors;
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

@Aqua.Test
@Description("Проверка ошибок при корректировки ставок по полу и возрасту " +
        "(кол-во корректировщиков, пересечения корректировок, значения age и gender) контроллера saveTextAdGroups")
@Stories(TestFeatures.Groups.ADJUSTMENT_DEMOGRAPHY)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class DemographyShowsNegativeAtGroupSaveTest extends MultiplierShowsNegativeAtGroupTestBase {

    private final static String MULTIPLIER_PCT = "151";
    private final static String INCORRECT_GENDER = "middle";
    private final static String INCORRECT_AGE = "0-171";

    @Parameterized.Parameter(0)
    public DemographyMultiplierErrors demographyMultiplierErrors;
    @Parameterized.Parameter(1)
    public DemographyMultiplierGroupErrors demographyMultiplierGroupErrors;
    @Parameterized.Parameter(2)
    public HierarchicalMultipliers hierarchicalMultipliers;
    @Parameterized.Parameter(3)
    public String description;

    @Parameterized.Parameters(name = "Неверные значения корректировки по полу и возрасту: {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {DemographyMultiplierErrors.ERROR_CONDITIONS_COUNT_TEXT,
                        DemographyMultiplierGroupErrors.ERROR_CONDITIONS_COUNT_TEXT, getWrongConditionCount(),
                        "Сохранение некорректного (больше максимального) кол-ва demography корректировщиков"},
                {DemographyMultiplierErrors.ERROR_INTERSECT_CONDITIONS_TEXT,
                        DemographyMultiplierGroupErrors.ERROR_INTERSECT_CONDITIONS_TEXT, getInvalidIntersectConditions(),
                        "Сохранение пересекающихся корректировок"},
                {DemographyMultiplierErrors.ERROR_INCORRECT_DATA,
                        DemographyMultiplierGroupErrors.ERROR_INCORRECT_DATA, getInvalidConditionParameters(),
                        "Сохранение неправильных значений"},
                {DemographyMultiplierErrors.ERROR_NULL_DATA,
                        DemographyMultiplierGroupErrors.ERROR_NULL_DATA, getNullConditionParameters(),
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
    protected String[] getErrorText() {
        return demographyMultiplierErrors.getErrorText().split("\n");
    }

    @Override
    protected String getSaveGroupErrorText() {
        return demographyMultiplierGroupErrors.getErrorText();
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8965")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        super.checkSaveGroupMobileMultiplierAtSaveTextAdGroups();
    }
}
