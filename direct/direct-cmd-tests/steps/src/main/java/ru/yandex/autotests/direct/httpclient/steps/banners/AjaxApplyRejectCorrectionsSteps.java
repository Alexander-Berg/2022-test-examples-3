package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.AjaxApplyRejectCorrectionsParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by shmykov on 11.06.15.
 */
public class AjaxApplyRejectCorrectionsSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера AjaxApplyRejectCorrectionsSteps")
    public DirectResponse AjaxApplyRejectCorrections(CSRFToken token, AjaxApplyRejectCorrectionsParameters params) {
        return execute(getRequestBuilder().post(CMD.AJAX_APPLY_REJECT_CORRECTION, token, params));
    }
}