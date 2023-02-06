package ru.yandex.autotests.direct.httpclient.firstAid.sendCampaignOptimizingOrder;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.SendCampaignOptimizingOrderParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.firstAid.SendCampaignOptimizingOrderErrors;
import ru.yandex.autotests.direct.httpclient.data.textresources.firstAid.SendCampaignOptimizingOrderResponses;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.05.15
 *         https://st.yandex-team.ru/TESTIRT-4994
 */

@Aqua.Test
@Description("Проверка отправки заявки на первую помощь контроллером sendCampaignOptimizingOrder")
@Stories(TestFeatures.FirstAid.SEND_CAMPAIGN_OPTIMIZING_ORDER)
@Features(TestFeatures.FIRST_AID)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SEND_CAMPAIGN_OPTIMIZING_ORDER)
@Tag(OldTag.YES)
public class SendCampaignOptimizingOrderTest {

    public static final String CLIENT = "at-direct-b-firstaid-c1";
    public static final String AGENCY_CLIENT = "at-direct-b-firstaid-ag-c";
    private static final Integer FIRST_AID_MIN_BUDGET = 300;
    private static final Float CAMPAIGN_MONEY = 1000f;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(new TestWatcher() {
        @Override
        protected void starting(org.junit.runner.Description description) {
            cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT);
        }
    }, bannersRule);


    private SendCampaignOptimizingOrderParameters sendCampaignOptimizingOrderParameters;

    @Before
    public void before() {


        cmdRule.darkSideSteps().getClientFakeSteps().setShowFaTeaserToUser(CLIENT, Status.YES);
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        sendCampaignOptimizingOrderParameters = new SendCampaignOptimizingOrderParameters();
        sendCampaignOptimizingOrderParameters.setBudget(FIRST_AID_MIN_BUDGET.toString());
        sendCampaignOptimizingOrderParameters.setUlogin(CLIENT);
    }

    private void activateCampaign(Float campaignMoney) {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignActive(bannersRule.getCampaignId());
        cmdRule.apiSteps().campaignFakeSteps().setCampaignSum(bannersRule.getCampaignId(), campaignMoney);
        sendCampaignOptimizingOrderParameters.setCid(String.valueOf(bannersRule.getCampaignId()));
    }

    @Test
    @Description("Отправляем заявку под ролью клиент")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10497")
    public void sendWithClientRoleTest() {
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        activateCampaign(CAMPAIGN_MONEY);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(TextResourceFormatter.resource(SendCampaignOptimizingOrderResponses.ORDER_ACCEPTED)
                        .toString()));
    }

    @Test
    @Description("Отправляем заявку под ролью супер")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10498")
    public void sendWithSuperRoleTest() {
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.SUPER).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.SUPER, User.get(Logins.SUPER).getPassword());
        activateCampaign(CAMPAIGN_MONEY);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(TextResourceFormatter.resource(SendCampaignOptimizingOrderResponses.ORDER_ACCEPTED)
                        .toString()));
    }

    @Test
    @Description("Отправляем заявку под ролью клиент, для которого уже была отправлена заявка на первую помощь")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10496")
    public void sendWithClientRoleWithoutFirstAidTeaserTest() {
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        activateCampaign(CAMPAIGN_MONEY);
        cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps().
                sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(SendCampaignOptimizingOrderErrors.ONLY_ONE_TIME).toString()));
    }

    @Test
    @Description("Отправляем заявку под ролью клиент, у которого нет активных кампаний с деньгами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10499")
    public void sendWithoutActiveCampaignsTest() {
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        activateCampaign(0f);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(SendCampaignOptimizingOrderErrors.ONLY_ONE_TIME).toString()));
    }

    @Test
    @Description("Отправляем заявку под чужим менеджером, проверяем, что у него нет прав")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10500")
    public void sendWithManagerRoleTest() {
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.MANAGER).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.MANAGER, User.get(Logins.MANAGER).getPassword());
        activateCampaign(CAMPAIGN_MONEY);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

    @Test
    @Description("Отправляем заявку под чужим агентством, проверяем, что у него нет прав")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10501")
    public void sendWithAgencyRoleTest() {
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.AGENCY).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.AGENCY, User.get(Logins.AGENCY).getPassword());
        activateCampaign(CAMPAIGN_MONEY);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }
}
