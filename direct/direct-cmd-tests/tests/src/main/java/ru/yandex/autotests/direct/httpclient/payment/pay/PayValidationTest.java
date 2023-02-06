package ru.yandex.autotests.direct.httpclient.payment.pay;

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
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignValidationErrors;
import ru.yandex.autotests.direct.httpclient.data.textresources.pay.PayErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;


/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 *         https://st.yandex-team.ru/TESTIRT-5002
 */

@Aqua.Test
@Description("Проверка валидации контроллера pay")
@Stories(TestFeatures.Payment.PAY)
@Features(TestFeatures.PAYMENT)
@Tag(CmdTag.PAY)
@Tag(OldTag.YES)
public class PayValidationTest {

    private static final String CLIENT_LOGIN = "at-direct-backend-c";
    private static final String ANOTHER_CLIENT_CID = "123";
    private static final String INCORRECT_CID = "abc";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
    }

    @Test
    @Description("Проверяем валидацию при пустом cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10560")
    public void emptyCidValidationTest() {
        DirectResponse response = cmdRule.oldSteps().paySteps().openPay(CLIENT_LOGIN, null);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CampaignValidationErrors.EMPTY_CID.toString());
    }

    @Test
    @Description("Проверяем валидацию при чужом cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10561")
    public void anotherClientCidValidationTest() {
        DirectResponse response = cmdRule.oldSteps().paySteps().openPay(CLIENT_LOGIN, ANOTHER_CLIENT_CID);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.RIGHTS_CHECK_ERROR.toString());
    }

    @Test
    @Description("Проверяем валидацию при некорректном cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10562")
    public void incorrectCidValidationTest() {
        DirectResponse response = cmdRule.oldSteps().paySteps().openPay(CLIENT_LOGIN, INCORRECT_CID);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, String.format(
                TextResourceFormatter.resource(CampaignValidationErrors.INCORRECT_CID).toString(), INCORRECT_CID));
    }

    @Test
    @Description("Проверяем валидацию непромодерированной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10563")
    public void noModerateCampaignValidationTest() {
        DirectResponse response = cmdRule.oldSteps().paySteps().openPay(CLIENT_LOGIN, String.valueOf(campaignId));
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(String.format(
                TextResourceFormatter.resource(PayErrors.CAMPAIGN_DOES_NOT_MODERATE).toString(), campaignId)));
    }

}
