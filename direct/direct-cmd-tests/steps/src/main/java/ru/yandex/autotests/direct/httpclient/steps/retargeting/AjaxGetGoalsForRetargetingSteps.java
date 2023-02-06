package ru.yandex.autotests.direct.httpclient.steps.retargeting;

import org.apache.http.client.methods.HttpPost;
import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.direct.httpclient.data.Headers.ACCEPT_JSON_HEADER;

/**
 * Created by shmykov on 27.01.15.
 * TESTIRT-4056
 */
public class AjaxGetGoalsForRetargetingSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера ajaxGetGoalsForRetargeting")
    public DirectResponse ajaxGetGoalsForRetargeting(BasicDirectFormParameters params, CSRFToken token) {
        return execute(baseAjaxRetargetingRequest(CMD.AJAX_GET_GOALS_FOR_RETARGETING, token, params));
    }

    private HttpPost baseAjaxRetargetingRequest(CMD controller, CSRFToken token,
                                                BasicDirectFormParameters params) {
        HttpPost postRequest = getRequestBuilder().post(controller, token, params);
        postRequest.addHeader(ACCEPT_JSON_HEADER);
        return postRequest;
    }
}