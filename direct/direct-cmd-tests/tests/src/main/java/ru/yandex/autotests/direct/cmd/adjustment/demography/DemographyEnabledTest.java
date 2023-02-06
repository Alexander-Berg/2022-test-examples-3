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
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyCondition;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyAgeEnum;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyGenderEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка корректировки ставок по полу и возрасту (параметр enabled)")
@Stories(TestFeatures.Campaigns.ADJUSTMENT_DEMOGRAPHY)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class DemographyEnabledTest extends MultiplierTestBase {

    @Parameterized.Parameter(value = 0)
    public Integer enabled;
    @Parameterized.Parameter(value = 1)
    public Integer expectedEnabled;

    @Parameterized.Parameters(name = "Значение параметра demography->enabled   было: {0} - стало: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {null, 0},
                {-1, 0},
                {0, 0},
                {3, 0},
                {1, 1}
        });
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers()
                .withDemographyMultiplier(new DemographyMultiplier()
                        .withEnabled(enabled)
                        .withConditions(getDemographyConditions()));
    }

    @Override
    protected HierarchicalMultipliers getExpectedHierarchicalMultipliers() {
        return new HierarchicalMultipliers()
                .withDemographyMultiplier(new DemographyMultiplier()
                        .withEnabled(expectedEnabled)
                        .withConditions(getDemographyConditions()));
    }

    private List<DemographyCondition> getDemographyConditions() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition()
                .withGender(DemographyGenderEnum.FEMALE.getKey())
                .withAge(DemographyAgeEnum.BETWEEN_0_AND_17.getKey())
                .withMultiplierPct(VALID_MULTIPLIER));
        return demographyConditionList;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8953")
    public void checkSaveMobileMultiplierAtSaveNewCamp() {
        super.checkSaveMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8954")
    public void checkSaveMobileMultiplierAtSaveCamp() {
        super.checkSaveMobileMultiplierAtSaveCamp();
    }
}
