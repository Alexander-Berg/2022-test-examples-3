package ru.yandex.autotests.direct.cmd.campaigns.geomultipliers;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.GeoMultiplier;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка в контроллера saveCamp: сохранение корректировок с граничными условиями")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SAVE_CAMP)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class GeoMultiplierPositiveTest extends GeoMultipliersValidationBaseTest{
    private static final String LARGEST_PERCENT= "1300";
    private static final String SMALLEST_PERCENT= "10";


    public GeoMultiplierPositiveTest(String percent) {
        this.percent = percent;
    }

    @Parameterized.Parameters(name = "Добавляем в кампанию минус слова {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {LARGEST_PERCENT},
                {SMALLEST_PERCENT},
        });
    }



    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10899")
    public void checkHierarchicalMultipliers() {
        saveCampAndCheck();
    }


    @Override
    protected void saveCamp(SaveCampRequest request) {
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);
    }

    @Override
    protected void checkCamp() {
        GeoMultiplier actualGeoMultiplier =
                bannersRule.getCurrentCampaign().getHierarchicalMultipliers().getGeoMultiplier();

        GeoMultiplier expectedGeoMultiplier = new GeoMultiplier()
                .withRegions(geoMultipliers);

        assertThat("гео-корректировки соотвествует ожиданиям", actualGeoMultiplier,
                beanDiffer(expectedGeoMultiplier).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}
