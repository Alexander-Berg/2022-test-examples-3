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
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.payment.Client;
import ru.yandex.autotests.direct.httpclient.data.payment.PayResponseBean;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 * Date: 16.06.15
 * https://st.yandex-team.ru/TESTIRT-5002
 */

@Aqua.Test
@Description("Проверка контроллера pay")
@Stories(TestFeatures.Payment.PAY)
@Features(TestFeatures.PAYMENT)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SHOW_REGISTER_LOGIN_PAGE)
@Tag(OldTag.YES)
public class PayTest {

    private static final String CLIENT_LOGIN = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_LOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private Long campaignId;
    private PayResponseBean expectedResponse;


    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignActive(bannersRule.getCampaignId());

        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());

        expectedResponse = new PayResponseBean();
        expectedResponse
                .setDirectDefaultPayRub(MoneyCurrency.get(Currency.RUB).getDefaultPaymentHintAmount().floatValue());
        expectedResponse.setClient(new Client().withNds(MoneyCurrency.get(Currency.RUB).getVatRate() * 100));
        expectedResponse
                .setToPay(expectedResponse.getDirectDefaultPayRub() + expectedResponse.getClient().getNds().floatValue() / 100
                        * expectedResponse.getDirectDefaultPayRub());


        expectedResponse.setCampaignId(String.valueOf(campaignId));
    }

    @Test
    @Description("Проверяем сумму по умлочанию, НДС, сумму платежа")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10559")
    public void parametersTest() {
        PayResponseBean actualResponse = cmdRule.oldSteps().paySteps().getPay(CLIENT_LOGIN, String.valueOf(campaignId));
        assertThat("Ответ контроллера соответсвует ожиданиям", actualResponse, beanEquivalent(expectedResponse));
    }
}
