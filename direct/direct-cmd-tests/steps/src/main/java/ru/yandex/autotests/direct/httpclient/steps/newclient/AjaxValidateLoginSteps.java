package ru.yandex.autotests.direct.httpclient.steps.newclient;

import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.data.newclient.AjaxValidateLoginParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.11.14
 */
public class AjaxValidateLoginSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера ajaxValidateLogin")
    public DirectResponse validateLogin(CSRFToken csrfToken, AjaxValidateLoginParameters ajaxValidateLoginParameters) {
        return execute(getRequestBuilder().post(CMD.AJAX_VALIDATE_LOGIN, csrfToken, ajaxValidateLoginParameters));
    }

    @Step("Проверяем, что в ответе контроллера ajaxValidateLogin нет ошибок")
    public void checkAjaxValidateLoginCorrectResponse(
            CSRFToken csrfToken, AjaxValidateLoginParameters ajaxValidateLoginParameters) {
        DirectResponse response = validateLogin(csrfToken, ajaxValidateLoginParameters);
        assertThat("статус ответа контроллера соответсвует ожиданиям", response,
                hasJsonProperty(Responses.STATUS.getPath(), equalTo("ok")));
        assertThat("ответ не содержит ошибки", response,
                hasJsonProperty("$.validation_errors", is(not(emptyCollectionOf(Object.class)))));
    }

    @Step("Проверяем, что в ответе контроллера ajaxValidateLogin код ошибки валидации удовлетворяет условию {0}")
    public void checkAjaxValidateLoginResponseValidationErrorCodes(
            CSRFToken csrfToken, AjaxValidateLoginParameters ajaxValidateLoginParameters,
            Matcher matcher) {
        DirectResponse response = validateLogin(csrfToken, ajaxValidateLoginParameters);
        assertThat("статус ответа контроллера соответсвует ожиданиям", response,
                hasJsonProperty(Responses.STATUS.getPath(), equalTo("ok")));
        assertThat("код ошибки соответствует ожиданиям", response,
                hasJsonProperty("$.validation_errors[0].code", matcher));
    }

    @Step("Проверяем, что в ответе контроллера ajaxValidateLogin код ошибки удовлетворяет условию {0}")
    public void checkAjaxValidateLoginResponseErrorCodes(CSRFToken csrfToken, AjaxValidateLoginParameters ajaxValidateLoginParameters,
                                                         Matcher matcher) {
        DirectResponse response = validateLogin(csrfToken, ajaxValidateLoginParameters);
        assertThat("код ошибки соответствует ожиданиям", response,
                hasJsonProperty("$.errors[0].code", matcher));
    }

}
