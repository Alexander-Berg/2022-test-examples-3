package ru.yandex.autotests.direct.httpclient.steps.retargeting;

import org.apache.http.client.methods.HttpPost;
import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.data.retargeting.AjaxDeleteRetargetingCondParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.direct.httpclient.data.Headers.ACCEPT_JSON_HEADER;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 26.01.15.
 * TESTIRT-4058
 */
public class AjaxDeleteRetargetingCondSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера AjaxDeleteRetargetingCondSteps")
    public DirectResponse ajaxDeleteRetargetingCond(CSRFToken token, AjaxDeleteRetargetingCondParameters params) {
        return execute(baseAjaxDeleteRetargetingRequest(CMD.AJAX_DELETE_RETARGETING_COND, token, params));
    }

    @Step("Проверка положительного ответа после удаления условия ретаргетинга")
    public void deleteRetargetingAndCheckPositiveResponse(CSRFToken token, AjaxDeleteRetargetingCondParameters params) {
        DirectResponse response = ajaxDeleteRetargetingCond(token, params);
        assertThat("успешный ответ контроллера", response,
                hasJsonProperty(Responses.RESULT.getPath(), equalTo("ok")));
    }

    @Step("Проверка текста ошибки при удалении условия ретаргетинга")
    public void deleteRetargetingAndCheckError(CSRFToken token, AjaxDeleteRetargetingCondParameters params, Matcher matcher) {
        DirectResponse response = ajaxDeleteRetargetingCond(token, params);
        assertThat("ответ контроллера содержит ошибку, удовлетворющую условию", response,
                hasJsonProperty(Responses.ERROR.getPath(), matcher));
    }

    private HttpPost baseAjaxDeleteRetargetingRequest(CMD controller, CSRFToken token,
                                                      AjaxDeleteRetargetingCondParameters params) {
        HttpPost postRequest = getRequestBuilder().post(controller, token, params);
        postRequest.addHeader(ACCEPT_JSON_HEADER);
        return postRequest;
    }

}
