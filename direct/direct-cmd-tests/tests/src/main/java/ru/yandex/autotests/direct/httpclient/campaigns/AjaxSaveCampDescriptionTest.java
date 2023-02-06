package ru.yandex.autotests.direct.httpclient.campaigns;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.ErrorNumbers;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.data.campaigns.AjaxSavecampDescriptionParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.direct.httpclient.data.ErrorCodes.NO_RIGHTS_FOR_OPERATION;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 12.11.14.
 * TESTIRT-3297
 */
@Aqua.Test
@Description("Проверка контроллера ajaxSaveCampDescription")
@Stories(TestFeatures.Campaigns.AJAX_SAVE_CAMP_DESCRIPTION)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CmdTag.AJAX_SAVE_CAMP_DESCRIPTION)
@Tag(OldTag.YES)
public class AjaxSaveCampDescriptionTest {

    private static final String CLIENT_LOGIN = "at-direct-b-agcl";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule()
            .overrideCampTemplate(new SaveCampRequest().withFor_agency(Logins.AGENCY))
            .withUlogin(CLIENT_LOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(Logins.AGENCY).withRules(bannersRule);

    private User agency = User.get(Logins.AGENCY);
    private CSRFToken csrfToken;
    private Long campaignId;
    private AjaxSavecampDescriptionParameters params;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        params = new AjaxSavecampDescriptionParameters();
        params.setCid(campaignId.toString());
        params.setUlogin(CLIENT_LOGIN);
        params.setDescription("common description");

        cmdRule.oldSteps().onPassport().authoriseAs(agency.getLogin(), agency.getPassword());
        csrfToken = getCsrfTokenFromCocaine(agency.getPassportUID());
    }

    @Test
    @Description("Сохранение корректного описания кампании, проверка ответа сервера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10288")
    public void saveCampDescriptionTest() {
        cmdRule.oldSteps().ajaxSaveCampDescriptionSteps()
                .checkSaveCampDescriptionResponseContent(csrfToken, params, equalTo("1"));
    }

    @Test
    @Description("Проверка сохраненного описания кампании через апи")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10289")
    public void checkSaveCampDescriptionWithApiTest() {
        cmdRule.oldSteps().ajaxSaveCampDescriptionSteps()
                .checkSaveCampDescriptionResponseContent(csrfToken, params, equalTo("1"));
        CampaignFakeInfo camp = cmdRule.apiSteps().campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("описание кампании в апи соответствует сохраненному",
                camp.getCampDescription(), equalTo(params.getDescription()));
    }

    @Test
    @Description("Проверка обрезания текста описания при превышении максимальной длины через апи")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10290")
    public void checkSaveCampDescriptionTrimWithApiTest() {
        params.setDescription(new PropertyLoader<>(AjaxSavecampDescriptionParameters.class).
                getHttpBean("ajaxSaveCampDescriptionParamsWithTooLongDescription").getDescription());
        cmdRule.oldSteps().ajaxSaveCampDescriptionSteps()
                .checkSaveCampDescriptionResponseContent(csrfToken, params, equalTo("1"));
        CampaignFakeInfo camp = cmdRule.apiSteps().campaignFakeSteps().fakeGetCampaignParams(campaignId);
        String expectedDescription = params.getDescription().substring(0, 4096);
        assertThat("описание кампании в апи соответствует сохраненному",
                camp.getCampDescription(), equalTo(expectedDescription));
    }

    @Test
    @Description("Сохранение null в описании кампании, проверка через апи")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10291")
    public void checkSaveNullCampDescriptionWithApiTest() {
        cmdRule.oldSteps().ajaxSaveCampDescriptionSteps().getAjaxSaveCampDescriptionResponse(csrfToken, params);
        params.setDescription(null);
        cmdRule.oldSteps().ajaxSaveCampDescriptionSteps()
                .checkSaveCampDescriptionResponseContent(csrfToken, params, equalTo("1"));
        CampaignFakeInfo camp = cmdRule.apiSteps().campaignFakeSteps().fakeGetCampaignParams(campaignId);
        assertThat("описание кампании в апи соответствует сохраненному",
                camp.getCampDescription(), equalTo(params.getDescription()));
    }

    @Test
    @Description("Сохранение описания в кампанию, которая не соответствует логину, проверка ответа сервера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10292")
    public void saveCampDescriptionWithWrongCidTest() {
        params.setCid("1");
        cmdRule.oldSteps().ajaxSaveCampDescriptionSteps()
                .checkSaveCampDescriptionErrorCode(csrfToken, params, equalTo(NO_RIGHTS_FOR_OPERATION));
    }

    @Test
    @Description("Сохранение описания с cid неверного формата. Проверка ответа сервера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10293")
    public void saveCampDescriptionWithWrongCidFormatTest() {
        params.setCid("abcd");
        cmdRule.oldSteps().ajaxSaveCampDescriptionSteps().checkSaveCampDescriptionErrorNumber(
                csrfToken, params, equalTo(ErrorNumbers.WRONG_CID_FORMAT_ERROR_NUMBER));
    }

    @Test
    @Description("Сохранение описания без cid. Проверка ответа сервера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10294")
    public void saveCampDescriptionWithoutCidTest() {
        params.setCid(null);
        cmdRule.oldSteps().ajaxSaveCampDescriptionSteps().checkSaveCampDescriptionErrorNumber(
                csrfToken, params, equalTo(ErrorNumbers.WITHOUT_CID_ERROR_NUMBER));
    }

    @Test
    @Description("Сохранение описания в кампанию, к которой нет доступа, проверка ответа сервера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10295")
    public void saveCampDescriptionWithoutRightsTest() {
        cmdRule.getApiStepsRule().as(Logins.SUPER);
        String loginWithoutMngr = "at-backend-withoutmngr";
        TestEnvironment.newDbSteps().useShardForLogin(loginWithoutMngr);
        Long uid = Long.valueOf(User.get(loginWithoutMngr).getPassportUID());
        Long cid = TestEnvironment.newDbSteps().campaignsSteps().getCampaignIdsByUid(uid).get(0);
        params.setUlogin(loginWithoutMngr);
        params.setCid(String.valueOf(cid));

        DirectResponse response = cmdRule.oldSteps()
                .ajaxSaveCampDescriptionSteps()
                .getAjaxSaveCampDescriptionResponse(csrfToken, params);

        String errorText = TextResourceFormatter.resource(CommonErrorsResource.NO_RIGHTS_FOR_OPERATION).toString();
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponse(response, hasJsonProperty(Responses.ERROR.getPath(), containsString(errorText)));
    }
}
