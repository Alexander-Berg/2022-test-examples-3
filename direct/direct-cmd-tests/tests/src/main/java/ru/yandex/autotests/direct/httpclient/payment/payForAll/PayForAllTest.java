package ru.yandex.autotests.direct.httpclient.payment.payForAll;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.httpclient.lite.utils.HttpUtils.getUrlParameterValue;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 *         https://st.yandex-team.ru/TESTIRT-5000
 */

@Aqua.Test
@Description("Проверка контроллера payForAll")
@Stories(TestFeatures.Payment.PAY_FOR_ALL)
@Features(TestFeatures.PAYMENT)
@Tag(CmdTag.PAY_FOR_ALL)
@Tag(OldTag.YES)
public class PayForAllTest {
    private static final String CLIENT_LOGIN = Logins.CLIENT_WITHOUT_WALLET;
    private static final String DEFAULT_SUM = "1770";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private CSRFToken csrfToken;


    @Before
    public void before() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignModerated(bannersRule.getCampaignId());
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT_LOGIN).getPassportUID());
    }

    @Test
    @Description("Проверяем редирект на страницу баланса")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10564")
    public void redirectTest() {
        DirectResponse response = cmdRule.oldSteps().payForAllSteps().payForAll(csrfToken, CLIENT_LOGIN,
                String.valueOf(bannersRule.getCampaignId()), DEFAULT_SUM, "1");
        cmdRule.oldSteps().commonSteps().checkRedirect(response, containsString("paypreview.xml"));
    }

    @Test
    @Description("Проверяем сумму c НДС через api баланса")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10566")
    public void sumWithVatTest() {
        DirectResponse response = cmdRule.oldSteps().payForAllSteps().payForAll(csrfToken, CLIENT_LOGIN,
                String.valueOf(bannersRule.getCampaignId()), DEFAULT_SUM, "1");
        String requestID = getUrlParameterValue(response.getParameterFromRedirect("retpath"), "request_id");
        cmdRule.apiSteps().balanceSteps().operator(User.get(CLIENT_LOGIN))
                .requestAmountShouldBe(requestID,
                        equalTo(Float.valueOf(DEFAULT_SUM)));
    }

    @Test
    @Description("Проверяем сумму без НДС через api баланса")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10565")
    public void sumWithoutVatTest() {
        DirectResponse response = cmdRule.oldSteps().payForAllSteps().payForAll(csrfToken, CLIENT_LOGIN,
                String.valueOf(bannersRule.getCampaignId()), DEFAULT_SUM, "0");
        String requestID = getUrlParameterValue(response.getParameterFromRedirect("retpath"), "request_id");
        cmdRule.apiSteps().balanceSteps().operator(User.get(CLIENT_LOGIN))
                .requestAmountShouldBe(requestID,
                        equalTo(Float.valueOf(DEFAULT_SUM) * (1 + (float) MoneyCurrency.get(Currency.RUB)
                                .getVatRate())));
    }
}
