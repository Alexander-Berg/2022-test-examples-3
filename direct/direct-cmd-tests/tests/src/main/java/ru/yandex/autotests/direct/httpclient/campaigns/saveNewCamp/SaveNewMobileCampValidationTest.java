package ru.yandex.autotests.direct.httpclient.campaigns.saveNewCamp;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.*;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignValidationErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка валидации параметров контроллера saveNewCamp при создании новой мобильной кампании")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.MOBILE)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(OldTag.YES)
public class SaveNewMobileCampValidationTest extends SaveNewCampValidationTestBase {

    @Override
    protected String getTemplateName() {
        return "mobileSaveCampParameters";
    }

    @Test
    @Description("Проверяем валидацию при сохранении кампании с пустым названием")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10388")
    public void incorrectNetworkTargetingValidationTest() {
        saveCampParameters.setNetworkTargeting("Inv");
        checkSaveNewCampValidation(TextResourceFormatter.resource(CampaignValidationErrors.INCORRECT_NETWORK_TARGETING).toString());
    }

    @Test
    @Description("Проверяем валидацию при сохранении кампании с пустым названием")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10368")
    public void incorrectDeviceTypeTargetingValidationTest() {
        saveCampParameters.setDeviceTypeTargeting("Inv2");
        checkSaveNewCampValidation(TextResourceFormatter.resource(CampaignValidationErrors.INCORRECT_DEVICE_TYPE_TARGETING).toString());
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10369")
    public void emptyCampaignNameValidationTest() {
        super.emptyCampaignNameValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10370")
    public void emptyEmailValidationTest() {
        super.emptyEmailValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10371")
    public void tooLongEmailValidationTest() {
        super.tooLongEmailValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10372")
    public void incorrectEmailValidationTest() {
        super.incorrectEmailValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10375")
    public void emptyStartDateValidationTest() {
        super.emptyStartDateValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10376")
    public void finishDateLessThanCurrentValidationTest() {
        super.finishDateLessThanCurrentValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10377")
    public void finishDateLessThanStartDateValidationTest() {
        super.finishDateLessThanStartDateValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10378")
    public void backspacesCampaignNameValidationTest() {
        super.backspacesCampaignNameValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10379")
    public void incorrectSymbolsCampaignNameValidationTest() {
        super.incorrectSymbolsCampaignNameValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10380")
    public void incorrectBroadMatchLimitValidationTest() {
        super.incorrectBroadMatchLimitValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10382")
    public void yandexDomainValidationTest() {
        super.yandexDomainValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10383")
    public void invalidDomainFormatValidationTest() {
        super.invalidDomainFormatValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10384")
    public void onlyThirdLevelDomainValidationTest() {
        super.onlyThirdLevelDomainValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10385")
    public void tooLongDomainDomainValidationTest() {
        super.tooLongDomainDomainValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10386")
    public void emptyStartDateAndDontShowAtYandexValidationTest() {
        super.emptyStartDateAndDontShowAtYandexValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10387")
    public void incorrectDisabledIpsValidationTest() {
        super.incorrectDisabledIpsValidationTest();
    }
}
