package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by shmykov on 28.05.15.
 */
public class AjaxUpdateShowConditionsSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера AjaxUpdatePhrasesAndPrices")
    public DirectResponse ajaxUpdateShowConditions(CSRFToken token, AjaxUpdateShowConditionsParameters params) {
        return execute(getRequestBuilder().post(CMD.AJAX_UPDATE_SHOW_CONDITIONS, token, params));
    }
}