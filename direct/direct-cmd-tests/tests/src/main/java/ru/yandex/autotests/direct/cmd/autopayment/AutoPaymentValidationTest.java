package ru.yandex.autotests.direct.cmd.autopayment;

import org.junit.Before;
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
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("валидация сохранения данных автоплатежа контроллером ajaxSaveAutopaySettings")
@Stories(TestFeatures.Wallet.AJAX_SAVE_AUTOPAY_SETTINGS)
@Features(TestFeatures.WALLET)
@Tag(CmdTag.AJAX_SAVE_AUTOPAY_SETTINGS)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
public class AutoPaymentValidationTest extends AutoPaymentTestBase {

    private static final String CLIENT = "at-direct-backend-rus-os2";
    private static final String WRONG_PAYMENT_ID = "111";
    private static final String TOO_LONG_VALUE = "300001";
    private static final String TOO_SHORT_VALUE = "299";
    private static final String MAX_PAYMENT_SUM = "249999.99";
    private static final String MAX_REMAINING_SUM = "1000000";

    private AjaxSaveAutopaySettingsRequest expectedRequest;

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Before
    public void before() {
        super.before();

        expectedRequest = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_AUTOPAY_SETTINGS,
                AjaxSaveAutopaySettingsRequest.class)
                .withCid(String.valueOf(walletId))
                .withPaymethodId(payMethodId);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9041")
    public void checkEmptyPaymethodType() {
        expectedRequest.getJsonAutopay().withPaymethodType("");
        check(AutoPaymentSettingsErrorsEnum.WRONG_INPUT_DATA.toString());
    }


    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9042")
    public void checkEmptyAutopayModeType() {
        expectedRequest.getJsonAutopay().withAutopayMode("");
        check(AutoPaymentSettingsErrorsEnum.WRONG_INPUT_DATA.toString());
    }


    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9043")
    public void checkEmptyPaymethodId() {
        expectedRequest.getJsonAutopay().withPaymethodId("");
        check(AutoPaymentSettingsErrorsEnum.WRONG_CARD.toString());
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9044")
    public void checkWrongPaymethodId() {
        expectedRequest.getJsonAutopay().withPaymethodId(WRONG_PAYMENT_ID);
        check(AutoPaymentSettingsErrorsEnum.WRONG_CARD.toString());
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9045")
    public void checkTooShortPaymentSum() {
        expectedRequest.getJsonAutopay().withPaymentSum(TOO_SHORT_VALUE);
        String errorText = String.format(AutoPaymentSettingsErrorsEnum.PAYMENT_SUM_TOO_SHORT.toString(),
                moneyCurrency.getMinInvoiceAmount().longValue(),
                moneyCurrency.getAbbreviation(DirectTestRunProperties.getInstance().getDirectCmdLocale()));
        check(errorText);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9046")
    public void checkTooLongPaymentSum() {
        expectedRequest.getJsonAutopay().withPaymentSum(TOO_LONG_VALUE);
        String errorText = String.format(AutoPaymentSettingsErrorsEnum.PAYMENT_SUM_TOO_LONG.toString(),
                MAX_PAYMENT_SUM,
                moneyCurrency.getAbbreviation(DirectTestRunProperties.getInstance().getDirectCmdLocale()));
        check(errorText);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9047")
    public void checkTooShortRemainingSum() {
        expectedRequest.getJsonAutopay().withRemainingSum("-1");
        check(AutoPaymentSettingsErrorsEnum.REMAINING_SUM_TOO_SHORT.toString());
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9048")
    public void checkTooLongRemainingSum() {
        expectedRequest.getJsonAutopay().withRemainingSum(MAX_REMAINING_SUM);
        check(AutoPaymentSettingsErrorsEnum.REMAINING_SUM_TOO_LONG.toString());
    }

    private void check(String errorText) {
        ErrorResponse errorResponse = cmdRule.cmdSteps().autopaySettingsSteps()
                .postAjaxSaveAutopaySettingsErrorResponse(expectedRequest);
        assertThat("ошибка соответствует ожидаемой", errorResponse.getError(),
                equalTo(errorText));
    }
}
