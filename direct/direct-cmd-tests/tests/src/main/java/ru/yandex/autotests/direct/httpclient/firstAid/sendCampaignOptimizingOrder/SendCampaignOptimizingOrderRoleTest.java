package ru.yandex.autotests.direct.httpclient.firstAid.sendCampaignOptimizingOrder;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.SendCampaignOptimizingOrderParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.firstAid.SendCampaignOptimizingOrderErrors;
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
 *         Date: 06.05.15
 *         https://st.yandex-team.ru/TESTIRT-4994
 */

@Aqua.Test
@Description("Проверка отправки заявки на первую помощь под разными ролями контроллером sendCampaignOptimizingOrder")
@Stories(TestFeatures.FirstAid.SEND_CAMPAIGN_OPTIMIZING_ORDER)
@Features(TestFeatures.FIRST_AID)
@Tag(CmdTag.SEND_CAMPAIGN_OPTIMIZING_ORDER)
@Tag(OldTag.YES)
@RunWith(Parameterized.class)
public class SendCampaignOptimizingOrderRoleTest {

    private static final Integer FIRST_AID_MIN_BUDGET = 300;
    private static final Float CAMPAIGN_MONEY = 1000f;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public String description;
    public String managerLogin;
    public String client;
    public BannersRule bannersRule;
    private CSRFToken csrfToken;


    private SendCampaignOptimizingOrderParameters sendCampaignOptimizingOrderParameters;

    public SendCampaignOptimizingOrderRoleTest(String description, String managerLogin, String client) {
        this.description = description;
        this.managerLogin = managerLogin;
        this.client = client;
        bannersRule = new TextBannersRule().withUlogin(client);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(managerLogin);
    }

    /*
        TODO: перейти на at-direct-b-firstaid-ag-c4 (с -c5), когда он приедет в Баланс (телепортировав по необходимости)
    */
    @Parameterized.Parameters(name = "Роль: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"Менеджер", Logins.MANAGER, "at-direct-b-firstaid-mngr-c"},
                {"Агентство", Logins.AGENCY, "at-direct-b-firstaid-ag-c5"},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {
        cmdRule.darkSideSteps().getClientFakeSteps().setShowFaTeaserToUser(client, Status.YES);
        sendCampaignOptimizingOrderParameters = new SendCampaignOptimizingOrderParameters();
        sendCampaignOptimizingOrderParameters.setBudget(FIRST_AID_MIN_BUDGET.toString());
        sendCampaignOptimizingOrderParameters.setUlogin(client);
        sendCampaignOptimizingOrderParameters.setCid(bannersRule.getCampaignId().toString());
        cmdRule.oldSteps().onPassport().authoriseAs(managerLogin, User.get(managerLogin).getPassword());
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(managerLogin).getPassportUID());
        cmdRule.apiSteps().campaignFakeSteps().setCampaignSum(bannersRule.getCampaignId(), CAMPAIGN_MONEY);
    }

    @Test
    @Description("Отправляем заявку, проверяем, что у данной роли нет прав")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10495")
    public void sendWithRoleTest() {
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(TextResourceFormatter.resource(SendCampaignOptimizingOrderErrors.ONLY_ONE_TIME).toString()));
    }

}
