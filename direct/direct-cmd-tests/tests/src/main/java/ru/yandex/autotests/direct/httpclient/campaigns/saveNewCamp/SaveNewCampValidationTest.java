package ru.yandex.autotests.direct.httpclient.campaigns.saveNewCamp;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.*;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка валидации параметров контроллера saveNewCamp при создании новой кампании")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(OldTag.YES)
public class SaveNewCampValidationTest extends SaveNewCampValidationTestBase {

    @Override
    protected String getTemplateName() {
        return "defaultSaveCampParameters";
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10349")
    public void emptyCampaignNameValidationTest() {
        super.emptyCampaignNameValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10350")
    public void emptyEmailValidationTest() {
        super.emptyEmailValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10351")
    public void tooLongEmailValidationTest() {
        super.tooLongEmailValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10352")
    public void incorrectEmailValidationTest() {
        super.incorrectEmailValidationTest();
    }
    
    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10355")
    public void emptyStartDateValidationTest() {
        super.emptyStartDateValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10356")
    public void finishDateLessThanCurrentValidationTest() {
        super.finishDateLessThanCurrentValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10357")
    public void finishDateLessThanStartDateValidationTest() {
        super.finishDateLessThanStartDateValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10358")
    public void backspacesCampaignNameValidationTest() {
        super.backspacesCampaignNameValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10359")
    public void incorrectSymbolsCampaignNameValidationTest() {
        super.incorrectSymbolsCampaignNameValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10360")
    public void incorrectBroadMatchLimitValidationTest() {
        super.incorrectBroadMatchLimitValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10362")
    public void yandexDomainValidationTest() {
        super.yandexDomainValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10363")
    public void invalidDomainFormatValidationTest() {
        super.invalidDomainFormatValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10364")
    public void onlyThirdLevelDomainValidationTest() {
        super.onlyThirdLevelDomainValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10365")
    public void tooLongDomainDomainValidationTest() {
        super.tooLongDomainDomainValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10366")
    public void emptyStartDateAndDontShowAtYandexValidationTest() {
        super.emptyStartDateAndDontShowAtYandexValidationTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10367")
    public void incorrectDisabledIpsValidationTest() {
        super.incorrectDisabledIpsValidationTest();
    }
}
