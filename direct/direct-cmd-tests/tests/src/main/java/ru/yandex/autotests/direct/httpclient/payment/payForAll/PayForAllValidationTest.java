package ru.yandex.autotests.direct.httpclient.payment.payForAll;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

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
import ru.yandex.autotests.direct.httpclient.data.payment.payForAll.PayForAllErrorCodes;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.pay.PayErrors;
import ru.yandex.autotests.direct.httpclient.data.textresources.pay.PayForAllErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 *         https://st.yandex-team.ru/TESTIRT-5000
 */

@Aqua.Test
@Description("Проверка валидации контроллера payforall")
@Stories(TestFeatures.Payment.PAY_FOR_ALL)
@Features(TestFeatures.PAYMENT)
@Tag(CmdTag.PAY_FOR_ALL)
@Tag(OldTag.YES)
public class PayForAllValidationTest {

    private static final String CLIENT_LOGIN = Logins.CLIENT_WITHOUT_WALLET;
    private static final String ANOTHER_CLIENT_CID = "123";
    private static final String DEFAULT_SUM = "1770";
    private static final String INCORRECT_SUM = "aa";
    private static final String LESS_THAN_MIN_SUM = "255";
    private static final String MORE_THAN_MAX_SUM = "100000000000000000000000000000000000";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private Long campaignId;
    private CSRFToken csrfToken;


    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT_LOGIN).getPassportUID());
        campaignId = bannersRule.getCampaignId();
    }

    @Test
    @Description("Проверяем валидацию при чужом cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10568")
    public void anotherClientCidValidationTest() {
        DirectResponse response = cmdRule.oldSteps().payForAllSteps().payForAll(csrfToken, CLIENT_LOGIN,
                ANOTHER_CLIENT_CID, DEFAULT_SUM, "1");
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.RIGHTS_CHECK_ERROR.toString());
    }

    @Test
    @Description("Проверяем валидацию непромодерированной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10569")
    public void noModerateCampaignValidationTest() {
        DirectResponse response = cmdRule.oldSteps().payForAllSteps().payForAll(csrfToken, CLIENT_LOGIN,
                String.valueOf(campaignId), DEFAULT_SUM, "1");
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(String.format(
                TextResourceFormatter.resource(PayErrors.CAMPAIGN_DOES_NOT_MODERATE).toString(), campaignId)));
    }

    @Test
    @Description("Проверяем валидацию при пустой сумме")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10567")
    public void emptySumValidationTest() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignModerated(campaignId);
        DirectResponse response = cmdRule.oldSteps().payForAllSteps().payForAll(csrfToken, CLIENT_LOGIN,
                String.valueOf(campaignId), null, "1");
        cmdRule.oldSteps().commonSteps().checkRedirect(response, containsString("error_code=" + PayForAllErrorCodes.EMPTY_SUM));
    }

    @Test
    @Description("Проверяем валидацию при некорректной сумме")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10570")
    public void incorrectSumValidationTest() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignModerated(campaignId);
        DirectResponse response = cmdRule.oldSteps().payForAllSteps().payForAll(csrfToken, CLIENT_LOGIN,
                String.valueOf(campaignId), INCORRECT_SUM, "1");
        cmdRule.oldSteps().commonSteps().checkRedirect(response, containsString("error_code=" + PayForAllErrorCodes.INCORRECT_SUM));
    }

    @Test
    @Description("Проверяем валидацию при сумме, меньше минимальной")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10571")
    public void lessThanMinSumValidationTest() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignModerated(campaignId);
        DirectResponse response = cmdRule.oldSteps().payForAllSteps().payForAll(csrfToken, CLIENT_LOGIN,
                String.valueOf(campaignId), LESS_THAN_MIN_SUM, "1");
        cmdRule.oldSteps().commonSteps().checkRedirect(response, containsString("error_code=" + PayForAllErrorCodes.LESS_THAN_MIN_SUM));
    }

    @Test
    @Description("Проверяем валидацию при сумме, больше допустимой")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10572")
    public void moreThanMaxSumValidationTest() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignModerated(campaignId);
        DirectResponse response = cmdRule.oldSteps().payForAllSteps().payForAll(csrfToken, CLIENT_LOGIN,
                String.valueOf(campaignId), MORE_THAN_MAX_SUM, "1");
        cmdRule.oldSteps().commonSteps().checkDirectResponseContent(response,
                containsString(PayForAllErrors.PAY_FOR_ALL_IS_UNAVAILABLE_NOW.toString()));
    }

}
