package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.phrases.ajaxgettransitionsbyphrases.AjaxGetTransitionsByPhrasesParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by shmykov on 09.06.15.
 */
public class AjaxGetTransitionsByPhrasesSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера AjaxGetTransitionsByPhrases")
    public DirectResponse ajaxGetTransitionsByPhrases(AjaxGetTransitionsByPhrasesParameters params, CSRFToken token) {
        return execute(getRequestBuilder().post(CMD.AJAX_GET_TRANSITIONS_BY_PHRASES, token, params));
    }
}