package ru.yandex.autotests.direct.httpclient.campaigns.showCamp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.campaigns.ShowCampResponseBean;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignValidationErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BeanConstraint;
import ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.WhiteListConstraint;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.06.15
 *         TESTIRT-4967
 */

@Aqua.Test
@Description("Проверка валидации контроллера showCamp")
@Stories(TestFeatures.Campaigns.SHOW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CmdTag.SHOW_CAMP)
@Tag(OldTag.YES)
public class ShowCampValidationTest {

    private static final String CLIENT_LOGIN = "at-direct-backend-c";
    private static final String LITE_CLIENT = "at-direct-adv-light";
    private static final String ANOTHER_CLIENT_CID = "123";
    private static final String INCORRECT_CID = "abc";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private ShowCampResponseBean expectedResponse;


    @Before
    public void before() {


        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
        expectedResponse = new ShowCampResponseBean();
    }

    @Test
    @Description("Проверяем валидацию при пустом cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10408")
    public void emptyCidValidationTest() {
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().openShowCamp(CLIENT_LOGIN, null);
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseErrorCMDText(response, CampaignValidationErrors.EMPTY_CID.toString());
    }

    @Test
    @Description("Проверяем валидацию при чужом cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10409")
    public void anotherClientCidValidationTest() {
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().openShowCamp(CLIENT_LOGIN, ANOTHER_CLIENT_CID);
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Проверяем валидацию при некорректном cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10410")
    public void incorrectCidValidationTest() {
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().openShowCamp(CLIENT_LOGIN, INCORRECT_CID);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, String.format(
                TextResourceFormatter.resource(CampaignValidationErrors.INCORRECT_CID).toString(), INCORRECT_CID));
    }

    @Test
    @Description("Проверяем, что у легкого клиента нет прав для редактирования кампании через excel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10411")
    public void canExportInExcelTest() {
        TestEnvironment.newDbSteps().useShardForLogin(LITE_CLIENT);
        cmdRule.oldSteps().onPassport().authoriseAs(LITE_CLIENT, User.get(LITE_CLIENT).getPassword());
        Long uid = Long.valueOf(User.get(LITE_CLIENT).getPassportUID());
        Long campaignId = TestEnvironment.newDbSteps().campaignsSteps()
                .getCampaignIdsByUid(uid).get(0);
        ShowCampResponseBean actualResponse =
                cmdRule.oldSteps().campaignsSteps().getShowCamp(LITE_CLIENT, String.valueOf(campaignId));
        BeanConstraint beanConstraint = new WhiteListConstraint().putFields("can_export_in_excel");
        assertThat("Ответ контроллера соответсвует ожиданиям", actualResponse,
                beanDiffer(expectedResponse).fields(beanConstraint));
    }

}
