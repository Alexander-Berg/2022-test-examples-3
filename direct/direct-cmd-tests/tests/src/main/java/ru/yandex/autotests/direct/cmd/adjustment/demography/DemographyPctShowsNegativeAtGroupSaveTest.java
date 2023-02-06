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
import ru.yandex.autotests.direct.cmd.data.groups.DemographyMultiplierPctGroupErrors;
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

@Aqua.Test
@Description("Проверка ошибок при корректировки ставок по полу и возрасту (параметр multiplier_pct) " +
        "контроллером saveTextAdGroups")
@Stories(TestFeatures.Groups.ADJUSTMENT_DEMOGRAPHY)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class DemographyPctShowsNegativeAtGroupSaveTest extends MultiplierShowsNegativeAtGroupTestBase {

    @Parameterized.Parameter(value = 0)
    public DemographyMultiplierPctErrors demographyMultiplierPctErrors;
    @Parameterized.Parameter(value = 1)
    public DemographyMultiplierPctGroupErrors demographyMultiplierPctGroupErrors;
    @Parameterized.Parameter(value = 2)
    public String description;

    @Parameterized.Parameters(name = "Значение параметра demography->multiplier_pct {0} ({2})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {DemographyMultiplierPctErrors.TOO_SHORT, DemographyMultiplierPctGroupErrors.TOO_SHORT,
                        "Сохранение значения меньше допустимого"},
                {DemographyMultiplierPctErrors.TOO_LONG, DemographyMultiplierPctGroupErrors.TOO_LONG,
                        "Сохранение значения больше допустимого"},
                {DemographyMultiplierPctErrors.NOT_AN_INT, DemographyMultiplierPctGroupErrors.NOT_AN_INT,
                        "Сохранение нецелого значения"},
                {DemographyMultiplierPctErrors.NOT_AN_INT_TOO_SHORT, DemographyMultiplierPctGroupErrors.NOT_AN_INT_TOO_SHORT,
                        "Сохранение нецелого значения меньше допустимого"},
                {DemographyMultiplierPctErrors.NOT_AN_INT_TOO_LONG, DemographyMultiplierPctGroupErrors.NOT_AN_INT_TOO_LONG,
                        "Сохранение нецелого значения больше допустимого"},
        });
    }

    @Override
    protected String[] getErrorText() {
        return new String[]{demographyMultiplierPctErrors.getErrorText()};
    }

    @Override
    protected String getSaveGroupErrorText() {
        return demographyMultiplierPctGroupErrors.getErrorText();
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition()
                .withGender(DemographyGenderEnum.FEMALE.getKey())
                .withAge(DemographyAgeEnum.BETWEEN_0_AND_17.getKey())
                .withMultiplierPct(demographyMultiplierPctErrors.getValue()));
        return new HierarchicalMultipliers()
                .withDemographyMultiplier(new DemographyMultiplier()
                        .withEnabled(1)
                        .withConditions(demographyConditionList));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8961")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        super.checkSaveGroupMobileMultiplierAtSaveTextAdGroups();
    }
}
