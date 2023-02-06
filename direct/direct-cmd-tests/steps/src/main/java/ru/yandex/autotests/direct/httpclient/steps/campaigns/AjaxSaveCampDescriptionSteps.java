package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import org.apache.http.client.methods.HttpGet;
import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.data.campaigns.AjaxSavecampDescriptionParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.direct.httpclient.data.Headers.ACCEPT_JSON_HEADER;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : shmykovo@yandex-team.ru
 *         Date: 12.11.14
 */
public class AjaxSaveCampDescriptionSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера ajaxSaveCampDescription")
    public DirectResponse getAjaxSaveCampDescriptionResponse(
            CSRFToken csrfToken, AjaxSavecampDescriptionParameters ajaxSavecampDescriptionParameters) {
        HttpGet getRequest = getRequestBuilder().get(CMD.AJAX_SAVE_CAMP_DESCRIPTION, csrfToken, ajaxSavecampDescriptionParameters);
        getRequest.addHeader(ACCEPT_JSON_HEADER);
        return execute(getRequest);
    }

    @Step("Проверяем, что ответ контроллера удовлетворяет условию {2}")
    public void checkSaveCampDescriptionResponseContent(CSRFToken csrfToken,
                                                        AjaxSavecampDescriptionParameters ajaxSavecampDescriptionParameters,
                                                        Matcher<String> matcher) {
        DirectResponse response = getAjaxSaveCampDescriptionResponse(csrfToken, ajaxSavecampDescriptionParameters);
        assertThat("содержимое ответа контроллера соответствует условию", response.getResponseContent().asString(), matcher);
    }

    @Step("Проверяем, что поле error_code в ответе контроллера соответствует {2}")
    public void checkSaveCampDescriptionErrorCode(CSRFToken csrfToken,
                                                        AjaxSavecampDescriptionParameters ajaxSavecampDescriptionParameters,
                                                        Matcher<String> matcher) {
        DirectResponse response = getAjaxSaveCampDescriptionResponse(csrfToken, ajaxSavecampDescriptionParameters);
        assertThat("поле error_code в ответе контроллера соответствует условию", response, hasJsonProperty(Responses.ERROR_CODE.getPath(), matcher));
    }

    @Step("Проверяем, что поле error_no в ответе контроллера соответствует {2}")
    public void checkSaveCampDescriptionErrorNumber(CSRFToken csrfToken,
                                                  AjaxSavecampDescriptionParameters ajaxSavecampDescriptionParameters,
                                                  Matcher<String> matcher) {
        DirectResponse response = getAjaxSaveCampDescriptionResponse(csrfToken, ajaxSavecampDescriptionParameters);
        assertThat("поле error_no в ответе контроллера соответствует условию", response, hasJsonProperty(Responses.ERROR_NO.getPath(), matcher));
    }
}
