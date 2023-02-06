package ru.yandex.autotests.direct.cmd.campaigns.save;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.CampaignErrors;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignErrorResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
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
@Description("Валидация параметров при сохранении существующей перформанс кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.PERFORMANCE)
public class SavePerformanceCampValidationTest extends SaveCampValidationBaseTest {

    private BannersRule bannersRule;

    public SavePerformanceCampValidationTest() {
        super(CampaignTypeEnum.DMO);
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Override
    public void before() {
        super.before();
        request.withCid(String.valueOf(bannersRule.getCampaignId()));
    }

    @Test
    @Description("Валидация пустого cid при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9468")
    public void testSaveCampEmptyCid() {
        request.setCid(null);
        sendAndCheckCommonError(TextResourceFormatter.resource(CampaignErrors.EMPTY_CID).toString());
    }

    @Test
    @Description("Валидация некорректного cid при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9469")
    public void testSaveCampInvalidCid() {
        String cid = "aaa";
        request.setCid(cid);
        sendAndCheckCommonError(String.format(TextResourceFormatter.resource(CampaignErrors.INCORRECT_CID).toString(), cid));
    }

    @Test
    @Description("Валидация пустого metrikaCounters при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9470")
    public void testSaveCampEmptyMetrikaCounters() {
        request.setMetrika_counters(null);
        //TODO поправить на правильную ошибку
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_METRIKA_COUNTER).toString());
    }

    @Override
    protected void sendAndCheckCampaignErrors(String... errors) {
        String error = StringUtils.join(errors, "\n");
        CampaignErrorResponse response = cmdRule.cmdSteps().campaignSteps().postSaveCampInvalidData(request);
        assertThat("ошибка соответствует ожидаемой", response.getCampaignErrors().getError(), equalTo(error));
    }

    @Override
    protected void sendAndCheckCommonError(String error) {
        CampaignErrorResponse response = cmdRule.cmdSteps().campaignSteps().postSaveCampInvalidData(request);
        assertThat("ошибка соответствует ожидаемой", response.getError(), equalTo(error));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9467")
    public void testSaveCampEmptyName() {
        super.testSaveCampEmptyName();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9471")
    public void testSaveCampWithOnlySpacesInName() {
        super.testSaveCampWithOnlySpacesInName();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9472")
    public void testSaveCampWithIncorrectCharsInName() {
        super.testSaveCampWithIncorrectCharsInName();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9473")
    public void testSaveCampEmptyEmail() {
        super.testSaveCampEmptyEmail();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9474")
    public void testSaveCampLongEmail() {
        super.testSaveCampLongEmail();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9475")
    public void testSaveCampInvalidEmail() {
        super.testSaveCampInvalidEmail();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9478")
    public void testSaveCampEmptyStartDate() {
        super.testSaveCampEmptyStartDate();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9479")
    public void testSaveCampFinishDateLowerThanCurrentDate() {
        super.testSaveCampFinishDateLowerThanCurrentDate();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9480")
    public void testSaveCampFinishDateLowerThanStartDate() {
        super.testSaveCampFinishDateLowerThanStartDate();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9481")
    public void testSaveCampDontShowAtYandex() {
        super.testSaveCampDontShowAtYandex();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9482")
    public void testSaveCampDontShowInvalidDomain() {
        super.testSaveCampDontShowInvalidDomain();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9483")
    public void testSaveCampDontShowInvalidSecondLevelDomain() {
        super.testSaveCampDontShowInvalidSecondLevelDomain();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9484")
    public void testSaveCampDontShowTooLongDomain() {
        super.testSaveCampDontShowTooLongDomain();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9485")
    public void testSaveCampInvalidTwoFields() {
        super.testSaveCampInvalidTwoFields();
    }
}
