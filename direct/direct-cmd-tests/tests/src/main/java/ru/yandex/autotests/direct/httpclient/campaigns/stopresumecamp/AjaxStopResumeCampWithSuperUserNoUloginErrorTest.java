package ru.yandex.autotests.direct.httpclient.campaigns.stopresumecamp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
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
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 12.11.14.
 * TESTIRT-3298
 */
@Aqua.Test
@Description("Проверка ошибки при вызове контроллера ajaxStopResumeCamp под супером, без указания ulogin")
@Stories(TestFeatures.Campaigns.AJAX_STOP_RESUME_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CmdTag.AJAX_STOP_RESUME_CAMP)
@Tag(OldTag.YES)
public class AjaxStopResumeCampWithSuperUserNoUloginErrorTest {
    private String clientLogin = "at-backend-stoprescamp";

    BannersRule bannersRule = new TextBannersRule().withUlogin(clientLogin);
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private String superLogin = Logins.SUPER;
    private CSRFToken csrfToken;
    private AjaxStopResumeCampParameters params;
    private String expectedStatusShow;

    @Before
    public void before() {
        params = new AjaxStopResumeCampParameters();
        params.setDoStop("1");
        params.setCid(bannersRule.getCampaignId().toString());

        expectedStatusShow = "Yes";

        cmdRule.oldSteps().onPassport().authoriseAs(superLogin, User.get(superLogin).getPassword());
        csrfToken = getCsrfTokenFromCocaine(User.get(superLogin).getPassportUID());
    }


    @Test
    @Description("Обращение к контроллеру под супером без параметра ulogin")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10426")
    public void stopResumeCampaignWithoutDoStopTest() {
        String errorText = TextResourceFormatter.resource(StopResumeCampErrors.NO_ULOGIN_ERROR).toString();
        cmdRule.oldSteps().ajaxStopResumeCampSteps().saveAndCheckError(csrfToken, params, equalTo(errorText));

        Campaign actualCamp =
                cmdRule.cmdSteps().campaignSteps().getCampaign(clientLogin, bannersRule.getCampaignId());
        assertThat("статус кампании в апи соответствует ожидаемому", actualCamp.getStatusShow(), equalTo(expectedStatusShow));
    }
}
