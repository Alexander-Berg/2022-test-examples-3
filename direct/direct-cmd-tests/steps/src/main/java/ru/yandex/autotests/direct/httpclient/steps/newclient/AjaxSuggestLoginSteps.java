package ru.yandex.autotests.direct.httpclient.steps.newclient;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Responses;
import ru.yandex.autotests.direct.httpclient.data.newclient.AjaxSuggestLoginParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.11.14
 */
public class AjaxSuggestLoginSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера ajaxSuggestLogin")
    public DirectResponse getLoginSuggest(CSRFToken csrfToken, AjaxSuggestLoginParameters ajaxSuggestLoginParameters) {
        return execute(getRequestBuilder().get(CMD.AJAX_SUGGEST_LOGIN, csrfToken, ajaxSuggestLoginParameters));
    }

    @Step("Проверяем, что ответ контроллера содержит список логинов, удовлетворяющий условию {0}")
    public void checkAjaxSuggestLoginResponse(CSRFToken csrfToken,
                                              AjaxSuggestLoginParameters ajaxSuggestLoginParameters,
                                              Matcher<List<String>> matcher) {
        DirectResponse response = getLoginSuggest(csrfToken, ajaxSuggestLoginParameters);
        assertThat("статус ответа контроллера соответсвует ожиданиям", response,
                hasJsonProperty(Responses.STATUS.getPath(), equalTo("ok")));
        List<String> suggestLogins = JsonPath.read(response.getResponseContent().asString(), "$.logins");
        assertThat("логины из ответа контроллера не удовлетворяют условию", suggestLogins, matcher);


    }


}
