package ru.yandex.autotests.direct.cmd.autopayment;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.autopayment.AjaxSaveAutopaySettingsRequest;
import ru.yandex.autotests.direct.cmd.data.autopayment.AutoPaymentSettingsErrorsEnum;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutopaySettingsPaymethodType;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.balancesimple.request.BindCreditCardRequest;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка недоступности подключения карты другого представителя")
@Stories(TestFeatures.Wallet.AJAX_SAVE_AUTOPAY_SETTINGS)
@Features(TestFeatures.WALLET)
@Tag(CmdTag.AJAX_SAVE_AUTOPAY_SETTINGS)
@Tag(CmdTag.AUTOPAY_SETTINGS)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@Ignore("не работает из-за проблем с Я.Деньгами")
public class AutoPaymentReprUnavailableTest {

    private static final String REPRESENTATIVE_CARD = "at-direct-backend-rus-os-p";
    private static final String REPRESENTATIVE_YDM = "at-direct-backend-rus-os10";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private AjaxSaveAutopaySettingsRequest expectedRequest;
    private String paymentId1;
    private String paymentId2;

    @Before
    public void before() {
        cmdRule.getApiStepsRule().as(Logins.SUPER);
        Long walletCid = (long) cmdRule.apiSteps().financeSteps()
                .enableAndGetSharedAccount(REPRESENTATIVE_CARD);
        expectedRequest = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_AUTOPAY_SETTINGS,
                AjaxSaveAutopaySettingsRequest.class)
                .withCid(String.valueOf(walletCid));

        Long campaignId = cmdRule.apiAggregationSteps().getAllCampaigns(REPRESENTATIVE_CARD)[0];
        cmdRule.apiSteps().makeCampaignModerated(campaignId.intValue());

        cmdRule.getApiStepsRule().as(REPRESENTATIVE_CARD);
        paymentId1 = cmdRule.apiSteps().balanceSimpleSteps()
                .bindCreditCard(new BindCreditCardRequest().defaultCreditCard()).getPaymentMethod();
        cmdRule.getApiStepsRule().as(REPRESENTATIVE_YDM);
        paymentId2 = cmdRule.apiSteps().balanceSimpleSteps().getPaymentMethod();
    }


    @Test
    @Description("Проверка невозможности включить автопополнение ЯД представителем с картой")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9034")
    public void checkNoRightsForSaveAutoPaymentCard() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(REPRESENTATIVE_CARD));
        expectedRequest.getJsonAutopay().withPaymethodType(AutopaySettingsPaymethodType.yandex_money.getLiteral());
        expectedRequest.withPaymethodId(paymentId2);

        check();
    }

    @Test
    @Description("Проверка невозможности включить автопополнение картой представителем с ЯД")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9035")
    public void checkNoRightsForSaveAutoPaymentYM() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(REPRESENTATIVE_YDM));
        expectedRequest.getJsonAutopay().withPaymethodType(AutopaySettingsPaymethodType.card.getLiteral());
        expectedRequest.withPaymethodId(paymentId1);

        check();
    }

    private void check() {
        ErrorResponse errorResponse = cmdRule.cmdSteps().autopaySettingsSteps()
                .postAjaxSaveAutopaySettingsErrorResponse(expectedRequest);
        assertThat("нет прав для выполнения операции", errorResponse.getError(),
                containsString(AutoPaymentSettingsErrorsEnum.WRONG_CARD.toString()));
    }

}
