package ru.yandex.autotests.direct.cmd.campaigns.save;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.TestCaseId;

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
@Description("Валидация параметров при создании кампании ГО на поиске")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.MCBANNER)
public class SaveNewMcbannerCampValidationTest extends SaveCampValidationBaseTest {

    public SaveNewMcbannerCampValidationTest() {
        super(CampaignTypeEnum.MCBANNER);
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
    @TestCaseId("10959")
    public void testSaveCampEmptyName() {
        super.testSaveCampEmptyName();
    }

    @Test
    @Override
    @TestCaseId("10960")
    public void testSaveCampWithOnlySpacesInName() {
        super.testSaveCampWithOnlySpacesInName();
    }

    @Test
    @Override
    @TestCaseId("10958")
    public void testSaveCampWithIncorrectCharsInName() {
        super.testSaveCampWithIncorrectCharsInName();
    }

    @Test
    @Override
    @TestCaseId("10961")
    public void testSaveCampEmptyEmail() {
        super.testSaveCampEmptyEmail();
    }

    @Test
    @Override
    @TestCaseId("10962")
    public void testSaveCampLongEmail() {
        super.testSaveCampLongEmail();
    }

    @Test
    @Override
    @TestCaseId("10963")
    public void testSaveCampInvalidEmail() {
        super.testSaveCampInvalidEmail();
    }

    @Test
    @Override
    @TestCaseId("10964")
    public void testSaveCampEmptyStartDate() {
        super.testSaveCampEmptyStartDate();
    }

    @Test
    @Override
    @TestCaseId("10965")
    public void testSaveCampFinishDateLowerThanCurrentDate() {
        super.testSaveCampFinishDateLowerThanCurrentDate();
    }

    @Test
    @Override
    @TestCaseId("10966")
    public void testSaveCampFinishDateLowerThanStartDate() {
        super.testSaveCampFinishDateLowerThanStartDate();
    }

    @Test
    @Override
    @TestCaseId("10967")
    public void testSaveCampDontShowAtYandex() {
        super.testSaveCampDontShowAtYandex();
    }

    @Test
    @Override
    @TestCaseId("10968")
    public void testSaveCampDontShowInvalidDomain() {
        super.testSaveCampDontShowInvalidDomain();
    }

    @Test
    @Override
    @TestCaseId("10969")
    public void testSaveCampDontShowInvalidSecondLevelDomain() {
        super.testSaveCampDontShowInvalidSecondLevelDomain();
    }

    @Test
    @Override
    @TestCaseId("10970")
    public void testSaveCampDontShowTooLongDomain() {
        super.testSaveCampDontShowTooLongDomain();
    }

    @Test
    @Override
    @TestCaseId("10971")
    public void testSaveCampInvalidTwoFields() {
        super.testSaveCampInvalidTwoFields();
    }
}
