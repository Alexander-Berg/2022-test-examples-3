package ru.yandex.autotests.direct.httpclient.firstAid.sendCampaignOptimizingOrder;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.OptimizingCampaignRequestsRecord;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.SendCampaignOptimizingOrderParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignValidationErrors;
import ru.yandex.autotests.direct.httpclient.data.textresources.firstAid.SendCampaignOptimizingOrderErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.05.15
 *         https://st.yandex-team.ru/TESTIRT-4994
 */

@Aqua.Test
@Description("Проверка валидации при отправке заявки на первую помощь контроллером sendCampaignOptimizingOrder")
@Stories(TestFeatures.FirstAid.SEND_CAMPAIGN_OPTIMIZING_ORDER)
@Features(TestFeatures.FIRST_AID)
@Tag(CmdTag.SEND_OPTIMIZE)
@Tag(OldTag.YES)
public class SendCampaignOptimizingOrderValidationTest {

    public static final String CLIENT = "at-direct-b-firstaid-c";
    private static final Integer FIRST_AID_MIN_BUDGET = 300;
    private static final String ANOTHER_CLENT_CID = "123";
    private static final Integer REQUEST_COMMENT_MAX_LENGTH = 300;
    private static final Float CAMPAIGN_MONEY = 1000f;
    @ClassRule
    public static final DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    private CSRFToken csrfToken;

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(new TestWatcher() {
        @Override
        protected void starting(org.junit.runner.Description description) {
            cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT);
        }
    }, bannersRule);


    private long campaignId;
    private SendCampaignOptimizingOrderParameters sendCampaignOptimizingOrderParameters;

    @Before
    public void before() {
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());

        campaignId = bannersRule.getCampaignId();
        cmdRule.apiAggregationSteps().activateCampaignWithMoney(CLIENT, bannersRule.getCampaignId(), CAMPAIGN_MONEY);

        cmdRule.darkSideSteps().getClientFakeSteps().setShowFaTeaserToUser(CLIENT, Status.YES);
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        sendCampaignOptimizingOrderParameters = new SendCampaignOptimizingOrderParameters();
        sendCampaignOptimizingOrderParameters.setCid(String.valueOf(campaignId));
        sendCampaignOptimizingOrderParameters.setBudget(FIRST_AID_MIN_BUDGET.toString());
        sendCampaignOptimizingOrderParameters.setUlogin(CLIENT);
    }

    @Test
    @Description("Проверяем валидацию c пустым номером кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10502")
    public void emptyCidValidationTest() {
        sendCampaignOptimizingOrderParameters.setCid(null);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                        .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_CID).toString());
    }

    @Test(expected = AssertionError.class)
    @Issue("https://st.yandex-team.ru/DIRECT-41689")
    @Description("Проверяем валидацию c пустым полем бюджет")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10504")
    public void emptyBudgetValidationTest() {
        sendCampaignOptimizingOrderParameters.setBudget(null);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        //TODO исправить на нужное сообщение после починки бага https://st.yandex-team.ru/DIRECT-41689
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_CID).toString());
    }

    @Test(expected = AssertionError.class)
    @Issue("https://st.yandex-team.ru/DIRECT-41689")
    @Description("Проверяем валидацию cо значением поля 'Бюджет' меньше допустимого")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10505")
    public void lessThanMinBudgetValidationTest() {
        sendCampaignOptimizingOrderParameters.setBudget(String.valueOf(FIRST_AID_MIN_BUDGET - 1));
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        //TODO исправить на нужное сообщение после починки бага https://st.yandex-team.ru/DIRECT-41689
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_CID).toString());
    }

    @Test
    @Description("Проверяем валидацию при нечисловом значении поля бюджет")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10506")
    public void notANumberBudgetValidationTest() {
        sendCampaignOptimizingOrderParameters.setBudget("abc");
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response,
                equalTo(TextResourceFormatter.resource(SendCampaignOptimizingOrderErrors.INCORRECT_BUDGET).toString()));
    }

    @Test
    @Description("Проверяем валидацию c номером кампании, не принадлежащей данному клиенту")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10503")
    public void anotherClientCidValidationTest() {
        sendCampaignOptimizingOrderParameters.setCid(ANOTHER_CLENT_CID);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response, containsString(
                TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

    @Test
    @Description("Проверяем обрезание поля 'Пожелание клиента'")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10507")
    public void longRequestCommentTest() {
        sendCampaignOptimizingOrderParameters
                .setRequestComment(StringUtils.repeat("m", REQUEST_COMMENT_MAX_LENGTH + 1));
        cmdRule.oldSteps().firstHelpSteps()
                .sendCampaignOptimizingOrder(sendCampaignOptimizingOrderParameters, csrfToken);
        int clientId = Integer.parseInt(User.get(CLIENT).getClientID());
        Integer clientShard = TestEnvironment.newDbSteps().shardingSteps().getShardByClientID(clientId);
        List<OptimizingCampaignRequestsRecord> optimizingCampaignRequests =
                TestEnvironment.newDbSteps().useShard(clientShard).optimizingCampaignRequestsSteps()
                        .getOptimizingCampaignRequests(campaignId);
        String expectedString = StringUtils.repeat("m", REQUEST_COMMENT_MAX_LENGTH);
        assertThat("Данные в Бд соответсвуют ожиданиям", optimizingCampaignRequests.get(0).getRequestComment(),
                equalTo(expectedString));
    }
}
