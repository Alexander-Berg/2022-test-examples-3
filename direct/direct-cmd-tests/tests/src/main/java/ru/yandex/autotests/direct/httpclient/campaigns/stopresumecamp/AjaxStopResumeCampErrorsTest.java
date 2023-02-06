package ru.yandex.autotests.direct.httpclient.campaigns.stopresumecamp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.AjaxStopResumeCampParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.StopResumeCampErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.data.ErrorCodes.NO_RIGHTS_FOR_OPERATION;
import static ru.yandex.autotests.direct.httpclient.data.ErrorNumbers.WITHOUT_CID_ERROR_NUMBER;
import static ru.yandex.autotests.direct.httpclient.data.ErrorNumbers.WRONG_CID_FORMAT_ERROR_NUMBER;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 12.11.14.
 * TESTIRT-3298
 */
@Aqua.Test
@Description("Проверка ошибок в контроллере ajaxStopResumeCamp")
@Stories(TestFeatures.Campaigns.AJAX_STOP_RESUME_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CmdTag.AJAX_STOP_RESUME_CAMP)
@Tag(OldTag.YES)
public class AjaxStopResumeCampErrorsTest {

    private static final String CLIENT = "at-backend-stoprescamp";

    BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();


    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private Long campaignId;
    private CSRFToken csrfToken;
    private AjaxStopResumeCampParameters params;
    private String expectedStatusShow;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        params = new AjaxStopResumeCampParameters();
        params.setUlogin(CLIENT);
        params.setDoStop("1");
        params.setCid(campaignId.toString());

        expectedStatusShow = "Yes";

        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
    }


    @Test
    @Description("Обращение к контроллеру без параметра do_stop")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10419")
    public void stopResumeCampaignWithoutDoStopTest() {
        params.setDoStop(null);
        String errorText = TextResourceFormatter.resource(StopResumeCampErrors.WITHOUT_DO_STOP_ERROR).toString();
        cmdRule.oldSteps().ajaxStopResumeCampSteps().saveAndCheckError(csrfToken, params, equalTo(errorText));
        String actualStatus = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, campaignId).getStatusShow();
        assertThat("статус кампании в апи соответствует ожидаемому", actualStatus, equalTo(expectedStatusShow));
    }

    @Test
    @Description("Обращение к контроллеру без параметра cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10420")
    public void stopResumeCampaignWithoutCidTest() {
        params.setCid(null);
        cmdRule.oldSteps().ajaxStopResumeCampSteps()
                .saveAndCheckErrorNumber(csrfToken, params, equalTo(WITHOUT_CID_ERROR_NUMBER));
        String actualStatus = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, campaignId).getStatusShow();
        assertThat("статус кампании в апи соответствует ожидаемому", actualStatus, equalTo(expectedStatusShow));
    }

    @Test
    @Description("Обращение к контроллеру с cid, не принадлежащим логину")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10421")
    public void stopResumeCampaignWithWrongCidTest() {
        params.setCid("1");
        cmdRule.oldSteps().ajaxStopResumeCampSteps()
                .saveAndCheckErrorCode(csrfToken, params, equalTo(NO_RIGHTS_FOR_OPERATION));
        String actualStatus = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, campaignId).getStatusShow();
        assertThat("статус кампании в апи соответствует ожидаемому", actualStatus, equalTo(expectedStatusShow));
    }

    @Test
    @Description("Обращение к контроллеру c неверным форматом cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10422")
    public void stopResumeCampaignWithWrongFormatCidTest() {
        params.setCid("abcd");
        cmdRule.oldSteps().ajaxStopResumeCampSteps()
                .saveAndCheckErrorNumber(csrfToken, params, equalTo(WRONG_CID_FORMAT_ERROR_NUMBER));
        String actualStatus = cmdRule.cmdSteps().campaignSteps().getCampaign(CLIENT, campaignId).getStatusShow();
        assertThat("статус кампании в апи соответствует ожидаемому", actualStatus, equalTo(expectedStatusShow));
    }
}
