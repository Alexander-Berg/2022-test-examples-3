package ru.yandex.autotests.direct.cmd.campaigns.save;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.CampaignErrors;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignErrorResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Валидация параметров при создании перформанс кампании")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.PERFORMANCE)
public class SaveNewPerformanceCampValidationTest extends SaveCampValidationBaseTest {

    public SaveNewPerformanceCampValidationTest() {
        super(CampaignTypeEnum.DMO);
    }

    @Test
    @Description("Валидация пустого metrikaCounters при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9451")
    public void testSaveCampEmptyMetrikaCounters() {
        request.setMetrika_counters(null);
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_METRIKA_COUNTER).toString());
    }

    @Override
    protected void sendAndCheckCampaignErrors(String... errors) {
        String error = StringUtils.join(errors, "\n");
        CampaignErrorResponse response = cmdRule.cmdSteps().campaignSteps().postSaveNewCampInvalidData(request);
        assertThat("ошибка соответствует ожидаемой", response.getCampaignErrors().getError(), equalTo(error));
    }

    @Override
    protected void sendAndCheckCommonError(String error) {
        CampaignErrorResponse response = cmdRule.cmdSteps().campaignSteps().postSaveNewCampInvalidData(request);
        assertThat("ошибка соответствует ожидаемой", response.getError(), equalTo(error));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9450")
    public void testSaveCampEmptyName() {
        super.testSaveCampEmptyName();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9452")
    public void testSaveCampWithOnlySpacesInName() {
        super.testSaveCampWithOnlySpacesInName();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9453")
    public void testSaveCampWithIncorrectCharsInName() {
        super.testSaveCampWithIncorrectCharsInName();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9454")
    public void testSaveCampEmptyEmail() {
        super.testSaveCampEmptyEmail();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9455")
    public void testSaveCampLongEmail() {
        super.testSaveCampLongEmail();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9456")
    public void testSaveCampInvalidEmail() {
        super.testSaveCampInvalidEmail();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9459")
    public void testSaveCampEmptyStartDate() {
        super.testSaveCampEmptyStartDate();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9460")
    public void testSaveCampFinishDateLowerThanCurrentDate() {
        super.testSaveCampFinishDateLowerThanCurrentDate();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9461")
    public void testSaveCampFinishDateLowerThanStartDate() {
        super.testSaveCampFinishDateLowerThanStartDate();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9462")
    public void testSaveCampDontShowAtYandex() {
        super.testSaveCampDontShowAtYandex();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9463")
    public void testSaveCampDontShowInvalidDomain() {
        super.testSaveCampDontShowInvalidDomain();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9464")
    public void testSaveCampDontShowInvalidSecondLevelDomain() {
        super.testSaveCampDontShowInvalidSecondLevelDomain();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9465")
    public void testSaveCampDontShowTooLongDomain() {
        super.testSaveCampDontShowTooLongDomain();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9466")
    public void testSaveCampInvalidTwoFields() {
        super.testSaveCampInvalidTwoFields();
    }
}
