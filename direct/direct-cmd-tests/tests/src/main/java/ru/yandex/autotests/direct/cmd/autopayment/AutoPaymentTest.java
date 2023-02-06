package ru.yandex.autotests.direct.cmd.autopayment;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.autopayment.AjaxSaveAutopaySettingsRequest;
import ru.yandex.autotests.direct.cmd.data.autopayment.AutopaySettingsResponse;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.autopayment.AutoPaymentHelper;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutopaySettingsPaymethodType;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.balancesimple.request.BindCreditCardRequest;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка сохранения данных автоплатежа контроллером ajaxSaveAutopaySettings")
@Stories(TestFeatures.Wallet.AJAX_SAVE_AUTOPAY_SETTINGS)
@Features(TestFeatures.WALLET)
@Tag(CmdTag.AJAX_SAVE_AUTOPAY_SETTINGS)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AutoPaymentTest {
    private static final String MIN_PAYMENT_SUM = "1000.000000";
    // Срок действия всегда заканчивается в следующем году
    private static final String CardExpirationYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR) % 100 + 1);
    @ClassRule
    public static ApiSteps api = new ApiSteps();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public String client;
    @Parameterized.Parameter(1)
    public AutopaySettingsPaymethodType paymethodType;
    private String payMethodId;
    private Long walletId;
    private AjaxSaveAutopaySettingsRequest expectedRequest;
    private int shard;

    @Parameterized.Parameters(name = "Под логином {0}, тип оплаты: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"yndx-direct-rus-os", AutopaySettingsPaymethodType.card},
                {"yndx-direct-light-os", AutopaySettingsPaymethodType.card},
                {"at-direct-backend-rus-os-p", AutopaySettingsPaymethodType.card},// представитель
        });
    }

    @Before
    public void before() {
        shard = api.userSteps.clientFakeSteps().getUserShard(client);
        api.userSteps.getDirectJooqDbSteps().useShard(shard);

        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign(client);
        api.userSteps.campaignFakeSteps().makeCampaignFullyModerated(campaignId);
        // подключаем общий счет
        api.userSteps.financeSteps().enableSharedAccount(client);
        walletId = api.userSteps.getDirectJooqDbSteps().campaignsSteps().getCampaignById(campaignId).getWalletCid();
        AutoPaymentHelper.deleteAutopaymentDB(walletId, client);

        expectedRequest = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_AUTOPAY_SETTINGS,
                AjaxSaveAutopaySettingsRequest.class)
                .withCid(String.valueOf(walletId))
                .withUlogin(client);
        expectedRequest.getJsonAutopay().withPaymentSum(MIN_PAYMENT_SUM);
        expectedRequest.getJsonAutopay().withPaymethodType(paymethodType.getLiteral());
        switch (paymethodType) {
            case card:
                cmdRule.getApiStepsRule().as(client);
                payMethodId = cmdRule.apiSteps().balanceSimpleSteps()
                        .bindCreditCard(new BindCreditCardRequest().defaultCreditCard().withExpirationYear(CardExpirationYear)).getPaymentMethod();
                break;
            case yandex_money:
                cmdRule.getApiStepsRule().as(client);
                payMethodId = cmdRule.apiSteps().balanceSimpleSteps().getPaymentMethod();
                break;
        }
        expectedRequest.withPaymethodId(payMethodId);
    }

    @Test
    @Description("Проверка включения автопополнения")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9037")
    public void checkEnableAutoPayment() {
        check();
    }

    @Test
    @Description("Проверка изменения автопополнения")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9038")
    public void checkChangeAutoPayment() {
        AutoPaymentHelper.enableAutopaymentDB(walletId, client);
        check();
    }

    private void check() {
        CommonResponse commonResponse = cmdRule.cmdSteps().autopaySettingsSteps()
                .postAjaxSaveAutopaySettings(expectedRequest);
        assumeThat("контроллер выполнился успешно", commonResponse.getResult(),
                equalTo("ok"));

        AutopaySettingsResponse actualResponse = cmdRule.cmdSteps().autopaySettingsSteps().getAutopaySettings(client);
        assertThat("данные сохранились", actualResponse,
                beanDiffer(AutopaySettingsResponse.fromAutopaySettingsRequest(expectedRequest))
                        .useCompareStrategy(onlyExpectedFields()));
    }
}
