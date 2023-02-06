package ru.yandex.autotests.direct.httpclient.steps.newclient;

import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.newclient.AjaxRegisterLoginParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.11.14
 */
public class AjaxRegisterLoginSteps extends DirectBackEndSteps {

    @Step("Регистрируем клиента агентства")
    public DirectResponse registerAgencyClient(CSRFToken csrfToken,
                                               AjaxRegisterLoginParameters ajaxRegisterLoginParameters) {
        return execute(getRequestBuilder().get(CMD.AJAX_REGISTER_LOGIN, csrfToken,
                ajaxRegisterLoginParameters));
    }

    @Step("Проверяем, что в ответе контроллера ajaxValidateLogin код ошибки валидации удовлетворяет условию {0}")
    public void checkAjaxValidateLoginResponseValidationErrorCodes(
            CSRFToken csrfToken, AjaxRegisterLoginParameters ajaxRegisterLoginParameters,
            Matcher matcher) {
        DirectResponse response = registerAgencyClient(csrfToken, ajaxRegisterLoginParameters);
        assertThat("Ответ контроллера содержит правильный код ошибки", response,
                hasJsonProperty("$..code", matcher));
    }

    @Step("Проверяем ошибку в ответе контроллера ajaxValidateLogin {0}")
    public void checkAjaxValidateLoginResponseError(
            CSRFToken csrfToken, AjaxRegisterLoginParameters ajaxRegisterLoginParameters,
            Matcher matcher) {
        DirectResponse response = registerAgencyClient(csrfToken, ajaxRegisterLoginParameters);
        assertThat("статус ответа содержит код ошибки", response,  hasJsonProperty("$.status", equalTo("error")));
        assertThat("массив ошибок удовлетворяет условию", response,  hasJsonProperty("$.errors", matcher));
    }
}
