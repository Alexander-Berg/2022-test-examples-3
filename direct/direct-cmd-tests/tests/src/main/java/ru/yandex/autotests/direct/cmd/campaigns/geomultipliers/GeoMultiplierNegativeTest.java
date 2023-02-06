package ru.yandex.autotests.direct.cmd.campaigns.geomultipliers;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignErrorResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка в контроллера saveCamp: сохранение некорректных корректировок")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SAVE_CAMP)
@RunWith(Parameterized.class)
public class GeoMultiplierNegativeTest extends GeoMultipliersValidationBaseTest {

    private static final String TOO_LARGE_PERCENT = "1301";
    private static final String TOO_SMALL_PERCENT = "9";
    private static final String ERROR_TEXT = "Поле задано неверно";
    private CampaignErrorResponse errorResponse;


    public GeoMultiplierNegativeTest(String percent) {
        this.percent = percent;
    }

    @Parameterized.Parameters(name = "Добавляем в кампанию минус слова {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {TOO_LARGE_PERCENT},
                {TOO_SMALL_PERCENT},
        });
    }


    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10898")
    public void checkHierarchicalMultipliers() {
        saveCampAndCheck();
    }


    @Override
    protected void saveCamp(SaveCampRequest request) {
        errorResponse = cmdRule.cmdSteps().campaignSteps().postSaveCampInvalidData(request);
    }

    @Override
    protected void checkCamp() {
        assumeThat("в ответе гео соответсвует ожиданиям", errorResponse.getCampaignErrors().getGeoChanges(),
                beanDiffer(geoChanges).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
        assertThat("ошибка соответсвует ожиданиям", errorResponse.getCampaignErrors().getError(),
                equalTo(CommonErrorsResource.INVALID_FIELD.toString()));
    }
}
