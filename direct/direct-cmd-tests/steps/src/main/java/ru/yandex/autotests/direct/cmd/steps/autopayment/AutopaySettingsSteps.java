package ru.yandex.autotests.direct.cmd.steps.autopayment;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.autopayment.AjaxResumeAutopayRequest;
import ru.yandex.autotests.direct.cmd.data.autopayment.AjaxSaveAutopaySettingsRequest;
import ru.yandex.autotests.direct.cmd.data.autopayment.AjaxUnbindCardRequest;
import ru.yandex.autotests.direct.cmd.data.autopayment.AutopaySettingsResponse;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AutopaySettingsPaymethodType;
import ru.yandex.qatools.allure.annotations.Step;

public class AutopaySettingsSteps extends DirectBackEndSteps {

    @Step("POST ajaxSaveAutopaySettings (сохранение данных об автоплатеже)")
    public CommonResponse postAjaxSaveAutopaySettings(AjaxSaveAutopaySettingsRequest request) {
        return post(CMD.AJAX_SAVE_AUTOPAY_SETTINGS, request, CommonResponse.class);
    }

    @Step("POST ajaxSaveAutopaySettings (неверное сохранение данных об автоплатеже)")
    public ErrorResponse postAjaxSaveAutopaySettingsErrorResponse(AjaxSaveAutopaySettingsRequest request) {
        return post(CMD.AJAX_SAVE_AUTOPAY_SETTINGS, request, ErrorResponse.class);
    }

    @Step("GET autopaySettings (получение данных об автоплатеже)")
    public AutopaySettingsResponse getAutopaySettings(BasicDirectRequest request) {
        return get(CMD.AUTOPAY_SETTINGS, request, AutopaySettingsResponse.class);
    }

    @Step("GET autopaySettings (получение данных об автоплатеже) для клиента {0}")
    public AutopaySettingsResponse getAutopaySettings(String client) {
        return getAutopaySettings((BasicDirectRequest) new BasicDirectRequest().withUlogin(client));
    }

    @Step("POST ajaxResumeAutopay (неверное возобновление автоплатежа)")
    public ErrorResponse postAjaxResumeAutopay(AjaxResumeAutopayRequest request) {
        return post(CMD.AJAX_RESUME_AUTOPAY, request, ErrorResponse.class);
    }

    @Step("GET ajaxGetBindingForm (получение ссылки на привязку карты)")
    public RedirectResponse getAjaxGetBindingForm(BasicDirectRequest request) {
        return post(CMD.AJAX_GET_BINDING_FORM, request, RedirectResponse.class);
    }

    @Step("GET ajaxGetBindingForm (получение ссылки на привязку карты) для клиента {0}")
    public RedirectResponse getAjaxGetBindingForm(String login) {
        return getAjaxGetBindingForm((BasicDirectRequest) new BasicDirectRequest().withUlogin(login));
    }

    @Step("POST ajaxUnbindCard (отвязка карты)")
    public CommonResponse postAjaxUnbindCard(AjaxUnbindCardRequest request) {
        return post(CMD.AJAX_UNBIND_CARD, request, CommonResponse.class);
    }

    @Step("отвязка карты с ид = {0}")
    public CommonResponse postAjaxUnbindCard(String paymethodId, AutopaySettingsPaymethodType paymethodType) {
        return postAjaxUnbindCard(new AjaxUnbindCardRequest()
                .withPaymethodId(paymethodId)
                .withPaymethodType(paymethodType.getLiteral()));
    }
}
