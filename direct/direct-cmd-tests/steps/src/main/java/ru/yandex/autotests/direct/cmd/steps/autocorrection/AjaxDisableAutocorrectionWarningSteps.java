package ru.yandex.autotests.direct.cmd.steps.autocorrection;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.autocorrection.AjaxDisableAutocorrectionWarningRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class AjaxDisableAutocorrectionWarningSteps extends DirectBackEndSteps {

    @Step("GET ajaxDisableAutocorrectionWarning (отключение сообщения об исправленном баннере)")
    public CommonResponse getAjaxDisableAutocorrectionWarning(AjaxDisableAutocorrectionWarningRequest request) {
        return get(CMD.AJAX_DISABLE_AUTOCORRECTION_WARNING, request, CommonResponse.class);
    }

    @Step("отключение сообщения об исправленном баннере (bid = {0}, login = {1})")
    public CommonResponse getAjaxDisableAutocorrectionWarning(Long bid, String login) {
        AjaxDisableAutocorrectionWarningRequest request = new AjaxDisableAutocorrectionWarningRequest();
        request.setBid(bid.toString());
        request.setUlogin(login);
        return getAjaxDisableAutocorrectionWarning(request);
    }

    public CommonResponse getAjaxDisableAutocorrectionWarning(Long bid) {
        return getAjaxDisableAutocorrectionWarning(bid, null);
    }
}
