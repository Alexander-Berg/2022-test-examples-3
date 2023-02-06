package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import org.apache.http.client.methods.HttpPost;
import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.data.campaigns.AjaxStopResumeCampParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.direct.httpclient.data.Headers.ACCEPT_JSON_HEADER;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : shmykov@yandex-team.ru)
 *         Date:12.11.14
 */
public class AjaxStopResumeCampSteps extends DirectBackEndSteps {

    @Step("Проверка, что поле status в ответе контроллера соответствует {2}")
    public void saveAndCheckStatus(CSRFToken csrfToken, AjaxStopResumeCampParameters params, Matcher<String> matcher) {
        DirectResponse response = getAjaxStopResumeCampResponse(csrfToken, params);
        assertThat("поле status в ответе контроллера соответствует условию", response,
                hasJsonProperty(Responses.STATUS.getPath(), matcher));
    }

    @Step("Проверка, что поле error в ответе контроллера соответствует {2}")
    public void saveAndCheckError(CSRFToken csrfToken, AjaxStopResumeCampParameters params, Matcher<String> matcher) {
        DirectResponse response = getAjaxStopResumeCampResponse(csrfToken, params);
        assertThat("поле error в ответе соответствует условию", response,
                hasJsonProperty(Responses.ERROR.getPath(), matcher));
    }

    @Step("Проверка, что поле error_no в ответе контроллера соответствует {2}")
    public void saveAndCheckErrorNumber(CSRFToken csrfToken, AjaxStopResumeCampParameters params, Matcher<String> matcher) {
        DirectResponse response = getAjaxStopResumeCampResponse(csrfToken, params);
        assertThat("поле error_no в ответе соответствует условию", response,
                hasJsonProperty(Responses.ERROR_NO.getPath(), matcher));
    }

    @Step("Проверка, что поле error_code в ответе контроллера соответствует {2}")
    public void saveAndCheckErrorCode(CSRFToken csrfToken, AjaxStopResumeCampParameters params, Matcher<String> matcher) {
        DirectResponse response = getAjaxStopResumeCampResponse(csrfToken, params);
        assertThat("поле error_code в ответе соответствует условию", response,
                hasJsonProperty(Responses.ERROR_CODE.getPath(), matcher));
    }

    @Step("получение ответа контроллера ajaxStopResumeCamp")
    public DirectResponse getAjaxStopResumeCampResponse(CSRFToken token, AjaxStopResumeCampParameters params) {
        HttpPost postRequest = getRequestBuilder().post(CMD.AJAX_STOP_RESUME_CAMP, token, params);
        postRequest.addHeader(ACCEPT_JSON_HEADER);
        DirectResponse response = execute(postRequest);
        return response;
    }
}
