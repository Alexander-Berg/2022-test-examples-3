package ru.yandex.autotests.direct.httpclient.firstAid.completeOptimizing;

import java.util.List;

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
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.OptimizingCampaignRequestsIsEasyUser;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.OptimizingCampaignRequestsReqType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.OptimizingCampaignRequestsRequestType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.OptimizingCampaignRequestsStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.OptimizingCampaignRequestsRecord;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.CompleteOptimizingRequestBean;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.CompleteOptimizingRequestBeanBuilder;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignValidationErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.direct.db.utils.JooqRecordDifferMatcher.recordDiffer;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 * Date: 07.05.15
 * https://st.yandex-team.ru/TESTIRT-4959
 */


@Aqua.Test
@Description("Завершение оптимизации кампании контроллером completeOptimizing")
@Stories(TestFeatures.FirstAid.COMPLETE_OPTIMIZING)
@Features(TestFeatures.FIRST_AID)
@Tag(TrunkTag.YES)
@Tag(CmdTag.COMPLETE_OPTIMIZING)
@Tag(OldTag.YES)
public class CompleteOptimizingTest {

    public static final String CLIENT = "at-direct-b-firstaid-c3";
    private static final Float CAMPAIGN_MONEY = 1000f;
    private static final String ANOTHER_CLIENT_CID = "123";
    private static final String DEFAULT_COMMENT = "default comment";
    @ClassRule
    public static final DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(new TestWatcher() {
        @Override
        protected void starting(org.junit.runner.Description description) {
            cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT);
        }
    }, bannersRule);

    private String optimizeRequestID;
    private CompleteOptimizingRequestBean completeOptimizingRequestBean;
    private CSRFToken csrfToken;

    @Before
    public void before() {
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.SUPER).getPassportUID());

        cmdRule.apiSteps().campaignFakeSteps().setCampaignSum(bannersRule.getCampaignId(), CAMPAIGN_MONEY);
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignActive(bannersRule.getCampaignId());

        cmdRule.darkSideSteps().getClientFakeSteps().setShowFaTeaserToUser(CLIENT, Status.YES);
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.SUPER, User.get(Logins.SUPER).getPassword());
        optimizeRequestID = cmdRule.oldSteps().firstHelpSteps().requestFirstHelp(CLIENT,
                bannersRule.getCampaignId().intValue(), csrfToken);

        cmdRule.oldSteps().firstHelpSteps().sendOptimize(CLIENT, bannersRule.getCampaignId().intValue(),
                bannersRule.getGroupId().toString(), optimizeRequestID, csrfToken);
        completeOptimizingRequestBean =
                new CompleteOptimizingRequestBeanBuilder().setOptimizeRequestId(optimizeRequestID)
                        .setCid(String.valueOf(bannersRule.getCampaignId())).setOptimizationComment(DEFAULT_COMMENT).
                        createCompleteOptimizingRequestBean();
    }

    @Test
    @Description("Завершаем оптимизацию кампании под ролью супер и проверяем редирект на страницу кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10490")
    public void completeOptimizingWithSuperRoleAndCheckRedirect() {
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .completeOptimizing(completeOptimizingRequestBean, csrfToken);
        cmdRule.oldSteps().commonSteps().checkRedirect(response, CMD.SHOW_CAMP);
    }

    @Test
    @Description("Завершаем оптимизацию кампании под ролью супер и проверяем данные в БД")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10491")
    public void completeOptimizingWithSuperRoleAndCheckDataAtDB() {
        OptimizingCampaignRequestsRecord expectedOptimizingCampaignRequests = new OptimizingCampaignRequestsRecord()
                .setCid(bannersRule.getCampaignId())
                .setRequestId(Long.valueOf(optimizeRequestID))
                .setStatus(OptimizingCampaignRequestsStatus.Ready)
                .setComment(DEFAULT_COMMENT)
                .setBudget(300d)
                .setIsEasyUser(OptimizingCampaignRequestsIsEasyUser.No)
                .setRequestType(OptimizingCampaignRequestsRequestType.Normal)
                .setAddedBannersCount(0L)
                .setReqType(OptimizingCampaignRequestsReqType.FirstAid);

        cmdRule.oldSteps().firstHelpSteps().completeOptimizing(completeOptimizingRequestBean, csrfToken);
        int clientId = Integer.parseInt(User.get(CLIENT).getClientID());
        Integer clientShard = TestEnvironment.newDbSteps().shardingSteps().getShardByClientID(clientId);
        List<OptimizingCampaignRequestsRecord> optimizingCampaignRequests =
                TestEnvironment.newDbSteps().useShard(clientShard).optimizingCampaignRequestsSteps()
                        .getOptimizingCampaignRequests(bannersRule.getCampaignId());
        assertThat("Данные в Бд в таблице optimizing_campaign_requests соответсвуют ожиданиям",
                optimizingCampaignRequests.get(0),
                recordDiffer(expectedOptimizingCampaignRequests).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Проверяем, что нет прав при завершении оптимизации кампании под ролью клиент")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10492")
    public void completeOptimizingWithClientRoleTest() {
        CSRFToken clientCsrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .completeOptimizing(completeOptimizingRequestBean, clientCsrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

    @Test
    @Description("Проверяем, что нет прав при завершении оптимизации кампании под ролью менеджер")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10493")
    public void completeOptimizingWithManagerRoleTest() {
        CSRFToken managerCsrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.MANAGER).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.MANAGER, User.get(Logins.MANAGER).getPassword());
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .completeOptimizing(completeOptimizingRequestBean, managerCsrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

    @Test
    @Description("Проверяем, что нет прав при завершении оптимизации кампании под ролью агентство")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10494")
    public void completeOptimizingWithAgencyRoleTest() {
        CSRFToken agencyCsrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.AGENCY).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.AGENCY, User.get(Logins.AGENCY).getPassword());
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .completeOptimizing(completeOptimizingRequestBean, agencyCsrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

    @Test
    @Description("Проверяем валидацию пустого cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10487")
    public void emptyCidValidationTest() {
        completeOptimizingRequestBean.setCid(null);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .completeOptimizing(completeOptimizingRequestBean, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_CID).toString());
    }

    @Test
    @Description("Проверяем валидацию при параметре cid, принадлежащей другому клиенту")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10488")
    public void anotherClientCidValidationTest() {
        completeOptimizingRequestBean.setCid(ANOTHER_CLIENT_CID);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .completeOptimizing(completeOptimizingRequestBean, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response,
                containsString(
                        TextResourceFormatter.resource(CampaignValidationErrors.INCORRECT_CID_OR_LOGIN).toString()));
    }

    @Test
    @Description("Проверяем валидацию при пустом параметре optimize_request_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10489")
    public void emptyOptimizeRequestIdValidationTest() {
        completeOptimizingRequestBean.setOptimizeRequestId(null);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .completeOptimizing(completeOptimizingRequestBean, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                TextResourceFormatter.resource(CommonErrorsResource.PARAMETER_REQUIRED).toString());
    }


}
