package ru.yandex.autotests.direct.httpclient.steps.retargeting;

import org.apache.http.client.methods.HttpPost;
import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.data.retargeting.AjaxSaveRetargetingCondParameters;
import ru.yandex.autotests.direct.httpclient.data.retargeting.Retargeting;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.direct.httpclient.data.Headers.*;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 29.09.14
 */
public class AjaxSaveRetargetingCondSteps extends DirectBackEndSteps {

    @Step("Проверка ответа при сохранении условий ретаргетинга для логина {0}")
    public void saveAndCheckResponseRetargeting(String login, Retargeting retargeting, CSRFToken csrfToken) {
        DirectResponse response = saveRetargetingConditions(login, retargeting, csrfToken);
        assertThat("успешное сохранение условий ретаргетинга", response,
                hasJsonProperty(Responses.RESULT.getPath(), equalTo("ok")));
    }

    @Step("Проверка ошибки при сохранении условий ретаргетинга для логина {0}")
    public void saveAndCheckErrorInResponseRetargeting(String login,  Retargeting retargeting,
                                                       Matcher matcher, CSRFToken csrfToken) {
        DirectResponse response = saveRetargetingConditions(login, retargeting, csrfToken);
        assertThat("поле error_type в ответе контроллера соответствует условию", response,
                hasJsonProperty(Responses.ERROR_TYPE.getPath(), matcher));
    }

    @Step("Проверка номера ошибки при сохранении условий ретаргетинга для логина {0}")
    public void saveAndCheckErrorNumberInResponseRetargeting(String login,  Retargeting retargeting,
                                                             Matcher matcher, CSRFToken csrfToken) {
        DirectResponse response = saveRetargetingConditions(login, retargeting, csrfToken);
        assertThat("поле error_no в ответе контроллера соответствует условию", response,
                hasJsonProperty(Responses.ERROR_NO.getPath(), matcher));
    }

    @Step("Сохранение условий ретаргетинга для логина {0}")
    public DirectResponse saveRetargetingConditions(String login, Retargeting retargeting, CSRFToken token) {
        DirectResponse response = execute(baseAjaxRetargetingRequest(login,
                retargeting, token, CMD.AJAX_SAVE_RETARGETING_COND));
        return response;
    }

    private HttpPost baseAjaxRetargetingRequest(
            String login, Retargeting retargeting, CSRFToken token, CMD controller) {
        AjaxSaveRetargetingCondParameters parameters = new AjaxSaveRetargetingCondParameters();
        parameters.setuLogin(login);
        String json = null;
        if(retargeting != null) {
            json = retargeting.toJson();
        }
        parameters.setJsonRetargetingCondition(json);
        HttpPost postRequest = getRequestBuilder().post(controller, token, parameters);
        postRequest.addHeader(ACCEPT_JSON_HEADER);
        return postRequest;
    }
}
