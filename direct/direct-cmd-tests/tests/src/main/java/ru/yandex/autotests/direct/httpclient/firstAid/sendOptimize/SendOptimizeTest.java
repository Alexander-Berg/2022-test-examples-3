package ru.yandex.autotests.direct.httpclient.firstAid.sendOptimize;

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
import ru.yandex.autotests.direct.httpclient.data.mediaplan.SendOptimizeParameters;
import ru.yandex.autotests.direct.httpclient.data.mediaplan.SendOptimizeParametersBuilder;
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
 *         Date: 06.05.15
 *         https://st.yandex-team.ru/TESTIRT-4981
 */

@Aqua.Test
@Description("Оптимизация групп баннеров контроллером sendOptimize")
@Stories(TestFeatures.FirstAid.SEND_OPTIMIZE)
@Features(TestFeatures.FIRST_AID)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SEND_CAMPAIGN_OPTIMIZING_ORDER)
@Tag(OldTag.YES)
public class SendOptimizeTest {

    public static final String CLIENT = "at-direct-b-firstaid-c2";
    private static final Float CAMPAIGN_MONEY = 1000f;
    private static final String ANOTHER_CLIENT_CID = "123";
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


    private Integer campaignId;
    private String optimizeRequestID;
    private SendOptimizeParameters sendOptimizeParameters;
    private CSRFToken csrfToken;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId().intValue();
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());

        cmdRule.apiAggregationSteps().activateCampaignWithMoney(CLIENT, bannersRule.getCampaignId(), CAMPAIGN_MONEY);

        cmdRule.darkSideSteps().getClientFakeSteps().setShowFaTeaserToUser(CLIENT, Status.YES);
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        optimizeRequestID = cmdRule.oldSteps().firstHelpSteps().requestFirstHelp(CLIENT, campaignId, csrfToken);

        sendOptimizeParameters = new SendOptimizeParametersBuilder()
                .setAdgroupIds(bannersRule.getGroupId().toString())
                .setCid(String.valueOf(campaignId))
                .setOptimizeRequestId(optimizeRequestID).createSendOptimizeParameters();
        sendOptimizeParameters.setUlogin(CLIENT);
    }

    @Test
    @Description("Оптимизируем баннеры под ролью супер и проверяем редирект на страницу рекомендаций")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10511")
    public void optimizeWithSuperRoleAndCheckRedirect() {
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.SUPER).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.SUPER, User.get(Logins.SUPER).getPassword());
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().sendOptimize(sendOptimizeParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkRedirect(response, CMD.SHOW_MEDIAPLAN);
    }

    @Test
    @Description("Оптимизируем баннеры под ролью супер и проверяем данные в БД")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10512")
    public void optimizeWithSuperRoleAndCheckDataAtDB() {
        OptimizingCampaignRequestsRecord expectedOptimizingCampaignRequests = new OptimizingCampaignRequestsRecord()
                .setCid(Long.valueOf(campaignId))
                .setStatus(OptimizingCampaignRequestsStatus.InProcess)
                .setBudget(300d)
                .setIsEasyUser(OptimizingCampaignRequestsIsEasyUser.No)
                .setRequestType(OptimizingCampaignRequestsRequestType.Normal)
                .setReqType(OptimizingCampaignRequestsReqType.FirstAid)
                .setRequestId(Long.valueOf(optimizeRequestID));

        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.SUPER).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.SUPER, User.get(Logins.SUPER).getPassword());
        cmdRule.oldSteps().campaignsSteps().sendOptimize(sendOptimizeParameters, csrfToken);
        int clientId = Integer.parseInt(User.get(CLIENT).getClientID());
        Integer clientShard = TestEnvironment.newDbSteps().shardingSteps().getShardByClientID(clientId);
        List<OptimizingCampaignRequestsRecord> optimizingCampaignRequests =
                TestEnvironment.newDbSteps().useShard(clientShard).optimizingCampaignRequestsSteps().getOptimizingCampaignRequests(
                        Long.valueOf(campaignId));
        assertThat("Данные в Бд в таблице optimizing_campaign_requests соответсвуют ожиданиям",
                optimizingCampaignRequests.get(0),
                recordDiffer(expectedOptimizingCampaignRequests).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Проверяем, что нет прав при оптимизации баннеров под ролью клиент")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10513")
    public void optimizeWithClientRoleTest() {
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().sendOptimize(sendOptimizeParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

    @Test
    @Description("Проверяем, что нет прав при оптимизации баннеров под ролью менеджер")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10514")
    public void optimizeWithManagerRoleTest() {
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.MANAGER).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.MANAGER, User.get(Logins.MANAGER).getPassword());
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().sendOptimize(sendOptimizeParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

    @Test
    @Description("Проверяем, что нет прав при оптимизации баннеров под ролью агентство")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10515")
    public void optimizeWithAgencyRoleTest() {
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.AGENCY).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(Logins.AGENCY, User.get(Logins.AGENCY).getPassword());
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().sendOptimize(sendOptimizeParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

    @Test
    @Description("Проверяем валидацию пустого cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10508")
    public void emptyCidValidationTest() {
        sendOptimizeParameters.setCid(null);
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().sendOptimize(sendOptimizeParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_CID).toString());
    }

    @Test
    @Description("Проверяем валидацию при параметре cid, принадлежащей другому клиенту")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10509")
    public void incorrectCidValidationTest() {
        sendOptimizeParameters.setCid(ANOTHER_CLIENT_CID);
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().sendOptimize(sendOptimizeParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

    @Test
    @Description("Проверяем валидацию при пустом параметре optimize_request_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10510")
    public void emptyOptimizeRequestIdValidationTest() {
        sendOptimizeParameters.setOptimizeRequestId(null);
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().sendOptimize(sendOptimizeParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                TextResourceFormatter.resource(CommonErrorsResource.PARAMETER_REQUIRED).toString());
    }

    @Test
    @Description("Проверяем валидацию при пустом параметре adgroup_ids")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10516")
    public void emptyAdGroupIdsValidationTest() {
        sendOptimizeParameters.setAdgroupIds(null);
        DirectResponse response = cmdRule.oldSteps().campaignsSteps().sendOptimize(sendOptimizeParameters, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(
                        TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString()));
    }

}
