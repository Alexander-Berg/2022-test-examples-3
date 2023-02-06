package ru.yandex.autotests.direct.httpclient.steps.newclient;

import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.data.newclient.AjaxValidatePasswordParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.11.14
 */
public class AjaxValidatePasswordSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера AjaxValidatePassword")
    public DirectResponse validatePassword(CSRFToken csrfToken,
                                           AjaxValidatePasswordParameters ajaxValidatePasswordParameters) {
        return execute(getRequestBuilder().post(CMD.AJAX_VALIDATE_PASSWORD, csrfToken, ajaxValidatePasswordParameters));
    }

    @Step("Проверяем, что в ответе контроллера ajaxValidateLogin код ошибки валидации удовлетворяет условию {0}")
    public void checkAjaxValidatePasswordResponseValidationErrorCodes(
            CSRFToken csrfToken, AjaxValidatePasswordParameters ajaxValidatePasswordParameters, Matcher matcher) {
        DirectResponse response = validatePassword(csrfToken, ajaxValidatePasswordParameters);
        assertThat("статус ответа контроллера соответсвует ожиданиям", response,
                hasJsonProperty(Responses.STATUS.getPath(), equalTo("ok")));
        assertThat("код ошибки соответсвует ожиданиям", response,
                hasJsonProperty("$.validation_errors[0].code", matcher));
    }

    @Step("Проверяем, что в ответе контроллера ajaxValidateLogin код ошибки удовлетворяет условию {0}")
    public void checkAjaxValidatePasswordResponseErrorsCodes(
            CSRFToken csrfToken, AjaxValidatePasswordParameters ajaxValidatePasswordParameters, Matcher matcher) {
        DirectResponse response = validatePassword(csrfToken, ajaxValidatePasswordParameters);
        assertThat("код ошибки соответсвует ожиданиям", response,
                hasJsonProperty("$.errors[0].code", matcher));
    }

    @Step("Проверяем, что в ответе контроллера ajaxValidateLogin код предупреждения удовлетворяет условию {0}")
    public void checkAjaxValidatePasswordResponseWarningCodes(
            CSRFToken csrfToken, AjaxValidatePasswordParameters ajaxValidatePasswordParameters, Matcher matcher) {
        DirectResponse response = validatePassword(csrfToken, ajaxValidatePasswordParameters);
        assertThat("статус ответа контроллера соответсвует ожиданиям", response,
                hasJsonProperty(Responses.STATUS.getPath(), equalTo("ok")));
        assertThat("код предупреждения соответсвует ожиданиям", response,
                hasJsonProperty("$.validation_warnings[0].code", matcher));
    }

    @Step("Проверяем, что в ответе контроллера ajaxValidateLogin не содержится ошибок и предупреждений")
    public void checkAjaxValidatePasswordCorrectResponse(
            CSRFToken csrfToken, AjaxValidatePasswordParameters ajaxValidatePasswordParameters) {
        DirectResponse response = validatePassword(csrfToken, ajaxValidatePasswordParameters);
        assertThat("статус ответа контроллера соответсвует ожиданиям", response,
                hasJsonProperty(Responses.STATUS.getPath(), equalTo("ok")));
        assertThat("ответ контроллера не содержит ошибку", response,
                hasJsonProperty("$.validation_errors", is(not((emptyCollectionOf(Object.class))))));
        assertThat("ответ контроллера не содержит предупреждений", response,
                hasJsonProperty("$.validation_warnings", is(not((emptyCollectionOf(Object.class))))));
    }
}
