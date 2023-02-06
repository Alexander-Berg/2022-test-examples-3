package ru.yandex.autotests.direct.cmd.adjustment.demography;

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
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Aqua.Test
@Description("Проверка настройки цен на мобильных устройствах для мобильной кампании (корректировка ставок demography_multiplier)")
@Stories(TestFeatures.Campaigns.ADJUSTMENT_DEMOGRAPHY)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class DemographyMultiplierMobileAppCampTest extends MultiplierTestBase {

    @Parameterized.Parameter(value = 0)
    public String multiplierPct;

    @Parameterized.Parameters(name = "Значение параметра multiplier_pct = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"151"},
                {"100"},
                {"0"}
        });
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition()
                .withGender(DemographyGenderEnum.FEMALE.getKey())
                .withAge(DemographyAgeEnum.BETWEEN_0_AND_17.getKey())
                .withMultiplierPct(multiplierPct));
        return new HierarchicalMultipliers()
                .withDemographyMultiplier(new DemographyMultiplier()
                        .withEnabled(1)
                        .withConditions(demographyConditionList));
    }

    @Override
    protected CampaignTypeEnum getCampaignTypeEnum() {
        return CampaignTypeEnum.MOBILE;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8955")
    public void checkSaveMobileMultiplierAtSaveNewCamp() {
        saveCampRequest.setMobileAppId(2450L);
        super.checkSaveMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("8956")
    public void checkSaveMobileMultiplierAtSaveCamp() {
        super.checkSaveMobileMultiplierAtSaveCamp();
    }
}
