package ru.yandex.autotests.direct.cmd.autopayment;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.autopayment.AjaxSaveAutopaySettingsRequest;
import ru.yandex.autotests.direct.cmd.data.autopayment.AutoPaymentSettingsErrorsEnum;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutopaySettingsPaymethodType;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("валидация сохранения данных автоплатежа ЯД контроллером ajaxSaveAutopaySettings")
@Stories(TestFeatures.Wallet.AJAX_SAVE_AUTOPAY_SETTINGS)
@Features(TestFeatures.WALLET)
@Tag(CmdTag.AJAX_SAVE_AUTOPAY_SETTINGS)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@Ignore("не работает из-за проблем с Я.Деньгами")
public class AutoPaymentYMValidationTest extends AutoPaymentTestBase {

    private static final String CLIENT = "at-direct-backend-ydm3";

    private AjaxSaveAutopaySettingsRequest expectedRequest;

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Before
    public void before() {
        super.prepareData();
        cmdRule.getApiStepsRule().as(getClient());
        payMethodId = cmdRule.apiSteps().balanceSimpleSteps()
                .getPaymentMethod();
        expectedRequest = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_AUTOPAY_SETTINGS,
                AjaxSaveAutopaySettingsRequest.class)
                .withCid(String.valueOf(walletId))
                .withPaymethodId(payMethodId);
        expectedRequest.getJsonAutopay().withPaymethodType(AutopaySettingsPaymethodType.yandex_money.getLiteral());
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9049")
    public void checkTooLongPaymentSum() {
        expectedRequest.getJsonAutopay().withPaymentSum("15001");
        String errorText = String.format(AutoPaymentSettingsErrorsEnum.PAYMENT_YM_SUM_TOO_LONG.toString(),
                15000,
                moneyCurrency.getAbbreviation(DirectTestRunProperties.getInstance().getDirectCmdLocale()));
        check(errorText);
    }

    private void check(String errorText) {
        ErrorResponse errorResponse = cmdRule.cmdSteps().autopaySettingsSteps()
                .postAjaxSaveAutopaySettingsErrorResponse(expectedRequest);
        assertThat("ошибка соответствует ожидаемой", errorResponse.getError(),
                equalTo(errorText));
    }
}
